package com.networkspeed.model;

import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * Represents the result of a complete speed test.
 */
public class SpeedTestResult {

    private String testId;
    private double downloadSpeedMbps;
    private double uploadSpeedMbps;
    private double latencyMs;
    private double jitterMs;
    private String serverName;
    private String serverId;
    private String clientIp;
    private Instant timestamp;
    private long downloadBytes;
    private long uploadBytes;
    private long downloadDurationMs;
    private long uploadDurationMs;

    public SpeedTestResult() {
        this.timestamp = Instant.now();
    }

    public String getTestId() {
        return testId;
    }

    public SpeedTestResult setTestId(String testId) {
        this.testId = testId;
        return this;
    }

    public double getDownloadSpeedMbps() {
        return downloadSpeedMbps;
    }

    public SpeedTestResult setDownloadSpeedMbps(double downloadSpeedMbps) {
        this.downloadSpeedMbps = downloadSpeedMbps;
        return this;
    }

    public double getUploadSpeedMbps() {
        return uploadSpeedMbps;
    }

    public SpeedTestResult setUploadSpeedMbps(double uploadSpeedMbps) {
        this.uploadSpeedMbps = uploadSpeedMbps;
        return this;
    }

    public double getLatencyMs() {
        return latencyMs;
    }

    public SpeedTestResult setLatencyMs(double latencyMs) {
        this.latencyMs = latencyMs;
        return this;
    }

    public double getJitterMs() {
        return jitterMs;
    }

    public SpeedTestResult setJitterMs(double jitterMs) {
        this.jitterMs = jitterMs;
        return this;
    }

    public String getServerName() {
        return serverName;
    }

    public SpeedTestResult setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public String getServerId() {
        return serverId;
    }

    public SpeedTestResult setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public String getClientIp() {
        return clientIp;
    }

    public SpeedTestResult setClientIp(String clientIp) {
        this.clientIp = clientIp;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public SpeedTestResult setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public long getDownloadBytes() {
        return downloadBytes;
    }

    public SpeedTestResult setDownloadBytes(long downloadBytes) {
        this.downloadBytes = downloadBytes;
        return this;
    }

    public long getUploadBytes() {
        return uploadBytes;
    }

    public SpeedTestResult setUploadBytes(long uploadBytes) {
        this.uploadBytes = uploadBytes;
        return this;
    }

    public long getDownloadDurationMs() {
        return downloadDurationMs;
    }

    public SpeedTestResult setDownloadDurationMs(long downloadDurationMs) {
        this.downloadDurationMs = downloadDurationMs;
        return this;
    }

    public long getUploadDurationMs() {
        return uploadDurationMs;
    }

    public SpeedTestResult setUploadDurationMs(long uploadDurationMs) {
        this.uploadDurationMs = uploadDurationMs;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("testId", testId)
                .put("downloadSpeedMbps", Math.round(downloadSpeedMbps * 100.0) / 100.0)
                .put("uploadSpeedMbps", Math.round(uploadSpeedMbps * 100.0) / 100.0)
                .put("latencyMs", Math.round(latencyMs * 100.0) / 100.0)
                .put("jitterMs", Math.round(jitterMs * 100.0) / 100.0)
                .put("serverName", serverName)
                .put("serverId", serverId)
                .put("clientIp", clientIp)
                .put("timestamp", timestamp != null ? timestamp.toString() : null)
                .put("downloadBytes", downloadBytes)
                .put("uploadBytes", uploadBytes)
                .put("downloadDurationMs", downloadDurationMs)
                .put("uploadDurationMs", uploadDurationMs);
    }
}
