import { createApp } from 'vue'
import { createPinia } from 'pinia'

import App from './App.vue'
import router from './router'
import { SESSION_EXPIRED_EVENT } from './api/client'
import './styles/base.css'

const app = createApp(App)

app.use(createPinia())
app.use(router)
app.mount('#app')

window.addEventListener(SESSION_EXPIRED_EVENT, () => {
  if (router.currentRoute.value.name !== 'login') {
    router.replace({
      name: 'login',
      query: {
        redirect: router.currentRoute.value.fullPath,
        reason: 'expired',
      },
    })
  }
})
