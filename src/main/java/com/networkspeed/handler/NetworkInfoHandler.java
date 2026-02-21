package com.networkspeed.handler;

import com.networkspeed.service.NetworkInfoService;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP handler for network information endpoints.
 */
public class NetworkInfoHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkInfoHandler.class);

    private final NetworkInfoService networkInfoService;

    public NetworkInfoHandler(NetworkInfoService networkInfoService) {
        this.networkInfoService = networkInfoService;
    }

    /**
     * GET /api/network/info - Get network information for the client.
     */
    public void getNetworkInfo(RoutingContext ctx) {
        String clientIp = getClientIp(ctx);
        LOG.info("Detected Client IP: {}", clientIp);

        networkInfoService.getNetworkInfo(clientIp)
                .subscribe().with(
                        info -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(info.toJson().encode()),
                        err -> {
                            LOG.error("Failed to get network info: {}", err.getMessage());
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(500)
                                    .endAndForget(new JsonObject()
                                            .put("error", "Failed to retrieve network info")
                                            .put("message", err.getMessage())
                                            .encode());
                        }
                );
    }

    private String getClientIp(RoutingContext ctx) {
        // Try common headers for proxy IP forwarding
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "CF-Connecting-IP",
            "True-Client-IP"
        };

        for (String header : headers) {
            String ip = ctx.request().getHeader(header);
            if (ip != null && !ip.isEmpty()) {
                // Handle multiple IPs (first one is client)
                if (ip.contains(",")) {
                    return ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        // Fallback to direct connection IP
        String remoteIp = ctx.request().remoteAddress().host();

        // If localhost or loopback, return null to signal "use server's public IP"
        if ("127.0.0.1".equals(remoteIp) || "0:0:0:0:0:0:0:1".equals(remoteIp) || "localhost".equals(remoteIp)) {
            return null;
        }

        return remoteIp;
    }
}
