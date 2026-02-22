package com.networkspeed.service;

import com.networkspeed.model.NetworkInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;


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
        // SSL enabled so we can call HTTPS endpoints (e.g. ipinfo.io)
        WebClientOptions options = new WebClientOptions().setSsl(true).setTrustAll(true);
        this.webClient = WebClient.create(vertx, options);
    }

    /**
     * Gather network information for the given client IP.
     */
    public Uni<NetworkInfo> getNetworkInfo(String clientIp) {
        return getExternalInfo(clientIp);
    }

    /**
     * Get external IP and ISP details for the client using ipinfo.io.
     * ipinfo.io has more accurate geolocation data than ip-api.com, particularly
     * for UK-hosted VPS providers where ip-api.com can return wrong countries.
     */
    private Uni<NetworkInfo> getExternalInfo(String clientIp) {
        // ipinfo.io: /json for self-lookup, /{ip}/json for specific IP
        String path;
        if (clientIp == null || clientIp.isEmpty() || clientIp.equals("127.0.0.1") || clientIp.equals("0:0:0:0:0:0:0:1")) {
            path = "/json";
        } else {
            path = "/" + clientIp + "/json";
        }

        LOG.info("Calling ipinfo.io on path {}", path);

        return webClient.get(443, "ipinfo.io", path)
                .timeout(5000)
                .send()
                .onItem()
                .transform(response -> {
                    NetworkInfo info = new NetworkInfo();
                    if (response.statusCode() == 200 && response.bodyAsJsonObject() != null) {
                        var json = response.bodyAsJsonObject();
                        LOG.info("Response from ipinfo.io - {}", json);

                        info.setExternalIp(json.getString("ip"));

                        // org field format: "AS12345 ISP Name" — strip the ASN prefix
                        String org = json.getString("org", "");
                        info.setIsp(org.replaceAll("^AS\\d+\\s*", ""));

                        info.setCity(json.getString("city"));
                        info.setRegionName(json.getString("region"));

                        // ipinfo.io returns ISO 3166-1 alpha-2 country codes (e.g. "GB");
                        // convert to full display name (e.g. "United Kingdom")
                        String countryCode = json.getString("country", "");
                        String countryName = countryCode.isEmpty()
                                ? ""
                                : new Locale("", countryCode).getDisplayCountry(Locale.ENGLISH);
                        info.setCountry(countryName);

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
