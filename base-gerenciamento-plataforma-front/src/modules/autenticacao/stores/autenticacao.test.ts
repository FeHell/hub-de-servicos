import { beforeEach, afterEach, describe, expect, it, vi } from 'vitest'
import { createPinia, setActivePinia } from 'pinia'
import { autenticacaoApi } from '../services/autenticacaoApi'
import { useAutenticacao } from './autenticacao'
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

function novosTokens() {
  return {
    tokenAcesso: 'access-de-teste',
    expiracaoTokenAcesso: new Date(Date.now() + 300_000).toISOString(),
  }
}

describe('sessão de autenticação', () => {
  beforeEach(() => {
    vi.useFakeTimers()
    setActivePinia(createPinia())
    vi.mocked(autenticacaoApi.login).mockResolvedValue(novosTokens())
    vi.mocked(autenticacaoApi.usuario).mockResolvedValue({
      id: 'usuario-teste',
      login: 'fernando',
      email: 'fernando@example.test',
      idAplicacao: 'aplicacao-teste',
      aplicacao: 'plataforma-admin',
    })
    vi.mocked(autenticacaoApi.permissoes).mockResolvedValue(['PLATAFORMA_ADMINISTRAR'])
    vi.mocked(autenticacaoApi.logout).mockResolvedValue(undefined)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.resetAllMocks()
  })

  it('confirma usuário e permissões e mantém os tokens fora do armazenamento persistente', async () => {
    const gravar = vi.spyOn(Storage.prototype, 'setItem')
    const sessao = useAutenticacao()
    await sessao.entrar(' fernando ', 'senha-teste')
    expect(autenticacaoApi.login).toHaveBeenCalledWith('fernando', 'senha-teste')
    expect(sessao.autenticado).toBe(true)
    expect(sessao.permissoes).toEqual(['PLATAFORMA_ADMINISTRAR'])
    expect(gravar).not.toHaveBeenCalled()
    await sessao.sair()
    expect(autenticacaoApi.logout).toHaveBeenCalledOnce()
    expect(sessao.autenticado).toBe(false)
  })

  it('compartilha a mesma renovação para chamadas simultâneas', async () => {
    const sessao = useAutenticacao()
    await sessao.entrar('fernando', 'senha-teste')
    let resolver!: (tokens: ReturnType<typeof novosTokens>) => void
    vi.mocked(autenticacaoApi.renovar).mockReturnValue(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )
    const primeira = sessao.renovarSessao()
    const segunda = sessao.renovarSessao()
    expect(autenticacaoApi.renovar).toHaveBeenCalledTimes(1)
    resolver(novosTokens())
    await Promise.all([primeira, segunda])
    expect(sessao.autenticado).toBe(true)
  })

  it('encerra a sessão se a renovação falhar', async () => {
    const sessao = useAutenticacao()
    await sessao.entrar('fernando', 'senha-teste')
    vi.mocked(autenticacaoApi.renovar).mockRejectedValue(new Error('falha de teste'))
    await expect(sessao.renovarSessao()).rejects.toThrow()
    expect(sessao.autenticado).toBe(false)
    expect(sessao.aviso).toContain('Entre novamente')
  })

  it('renova automaticamente antes de o access token expirar', async () => {
    const sessao = useAutenticacao()
    await sessao.entrar('fernando', 'senha-teste')
    vi.mocked(autenticacaoApi.renovar).mockImplementation(async () => novosTokens())
    await vi.advanceTimersByTimeAsync(270_000)
    expect(autenticacaoApi.renovar).toHaveBeenCalledTimes(1)
    expect(sessao.autenticado).toBe(true)
  })

  it('recusa uma identidade devolvida no contexto de outra aplicação', async () => {
    vi.mocked(autenticacaoApi.usuario).mockResolvedValue({
      id: 'usuario-teste',
      login: 'fernando',
      email: 'fernando@example.test',
      idAplicacao: 'outra-aplicacao',
      aplicacao: 'outra-aplicacao',
    })
    const sessao = useAutenticacao()
    await expect(sessao.entrar('fernando', 'senha-teste')).rejects.toThrow('confirmar seu acesso')
    expect(sessao.autenticado).toBe(false)
    expect(sessao.permissoes).toEqual([])
  })

  it('aguarda a rotação em andamento antes do logout', async () => {
    const sessao = useAutenticacao()
    await sessao.entrar('fernando', 'senha-teste')
    let resolver!: (tokens: ReturnType<typeof novosTokens>) => void
    vi.mocked(autenticacaoApi.renovar).mockReturnValue(
      new Promise((resolve) => {
        resolver = resolve
      }),
    )
    const renovacao = sessao.renovarSessao()
    const saida = sessao.sair()
    expect(autenticacaoApi.logout).not.toHaveBeenCalled()
    resolver({ ...novosTokens(), tokenAcesso: 'access-rotacionado' })
    await Promise.all([renovacao, saida])
    expect(autenticacaoApi.logout).toHaveBeenCalledOnce()
    expect(sessao.autenticado).toBe(false)
  })

  it('restaura a sessão sem credenciais digitadas e reutiliza a verificação inicial', async () => {
    vi.mocked(autenticacaoApi.renovar).mockResolvedValue(novosTokens())
    const sessao = useAutenticacao()
    const restauracao = sessao.restaurarSessao()
    expect(sessao.restaurando).toBe(true)
    await Promise.all([restauracao, sessao.restaurarSessao()])
    expect(autenticacaoApi.renovar).toHaveBeenCalledOnce()
    expect(autenticacaoApi.renovar).toHaveBeenCalledWith()
    expect(autenticacaoApi.login).not.toHaveBeenCalled()
    expect(sessao.usuario?.login).toBe('fernando')
    expect(sessao.permissoes).toEqual(['PLATAFORMA_ADMINISTRAR'])
    expect(sessao.restaurando).toBe(false)
  })

  it('permanece deslogado quando não há cookie válido', async () => {
    vi.mocked(autenticacaoApi.renovar).mockRejectedValue(new ErroApi('Sessão inválida', 401))
    const sessao = useAutenticacao()
    await sessao.restaurarSessao()
    expect(sessao.autenticado).toBe(false)
    expect(sessao.restaurando).toBe(false)
    expect(sessao.aviso).toBe('')
    expect(autenticacaoApi.usuario).not.toHaveBeenCalled()
  })

  it('avisa e permite repetir o logout se o servidor não confirmar o encerramento', async () => {
    const sessao = useAutenticacao()
    await sessao.entrar('fernando', 'senha-teste')
    vi.mocked(autenticacaoApi.logout).mockRejectedValueOnce(new Error('falha de teste'))
    expect(await sessao.sair()).toBe(false)
    expect(sessao.autenticado).toBe(true)
    expect(sessao.aviso).toContain('Tente sair novamente')
    expect(await sessao.sair()).toBe(true)
    expect(sessao.autenticado).toBe(false)
  })
})
