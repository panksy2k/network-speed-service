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

    public int getUploadSizeMb() {
        return config.getJsonObject("speedtest", new JsonObject()).getInteger("uploadSizeMb", 5);
    }

    public int getSpeedTestTimeout() {
        return config.getJsonObject("speedtest", new JsonObject()).getInteger("timeoutSeconds", 30);
    }

    public int getDefaultPingCount() {
        return config.getJsonObject("speedtest", new JsonObject()).getInteger("defaultPingCount", 5);
    }

    public int getMaxConcurrentTests() {
        return config.getJsonObject("speedtest", new JsonObject()).getInteger("maxConcurrentTests", 10);
    }

    // Server Discovery Configuration
    public int getServerRefreshInterval() {
        return config.getJsonObject("serverDiscovery", new JsonObject()).getInteger("refreshIntervalMinutes", 60);
    }

    public int getMaxServers() {
        return config.getJsonObject("serverDiscovery", new JsonObject()).getInteger("maxServers", 50);
    }

    public String getDefaultRegion() {
        return config.getJsonObject("serverDiscovery", new JsonObject()).getString("defaultRegion", "auto");
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
