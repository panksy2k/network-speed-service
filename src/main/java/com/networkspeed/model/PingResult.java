package com.networkspeed.model;

import io.vertx.core.json.JsonObject;

import java.time.Instant;
import java.util.List;

/**
 * Represents the result of a ping/latency measurement.
 */
public class PingResult {

    private String serverId;
    private String serverName;
    private double minLatencyMs;
    private double maxLatencyMs;
    private double avgLatencyMs;
    private double jitterMs;
    private int packetsSent;
    private int packetsReceived;
    private double packetLossPercent;
    private List<Double> latencies;
    private Instant timestamp;

    public PingResult() {
        this.timestamp = Instant.now();
    }

    public String getServerId() {
        return serverId;
    }

    public PingResult setServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public String getServerName() {
        return serverName;
    }

    public PingResult setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public double getMinLatencyMs() {
        return minLatencyMs;
    }

    public PingResult setMinLatencyMs(double minLatencyMs) {
        this.minLatencyMs = minLatencyMs;
        return this;
    }

    public double getMaxLatencyMs() {
        return maxLatencyMs;
    }

    public PingResult setMaxLatencyMs(double maxLatencyMs) {
        this.maxLatencyMs = maxLatencyMs;
        return this;
    }

    public double getAvgLatencyMs() {
        return avgLatencyMs;
    }

    public PingResult setAvgLatencyMs(double avgLatencyMs) {
        this.avgLatencyMs = avgLatencyMs;
        return this;
    }

    public double getJitterMs() {
        return jitterMs;
    }

    public PingResult setJitterMs(double jitterMs) {
        this.jitterMs = jitterMs;
        return this;
    }

    public int getPacketsSent() {
        return packetsSent;
    }

    public PingResult setPacketsSent(int packetsSent) {
        this.packetsSent = packetsSent;
        return this;
    }

    public int getPacketsReceived() {
        return packetsReceived;
    }

    public PingResult setPacketsReceived(int packetsReceived) {
        this.packetsReceived = packetsReceived;
        return this;
    }

    public double getPacketLossPercent() {
        return packetLossPercent;
    }

    public PingResult setPacketLossPercent(double packetLossPercent) {
        this.packetLossPercent = packetLossPercent;
        return this;
    }

    public List<Double> getLatencies() {
        return latencies;
    }

    public PingResult setLatencies(List<Double> latencies) {
        this.latencies = latencies;
        return this;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public PingResult setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("serverId", serverId)
                .put("serverName", serverName)
                .put("minLatencyMs", Math.round(minLatencyMs * 100.0) / 100.0)
                .put("maxLatencyMs", Math.round(maxLatencyMs * 100.0) / 100.0)
                .put("avgLatencyMs", Math.round(avgLatencyMs * 100.0) / 100.0)
                .put("jitterMs", Math.round(jitterMs * 100.0) / 100.0)
                .put("packetsSent", packetsSent)
                .put("packetsReceived", packetsReceived)
                .put("packetLossPercent", Math.round(packetLossPercent * 100.0) / 100.0)
                .put("latencies", latencies)
                .put("timestamp", timestamp != null ? timestamp.toString() : null);
    }
}
