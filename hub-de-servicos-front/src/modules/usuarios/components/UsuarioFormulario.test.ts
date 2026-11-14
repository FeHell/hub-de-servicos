import { flushPromises, mount } from '@vue/test-utils'
import { createPinia } from 'pinia'
import { describe, expect, it, vi } from 'vitest'
import { createMemoryHistory, createRouter, RouterView } from 'vue-router'
import type { UsuarioListagem } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'
import UsuarioFormulario from './UsuarioFormulario.vue'

vi.mock('../services/usuariosApi', () => ({ useUsuariosApi: vi.fn() }))

const visitante = 'PLATAFORMA_VISITANTE'
const novaPermissao = 'PLATAFORMA_TESTE'
const conta: UsuarioListagem = {
  id: 'conta-teste',
  login: 'usuario-teste',
  email: 'usuario@example.test',
  status: 'ATIVO',
  dataCriacao: '2026-11-12T10:00:00Z',
  tipo: 'USUARIO',
  permissoes: [],
  permissoesPlataforma: [visitante],
}

function prepararApi() {
  const criar = vi
    .fn()
    .mockResolvedValue({ ...conta, permissoesPlataforma: [novaPermissao, visitante] })
  const atualizar = vi.fn().mockImplementation(async (_id, dados) => ({ ...conta, ...dados }))
  vi.mocked(useUsuariosApi).mockReturnValue({
    listarPermissoesPlataforma: vi.fn().mockResolvedValue([
      { id: '1', codigo: visitante, nome: 'Visitante', status: 'ATIVO' },
      { id: '2', codigo: novaPermissao, nome: 'Permissão futura', status: 'ATIVO' },
    ]),
    criar,
    atualizar,
  } as unknown as ReturnType<typeof useUsuariosApi>)
  return { criar, atualizar }
}

describe('saída do formulário de usuários', () => {
  it('preserva o rascunho ao continuar e navega apenas após o descarte', async () => {
    prepararApi()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/cadastros/usuarios',
          name: 'usuarios',
          component: { template: '<p>Pesquisa</p>' },
        },
        { path: '/cadastros/usuarios/novo', name: 'usuarios-novo', component: UsuarioFormulario },
      ],
    })
    await router.push({ name: 'usuarios-novo' })
    const tela = mount(RouterView, {
      global: {
        plugins: [createPinia(), router],
        stubs: { DialogoBase: { template: '<section role="dialog"><slot /></section>' } },
      },
    })
    await tela.get('#usuario-form-login').setValue('rascunho')
    const voltar = router.push({ name: 'usuarios' })
    await flushPromises()
    const dialogo = tela.get('[role="dialog"]')
    await dialogo.findAll('button')[0]?.trigger('click')
    await voltar
    expect(router.currentRoute.value.name).toBe('usuarios-novo')
    expect((tela.get('#usuario-form-login').element as HTMLInputElement).value).toBe('rascunho')

    const descartar = router.push({ name: 'usuarios' })
    await flushPromises()
    await tela.get('[role="dialog"]').findAll('button')[1]?.trigger('click')
    await descartar
    expect(router.currentRoute.value.name).toBe('usuarios')
    tela.unmount()
  })
})

describe('permissões da plataforma no formulário', () => {
  it('preserva as concessões no servidor ao salvar apenas os dados do usuário', async () => {
    const { atualizar } = prepararApi()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: UsuarioFormulario, props: { usuario: conta } }],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    await tela.get('#usuario-form-email').setValue('atualizado@example.test')
    await tela.get('form').trigger('submit')
    await flushPromises()

    expect(atualizar).toHaveBeenCalledExactlyOnceWith(conta.id, {
      login: conta.login,
      email: 'atualizado@example.test',
      status: conta.status,
    })
    expect(tela.find<HTMLInputElement>(`input[value="${visitante}"]`).element.checked).toBe(true)
    tela.unmount()
  })

  it('envia uma lista vazia quando o operador remove explicitamente a última concessão', async () => {
    const { atualizar } = prepararApi()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: UsuarioFormulario, props: { usuario: conta } }],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    await tela.get(`input[value="${visitante}"]`).setValue(false)
    await tela.get('form').trigger('submit')
    await flushPromises()

    expect(atualizar).toHaveBeenCalledExactlyOnceWith(
      conta.id,
      expect.objectContaining({ permissoesPlataforma: [] }),
    )
    tela.unmount()
  })

  it.each([false, true])('salva opções do catálogo no cadastro ou edição (%s)', async (edicao) => {
    const { criar, atualizar } = prepararApi()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: UsuarioFormulario,
          props: edicao ? { usuario: conta } : {},
        },
      ],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    expect(tela.get('legend').text()).toBe('Permissões da Plataforma')
    expect(tela.text()).toContain('Permissão futura')
    const [opcaoVisitante, opcaoFutura] = tela.findAll<HTMLInputElement>('input[type="checkbox"]')
    expect(opcaoVisitante?.element.checked).toBe(edicao)

    await opcaoFutura?.setValue(true)
    await opcaoVisitante?.setValue(!edicao)
    if (!edicao) {
      await tela.get('#usuario-form-login').setValue(conta.login)
      await tela.get('#usuario-form-email').setValue(conta.email)
      await tela.get('#usuario-form-senha').setValue('Senha-inicial-teste!')
    }
    await tela.get('form').trigger('submit')
    await flushPromises()

    if (edicao) {
      expect(atualizar).toHaveBeenCalledWith(
        conta.id,
        expect.objectContaining({
          permissoesPlataforma: [novaPermissao],
        }),
      )
    } else {
      expect(criar).toHaveBeenCalledWith(
        expect.objectContaining({
          permissoesPlataforma: [novaPermissao, visitante],
        }),
      )
    }
    tela.unmount()
  })
})
