import { createRouter, createWebHistory } from 'vue-router'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import { rotasAdministrativas } from './menu'

export const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  scrollBehavior: () => ({ top: 0, left: 0 }),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/modules/autenticacao/pages/LoginPagina.vue'),
      meta: { titulo: 'Entrar' },
    },
    {
      path: '/',
      component: () => import('@/app/layouts/LayoutAdministrativo.vue'),
      meta: { exigeAutenticacao: true },
      children: rotasAdministrativas,
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
})

router.beforeEach(async (destino) => {
  const autenticacao = useAutenticacao()
  await autenticacao.restaurarSessao()
  if (destino.meta.exigeAutenticacao && !autenticacao.autenticado) return { name: 'login' }
  if (destino.name === 'login' && autenticacao.autenticado) return { name: 'inicio' }
  if (
    typeof destino.meta.permissao === 'string' &&
    !autenticacao.temPermissao(destino.meta.permissao)
  ) {
    return { name: 'inicio' }
  }
})

router.afterEach((destino) => {
  document.title = `${String(destino.meta.titulo || 'Início')} · Plataforma`
})
