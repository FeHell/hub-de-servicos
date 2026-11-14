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
  it.each([
    ['NAO_INFORMADO', 'Não informado'],
    ['PENDENTE', 'Pendente de confirmação'],
    ['VERIFICADO', 'Verificado'],
  ] as const)('apresenta %s dentro do campo e salva o nome carregado', async (situacao, rotulo) => {
    const { atualizar } = prepararApi()
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: UsuarioFormulario,
          props: {
            usuario: {
              ...conta,
              nome: 'Ana Cheff',
              telefone: situacao === 'NAO_INFORMADO' ? null : '+5534999999999',
              situacaoTelefone: situacao,
            },
          },
        },
      ],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    const telefone = tela.get('#usuario-form-telefone')
    expect(telefone.attributes('aria-describedby')).toBe('estado-telefone')
    const controle = telefone.element.closest('.campo__controle')
    expect(controle?.querySelector('#estado-telefone')?.getAttribute('title')).toBe(rotulo)
    expect(controle?.querySelector('#estado-telefone svg')).not.toBeNull()
    const nome = tela.get<HTMLInputElement>('#usuario-form-nome')
    expect(nome.element.value).toBe('Ana Cheff')
    await nome.setValue(' Ana Silva ')
    await tela.get('form').trigger('submit')
    await flushPromises()
    expect(atualizar).toHaveBeenCalledExactlyOnceWith(conta.id, {
      login: conta.login,
      email: conta.email,
      status: conta.status,
      nome: 'Ana Silva',
    })
    tela.unmount()
  })

  it('carrega o telefone verificado e salva um novo número normalizado como pendente', async () => {
    const { atualizar } = prepararApi()
    atualizar.mockImplementation(async (_id, dados) => ({
      ...conta,
      ...dados,
      situacaoTelefone: 'PENDENTE',
    }))
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [
        {
          path: '/',
          component: UsuarioFormulario,
          props: {
            usuario: { ...conta, telefone: '+5534999999999', situacaoTelefone: 'VERIFICADO' },
          },
        },
      ],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    const telefone = tela.get<HTMLInputElement>('#usuario-form-telefone')
    expect(telefone.element.value).toBe('(34) 99999-9999')
    expect(tela.text()).toContain('Verificado')
    await telefone.setValue('1234')
    await tela.get('form').trigger('submit')
    expect(atualizar).not.toHaveBeenCalled()
    expect(tela.text()).toContain('Informe um telefone brasileiro válido com DDD.')
    await telefone.setValue('34988887777')
    await tela.get('form').trigger('submit')
    await flushPromises()
    expect(atualizar).toHaveBeenCalledExactlyOnceWith(conta.id, {
      login: conta.login,
      email: conta.email,
      status: conta.status,
      telefone: '+5534988887777',
    })
    expect(tela.text()).toContain('Pendente de confirmação')
    tela.unmount()
  })

  it('envia Empresa e Trabalhador juntos usando os códigos retornados pelo catálogo', async () => {
    const { criar } = prepararApi()
    const empresa = 'PLATAFORMA_EMPRESA'
    const trabalhador = 'PLATAFORMA_TRABALHADOR'
    vi.mocked(useUsuariosApi().listarPermissoesPlataforma).mockResolvedValue([
      { id: 'e', codigo: empresa, nome: 'Empresa', status: 'ATIVO', descricao: null },
      { id: 't', codigo: trabalhador, nome: 'Trabalhador', status: 'ATIVO', descricao: null },
    ])
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/', component: UsuarioFormulario }],
    })
    await router.push('/')
    const tela = mount(RouterView, { global: { plugins: [router] } })
    await flushPromises()
    expect(tela.get('fieldset').text()).toContain('Empresa')
    expect(tela.get('fieldset').text()).toContain('Trabalhador')
    await tela.get(`input[value="${empresa}"]`).setValue(true)
    await tela.get(`input[value="${trabalhador}"]`).setValue(true)
    await tela.get('#usuario-form-login').setValue(conta.login)
    await tela.get('#usuario-form-email').setValue(conta.email)
    await tela.get('#usuario-form-senha').setValue('Senha-inicial-teste!')
    await tela.get('#usuario-form-telefone').setValue('(34) 99999-9999')
    await tela.get('form').trigger('submit')
    await flushPromises()
    expect(criar).toHaveBeenCalledWith(
      expect.objectContaining({
        telefone: '+5534999999999',
        permissoesPlataforma: [empresa, trabalhador],
      }),
    )
    tela.unmount()
  })

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
