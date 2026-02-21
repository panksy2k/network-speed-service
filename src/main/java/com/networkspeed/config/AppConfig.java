package com.networkspeed.config;

import io.vertx.core.json.JsonObject;

/**
 * Application configuration holder.
 * Provides type-safe access to configuration properties.
 */
public class AppConfig {

    private final JsonObject config;

    public AppConfig(JsonObject config) {
        this.config = config;
    }

    // Server Configuration
    public int getHttpPort() {
        return config.getJsonObject("server", new JsonObject()).getInteger("port", 8080);
    }

    public String getHttpHost() {
        return config.getJsonObject("server", new JsonObject()).getString("host", "0.0.0.0");
    }

    // Speed Test Configuration
    public int getDownloadSizeMb() {
        return config.getJsonObject("speedtest", new JsonObject()).getInteger("downloadSizeMb", 10);
    }

    // Verticle Instances Configuration
    public int getHttpVerticleInstances() {
        return config.getJsonObject("verticles", new JsonObject()).getInteger("http", 1);
    }

    public int getSpeedTestVerticleInstances() {
        return config.getJsonObject("verticles", new JsonObject()).getInteger("speedTest", 2);
    }

    public int getNetworkInfoVerticleInstances() {
        return config.getJsonObject("verticles", new JsonObject()).getInteger("networkInfo", 1);
    }

    // Get raw config
    public JsonObject getRawConfig() {
        return config;
    }
}
