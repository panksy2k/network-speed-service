package com.networkspeed.model;

import io.vertx.core.json.JsonObject;

/**
 * Represents a test server that can be used for speed/latency measurements.
 */
public class TestServer {

    private String id;
    private String name;
    private String host;
    private int port;
    private String country;
    private String city;
    private String region;
    private double latitude;
    private double longitude;
    private String sponsor;
    private double distanceKm;
    private boolean available;

    public TestServer() {
    }

    public TestServer(JsonObject json) {
        this.id = json.getString("id");
        this.name = json.getString("name");
        this.host = json.getString("host");
        this.port = json.getInteger("port", 80);
        this.country = json.getString("country");
        this.city = json.getString("city");
        this.region = json.getString("region");
        this.latitude = json.getDouble("latitude", 0.0);
        this.longitude = json.getDouble("longitude", 0.0);
        this.sponsor = json.getString("sponsor");
        this.distanceKm = json.getDouble("distanceKm", 0.0);
        this.available = json.getBoolean("available", true);
    }

    public String getId() {
        return id;
    }

    public TestServer setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public TestServer setName(String name) {
        this.name = name;
        return this;
    }

    public String getHost() {
        return host;
    }

    public TestServer setHost(String host) {
        this.host = host;
        return this;
    }

    public int getPort() {
        return port;
    }

    public TestServer setPort(int port) {
        this.port = port;
        return this;
    }

    public String getCountry() {
        return country;
    }

    public TestServer setCountry(String country) {
        this.country = country;
        return this;
    }

    public String getCity() {
        return city;
    }

    public TestServer setCity(String city) {
        this.city = city;
        return this;
    }

    public String getRegion() {
        return region;
    }

    public TestServer setRegion(String region) {
        this.region = region;
        return this;
    }

    public double getLatitude() {
        return latitude;
    }

    public TestServer setLatitude(double latitude) {
        this.latitude = latitude;
        return this;
    }

    public double getLongitude() {
        return longitude;
    }

    public TestServer setLongitude(double longitude) {
        this.longitude = longitude;
        return this;
    }

    public String getSponsor() {
        return sponsor;
    }

    public TestServer setSponsor(String sponsor) {
        this.sponsor = sponsor;
        return this;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public TestServer setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
        return this;
    }

    public boolean isAvailable() {
        return available;
    }

    public TestServer setAvailable(boolean available) {
        this.available = available;
        return this;
    }

    public JsonObject toJson() {
        return new JsonObject()
                .put("id", id)
                .put("name", name)
                .put("host", host)
                .put("port", port)
                .put("country", country)
                .put("city", city)
                .put("region", region)
                .put("latitude", latitude)
                .put("longitude", longitude)
                .put("sponsor", sponsor)
                .put("distanceKm", Math.round(distanceKm * 100.0) / 100.0)
                .put("available", available);
    }
}
