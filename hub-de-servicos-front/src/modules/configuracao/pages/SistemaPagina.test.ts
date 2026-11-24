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
const consultarDevMode = vi.fn()
const alterarDevMode = vi.fn()
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
    {
      chave: 'stone.dev.secretKey',
      sensivel: true,
      configurada: !!chave,
      valor: chave,
      status: 'ATIVO',
    },
  ],
})

beforeEach(() => {
  vi.resetAllMocks()
  provedores.mockResolvedValue([{ codigo: 'INFOBIP', nome: 'Infobip' }])
  consultar.mockResolvedValue({ valor: 'INFOBIP', status: 'ATIVO' })
  consultarDevMode.mockResolvedValue({ devMode: true })
  alterarDevMode.mockResolvedValue({ devMode: false })
  listarServicos.mockResolvedValue([infobip(), stone()])
  consultarServico.mockImplementation((servico) =>
    Promise.resolve(servico === 'INFOBIP' ? infobip() : stone()),
  )
  vi.mocked(useConfiguracaoApi).mockReturnValue({
    listarProvedoresSms: provedores,
    consultarProvedorSms: consultar,
    alterarProvedorSms: salvar,
    consultarDevMode,
    alterarDevMode,
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
    await pagina.get('[aria-labelledby=titulo-sms] form').trigger('submit')
    await flushPromises()

    expect(pagina.get('[role="alert"]').text()).toBe('Provedor de SMS indisponível.')
    expect(pagina.text()).toContain('Provedor ativo Infobip')
    await pagina.get('[aria-labelledby=titulo-sms] form').trigger('submit')
    await flushPromises()

    expect(salvar).toHaveBeenLastCalledWith('SIMULADO')
    expect(pagina.get('[aria-labelledby=titulo-sms] [role="status"]').text()).toBe(
      'Provedor de SMS atualizado.',
    )
    expect(pagina.text()).toContain('Provedor ativo Provedor simulado')

    pagina.unmount()
  })

  it('indica configuração inativa e impede alteração pela tela', async () => {
    consultar.mockResolvedValue({ valor: 'INFOBIP', status: 'INATIVO' })
    const pagina = mount(SistemaPagina)
    await flushPromises()

    expect(pagina.text()).toContain('O envio de SMS está inativo.')
    expect(pagina.get('select').attributes('disabled')).toBeDefined()
    await pagina.get('[aria-labelledby=titulo-sms] form').trigger('submit')
    expect(salvar).not.toHaveBeenCalled()

    pagina.unmount()
  })
})

it('edita um único DEV_MODE global e separa as sete configurações Stone por ambiente', async () => {
  const identificadores = [
    'stone.dev.accountId',
    'stone.dev.publicKey',
    'stone.dev.secretKey',
    'stone.dev.recipientMaster',
    'stone.prod.accountId',
    'stone.prod.publicKey',
    'stone.prod.recipientMaster',
  ]
  consultarServico.mockImplementation((servico) =>
    Promise.resolve(
      servico === 'INFOBIP'
        ? infobip()
        : {
            servico: 'STONE',
            configuracoes: identificadores.map((chave) => ({
              chave,
              sensivel: chave.endsWith('secretKey'),
              configurada: false,
              status: 'ATIVO',
            })),
          },
    ),
  )
  const pagina = mount(SistemaPagina)
  await flushPromises()
  const ambiente = pagina.get('[aria-labelledby=titulo-ambiente]')
  expect(pagina.findAll('[role=switch]')).toHaveLength(1)
  const toggle = ambiente.get<HTMLInputElement>('[role=switch]')
  expect(toggle.element.checked).toBe(true)
  expect(ambiente.text()).toContain('Ativado')
  await toggle.setValue(false)
  expect(ambiente.text()).toContain('Desativado')
  await flushPromises()
  expect(alterarDevMode).toHaveBeenCalledExactlyOnceWith(false)
  expect(pagina.get('[aria-labelledby=titulo-ambiente]').text()).toContain('Produção (PROD)')
  const card = pagina.get('[aria-labelledby=servico-stone]')
  expect(card.findAll('input[type=text]')).toHaveLength(7)
  expect(card.find('#servico-stone-prod-secretkey').exists()).toBe(false)
  expect(card.text()).toContain('A chave secreta de produção ficará disponível')
  expect(card.findAll('h4').map((titulo) => titulo.text())).toEqual([
    'Desenvolvimento (DEV)',
    'Produção (PROD)',
  ])
  pagina.unmount()
})

it('carrega o toggle desligado e edita a chave PROD quando ela faz parte do catálogo da API', async () => {
  consultarDevMode.mockResolvedValue({ devMode: false })
  alterarDevMode.mockResolvedValue({ devMode: true })
  let valor = 'credencial-producao-de-teste'
  consultarServico.mockImplementation((servico) =>
    Promise.resolve(
      servico === 'INFOBIP'
        ? infobip()
        : {
            servico: 'STONE',
            configuracoes: [
              ...stone().configuracoes,
              {
                chave: 'stone.prod.secretKey',
                sensivel: true,
                configurada: true,
                valor,
                status: 'ATIVO',
              },
            ],
          },
    ),
  )
  atualizarServico.mockImplementation(async (_, valores) => {
    valor = valores['stone.prod.secretKey']
  })
  const pagina = mount(SistemaPagina)
  await flushPromises()
  const ambiente = pagina.get('[aria-labelledby=titulo-ambiente]')
  const toggle = ambiente.get<HTMLInputElement>('[role=switch]')
  expect(toggle.element.checked).toBe(false)
  expect(ambiente.text()).toContain('Desativado')
  await toggle.setValue(true)
  await flushPromises()
  expect(alterarDevMode).toHaveBeenCalledExactlyOnceWith(true)

  const formulario = pagina.get('form[aria-labelledby=servico-stone]')
  const campo = formulario.get<HTMLInputElement>('#servico-stone-prod-secretkey')
  expect(campo.element.type).toBe('text')
  expect(campo.element.value).toBe(valor)
  campo.element.select()
  expect(
    campo.element.value.slice(campo.element.selectionStart!, campo.element.selectionEnd!),
  ).toBe(valor)
  await campo.setValue('credencial-producao-atualizada')
  await formulario.trigger('submit')
  await flushPromises()
  expect(atualizarServico).toHaveBeenCalledExactlyOnceWith('STONE', {
    'stone.prod.secretKey': 'credencial-producao-atualizada',
  })
  expect(formulario.get<HTMLInputElement>('#servico-stone-prod-secretkey').element.value).toBe(
    valor,
  )
  expect(formulario.text()).not.toContain('A chave secreta de produção ficará disponível')
  pagina.unmount()
})

it('mostra as chaves DEV e PROD juntas e omite a chave apagada ao salvar outros campos', async () => {
  const resposta = {
    servico: 'STONE',
    configuracoes: [
      ...stone('chave-dev-de-teste').configuracoes,
      {
        chave: 'stone.prod.secretKey',
        sensivel: true,
        configurada: true,
        valor: 'chave-prod-de-teste',
        status: 'ATIVO',
      },
      {
        chave: 'stone.prod.accountId',
        sensivel: false,
        configurada: true,
        valor: 'conta-atual',
        status: 'ATIVO',
      },
    ],
  }
  consultarServico.mockImplementation((servico) =>
    Promise.resolve(servico === 'STONE' ? resposta : infobip()),
  )
  atualizarServico.mockResolvedValue(undefined)
  const gravacao = vi.spyOn(Storage.prototype, 'setItem')
  const pagina = mount(SistemaPagina)
  try {
    await flushPromises()
    const formulario = pagina.get('form[aria-labelledby=servico-stone]')
    for (const [id, valor] of [
      ['#servico-stone-dev-secretkey', 'chave-dev-de-teste'],
      ['#servico-stone-prod-secretkey', 'chave-prod-de-teste'],
    ]) {
      const campo = formulario.get<HTMLInputElement>(id).element
      expect(campo.type).toBe('text')
      campo.select()
      expect(campo.value.slice(campo.selectionStart!, campo.selectionEnd!)).toBe(valor)
    }
    await formulario.get('#servico-stone-dev-secretkey').setValue(' ')
    await formulario.get('#servico-stone-prod-accountid').setValue('nova-conta')
    await formulario.trigger('submit')
    await flushPromises()
    expect(atualizarServico).toHaveBeenCalledExactlyOnceWith('STONE', {
      'stone.prod.accountId': 'nova-conta',
    })
    expect(formulario.get<HTMLInputElement>('#servico-stone-dev-secretkey').element.value).toBe(
      'chave-dev-de-teste',
    )
    expect(gravacao).not.toHaveBeenCalled()
  } finally {
    gravacao.mockRestore()
    pagina.unmount()
  }
})

it('restaura o estado salvo quando o backend rejeita a alteração do toggle', async () => {
  alterarDevMode.mockRejectedValue(new ErroApi('Indisponível', 503))
  const pagina = mount(SistemaPagina)
  await flushPromises()
  const ambiente = pagina.get('[aria-labelledby=titulo-ambiente]')
  const toggle = ambiente.get<HTMLInputElement>('[role=switch]')
  await toggle.setValue(false)
  await flushPromises()
  expect(alterarDevMode).toHaveBeenCalledExactlyOnceWith(false)
  expect(toggle.element.checked).toBe(true)
  expect(ambiente.text()).toContain('Desenvolvimento (DEV)')
  expect(ambiente.get('[role=alert]').text()).toContain('Não foi possível atualizar')
  pagina.unmount()
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
        ['#servico-stone-dev-secretkey', chaveStone],
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
    ['STONE', 'stone.dev.secretKey', '#servico-stone-dev-secretkey'],
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
    expect(pagina.find('#servico-stone-dev-secretkey').exists()).toBe(false)
    expect(pagina.text()).toContain('Não foi possível consultar as configurações deste serviço.')
    expect(pagina.text()).not.toContain('mensagem privada omitida')
    pagina.unmount()
  })

  it('mostra erro seguro se o servidor recusar uma alteração', async () => {
    atualizarServico.mockRejectedValue(new ErroApi('não exibir conteúdo sensível', 503))
    const pagina = mount(SistemaPagina)
    await flushPromises()
    const formulario = pagina.get('form[aria-labelledby=servico-stone]')
    await formulario.get('#servico-stone-dev-secretkey').setValue('teste-ficticia')
    await formulario.trigger('submit')
    await flushPromises()
    expect(formulario.text()).toContain('Configure a chave mestra no backend')
    expect(formulario.text()).not.toContain('não exibir conteúdo sensível')
    pagina.unmount()
  })
})
