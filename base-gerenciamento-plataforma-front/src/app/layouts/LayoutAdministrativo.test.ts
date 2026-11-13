import { mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import LayoutAdministrativo from './LayoutAdministrativo.vue'

describe('menu lateral no desktop', () => {
  it('recolhe ao clicar no conteúdo ou no item da rota atual', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        { path: '/', name: 'inicio', component: { template: '<button>Conteúdo</button>' } },
        { path: '/configuracoes/aparencia', name: 'aparencia', component: { template: '<div />' } },
      ],
    })
    await router.push('/')
    const layout = mount(LayoutAdministrativo, {
      attachTo: document.body,
      global: { plugins: [createPinia(), router] },
    })
    const acionador = layout.get('button[aria-controls="menu-principal"]')
    await acionador.trigger('click')
    expect(acionador.attributes('aria-expanded')).toBe('true')
    await layout.get('main button').trigger('click')
    expect(acionador.attributes('aria-expanded')).toBe('false')
    await acionador.trigger('click')
    await layout.get('a[aria-label="Início"]').trigger('click')
    expect(acionador.attributes('aria-expanded')).toBe('false')
    expect(layout.get('#menu-principal').classes()).toContain('menu-lateral--recolhido-pelo-item')
    await layout.get('#menu-principal').trigger('mouseleave')
    expect(layout.get('#menu-principal').classes()).not.toContain(
      'menu-lateral--recolhido-pelo-item',
    )
    layout.unmount()
  })
})
