package com.networkspeed.service;

import com.networkspeed.config.AppConfig;
import com.networkspeed.model.TestServer;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Service for discovering and managing test servers.
 * Maintains a list of available servers sorted by proximity.
 */
public class ServerDiscoveryService {

    private static final Logger LOG = LoggerFactory.getLogger(ServerDiscoveryService.class);

    private final Vertx vertx;
    private final WebClient webClient;
    private final AppConfig appConfig;
    private final List<TestServer> servers = new CopyOnWriteArrayList<>();
    private double clientLatitude;
    private double clientLongitude;

    public ServerDiscoveryService(Vertx vertx, AppConfig appConfig) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
        this.appConfig = appConfig;
    }

    /**
     * Initialize the server list with built-in default servers.
     * In production, these would be fetched from a remote API or database.
     */
    public Uni<Void> initialize() {
        LOG.info("Initializing server discovery...");
        return loadDefaultServers()
                .onItem().transformToUni(v -> detectClientLocation())
                .onItem().invoke(v -> {
                    sortServersByDistance();
                    LOG.info("Server discovery initialized with {} servers", servers.size());
                })
                .replaceWithVoid();
    }

    /**
     * Get all available test servers sorted by distance.
     */
    public Uni<List<TestServer>> getServers() {
        if (servers.isEmpty()) {
            return initialize().onItem().transform(v -> new ArrayList<>(servers));
        }
        return Uni.createFrom().item(new ArrayList<>(servers));
    }

    /**
     * Get a specific server by ID.
     */
    public Uni<TestServer> getServerById(String serverId) {
        return Uni.createFrom().item(
                servers.stream()
                        .filter(s -> s.getId().equals(serverId))
                        .findFirst()
                        .orElse(null)
        );
    }

    /**
     * Get the nearest server based on client geo-location.
     */
    public Uni<TestServer> getNearestServer() {
        return getServers()
                .onItem().transform(serverList -> {
                    if (serverList.isEmpty()) {
                        throw new IllegalStateException("No test servers available");
                    }
                    return serverList.get(0); // Already sorted by distance
                });
    }

    /**
     * Get servers filtered by region/country.
     */
    public Uni<List<TestServer>> getServersByRegion(String region) {
        return getServers()
                .onItem().transform(serverList ->
                        serverList.stream()
                                .filter(s -> region.equalsIgnoreCase(s.getRegion())
                                        || region.equalsIgnoreCase(s.getCountry()))
                                .toList()
                );
    }

    /**
     * Load default/built-in test servers.
     */
    private Uni<Void> loadDefaultServers() {
        // Default servers - in production, these would come from an API
        JsonArray defaultServers = new JsonArray()
                .add(new JsonObject()
                        .put("id", "srv-us-east-1")
                        .put("name", "US East (Virginia)")
                        .put("host", "speedtest-us-east.example.com")
                        .put("port", 80)
                        .put("country", "US")
                        .put("city", "Ashburn")
                        .put("region", "us-east")
                        .put("latitude", 39.0438)
                        .put("longitude", -77.4874)
                        .put("sponsor", "Example Networks"))
                .add(new JsonObject()
                        .put("id", "srv-us-west-1")
                        .put("name", "US West (Oregon)")
                        .put("host", "speedtest-us-west.example.com")
                        .put("port", 80)
                        .put("country", "US")
                        .put("city", "Portland")
                        .put("region", "us-west")
                        .put("latitude", 45.5155)
                        .put("longitude", -122.6789)
                        .put("sponsor", "Example Networks"))
                .add(new JsonObject()
                        .put("id", "srv-eu-west-1")
                        .put("name", "EU West (London)")
                        .put("host", "speedtest-eu-west.example.com")
                        .put("port", 80)
                        .put("country", "UK")
                        .put("city", "London")
                        .put("region", "eu-west")
                        .put("latitude", 51.5074)
                        .put("longitude", -0.1278)
                        .put("sponsor", "Example Networks"))
                .add(new JsonObject()
                        .put("id", "srv-ap-south-1")
                        .put("name", "Asia Pacific (Mumbai)")
                        .put("host", "speedtest-ap-south.example.com")
                        .put("port", 80)
                        .put("country", "IN")
                        .put("city", "Mumbai")
                        .put("region", "ap-south")
                        .put("latitude", 19.0760)
                        .put("longitude", 72.8777)
                        .put("sponsor", "Example Networks"))
                .add(new JsonObject()
                        .put("id", "srv-ap-east-1")
                        .put("name", "Asia Pacific (Singapore)")
                        .put("host", "speedtest-ap-east.example.com")
                        .put("port", 80)
                        .put("country", "SG")
                        .put("city", "Singapore")
                        .put("region", "ap-east")
                        .put("latitude", 1.3521)
                        .put("longitude", 103.8198)
                        .put("sponsor", "Example Networks"));

        servers.clear();
        for (int i = 0; i < defaultServers.size(); i++) {
            servers.add(new TestServer(defaultServers.getJsonObject(i)));
        }

        return Uni.createFrom().voidItem();
    }

    /**
     * Detect client location using a geolocation API.
     */
    private Uni<Void> detectClientLocation() {
        return webClient.get(80, "ip-api.com", "/json")
                .timeout(5000)
                .send()
                .onItem().invoke(response -> {
                    if (response.statusCode() == 200 && response.bodyAsJsonObject() != null) {
                        var json = response.bodyAsJsonObject();
                        clientLatitude = json.getDouble("lat", 0.0);
                        clientLongitude = json.getDouble("lon", 0.0);
                        LOG.info("Client location detected: lat={}, lon={}",
                                clientLatitude, clientLongitude);
                    }
                })
                .onFailure().invoke(err ->
                        LOG.warn("Failed to detect client location: {}", err.getMessage()))
                .replaceWithVoid()
                .onFailure().recoverWithItem((Void) null);
    }

    /**
     * Sort servers by distance from client location.
     */
    private void sortServersByDistance() {
        for (TestServer server : servers) {
            double distance = haversineDistance(
                    clientLatitude, clientLongitude,
                    server.getLatitude(), server.getLongitude());
            server.setDistanceKm(distance);
        }
        servers.sort(Comparator.comparingDouble(TestServer::getDistanceKm));
    }

    /**
     * Calculate distance between two coordinates using the Haversine formula.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth's radius in km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    /**
     * Refresh the server list (can be called periodically).
     */
    public Uni<Void> refresh() {
        LOG.info("Refreshing server list...");
        return initialize();
    }
}
