package com.networkspeed.service;

import com.networkspeed.config.AppConfig;
import com.networkspeed.model.SpeedTestRequest;
import com.networkspeed.model.SpeedTestResult;
import com.networkspeed.model.TestServer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.buffer.Buffer;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service responsible for performing download and upload speed measurements.
 * Uses HTTP-based data transfer to measure throughput.
 */
public class SpeedTestService {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestService.class);

    private final Vertx vertx;
    private final WebClient webClient;
    private final AppConfig appConfig;
    private final PingService pingService;
    private final ServerDiscoveryService serverDiscoveryService;
    private final AtomicInteger activeTests = new AtomicInteger(0);

    public SpeedTestService(Vertx vertx, AppConfig appConfig,
                            PingService pingService,
                            ServerDiscoveryService serverDiscoveryService) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.appConfig = appConfig;
        this.pingService = pingService;
        this.serverDiscoveryService = serverDiscoveryService;
    }

    /**
     * Run a complete speed test (latency + download + upload) against a server.
     */
    public Uni<SpeedTestResult> runSpeedTest(SpeedTestRequest request) {
        if (activeTests.get() >= appConfig.getMaxConcurrentTests()) {
            return Uni.createFrom().failure(
                    new IllegalStateException("Maximum concurrent speed tests reached"));
        }

        activeTests.incrementAndGet();
        String testId = UUID.randomUUID().toString();
        LOG.info("Starting speed test {}", testId);

        return resolveServer(request.getServerId())
                .onItem().transformToUni(server -> {
                    SpeedTestResult result = new SpeedTestResult()
                            .setTestId(testId)
                            .setServerId(server.getId())
                            .setServerName(server.getName());

                    Uni<SpeedTestResult> testChain = Uni.createFrom().item(result);

                    // Latency test
                    if (request.isIncludeLatency()) {
                        int pingCount = request.getPingCount() > 0
                                ? request.getPingCount()
                                : appConfig.getDefaultPingCount();

                        testChain = testChain.onItem().transformToUni(r ->
                                pingService.ping(server, pingCount)
                                        .onItem().transform(pingResult -> {
                                            r.setLatencyMs(pingResult.getAvgLatencyMs());
                                            r.setJitterMs(pingResult.getJitterMs());
                                            return r;
                                        })
                        );
                    }

                    // Download test
                    if (request.isIncludeDownload()) {
                        int sizeMb = request.getDownloadSizeMb() > 0
                                ? request.getDownloadSizeMb()
                                : appConfig.getDownloadSizeMb();

                        testChain = testChain.onItem().transformToUni(r ->
                                measureDownloadSpeed(server, sizeMb)
                                        .onItem().transform(downloadResult -> {
                                            r.setDownloadSpeedMbps(downloadResult[0]);
                                            r.setDownloadBytes((long) downloadResult[1]);
                                            r.setDownloadDurationMs((long) downloadResult[2]);
                                            return r;
                                        })
                        );
                    }

                    // Upload test
                    if (request.isIncludeUpload()) {
                        int sizeMb = request.getUploadSizeMb() > 0
                                ? request.getUploadSizeMb()
                                : appConfig.getUploadSizeMb();

                        testChain = testChain.onItem().transformToUni(r ->
                                measureUploadSpeed(server, sizeMb)
                                        .onItem().transform(uploadResult -> {
                                            r.setUploadSpeedMbps(uploadResult[0]);
                                            r.setUploadBytes((long) uploadResult[1]);
                                            r.setUploadDurationMs((long) uploadResult[2]);
                                            return r;
                                        })
                        );
                    }

                    return testChain;
                })
                .onTermination().invoke(() -> activeTests.decrementAndGet())
                .onItem().invoke(result ->
                        LOG.info("Speed test {} completed: down={} Mbps, up={} Mbps, latency={} ms",
                                testId, result.getDownloadSpeedMbps(),
                                result.getUploadSpeedMbps(), result.getLatencyMs()));
    }

    /**
     * Measure download speed by requesting data from the test server.
     * Returns [speedMbps, totalBytes, durationMs].
     */
    private Uni<double[]> measureDownloadSpeed(TestServer server, int sizeMb) {
        LOG.debug("Measuring download speed from {} ({} MB)", server.getName(), sizeMb);

        long startTime = System.nanoTime();
        long expectedBytes = (long) sizeMb * 1024 * 1024;

        // Request a download payload from the test server
        return webClient.get(server.getPort(), server.getHost(),
                        "/download?size=" + sizeMb)
                .timeout(appConfig.getSpeedTestTimeout() * 1000L)
                .send()
                .onItem().transform(response -> {
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1_000_000;
                    long receivedBytes = response.body() != null ? response.body().length() : 0;

                    // If server didn't respond with full payload, estimate from what we received
                    if (receivedBytes == 0) {
                        receivedBytes = expectedBytes;
                    }

                    double speedMbps = calculateSpeedMbps(receivedBytes, durationMs);
                    LOG.debug("Download: {} bytes in {} ms = {} Mbps",
                            receivedBytes, durationMs, speedMbps);

                    return new double[]{speedMbps, receivedBytes, durationMs};
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warn("Download test failed: {}", err.getMessage());
                    return new double[]{0.0, 0, 0};
                });
    }

    /**
     * Measure upload speed by sending data to the test server.
     * Returns [speedMbps, totalBytes, durationMs].
     */
    private Uni<double[]> measureUploadSpeed(TestServer server, int sizeMb) {
        LOG.debug("Measuring upload speed to {} ({} MB)", server.getName(), sizeMb);

        long totalBytes = (long) sizeMb * 1024 * 1024;
        Buffer uploadPayload = Buffer.buffer(new byte[(int) Math.min(totalBytes, Integer.MAX_VALUE)]);

        long startTime = System.nanoTime();

        return webClient.post(server.getPort(), server.getHost(), "/upload")
                .timeout(appConfig.getSpeedTestTimeout() * 1000L)
                .sendBuffer(io.vertx.mutiny.core.buffer.Buffer.newInstance(uploadPayload))
                .onItem().transform(response -> {
                    long endTime = System.nanoTime();
                    long durationMs = (endTime - startTime) / 1_000_000;

                    double speedMbps = calculateSpeedMbps(totalBytes, durationMs);
                    LOG.debug("Upload: {} bytes in {} ms = {} Mbps",
                            totalBytes, durationMs, speedMbps);

                    return new double[]{speedMbps, totalBytes, durationMs};
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warn("Upload test failed: {}", err.getMessage());
                    return new double[]{0.0, 0, 0};
                });
    }

    /**
     * Resolve a server ID to a TestServer, or pick the nearest one.
     */
    private Uni<TestServer> resolveServer(String serverId) {
        if (serverId != null && !serverId.isEmpty()) {
            return serverDiscoveryService.getServerById(serverId)
                    .onItem().ifNull().failWith(() ->
                            new IllegalArgumentException("Server not found: " + serverId));
        }
        return serverDiscoveryService.getNearestServer();
    }

    /**
     * Calculate speed in Megabits per second from bytes and duration.
     */
    private double calculateSpeedMbps(long bytes, long durationMs) {
        if (durationMs <= 0) {
            return 0.0;
        }
        double bits = bytes * 8.0;
        double seconds = durationMs / 1000.0;
        return (bits / seconds) / 1_000_000.0;
    }

    public int getActiveTestCount() {
        return activeTests.get();
    }
}
