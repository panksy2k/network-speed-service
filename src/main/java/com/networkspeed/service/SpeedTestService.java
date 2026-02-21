package com.networkspeed.service;

import io.vertx.core.buffer.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Random;

/**
 * Service providing utilities for client-side speed tests.
 * Generates data for downloads and calculates speed for uploads.
 */
public class SpeedTestService {

    private static final Logger LOG = LoggerFactory.getLogger(SpeedTestService.class);
    private static final int MAX_DOWNLOAD_SIZE_MB = 100; // Cap to prevent OOM

    /**
     * Generate random data for download test.
     */
    public Buffer generateDownloadData(int sizeMb) {
        if (sizeMb <= 0) {
            sizeMb = 1;
        }
        if (sizeMb > MAX_DOWNLOAD_SIZE_MB) {
            sizeMb = MAX_DOWNLOAD_SIZE_MB;
        }

        LOG.info("Generating {} MB of data for download test", sizeMb);
        byte[] data = new byte[sizeMb * 1024 * 1024];
        new Random().nextBytes(data);
        return Buffer.buffer(data);
    }

    /**
     * Calculate speed in Megabits per second.
     */
    public double calculateSpeedMbps(long bytes, long durationMs) {
        if (durationMs <= 0) {
            return 0.0;
        }
        double bits = bytes * 8.0;
        double seconds = durationMs / 1000.0;
        return (bits / seconds) / 1_000_000.0;
    }
}
