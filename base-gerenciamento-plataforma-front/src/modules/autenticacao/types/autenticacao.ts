export interface AcessoNavegador {
  tokenAcesso: string
  expiracaoTokenAcesso: string
}

export interface UsuarioAutenticado {
  id: string
  login: string
  email: string
  idAplicacao: string
  aplicacao: string
}
