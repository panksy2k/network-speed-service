import { createApp } from 'vue'
import CarbonVue3 from '@carbon/vue'
import './styles.scss'
import App from './App.vue'

const app = createApp(App)
app.use(CarbonVue3)
app.mount('#app')
