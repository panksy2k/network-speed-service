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
     * GET /api/network/info - Get comprehensive network information.
     */
    public void getNetworkInfo(RoutingContext ctx) {
        networkInfoService.getNetworkInfo()
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

    /**
     * GET /api/network/wifi - Get WiFi-specific details.
     */
    public void getWifiDetails(RoutingContext ctx) {
        networkInfoService.getWifiDetails()
                .subscribe().with(
                        info -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(info.toJson().encode()),
                        err -> {
                            LOG.error("Failed to get WiFi details: {}", err.getMessage());
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(500)
                                    .endAndForget(new JsonObject()
                                            .put("error", "Failed to retrieve WiFi details")
                                            .put("message", err.getMessage())
                                            .encode());
                        }
                );
    }
}
