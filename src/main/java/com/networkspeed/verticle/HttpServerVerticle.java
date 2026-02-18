package com.networkspeed.verticle;

import com.networkspeed.config.AppConfig;
import com.networkspeed.handler.ErrorHandler;
import com.networkspeed.handler.NetworkInfoHandler;
import com.networkspeed.handler.ServerSelectionHandler;
import com.networkspeed.handler.SpeedTestHandler;
import com.networkspeed.service.NetworkInfoService;
import com.networkspeed.service.PingService;
import com.networkspeed.service.ServerDiscoveryService;
import com.networkspeed.service.SpeedTestService;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import io.vertx.mutiny.ext.web.handler.CorsHandler;
import io.vertx.mutiny.ext.web.handler.LoggerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

/**
 * Verticle responsible for HTTP server and REST API routing.
 */
public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(HttpServerVerticle.class);

    private AppConfig appConfig;
    private SpeedTestHandler speedTestHandler;
    private NetworkInfoHandler networkInfoHandler;
    private ServerSelectionHandler serverSelectionHandler;
    private ErrorHandler errorHandler;

    @Override
    public Uni<Void> asyncStart() {
        LOG.info("Starting HttpServerVerticle...");

        appConfig = new AppConfig(config());

        // Initialize services
        PingService pingService = new PingService(vertx);
        ServerDiscoveryService serverDiscoveryService = new ServerDiscoveryService(vertx, appConfig);
        SpeedTestService speedTestService = new SpeedTestService(vertx, appConfig, pingService, serverDiscoveryService);
        NetworkInfoService networkInfoService = new NetworkInfoService(vertx);

        // Initialize handlers
        speedTestHandler = new SpeedTestHandler(speedTestService);
        networkInfoHandler = new NetworkInfoHandler(networkInfoService);
        serverSelectionHandler = new ServerSelectionHandler(serverDiscoveryService, pingService);
        errorHandler = new ErrorHandler();

        // Create router
        Router router = createRouter();

        // Initialize server discovery, then start HTTP server
        return serverDiscoveryService.initialize()
                .onItem().transformToUni(v ->
                        vertx.createHttpServer()
                                .requestHandler(router)
                                .listen(appConfig.getHttpPort(), appConfig.getHttpHost())
                )
                .onItem().invoke(server ->
                        LOG.info("HTTP server started on {}:{}", appConfig.getHttpHost(), server.actualPort()))
                .replaceWithVoid();
    }

    private Router createRouter() {
        Router router = Router.router(vertx);

        // Add common handlers
        router.route().handler(LoggerHandler.create());
        router.route().handler(BodyHandler.create());
        router.route().handler(createCorsHandler());

        // Health check endpoint
        router.get("/health").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .endAndForget(new JsonObject()
                            .put("status", "UP")
                            .put("service", "network-speed-service")
                            .encode());
        });

        // API version info
        router.get("/api").handler(ctx -> {
            ctx.response()
                    .putHeader("Content-Type", "application/json")
                    .setStatusCode(200)
                    .endAndForget(new JsonObject()
                            .put("name", "Network Speed Service API")
                            .put("version", "1.0.0")
                            .put("description", "Reactive network speed test service using Vert.x and SmallRye Mutiny")
                            .encode());
        });

        // Speed test endpoints
        router.post("/api/speedtest").handler(speedTestHandler::runSpeedTest);
        router.get("/api/speedtest/download").handler(speedTestHandler::runDownloadTest);
        router.get("/api/speedtest/upload").handler(speedTestHandler::runUploadTest);
        router.get("/api/speedtest/status").handler(speedTestHandler::getStatus);

        // Network info endpoints
        router.get("/api/network/info").handler(networkInfoHandler::getNetworkInfo);
        router.get("/api/network/wifi").handler(networkInfoHandler::getWifiDetails);

        // Server selection endpoints
        router.get("/api/servers").handler(serverSelectionHandler::listServers);
        router.get("/api/servers/nearest").handler(serverSelectionHandler::getNearestServer);
        router.get("/api/servers/:id").handler(serverSelectionHandler::getServer);
        router.get("/api/servers/:id/ping").handler(serverSelectionHandler::pingServer);
        router.post("/api/servers/refresh").handler(serverSelectionHandler::refreshServers);

        // 404 handler for unmatched routes
        router.route().last().handler(errorHandler::handleNotFound);

        // Global error handler
        router.route().failureHandler(errorHandler::handle);

        return router;
    }

    private CorsHandler createCorsHandler() {
        Set<String> allowedHeaders = new HashSet<>();
        allowedHeaders.add("x-requested-with");
        allowedHeaders.add("Access-Control-Allow-Origin");
        allowedHeaders.add("origin");
        allowedHeaders.add("Content-Type");
        allowedHeaders.add("accept");
        allowedHeaders.add("Authorization");

        Set<io.vertx.core.http.HttpMethod> allowedMethods = new HashSet<>();
        allowedMethods.add(io.vertx.core.http.HttpMethod.GET);
        allowedMethods.add(io.vertx.core.http.HttpMethod.POST);
        allowedMethods.add(io.vertx.core.http.HttpMethod.PUT);
        allowedMethods.add(io.vertx.core.http.HttpMethod.DELETE);
        allowedMethods.add(io.vertx.core.http.HttpMethod.OPTIONS);

        return CorsHandler.create()
                .addOrigin("*")
                .allowedHeaders(allowedHeaders)
                .allowedMethods(allowedMethods);
    }

    @Override
    public Uni<Void> asyncStop() {
        LOG.info("Stopping HttpServerVerticle...");
        return Uni.createFrom().voidItem();
    }
}
