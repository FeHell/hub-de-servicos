import { mount, flushPromises } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { createMemoryHistory, createRouter } from 'vue-router'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import LoginPagina from './LoginPagina.vue'
import { autenticacaoApi } from '../services/autenticacaoApi'
import { ErroApi } from '@/services/clienteHttp'

vi.mock('../services/autenticacaoApi', () => ({
  autenticacaoApi: {
    login: vi.fn(),
    usuario: vi.fn(),
    permissoes: vi.fn(),
    renovar: vi.fn(),
    logout: vi.fn(),
  },
}))

async function montar() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/login', name: 'login', component: { template: '<div />' } },
      { path: '/', name: 'inicio', component: { template: '<div />' } },
    ],
  })
  await router.push('/login')
  const pagina = mount(LoginPagina, {
    attachTo: document.body,
    global: { plugins: [createPinia(), router] },
  })
  return { pagina, router }
}

describe('formulário de login', () => {
  beforeEach(() => vi.resetAllMocks())

  it('valida os campos e move o foco para o primeiro campo inválido', async () => {
    const { pagina } = await montar()
    await pagina.get('form').trigger('submit')
    expect(pagina.text()).toContain('Informe seu usuário.')
    expect(pagina.text()).toContain('Informe sua senha.')
    expect(document.activeElement?.id).toBe('login')
    expect(autenticacaoApi.login).not.toHaveBeenCalled()
    pagina.unmount()
  })

  it('mostra a senha pelo botão e preserva os campos após um erro de credenciais', async () => {
    vi.mocked(autenticacaoApi.login).mockRejectedValue(new ErroApi('Credenciais inválidas.', 401))
    const { pagina } = await montar()
    await pagina.get('#login').setValue('fernando')
    await pagina.get('#senha').setValue('senha-teste')
    await pagina.get('button[aria-label="Mostrar senha"]').trigger('click')
    expect(pagina.get('#senha').attributes('type')).toBe('text')
    await pagina.get('form').trigger('submit')
    await flushPromises()
    expect((pagina.get('#login').element as HTMLInputElement).value).toBe('fernando')
    expect((pagina.get('#senha').element as HTMLInputElement).value).toBe('senha-teste')
    expect(pagina.get('[role="alert"]').text()).toContain('Confira seu usuário')
    pagina.unmount()
  })
})
