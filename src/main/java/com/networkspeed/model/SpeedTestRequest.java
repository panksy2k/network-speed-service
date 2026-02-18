package com.networkspeed.model;

import io.vertx.core.json.JsonObject;

/**
 * Represents a request to initiate a speed test.
 */
public class SpeedTestRequest {

    private String serverId;
    private boolean includeDownload;
    private boolean includeUpload;
    private boolean includeLatency;
    private int downloadSizeMb;
    private int uploadSizeMb;
    private int pingCount;

    public SpeedTestRequest() {
        this.includeDownload = true;
        this.includeUpload = true;
        this.includeLatency = true;
    }

    public static SpeedTestRequest fromJson(JsonObject json) {
        SpeedTestRequest request = new SpeedTestRequest();
        if (json == null) {
            return request;
        }
        request.serverId = json.getString("serverId");
        request.includeDownload = json.getBoolean("includeDownload", true);
        request.includeUpload = json.getBoolean("includeUpload", true);
        request.includeLatency = json.getBoolean("includeLatency", true);
        request.downloadSizeMb = json.getInteger("downloadSizeMb", 0);
        request.uploadSizeMb = json.getInteger("uploadSizeMb", 0);
        request.pingCount = json.getInteger("pingCount", 0);
        return request;
    }

    public String getServerId() {
        return serverId;
    }

    public SpeedTestRequest setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public boolean isIncludeDownload() {
        return includeDownload;
    }

    public SpeedTestRequest setIncludeDownload(boolean includeDownload) {
        this.includeDownload = includeDownload;
        return this;
    }

    public boolean isIncludeUpload() {
        return includeUpload;
    }

    public SpeedTestRequest setIncludeUpload(boolean includeUpload) {
        this.includeUpload = includeUpload;
        return this;
    }

    public boolean isIncludeLatency() {
        return includeLatency;
    }

    public SpeedTestRequest setIncludeLatency(boolean includeLatency) {
        this.includeLatency = includeLatency;
        return this;
    }

    public int getDownloadSizeMb() {
        return downloadSizeMb;
    }

    public SpeedTestRequest setDownloadSizeMb(int downloadSizeMb) {
        this.downloadSizeMb = downloadSizeMb;
        return this;
    }

    public int getUploadSizeMb() {
        return uploadSizeMb;
    }

    public SpeedTestRequest setUploadSizeMb(int uploadSizeMb) {
        this.uploadSizeMb = uploadSizeMb;
        return this;
    }

    public int getPingCount() {
        return pingCount;
    }

    public SpeedTestRequest setPingCount(int pingCount) {
        this.pingCount = pingCount;
        return this;
    }
}
