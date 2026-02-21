# Network Speed Service

A reactive network speed test service built with **Vert.x 4** and **SmallRye Mutiny**. Measures upload/download speeds, network latency, WiFi details, and supports test server selection.

## Features

- **Speed Testing** – Measure download and upload speeds via HTTP-based data transfer
- **Latency & Jitter** – Ping test servers with aggregated min/max/avg latency and jitter calculation
- **Network Info** – Detect WiFi SSID, signal strength, frequency, link speed, MAC, IP, ISP
- **Server Selection** – Auto-discover nearest test server via geolocation, or select manually
- **Reactive** – Fully non-blocking using Vert.x event loop and Mutiny reactive streams

## Tech Stack

- Java 17
- Vert.x 4.5.9 (Core, Web, Web Client, Config)
- SmallRye Mutiny 3.14.0
- Jackson for JSON serialization
- SLF4J + Logback for logging
- JUnit 5 + Vert.x JUnit5 for testing

## Project Structure

```
src/main/java/com/networkspeed/
├── MainApplication.java          # Entry point
├── config/
│   └── AppConfig.java            # Type-safe configuration
├── handler/
│   ├── ErrorHandler.java         # Global error handling
│   ├── NetworkInfoHandler.java   # Network info endpoints
│   ├── ServerSelectionHandler.java # Server management endpoints
│   └── SpeedTestHandler.java     # Speed test endpoints
├── model/
│   ├── NetworkInfo.java          # WiFi/network details
│   ├── PingResult.java           # Latency measurement result
│   ├── SpeedTestRequest.java     # Speed test parameters
│   ├── SpeedTestResult.java      # Speed test results
│   └── TestServer.java           # Test server representation
├── service/
│   ├── NetworkInfoService.java   # Network info gathering
│   ├── PingService.java          # Latency measurement
│   ├── ServerDiscoveryService.java # Server discovery & geo-sort
│   └── SpeedTestService.java     # Download/upload speed measurement
└── verticle/
    ├── HttpServerVerticle.java   # HTTP server & routing
    └── MainVerticle.java         # Verticle orchestration
```

## API Endpoints

### Speed Test
- `POST /api/speedtest` – Run a full speed test (latency + download + upload)
- `GET /api/speedtest/download` – Download speed only
- `GET /api/speedtest/upload` – Upload speed only
- `GET /api/speedtest/status` – Active test count

### Network Info
- `GET /api/network/info` – Comprehensive network info (IP, MAC, ISP, etc.)
- `GET /api/network/wifi` – WiFi-specific details (SSID, signal, frequency)

### Servers
- `GET /api/servers` – List all test servers (optional `?region=` filter)
- `GET /api/servers/nearest` – Get nearest server by geolocation
- `GET /api/servers/:id` – Get server details
- `GET /api/servers/:id/ping?count=5` – Ping a server
- `POST /api/servers/refresh` – Refresh server list

### System
- `GET /health` – Health check
- `GET /api` – API version info

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+

### Build
```bash
mvn clean package
```

### Run
```bash
mvn exec:java
# or
java -jar target/network-speed-service-1.0.0-SNAPSHOT.jar
```

The service starts on `http://localhost:8090` by default.

### Configuration

Edit `src/main/resources/config.yaml` to customize server port, speed test parameters, and server discovery settings. Configuration can also be overridden via environment variables.

### Deployment on cloud host using Anisble playbook
ansible-playbook -i ansible/inventory.ini ansible/deploy-networkspeed.yml
