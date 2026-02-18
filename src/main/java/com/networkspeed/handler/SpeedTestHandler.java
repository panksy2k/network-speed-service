package com.networkspeed.handler;

import com.networkspeed.model.SpeedTestRequest;
import com.networkspeed.service.SpeedTestService;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP handler for speed test endpoints.
 */
public class SpeedTestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestHandler.class);

    private final SpeedTestService speedTestService;

    public SpeedTestHandler(SpeedTestService speedTestService) {
        this.speedTestService = speedTestService;
    }

    /**
     * POST /api/speedtest - Run a complete speed test.
     */
    public void runSpeedTest(RoutingContext ctx) {
        SpeedTestRequest request;
        try {
            request = SpeedTestRequest.fromJson(ctx.body().asJsonObject());
        } catch (Exception e) {
            request = new SpeedTestRequest();
        }

        speedTestService.runSpeedTest(request)
                .subscribe().with(
                        result -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(result.toJson().encode()),
                        err -> {
                            LOG.error("Speed test failed: {}", err.getMessage());
                            int status = err instanceof IllegalStateException ? 429 : 500;
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(status)
                                    .endAndForget(new JsonObject()
                                            .put("error", err.getMessage())
                                            .encode());
                        }
                );
    }

    /**
     * GET /api/speedtest/download - Run only download speed test.
     */
    public void runDownloadTest(RoutingContext ctx) {
        String serverId = ctx.request().getParam("serverId");
        int sizeMb = parseIntParam(ctx, "sizeMb", 0);

        SpeedTestRequest request = new SpeedTestRequest()
                .setServerId(serverId)
                .setIncludeDownload(true)
                .setIncludeUpload(false)
                .setIncludeLatency(false)
                .setDownloadSizeMb(sizeMb);

        speedTestService.runSpeedTest(request)
                .subscribe().with(
                        result -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(result.toJson().encode()),
                        err -> handleError(ctx, err)
                );
    }

    /**
     * GET /api/speedtest/upload - Run only upload speed test.
     */
    public void runUploadTest(RoutingContext ctx) {
        String serverId = ctx.request().getParam("serverId");
        int sizeMb = parseIntParam(ctx, "sizeMb", 0);

        SpeedTestRequest request = new SpeedTestRequest()
                .setServerId(serverId)
                .setIncludeDownload(false)
                .setIncludeUpload(true)
                .setIncludeLatency(false)
                .setUploadSizeMb(sizeMb);

        speedTestService.runSpeedTest(request)
                .subscribe().with(
                        result -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(result.toJson().encode()),
                        err -> handleError(ctx, err)
                );
    }

    /**
     * GET /api/speedtest/status - Get current speed test status.
     */
    public void getStatus(RoutingContext ctx) {
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .endAndForget(new JsonObject()
                        .put("activeTests", speedTestService.getActiveTestCount())
                        .put("status", "ready")
                        .encode());
    }

    private int parseIntParam(RoutingContext ctx, String name, int defaultValue) {
        String value = ctx.request().getParam(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException ignored) {
            }
        }
        return defaultValue;
    }

    private void handleError(RoutingContext ctx, Throwable err) {
        LOG.error("Request failed: {}", err.getMessage());
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(500)
                .endAndForget(new JsonObject()
                        .put("error", err.getMessage())
                        .encode());
    }
}
