<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axios from 'axios';
import { useSpeedTest } from './composables/useSpeedTest';

interface NetworkInfo {
    externalIp?: string;
    isp?: string;
    networkType?: string;
    city?: string;
    regionName?: string;
    country?: string;
}

const networkInfo = ref<NetworkInfo>({});
const { state, startTest } = useSpeedTest();

onMounted(async () => {
    try {
        const response = await axios.get('/api/network/info');
        networkInfo.value = response.data;
    } catch (e) {
        console.error('Failed to load network info', e);
    }
});
</script>

<template>
  <div class="container cds--grid">
    <div class="cds--row">
      <div class="cds--col-lg-16">
        <header class="header">
          <h1 class="landing-page__heading">Network Speed Test</h1>
          
          <cv-tile v-if="networkInfo.externalIp" class="network-info-tile">
            <div class="network-info-item">
              <span class="label">IP:</span> {{ networkInfo.externalIp }}
            </div>
            <div class="network-info-item">
              <span class="label">ISP:</span> {{ networkInfo.isp }}
            </div>
            <div v-if="networkInfo.city" class="network-info-item">
              <span class="label">Location:</span> {{ networkInfo.city }}, {{ networkInfo.regionName }}, {{ networkInfo.country }}
            </div>
          </cv-tile>
          <cv-tile v-else class="loading-info-tile">
            <cv-loading description="Loading network info" small />
            <span style="margin-left: 1rem">Loading network info...</span>
          </cv-tile>
        </header>

        <main>
          <div class="gauges cds--row">
            <div class="gauge cds--col-md-4 cds--col-sm-4">
              <div class="value">{{ state.ping !== null ? state.ping : '-' }}</div>
              <div class="label">Ping (ms)</div>
            </div>
            <div class="gauge cds--col-md-4 cds--col-sm-4">
              <div class="value">{{ state.downloadSpeed !== null ? state.downloadSpeed : '-' }}</div>
              <div class="label">Download (Mbps)</div>
            </div>
            <div class="gauge cds--col-md-4 cds--col-sm-4">
              <div class="value">{{ state.uploadSpeed !== null ? state.uploadSpeed : '-' }}</div>
              <div class="label">Upload (Mbps)</div>
            </div>
          </div>

          <div class="controls">
            <cv-button 
              @click="startTest" 
              :disabled="state.isRunning"
              kind="primary"
              size="xl"
              class="fancy-start-button"
            >
              {{ state.isRunning ? 'Running...' : 'Start Test' }}
            </cv-button>
          </div>

          <div class="status-bar" v-if="state.isRunning || state.progress > 0">
            <div class="status-text">{{ state.status }}</div>
            <cv-progress-bar
              :value="state.progress"
              :status="state.isRunning ? 'active' : 'finished'"
              label="Test Progress"
              helper-text=""
            />
          </div>
        </main>
      </div>
    </div>
  </div>
</template>

<style scoped>
.container {
  max-width: 100%;
  padding: 2rem;
  min-height: 100vh;
  background-color: var(--cds-background);
  color: var(--cds-text-primary);
}

.header {
  text-align: center;
  margin-bottom: 3rem;
}

h1 {
  font-size: 2.5rem;
  margin-bottom: 2rem;
  color: var(--cds-text-primary);
}

.network-info-tile, .loading-info-tile {
  display: inline-flex;
  gap: 2rem;
  padding: 1rem 2rem;
  align-items: center;
  justify-content: center;
  margin-bottom: 1rem;
}

.network-info-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.network-info-item .label {
  font-weight: bold;
  font-size: 0.75rem;
  color: var(--cds-text-secondary);
  text-transform: uppercase;
}

.gauges {
  display: flex;
  justify-content: center;
  gap: 2rem;
  margin-bottom: 3rem;
  text-align: center;
}

.gauge {
  background: var(--cds-layer);
  padding: 2rem;
  margin: 0.5rem;
  border-bottom: 1px solid var(--cds-border-subtle);
}

.gauge .value {
  font-size: 3.5rem; /* Carbon typography */
  font-weight: 300;
  color: var(--cds-text-primary);
  margin-bottom: 0.5rem;
}

.gauge .label {
  color: var(--cds-text-secondary);
  font-size: 0.875rem;
  text-transform: uppercase;
  letter-spacing: 0.32px;
}

.controls {
  text-align: center;
  margin-bottom: 3rem;
}

.status-bar {
  max-width: 600px;
  margin: 0 auto;
}

.status-text {
  margin-bottom: 0.5rem;
  color: var(--cds-text-secondary);
  text-align: center;
}
.fancy-start-button {
  padding: 1.5rem 3rem;
  font-size: 1.5rem;
  font-weight: bold;
  border-radius: 50px;
  background-image: linear-gradient(45deg, #FF5F6D, #FFC371);
  border: none;
  box-shadow: 0 4px 15px 0 rgba(255, 100, 120, 0.75);
  transition: all 0.3s ease-in-out;
}

.fancy-start-button:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px 0 rgba(255, 100, 120, 0.75);
}
</style>
