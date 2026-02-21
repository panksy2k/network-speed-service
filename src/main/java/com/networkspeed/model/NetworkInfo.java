package com.networkspeed.model;

import io.vertx.core.json.JsonObject;

import java.time.Instant;

/**
 * Represents network/WiFi information for the client.
 */
public class NetworkInfo {

    private String ssid;
    private String bssid;
    private String networkType;  // wifi, ethernet, cellular
    private int signalStrength;  // dBm or percentage
    private int frequency;       // MHz
    private int linkSpeed;       // Mbps
    private String macAddress;
    private String ipAddress;
    private String gateway;
    private String dns;
    private String isp;
    private String externalIp;
    private Instant timestamp;
    private String city;
    private String regionName;
    private String country;

    public NetworkInfo() {
        this.timestamp = Instant.now();
    }

    public String getSsid() { return ssid; }
    public NetworkInfo setSsid(String ssid) { this.ssid = ssid; return this; }

    public String getBssid() { return bssid; }
    public NetworkInfo setBssid(String bssid) { this.bssid = bssid; return this; }

    public String getNetworkType() { return networkType; }
    public NetworkInfo setNetworkType(String networkType) { this.networkType = networkType; return this; }

    public int getSignalStrength() { return signalStrength; }
    public NetworkInfo setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; return this; }

    public int getFrequency() { return frequency; }
    public NetworkInfo setFrequency(int frequency) { this.frequency = frequency; return this; }

    public int getLinkSpeed() { return linkSpeed; }
    public NetworkInfo setLinkSpeed(int linkSpeed) { this.linkSpeed = linkSpeed; return this; }

    public String getMacAddress() { return macAddress; }
    public NetworkInfo setMacAddress(String macAddress) { this.macAddress = macAddress; return this; }

    public String getIpAddress() { return ipAddress; }
    public NetworkInfo setIpAddress(String ipAddress) { this.ipAddress = ipAddress; return this; }

    public String getGateway() { return gateway; }
    public NetworkInfo setGateway(String gateway) { this.gateway = gateway; return this; }

    public String getDns() { return dns; }
    public NetworkInfo setDns(String dns) { this.dns = dns; return this; }

    public String getIsp() { return isp; }
    public NetworkInfo setIsp(String isp) { this.isp = isp; return this; }

    public String getExternalIp() { return externalIp; }
    public NetworkInfo setExternalIp(String externalIp) { this.externalIp = externalIp; return this; }

    public Instant getTimestamp() { return timestamp; }
    public NetworkInfo setTimestamp(Instant timestamp) { this.timestamp = timestamp; return this; }

    public String getCity() { return city; }
    public NetworkInfo setCity(String city) { this.city = city; return this; }

    public String getRegionName() { return regionName; }
    public NetworkInfo setRegionName(String regionName) { this.regionName = regionName; return this; }

    public String getCountry() { return country; }
    public NetworkInfo setCountry(String country) { this.country = country; return this; }

    public JsonObject toJson() {
        return new JsonObject()
                .put("ssid", ssid)
                .put("bssid", bssid)
                .put("networkType", networkType)
                .put("signalStrength", signalStrength)
                .put("frequency", frequency)
                .put("linkSpeed", linkSpeed)
                .put("macAddress", macAddress)
                .put("ipAddress", ipAddress)
                .put("gateway", gateway)
                .put("dns", dns)
                .put("isp", isp)
                .put("externalIp", externalIp)
                .put("city", city)
                .put("regionName", regionName)
                .put("country", country)
                .put("timestamp", timestamp != null ? timestamp.toString() : null);
    }
}
