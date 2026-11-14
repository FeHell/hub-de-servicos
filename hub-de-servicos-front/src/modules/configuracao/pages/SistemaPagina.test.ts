import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { ErroApi } from '@/services/clienteHttp'
import { useConfiguracaoApi } from '../services/configuracaoApi'
import SistemaPagina from './SistemaPagina.vue'

vi.mock('../services/configuracaoApi', () => ({
  useConfiguracaoApi: vi.fn(),
}))

const provedores = vi.fn()
const consultar = vi.fn()
const salvar = vi.fn()
const listarServicos = vi.fn()
const consultarServico = vi.fn()
const atualizarServico = vi.fn()
const infobip = (chave?: string, url = 'https://conta-teste.api.infobip.com') => ({
  servico: 'INFOBIP',
  configuracoes: [
    {
      chave: 'INFOBIP_API_KEY',
      sensivel: true,
      configurada: !!chave,
      valor: chave,
      status: 'ATIVO',
    },
    { chave: 'INFOBIP_BASE_URL', sensivel: false, configurada: true, valor: url, status: 'ATIVO' },
  ],
})
const stone = (chave?: string) => ({
  servico: 'STONE',
  configuracoes: [
    { chave: 'STONE_API_KEY', sensivel: true, configurada: !!chave, valor: chave, status: 'ATIVO' },
  ],
})

beforeEach(() => {
  vi.resetAllMocks()
  provedores.mockResolvedValue([{ codigo: 'INFOBIP', nome: 'Infobip' }])
  consultar.mockResolvedValue({ valor: 'INFOBIP', status: 'ATIVO' })
  listarServicos.mockResolvedValue([infobip(), stone()])
  consultarServico.mockImplementation((servico) =>
    Promise.resolve(servico === 'INFOBIP' ? infobip() : stone()),
  )
  vi.mocked(useConfiguracaoApi).mockReturnValue({
    listarProvedoresSms: provedores,
    consultarProvedorSms: consultar,
    alterarProvedorSms: salvar,
    listarServicos,
    consultarServico,
    atualizarServico,
  })
})

describe('configuração de SMS', () => {
  it('mostra o provedor ativo e as opções retornadas pela API', async () => {
    const pagina = mount(SistemaPagina)
    await flushPromises()

    expect(pagina.text()).toContain('Provedor ativo Infobip')
    expect(pagina.findAll('option').map((opcao) => opcao.text())).toEqual(['Infobip'])
    expect(pagina.get('button[type="submit"]').attributes('disabled')).toBeDefined()

    pagina.unmount()
  })

  it('salva a seleção da API e mostra sucesso ou erro', async () => {
    provedores.mockResolvedValue([
      { codigo: 'INFOBIP', nome: 'Infobip' },
      { codigo: 'SIMULADO', nome: 'Provedor simulado' },
    ])
    salvar.mockRejectedValueOnce(new ErroApi('Provedor de SMS indisponível.', 400))
    salvar.mockResolvedValueOnce({ valor: 'SIMULADO', status: 'ATIVO' })

    const pagina = mount(SistemaPagina)
    await flushPromises()
    await pagina.get('select').setValue('SIMULADO')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(pagina.get('[role="alert"]').text()).toBe('Provedor de SMS indisponível.')
    expect(pagina.text()).toContain('Provedor ativo Infobip')
    await pagina.get('form').trigger('submit')
    await flushPromises()

    expect(salvar).toHaveBeenLastCalledWith('SIMULADO')
    expect(pagina.get('[role="status"]').text()).toBe('Provedor de SMS atualizado.')
    expect(pagina.text()).toContain('Provedor ativo Provedor simulado')

    pagina.unmount()
  })

  it('indica configuração inativa e impede alteração pela tela', async () => {
    consultar.mockResolvedValue({ valor: 'INFOBIP', status: 'INATIVO' })
    const pagina = mount(SistemaPagina)
    await flushPromises()

    expect(pagina.text()).toContain('O envio de SMS está inativo.')
    expect(pagina.get('select').attributes('disabled')).toBeDefined()
    await pagina.get('form').trigger('submit')
    expect(salvar).not.toHaveBeenCalled()

    pagina.unmount()
  })
})

describe('credenciais dos serviços', () => {
  it('exibe as duas chaves integrais e selecionáveis sem escrever em storage ou console', async () => {
    const chaveInfobip = 'infobip-ficticia-completa+-'
    const chaveStone = 'stone-ficticia-completa+-'
    const gravar = vi.spyOn(Storage.prototype, 'setItem')
    const logs = ['log', 'info', 'debug', 'warn', 'error'].map((metodo) =>
      vi.spyOn(console, metodo as 'log'),
    )
    consultarServico.mockImplementation((servico) =>
      Promise.resolve(servico === 'INFOBIP' ? infobip(chaveInfobip) : stone(chaveStone)),
    )
    const pagina = mount(SistemaPagina)
    try {
      await flushPromises()
      expect(consultarServico.mock.calls).toEqual([['INFOBIP'], ['STONE']])
      for (const [seletor, chave] of [
        ['#servico-infobip-api-key', chaveInfobip],
        ['#servico-stone-api-key', chaveStone],
      ]) {
        const campo = pagina.get<HTMLInputElement>(seletor).element
        expect(campo.type).toBe('text')
        expect(campo.value).toBe(chave)
        campo.select()
        expect(campo.value.slice(campo.selectionStart!, campo.selectionEnd!)).toBe(chave)
      }
      expect(pagina.get<HTMLInputElement>('#servico-infobip-base-url').element.value).toBe(
        'https://conta-teste.api.infobip.com',
      )
      expect(gravar).not.toHaveBeenCalled()
      for (const log of logs)
        expect(JSON.stringify(log.mock.calls)).not.toContain('ficticia-completa')
    } finally {
      pagina.unmount()
      gravar.mockRestore()
      for (const log of logs) log.mockRestore()
    }
  })

  it('envia só a URL quando a chave estiver vazia ou inalterada e volta a mostrar a chave salva', async () => {
    const url = 'https://nova-conta.api.infobip.com'
    const segredo = 'infobip-ficticia'
    consultarServico.mockImplementation((servico) =>
      Promise.resolve(servico === 'INFOBIP' ? infobip(segredo) : stone()),
    )
    atualizarServico.mockResolvedValue(infobip(undefined, url))
    const pagina = mount(SistemaPagina)
    await flushPromises()
    const formulario = pagina.get('form[aria-labelledby=servico-infobip]')
    const campoUrl = formulario.get<HTMLInputElement>('#servico-infobip-base-url')
    expect(formulario.text()).toContain('Credencial configurada')
    expect(pagina.text()).toContain('Credencial não configurada')
    expect(formulario.get('button[type=submit]').attributes('disabled')).toBeDefined()
    await formulario.get('#servico-infobip-api-key').setValue('  ')
    await campoUrl.setValue(url)
    consultarServico.mockImplementation((servico) =>
      Promise.resolve(servico === 'INFOBIP' ? infobip(segredo, url) : stone()),
    )
    await formulario.trigger('submit')
    await flushPromises()
    expect(atualizarServico).toHaveBeenCalledExactlyOnceWith('INFOBIP', { INFOBIP_BASE_URL: url })
    expect(formulario.text()).toContain('Configurações salvas.')
    expect(formulario.get<HTMLInputElement>('#servico-infobip-api-key').element.value).toBe(segredo)
    pagina.unmount()
  })

  it.each([
    ['INFOBIP', 'INFOBIP_API_KEY', '#servico-infobip-api-key'],
    ['STONE', 'STONE_API_KEY', '#servico-stone-api-key'],
  ] as const)(
    'cadastra, substitui e recupera a chave de %s ao reabrir',
    async (codigo, campo, seletor) => {
      let salva: string | undefined
      consultarServico.mockImplementation((servico) =>
        Promise.resolve(
          servico === 'INFOBIP'
            ? infobip(codigo === servico ? salva : undefined)
            : stone(codigo === servico ? salva : undefined),
        ),
      )
      atualizarServico.mockImplementation((servico, valores) => {
        salva = valores[campo]
        return Promise.resolve(servico === 'INFOBIP' ? infobip() : stone())
      })
      const pagina = mount(SistemaPagina)
      await flushPromises()
      const formulario = pagina.get(`form[aria-labelledby=servico-${codigo.toLowerCase()}]`)
      expect(formulario.get<HTMLInputElement>(seletor).element.value).toBe('')
      for (const chave of ['teste-ficticia-inicial', 'teste-ficticia-nova']) {
        await formulario.get(seletor).setValue(chave)
        await formulario.trigger('submit')
        await flushPromises()
        expect(atualizarServico).toHaveBeenLastCalledWith(codigo, { [campo]: chave })
        expect(formulario.get<HTMLInputElement>(seletor).element.value).toBe(chave)
        expect(formulario.text()).toContain('Credencial configurada')
      }
      pagina.unmount()
      const reaberta = mount(SistemaPagina)
      await flushPromises()
      expect(reaberta.get<HTMLInputElement>(seletor).element.value).toBe('teste-ficticia-nova')
      reaberta.unmount()
    },
  )

  it('mantém o outro serviço disponível se uma consulta for recusada', async () => {
    consultarServico.mockImplementation((servico) =>
      servico === 'STONE'
        ? Promise.reject(new ErroApi('mensagem privada omitida', 403))
        : Promise.resolve(infobip('infobip-ficticia')),
    )
    const pagina = mount(SistemaPagina)
    await flushPromises()
    expect(pagina.get<HTMLInputElement>('#servico-infobip-api-key').element.value).toBe(
      'infobip-ficticia',
    )
    expect(pagina.find('#servico-stone-api-key').exists()).toBe(false)
    expect(pagina.text()).toContain('Não foi possível consultar as configurações deste serviço.')
    expect(pagina.text()).not.toContain('mensagem privada omitida')
    pagina.unmount()
  })

  it('mostra erro seguro se o servidor recusar uma alteração', async () => {
    atualizarServico.mockRejectedValue(new ErroApi('não exibir conteúdo sensível', 503))
    const pagina = mount(SistemaPagina)
    await flushPromises()
    const formulario = pagina.get('form[aria-labelledby=servico-stone]')
    await formulario.get('#servico-stone-api-key').setValue('teste-ficticia')
    await formulario.trigger('submit')
    await flushPromises()
    expect(formulario.text()).toContain('Configure a chave mestra no backend')
    expect(formulario.text()).not.toContain('não exibir conteúdo sensível')
    pagina.unmount()
  })
})
