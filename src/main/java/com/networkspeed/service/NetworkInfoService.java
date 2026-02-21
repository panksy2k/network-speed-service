package com.networkspeed.service;

import com.networkspeed.model.NetworkInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Service for gathering network and WiFi information.
 * Retrieves local network interface details and external IP/ISP info.
 */
public class NetworkInfoService {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkInfoService.class);

    private final Vertx vertx;
    private final WebClient webClient;

    public NetworkInfoService(Vertx vertx) {
        this.vertx = vertx;
        this.webClient = WebClient.create(vertx);
    }

    /**
     * Gather network information for the given client IP.
     */
    public Uni<NetworkInfo> getNetworkInfo(String clientIp) {
        return getExternalInfo(clientIp);
    }

    /**
     * Get external IP and ISP details for the client using a public API.
     */
    private Uni<NetworkInfo> getExternalInfo(String clientIp) {
        String url;
        if (clientIp == null || clientIp.isEmpty() || clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1")) {
            // No client IP or localhost -> ask API for *our* (the caller's) public IP
            url = "/json";
        } else {
            url = "/json/" + clientIp;
        }

        LOG.info("Calling ip-api on URL {}", url);

        return webClient.get(80, "ip-api.com", url)
                .timeout(5000)
                .send()
                .onItem()
                .transform(response -> {
                    NetworkInfo info = new NetworkInfo();
                    if (response.statusCode() == 200 && response.bodyAsJsonObject() != null) {
                        var json = response.bodyAsJsonObject();
                        LOG.info("Response from ip-api.com - {}", json);

                        info.setExternalIp(json.getString("query"));
                        info.setIsp(json.getString("isp"));
                        info.setCity(json.getString("city"));
                        info.setRegionName(json.getString("regionName"));
                        info.setCountry(json.getString("country"));

                        // Since we can't know the client's local network type, we omit it or set to unknown
                        info.setNetworkType("unknown");
                    } else {
                        info.setNetworkType("unknown");
                        LOG.warn("Failed to get external IP info for {}: Status {}", clientIp, response.statusCode());
                    }
                    return info;
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warn("Failed to get external IP info: {}", err.getMessage());
                    return new NetworkInfo().setNetworkType("unknown");
                });
    }
}
