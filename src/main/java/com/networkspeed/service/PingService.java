package com.networkspeed.service;

import com.networkspeed.model.PingResult;
import com.networkspeed.model.TestServer;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Service for measuring network latency and jitter using HTTP-based pings.
 */
public class PingService {

    private static final Logger LOG = LoggerFactory.getLogger(PingService.class);

    private final Vertx vertx;
    private final WebClient webClient;

    public PingService(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
    }

    /**
     * Perform a series of pings to a test server and return aggregated results.
     */
    public Uni<PingResult> ping(TestServer server, int count) {
        LOG.debug("Pinging {} ({}) {} times", server.getName(), server.getHost(), count);

        return Multi.createFrom().range(0, count)
                .onItem().transformToUniAndConcatenate(i -> singlePing(server))
                .collect().asList()
                .onItem().transform(latencies -> aggregateResults(server, latencies, count));
    }

    /**
     * Perform a single HTTP ping to the server and measure round-trip time.
     */
    private Uni<Double> singlePing(TestServer server) {
        long startTime = System.nanoTime();

        return webClient.get(server.getPort(), server.getHost(), "/ping")
                .timeout(5000)
                .send()
                .onItem().transform(response -> {
                    long endTime = System.nanoTime();
                    double latencyMs = (endTime - startTime) / 1_000_000.0;
                    LOG.trace("Ping to {}: {} ms", server.getHost(), latencyMs);
                    return latencyMs;
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.debug("Ping failed to {}: {}", server.getHost(), err.getMessage());
                    return -1.0; // Indicate failure
                });
    }

    /**
     * Aggregate individual ping results into a PingResult.
     */
    private PingResult aggregateResults(TestServer server, List<Double> allLatencies, int totalSent) {
        List<Double> successfulLatencies = new ArrayList<>();
        for (Double latency : allLatencies) {
            if (latency >= 0) {
                successfulLatencies.add(latency);
            }
        }

        PingResult result = new PingResult()
                .setServerId(server.getId())
                .setServerName(server.getName())
                .setPacketsSent(totalSent)
                .setPacketsReceived(successfulLatencies.size());

        if (successfulLatencies.isEmpty()) {
            result.setMinLatencyMs(0)
                    .setMaxLatencyMs(0)
                    .setAvgLatencyMs(0)
                    .setJitterMs(0)
                    .setPacketLossPercent(100.0)
                    .setLatencies(Collections.emptyList());
        } else {
            double min = successfulLatencies.stream().mapToDouble(Double::doubleValue).min().orElse(0);
            double max = successfulLatencies.stream().mapToDouble(Double::doubleValue).max().orElse(0);
            double avg = successfulLatencies.stream().mapToDouble(Double::doubleValue).average().orElse(0);
            double jitter = calculateJitter(successfulLatencies);
            double packetLoss = ((totalSent - successfulLatencies.size()) / (double) totalSent) * 100.0;

            result.setMinLatencyMs(min)
                    .setMaxLatencyMs(max)
                    .setAvgLatencyMs(avg)
                    .setJitterMs(jitter)
                    .setPacketLossPercent(packetLoss)
                    .setLatencies(successfulLatencies);
        }

        LOG.debug("Ping results for {}: avg={} ms, jitter={} ms, loss={}%",
                server.getName(), result.getAvgLatencyMs(),
                result.getJitterMs(), result.getPacketLossPercent());

        return result;
    }

    /**
     * Calculate jitter as the average absolute difference between consecutive latencies.
     */
    private double calculateJitter(List<Double> latencies) {
        if (latencies.size() < 2) {
            return 0.0;
        }

        double totalDiff = 0;
        for (int i = 1; i < latencies.size(); i++) {
            totalDiff += Math.abs(latencies.get(i) - latencies.get(i - 1));
        }
        return totalDiff / (latencies.size() - 1);
    }
}
