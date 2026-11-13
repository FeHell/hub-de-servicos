import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { ambiente } from '@/app/config/ambiente'
import { ErroApi, requisitar, type OpcoesRequisicao } from '@/services/clienteHttp'
import { autenticacaoApi } from '../services/autenticacaoApi'
import type { AcessoNavegador, UsuarioAutenticado } from '../types/autenticacao'

export const useAutenticacao = defineStore('autenticacao', () => {
  const usuario = ref<UsuarioAutenticado | null>(null)
  const permissoes = ref<string[]>([])
  const enviando = ref(false)
  const saindo = ref(false)
  const restaurando = ref(false)
  const aviso = ref('')
  const autenticado = computed(() => usuario.value !== null)

  let temporizador: ReturnType<typeof setTimeout> | undefined
  let renovacao: Promise<void> | null = null
  let restauracao: Promise<void> | undefined
  let geracao = 0
  let expiracaoAcesso = 0
  let tokenAcesso = ''

  function limpar(mensagem = '') {
    geracao += 1
    clearTimeout(temporizador)
    renovacao = null
    expiracaoAcesso = 0
    tokenAcesso = ''
    usuario.value = null
    permissoes.value = []
    aviso.value = mensagem
  }

  async function confirmarSessao(resposta: AcessoNavegador, atual: number) {
    const expira = Date.parse(resposta.expiracaoTokenAcesso)
    if (!resposta.tokenAcesso || !Number.isFinite(expira) || expira <= Date.now()) {
      throw new ErroApi('O servidor retornou uma sessão inválida. Tente entrar novamente.', 502)
    }
    const [identidade, codigos] = await Promise.all([
      autenticacaoApi.usuario(resposta.tokenAcesso),
      autenticacaoApi.permissoes(resposta.tokenAcesso),
    ])
    if (atual !== geracao) return
    if (
      identidade.aplicacao !== ambiente.slugAplicacao ||
      !identidade.id ||
      !Array.isArray(codigos)
    ) {
      throw new ErroApi('Não foi possível confirmar seu acesso à aplicação.', 401)
    }
    usuario.value = identidade
    permissoes.value = codigos
    expiracaoAcesso = expira
    tokenAcesso = resposta.tokenAcesso
    agendarRenovacao()
  }

  function agendarRenovacao() {
    clearTimeout(temporizador)
    temporizador = setTimeout(
      () => void renovarSessao().catch(() => undefined),
      Math.max(1000, expiracaoAcesso - Date.now() - 30_000),
    )
  }

  async function renovarSessao(): Promise<void> {
    if (renovacao) return renovacao
    const atual = geracao
    const tarefa = (async () => {
      try {
        const resposta = await autenticacaoApi.renovar()
        if (atual === geracao) await confirmarSessao(resposta, atual)
      } catch (erro) {
        if (atual === geracao) {
          const mensagem = autenticado.value
            ? 'Sua sessão terminou. Entre novamente para continuar.'
            : erro instanceof ErroApi && erro.status === 401
              ? ''
              : 'Não foi possível verificar sua sessão. Tente novamente.'
          limpar(mensagem)
        }
        throw erro
      }
    })()
    renovacao = tarefa
    try {
      await tarefa
    } finally {
      if (renovacao === tarefa) renovacao = null
    }
  }

  function restaurarSessao() {
    if (!restauracao) {
      restaurando.value = true
      restauracao = renovarSessao()
        .catch(() => undefined)
        .finally(() => {
          restaurando.value = false
        })
    }
    return restauracao
  }

  function temPermissao(codigo: string) {
    return permissoes.value.includes(codigo)
  }

  async function requisitarAutenticado<T>(
    caminho: string,
    opcoes: Pick<OpcoesRequisicao, 'metodo' | 'corpo'> = {},
  ): Promise<T> {
    const identidade = usuario.value
    const atual = geracao

    function confirmarIdentidade() {
      if (
        !identidade ||
        atual !== geracao ||
        saindo.value ||
        identidade.id !== usuario.value?.id ||
        identidade.idAplicacao !== usuario.value.idAplicacao
      ) {
        throw new ErroApi('Entre novamente para continuar.', 401)
      }
    }

    confirmarIdentidade()
    if (expiracaoAcesso <= Date.now()) await renovarSessao()
    confirmarIdentidade()
    const tokenEnviado = tokenAcesso
    try {
      return await requisitar<T>(caminho, { ...opcoes, token: tokenEnviado })
    } catch (erro) {
      if (!(erro instanceof ErroApi) || erro.status !== 401) throw erro
      confirmarIdentidade()
      if (tokenAcesso === tokenEnviado) await renovarSessao()
      confirmarIdentidade()
      return requisitar<T>(caminho, { ...opcoes, token: tokenAcesso })
    }
  }

  async function entrar(login: string, senha: string) {
    if (enviando.value || saindo.value) return
    enviando.value = true
    limpar()
    const atual = geracao
    try {
      const resposta = await autenticacaoApi.login(login.trim(), senha)
      if (atual === geracao) await confirmarSessao(resposta, atual)
    } catch (erro) {
      if (atual === geracao) limpar()
      throw erro
    } finally {
      enviando.value = false
    }
  }

  async function sair() {
    if (saindo.value) return false
    saindo.value = true
    clearTimeout(temporizador)
    try {
      if (renovacao) await renovacao.catch(() => undefined)
      clearTimeout(temporizador)
      await autenticacaoApi.logout()
      limpar()
      return true
    } catch {
      aviso.value = 'Não foi possível encerrar a sessão. Tente sair novamente.'
      if (autenticado.value) agendarRenovacao()
      return false
    } finally {
      saindo.value = false
    }
  }

  return {
    usuario,
    permissoes,
    enviando,
    saindo,
    restaurando,
    aviso,
    autenticado,
    temPermissao,
    requisitarAutenticado,
    entrar,
    sair,
    renovarSessao,
    restaurarSessao,
  }
})
