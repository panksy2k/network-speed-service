package com.networkspeed.handler;

import com.networkspeed.service.SpeedTestService;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

/**
 * HTTP handler for speed test endpoints.
 * Acts as a target for client-side speed tests.
 */
public class SpeedTestHandler {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestHandler.class);

    private final SpeedTestService speedTestService;

    public SpeedTestHandler(SpeedTestService speedTestService) {
        this.speedTestService = speedTestService;
    }

    /**
     * GET /api/speedtest/download - Stream data to client for download test.
     */
    public void runDownloadTest(RoutingContext ctx) {
        int sizeMb = parseIntParam(ctx, "sizeMb", 10);

        try {
            Buffer data = speedTestService.generateDownloadData(sizeMb);

            ctx.response()
                    .putHeader("Content-Type", "application/octet-stream")
                    .putHeader("Content-Length", String.valueOf(data.length()))
                    // Disable caching so every test is real
                    .putHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                    .putHeader("Pragma", "no-cache")
                    .setStatusCode(200)
                    .endAndForget(io.vertx.mutiny.core.buffer.Buffer.newInstance(data));
        } catch (Exception e) {
            handleError(ctx, e);
        }
    }

    /**
     * POST /api/speedtest/upload - Receive data from client for upload test.
     */
    public void runUploadTest(RoutingContext ctx) {
        String clientTimeStr = ctx.request().getHeader("X-Client-Timestamp");
        long clientTimeMs = 0;
        if (clientTimeStr != null) {
            try {
                clientTimeMs = Long.parseLong(clientTimeStr);
            } catch (NumberFormatException e) {
                LOG.warn("Invalid X-Client-Timestamp: {}", clientTimeStr);
            }
        }

        io.vertx.mutiny.core.buffer.Buffer body = ctx.body().buffer();
        if(body == null) {
          ctx.response()
            .putHeader("Content-Type", "application/json")
            .setStatusCode(400).end();
        }

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .endAndForget();
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
