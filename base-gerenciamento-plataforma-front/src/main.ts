import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import { router } from './app/router'
import { useTema } from './modules/aparencia/stores/tema'
import './styles/tokens/base.css'
import './styles/themes/claro.css'
import './styles/global/base.css'

const pinia = createPinia()
useTema(pinia)
createApp(App).use(pinia).use(router).mount('#app')
