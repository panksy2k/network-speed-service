package com.networkspeed;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.networkspeed.verticle.NetworkSpeedMainVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.jackson.DatabindCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main entry point for the Network Speed Service application.
 */
public class MainApplication {

    private static final Logger LOG = LoggerFactory.getLogger(MainApplication.class);

    public static void main(String[] args) {
        LOG.info("Initializing Network Speed Service...");

        // Configure Jackson ObjectMapper for Vert.x JSON handling
        configureJackson();

        // Create Vert.x instance with options
        VertxOptions options = new VertxOptions()
                .setPreferNativeTransport(true)
                .setWorkerPoolSize(20)
                .setEventLoopPoolSize(Runtime.getRuntime().availableProcessors() * 2);

        Vertx vertx = Vertx.vertx(options);

        // Deploy MainVerticle
        vertx.deployVerticle(new NetworkSpeedMainVerticle())
                .onSuccess(id -> {
                    LOG.info("Application started successfully. Deployment ID: {}", id);
                    LOG.info("Server is running at http://localhost:8090");
                    LOG.info("Health check: http://localhost:8090/health");
                    LOG.info("API info: http://localhost:8090/api");
                })
                .onFailure(err -> {
                    LOG.error("Failed to start application: {}", err.getMessage(), err);
                    vertx.close();
                    System.exit(1);
                });

        // Add shutdown hook for graceful shutdown
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOG.info("Shutting down application...");
            vertx.close()
                    .onSuccess(v -> LOG.info("Application shut down successfully"))
                    .onFailure(err -> LOG.error("Error during shutdown: {}", err.getMessage()));
        }));
    }

    private static void configureJackson() {
        ObjectMapper mapper = DatabindCodec.mapper();

        // Register JavaTimeModule for Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());

        // Configure serialization
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.disable(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);

        // Configure deserialization
        mapper.disable(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        LOG.info("Jackson ObjectMapper configured");
    }
}
