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

beforeEach(() => {
  vi.resetAllMocks()
  provedores.mockResolvedValue([{ codigo: 'INFOBIP', nome: 'Infobip' }])
  consultar.mockResolvedValue({ valor: 'INFOBIP', status: 'ATIVO' })
  vi.mocked(useConfiguracaoApi).mockReturnValue({
    listarProvedoresSms: provedores,
    consultarProvedorSms: consultar,
    alterarProvedorSms: salvar,
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
