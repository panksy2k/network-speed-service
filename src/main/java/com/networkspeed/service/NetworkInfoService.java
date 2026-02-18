package com.networkspeed.service;

import com.networkspeed.model.NetworkInfo;
import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.core.Vertx;
import io.vertx.mutiny.ext.web.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

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
     * Gather comprehensive network information including local and external details.
     */
    public Uni<NetworkInfo> getNetworkInfo() {
        return vertx.<NetworkInfo>executeBlocking(() -> {
            try {
                return gatherLocalNetworkInfo();
            } catch (Exception e) {
                LOG.error("Failed to gather local network info: {}", e.getMessage());
                return new NetworkInfo().setNetworkType("unknown");
            }
        }).onItem().transformToUni(this::enrichWithExternalInfo);
    }

    /**
     * Gather local network interface information (IP, MAC, etc.)
     */
    private NetworkInfo gatherLocalNetworkInfo() {
        NetworkInfo info = new NetworkInfo();

        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface ni : Collections.list(interfaces)) {
                if (ni.isLoopback() || !ni.isUp()) {
                    continue;
                }

                // Detect WiFi interfaces (common naming on macOS/Linux)
                String name = ni.getDisplayName().toLowerCase();
                boolean isWifi = name.contains("wi-fi") || name.contains("wlan")
                        || name.contains("en0") || name.contains("wifi");

                if (isWifi || name.contains("eth") || name.contains("en")) {
                    info.setNetworkType(isWifi ? "wifi" : "ethernet");

                    // MAC address
                    byte[] mac = ni.getHardwareAddress();
                    if (mac != null) {
                        StringBuilder macStr = new StringBuilder();
                        for (int i = 0; i < mac.length; i++) {
                            macStr.append(String.format("%02X%s", mac[i],
                                    (i < mac.length - 1) ? ":" : ""));
                        }
                        info.setMacAddress(macStr.toString());
                    }

                    // IP addresses
                    Enumeration<InetAddress> addresses = ni.getInetAddresses();
                    for (InetAddress addr : Collections.list(addresses)) {
                        if (!addr.isLoopbackAddress() && addr.getHostAddress().contains(".")) {
                            info.setIpAddress(addr.getHostAddress());
                            break;
                        }
                    }

                    // Link speed is not directly available from Java's NetworkInterface
                    // It would need platform-specific commands (e.g., iwconfig on Linux)
                    info.setLinkSpeed(0);

                    break; // Use the first active non-loopback interface
                }
            }
        } catch (SocketException e) {
            LOG.error("Error reading network interfaces: {}", e.getMessage());
        }

        return info;
    }

    /**
     * Enrich network info with external IP and ISP details using a public API.
     */
    private Uni<NetworkInfo> enrichWithExternalInfo(NetworkInfo info) {
        return webClient.get(80, "ip-api.com", "/json")
                .timeout(5000)
                .send()
                .onItem().transform(response -> {
                    if (response.statusCode() == 200 && response.bodyAsJsonObject() != null) {
                        var json = response.bodyAsJsonObject();
                        info.setExternalIp(json.getString("query"));
                        info.setIsp(json.getString("isp"));
                    }
                    return info;
                })
                .onFailure().recoverWithItem(err -> {
                    LOG.warn("Failed to get external IP info: {}", err.getMessage());
                    return info;
                });
    }

    /**
     * Get WiFi-specific details using platform commands.
     * This runs OS-specific commands to get SSID, signal strength, etc.
     */
    public Uni<NetworkInfo> getWifiDetails() {
        return vertx.<NetworkInfo>executeBlocking(() -> {
            NetworkInfo info = new NetworkInfo();
            info.setNetworkType("wifi");

            try {
                // macOS: use airport command
                String os = System.getProperty("os.name", "").toLowerCase();
                ProcessBuilder pb;

                if (os.contains("mac")) {
                    pb = new ProcessBuilder(
                            "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/Current/Resources/airport",
                            "-I");
                } else if (os.contains("linux")) {
                    pb = new ProcessBuilder("iwgetid", "--raw");
                } else {
                    // Windows or unsupported
                    pb = new ProcessBuilder("netsh", "wlan", "show", "interfaces");
                }

                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                process.waitFor();

                parseWifiOutput(info, output, os);
            } catch (Exception e) {
                LOG.warn("Failed to get WiFi details: {}", e.getMessage());
            }

            return info;
        });
    }

    /**
     * Parse platform-specific WiFi command output.
     */
    private void parseWifiOutput(NetworkInfo info, String output, String os) {
        if (os.contains("mac")) {
            for (String line : output.split("\n")) {
                line = line.trim();
                if (line.startsWith("SSID:")) {
                    info.setSsid(line.substring(5).trim());
                } else if (line.startsWith("BSSID:")) {
                    info.setBssid(line.substring(6).trim());
                } else if (line.startsWith("agrCtlRSSI:")) {
                    try {
                        info.setSignalStrength(Integer.parseInt(line.substring(11).trim()));
                    } catch (NumberFormatException ignored) {
                    }
                } else if (line.startsWith("channel:")) {
                    // Parse channel to estimate frequency
                    try {
                        String channelStr = line.substring(8).trim().split(",")[0];
                        int channel = Integer.parseInt(channelStr);
                        info.setFrequency(channelToFrequency(channel));
                    } catch (Exception ignored) {
                    }
                } else if (line.startsWith("lastTxRate:")) {
                    try {
                        info.setLinkSpeed(Integer.parseInt(line.substring(11).trim()));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } else if (os.contains("linux")) {
            // iwgetid output is just the SSID
            info.setSsid(output.trim());
        }
    }

    /**
     * Convert WiFi channel number to approximate frequency in MHz.
     */
    private int channelToFrequency(int channel) {
        if (channel >= 1 && channel <= 14) {
            // 2.4 GHz band
            return 2412 + (channel - 1) * 5;
        } else if (channel >= 36 && channel <= 177) {
            // 5 GHz band
            return 5000 + channel * 5;
        }
        return 0;
    }
}
