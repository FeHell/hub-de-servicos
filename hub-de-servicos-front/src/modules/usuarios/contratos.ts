import { opcoesEnum, type ValorEnum } from '@/shared/traducoes/enums'

export type StatusUsuario = ValorEnum<'statusUsuario'>
export type TipoAcesso = ValorEnum<'tipoAcesso'>
type StatusCadastro = ValorEnum<'statusCadastro'>

export interface Usuario {
  id: string
  login: string
  email: string
  status: StatusUsuario
  dataCriacao: string
  permissoesPlataforma: string[]
  telefone?: string | null
  situacaoTelefone?: ValorEnum<'situacaoTelefone'>
}

export interface UsuarioListagem extends Usuario {
  tipo: TipoAcesso
  permissoes: { aplicacao: string; codigo: string }[]
}

export interface Pagina<T> {
  itens: T[]
  pagina: number
  tamanho: number
  total: number
}

export interface FiltrosUsuarios {
  login: string
  email: string
  status: StatusUsuario | ''
  tipo: TipoAcesso | ''
}

export interface DashboardUsuarios {
  total: number
  recentes: number
  porTipo: { tipo: TipoAcesso; total: number }[]
  porPermissao: { aplicacao: string; codigo: string; total: number }[]
  porStatus: { status: StatusUsuario; total: number }[]
  porMes: { mes: string; total: number }[]
}

export interface NovoUsuario {
  login: string
  email: string
  senha: string
  status: StatusUsuario
  permissoesPlataforma: string[]
  telefone?: string | null
}

export type AtualizarUsuario = Pick<Usuario, 'login' | 'email' | 'status'> &
  Partial<Pick<NovoUsuario, 'permissoesPlataforma' | 'telefone'>>

export const opcoesStatus = opcoesEnum('statusUsuario')
export const opcoesTipo = opcoesEnum('tipoAcesso')

export interface AplicacaoAcesso {
  id: string
  slug: string
  nome: string
  status: StatusCadastro
}

export interface PerfilAcesso {
  id: string
  nome: string
  descricao: string | null
  status: StatusCadastro
}

export interface PermissaoAcesso {
  id: string
  codigo: string
  descricao: string | null
  status: StatusCadastro
}

export interface PermissaoPlataforma extends PermissaoAcesso {
  nome: string
}
