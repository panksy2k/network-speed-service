package com.networkspeed.verticle;

import com.networkspeed.config.AppConfig;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.config.ConfigRetriever;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main verticle that orchestrates the deployment of all other verticles.
 */
public class MainVerticle extends AbstractVerticle {

    private static final Logger LOG = LoggerFactory.getLogger(MainVerticle.class);

    private JsonObject config;
    private AppConfig appConfig;

    @Override
    public Uni<Void> asyncStart() {
        LOG.info("Starting Network Speed Service...");

        return loadConfig()
                .onItem()
                .invoke(cfg -> {
                    this.config = cfg;
                    this.appConfig = new AppConfig(cfg);
                })
                .log("Configuration loaded successfully")
                .onItem()
                .transformToUni(cfg -> deployVerticles())
                .onItem()
                .invoke(() -> LOG.info("Network Speed Service started successfully"))
                .replaceWithVoid();
    }

    private Uni<JsonObject> loadConfig() {
        ConfigStoreOptions fileStore = new ConfigStoreOptions()
                .setType("file")
                .setFormat("yaml")
                .setConfig(new JsonObject().put("path", "config.yaml"));

        ConfigStoreOptions envStore = new ConfigStoreOptions()
                .setType("env");

        ConfigStoreOptions sysStore = new ConfigStoreOptions()
                .setType("sys");

        ConfigRetrieverOptions options = new ConfigRetrieverOptions()
                .addStore(fileStore)
                .addStore(envStore)
                .addStore(sysStore);

        ConfigRetriever retriever = ConfigRetriever.create(vertx, options);

        return retriever.getConfig()
                .onFailure().recoverWithItem(e -> {
                    LOG.warn("Failed to load config file, using defaults: {}", e.getMessage());
                    return new JsonObject();
                });
    }

    private Uni<Void> deployVerticles() {
        LOG.info("Deploying verticles...");

        return deployHttpServerVerticle()
                .onItem()
                .invoke(id -> LOG.info("HttpServerVerticle deployed: {}", id))
                .replaceWithVoid();
    }

    private Uni<String> deployHttpServerVerticle() {
        DeploymentOptions options = new DeploymentOptions()
                .setConfig(config)
                .setInstances(appConfig.getHttpVerticleInstances());

        return vertx.deployVerticle(HttpServerVerticle::new, options);
    }

    @Override
    public Uni<Void> asyncStop() {
        LOG.info("Stopping Network Speed Service...");
        return Uni.createFrom().voidItem();
    }
}
