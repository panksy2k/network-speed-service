import { ref } from 'vue';
import axios from 'axios';

export interface SpeedTestState {
    ping: number | null;
    downloadSpeed: number | null;
    uploadSpeed: number | null;
    isRunning: boolean;
    progress: number;
    status: string;
}

export function useSpeedTest() {
    const state = ref<SpeedTestState>({
        ping: null,
        downloadSpeed: null,
        uploadSpeed: null,
        isRunning: false,
        progress: 0,
        status: 'Idle'
    });

    const runPingTest = async () => {
        state.value.status = 'Measuring Ping...';
        const pings = [];
        for (let i = 0; i < 5; i++) {
            const start = performance.now();
            await axios.get('/health?t=' + Date.now()); // Prevent caching
            const end = performance.now();
            pings.push(end - start);
        }
        const avgPing = pings.reduce((a, b) => a + b, 0) / pings.length;
        state.value.ping = Math.round(avgPing);
        state.value.progress = 10;
    };

    const runDownloadTest = async () => {
        state.value.status = 'Measuring Download...';
        const sizeMb = 10; // Start with 10MB
        const startTime = performance.now();

        try {
            const response = await axios.get(`/api/speedtest/download?sizeMb=${sizeMb}`, {
                responseType: 'blob',
                onDownloadProgress: (progressEvent) => {
                    if (progressEvent.total) {
                        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                        // Map 0-100% download to 10-50% total progress
                        state.value.progress = 10 + (percent * 0.4);
                    }
                }
            });

            const endTime = performance.now();
            const durationSeconds = (endTime - startTime) / 1000;
            const bitsLoaded = response.data.size * 8;
            const speedMbps = (bitsLoaded / durationSeconds) / 1_000_000;

            state.value.downloadSpeed = parseFloat(speedMbps.toFixed(2));
        } catch (e) {
            console.error('Download test failed', e);
        }
    };

    const runUploadTest = async () => {
        state.value.status = 'Measuring Upload...';
        const sizeMb = 5; // 5MB payload
        const sizeBytes = sizeMb * 1024 * 1024;
        const data = new Uint8Array(sizeBytes); // Random-ish data
        
        const startTime = performance.now();
        
        try {
            await axios.post('/api/speedtest/upload', data, {
                headers: {
                    'Content-Type': 'application/octet-stream'
                },
                onUploadProgress: (progressEvent) => {
                    if (progressEvent.total) {
                        const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
                        // Map 0-100% upload to 50-90% total progress
                        state.value.progress = 50 + (percent * 0.4);
                    }
                }
            });
            
            const endTime = performance.now();
            const durationSeconds = (endTime - startTime) / 1000;
            const bitsUploaded = sizeBytes * 8;
            const speedMbps = (bitsUploaded / durationSeconds) / 1_000_000;
            
            state.value.uploadSpeed = parseFloat(speedMbps.toFixed(2));
        } catch (e) {
            console.error('Upload test failed', e);
        }
    };

    const startTest = async () => {
        if (state.value.isRunning) return;

        state.value.isRunning = true;
        state.value.ping = null;
        state.value.downloadSpeed = null;
        state.value.uploadSpeed = null;
        state.value.progress = 0;

        try {
            await runPingTest();
            await runDownloadTest();
            await runUploadTest();
            state.value.status = 'Completed';
            state.value.progress = 100;
        } catch (e) {
            state.value.status = 'Error';
            console.error(e);
        } finally {
            state.value.isRunning = false;
        }
    };

    return {
        state,
        startTest
    };
}
