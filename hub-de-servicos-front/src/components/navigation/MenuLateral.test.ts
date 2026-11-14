import { mount } from '@vue/test-utils'
import { nextTick } from 'vue'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import MenuLateral from './MenuLateral.vue'

describe('menu lateral no mobile', () => {
  it('abre o diálogo, aceita Escape e permite fechar pelo fundo', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'inicio', component: { template: '<div />' } },
        { path: '/configuracoes/aparencia', name: 'aparencia', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    const painel = mount(MenuLateral, {
      props: { compacto: true, aberto: false, saindo: false },
      attachTo: document.body,
      global: {
        plugins: [router],
      },
    })
    expect(painel.get('aside').attributes('aria-hidden')).toBe('true')
    const acionador = document.createElement('button')
    document.body.append(acionador)
    acionador.focus()
    await painel.setProps({ aberto: true })
    await nextTick()
    expect(painel.get('aside').attributes('aria-modal')).toBe('true')
    expect(document.body.style.overflow).toBe('hidden')
    expect(document.activeElement).toBe(painel.get('button').element)
    await painel.get('button').trigger('keydown', { key: 'Tab', shiftKey: true })
    expect(document.activeElement).toBe(painel.get('.menu__sair').element)
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }))
    expect(painel.emitted('fechar')).toHaveLength(1)
    await painel.get('.menu-fundo').trigger('click')
    expect(painel.emitted('fechar')).toHaveLength(2)
    await painel.setProps({ aberto: false })
    await nextTick()
    expect(document.activeElement).toBe(acionador)
    expect(document.body.style.overflow).toBe('')
    painel.unmount()
  })
})

it('mostra grupos autorizados e mantém o submenu selecionado na edição', async () => {
  const componente = { template: '<div />' }
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/', name: 'inicio', component: componente },
      { path: '/configuracoes/aparencia', name: 'aparencia', component: componente },
      { path: '/cadastros/usuarios', name: 'usuarios', component: componente },
      {
        path: '/cadastros/usuarios/:id/editar',
        name: 'usuarios-editar',
        component: componente,
        meta: { itemMenu: 'usuarios' },
      },
      { path: '/configuracoes/sistema', name: 'configuracoes-sistema', component: componente },
      { path: '/relatorios/dashboard-usuarios', name: 'dashboard-usuarios', component: componente },
    ],
  })
  await router.push('/')
  const painel = mount(MenuLateral, {
    props: { compacto: false, aberto: true, saindo: false },
    global: { plugins: [router] },
  })
  const cadastros = '[aria-controls="submenu-cadastros"]'
  expect(painel.find(cadastros).exists()).toBe(false)
  const configuracoes = '[aria-controls="submenu-configuracoes"]'
  expect(painel.find(configuracoes).exists()).toBe(false)
  await painel.setProps({ permissoes: ['PLATAFORMA_VISITANTE'] })
  expect(painel.find(configuracoes).exists()).toBe(false)
  await painel.setProps({ permissoes: ['PLATAFORMA_ADMINISTRAR'] })
  expect(painel.get('#submenu-configuracoes a').text()).toBe('Sistema')
  const grupo = painel.get(cadastros)
  await grupo.trigger('click')
  expect(grupo.attributes('aria-expanded')).toBe('true')
  await grupo.trigger('click')
  expect(grupo.attributes('aria-expanded')).toBe('false')
  await router.push('/cadastros/usuarios/123/editar')
  await nextTick()
  expect(grupo.attributes('aria-expanded')).toBe('true')
  expect(painel.get('#submenu-cadastros a').classes()).toContain('menu__subitem--ativo')
  expect(painel.get('[aria-controls="submenu-relatorios"]').attributes('aria-expanded')).toBe(
    'false',
  )
  painel.unmount()
})
