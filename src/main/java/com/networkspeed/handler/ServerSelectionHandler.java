package com.networkspeed.handler;

import com.networkspeed.model.TestServer;
import com.networkspeed.service.PingService;
import com.networkspeed.service.ServerDiscoveryService;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * HTTP handler for test server selection and management.
 */
public class ServerSelectionHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ServerSelectionHandler.class);

    private final ServerDiscoveryService serverDiscoveryService;
    private final PingService pingService;

    public ServerSelectionHandler(ServerDiscoveryService serverDiscoveryService, PingService pingService) {
        this.serverDiscoveryService = serverDiscoveryService;
        this.pingService = pingService;
    }

    /**
     * GET /api/servers - List all available test servers.
     */
    public void listServers(RoutingContext ctx) {
        String region = ctx.request().getParam("region");

        var serversFuture = (region != null && !region.isEmpty())
                ? serverDiscoveryService.getServersByRegion(region)
                : serverDiscoveryService.getServers();

        serversFuture.subscribe().with(
                servers -> {
                    JsonArray array = new JsonArray();
                    servers.forEach(s -> array.add(s.toJson()));
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(200)
                            .endAndForget(new JsonObject()
                                    .put("servers", array)
                                    .put("count", servers.size())
                                    .encode());
                },
                err -> {
                    LOG.error("Failed to list servers: {}", err.getMessage());
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(500)
                            .endAndForget(new JsonObject()
                                    .put("error", "Failed to retrieve servers")
                                    .encode());
                }
        );
    }

    /**
     * GET /api/servers/nearest - Get the nearest test server.
     */
    public void getNearestServer(RoutingContext ctx) {
        serverDiscoveryService.getNearestServer()
                .subscribe().with(
                        server -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(server.toJson().encode()),
                        err -> {
                            LOG.error("Failed to find nearest server: {}", err.getMessage());
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(500)
                                    .endAndForget(new JsonObject()
                                            .put("error", "No servers available")
                                            .encode());
                        }
                );
    }

    /**
     * GET /api/servers/:id - Get a specific server by ID.
     */
    public void getServer(RoutingContext ctx) {
        String serverId = ctx.pathParam("id");

        serverDiscoveryService.getServerById(serverId)
                .subscribe().with(
                        server -> {
                            if (server == null) {
                                ctx.response()
                                        .putHeader("Content-Type", "application/json")
                                        .setStatusCode(404)
                                        .endAndForget(new JsonObject()
                                                .put("error", "Server not found")
                                                .put("serverId", serverId)
                                                .encode());
                            } else {
                                ctx.response()
                                        .putHeader("Content-Type", "application/json")
                                        .setStatusCode(200)
                                        .endAndForget(server.toJson().encode());
                            }
                        },
                        err -> {
                            LOG.error("Failed to get server: {}", err.getMessage());
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(500)
                                    .endAndForget(new JsonObject()
                                            .put("error", err.getMessage())
                                            .encode());
                        }
                );
    }

    /**
     * GET /api/servers/:id/ping - Ping a specific server.
     */
    public void pingServer(RoutingContext ctx) {
        String serverId = ctx.pathParam("id");
        int count = 5;
        String countParam = ctx.request().getParam("count");
        if (countParam != null) {
            try {
                count = Integer.parseInt(countParam);
            } catch (NumberFormatException ignored) {
            }
        }

        int finalCount = count;
        serverDiscoveryService.getServerById(serverId)
                .onItem().ifNull().failWith(() ->
                        new IllegalArgumentException("Server not found: " + serverId))
                .onItem().transformToUni(server ->
                        pingService.ping(server, finalCount))
                .subscribe().with(
                        result -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(result.toJson().encode()),
                        err -> {
                            LOG.error("Ping failed: {}", err.getMessage());
                            int status = err instanceof IllegalArgumentException ? 404 : 500;
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
     * POST /api/servers/refresh - Refresh the server list.
     */
    public void refreshServers(RoutingContext ctx) {
        serverDiscoveryService.refresh()
                .subscribe().with(
                        v -> ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(200)
                                .endAndForget(new JsonObject()
                                        .put("message", "Server list refreshed")
                                        .encode()),
                        err -> {
                            LOG.error("Failed to refresh servers: {}", err.getMessage());
                            ctx.response()
                                    .putHeader("Content-Type", "application/json")
                                    .setStatusCode(500)
                                    .endAndForget(new JsonObject()
                                            .put("error", "Failed to refresh servers")
                                            .encode());
                        }
                );
    }
}
