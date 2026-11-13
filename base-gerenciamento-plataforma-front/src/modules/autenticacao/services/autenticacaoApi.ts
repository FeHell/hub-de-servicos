import { ambiente } from '@/app/config/ambiente'
import { requisitar } from '@/services/clienteHttp'
import type { AcessoNavegador, UsuarioAutenticado } from '../types/autenticacao'

const prefixo = `/aplicacoes/${ambiente.slugAplicacao}/autenticacao`

function requisitarSessao<T>(acao: 'login' | 'renovar' | 'logout', corpo?: unknown) {
  const executar = () =>
    requisitar<T>(`${prefixo}/navegador/${acao}`, { metodo: 'POST', corpo, sessaoNavegador: true })

  // O navegador compartilha o cookie entre abas. Uma renovação precisa terminar
  // antes que outra envie o cookie rotacionado.
  return navigator.locks
    ? navigator.locks.request(`plataforma-sessao:${ambiente.slugAplicacao}`, executar)
    : executar()
}

export const autenticacaoApi = {
  login(login: string, senha: string) {
    return requisitarSessao<AcessoNavegador>('login', { login, senha })
  },
  renovar() {
    return requisitarSessao<AcessoNavegador>('renovar')
  },
  usuario(token: string) {
    return requisitar<UsuarioAutenticado>(`${prefixo}/eu`, { token })
  },
  permissoes(token: string) {
    return requisitar<string[]>(`${prefixo}/permissoes`, { token })
  },
  logout() {
    return requisitarSessao<void>('logout')
  },
}
