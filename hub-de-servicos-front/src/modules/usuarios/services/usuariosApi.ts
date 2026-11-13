import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import type {
  AplicacaoAcesso,
  AtualizarUsuario,
  DashboardUsuarios,
  FiltrosUsuarios,
  NovoUsuario,
  Pagina,
  PerfilAcesso,
  PermissaoAcesso,
  PermissaoPlataforma,
  Usuario,
  UsuarioListagem,
} from '../contratos'

const base = '/admin/usuarios'
const aplicacao = (slug: string) => `/aplicacoes/${encodeURIComponent(slug)}/admin`
const vinculo = (slug: string, id: string) =>
  `${aplicacao(slug)}/usuarios/${encodeURIComponent(id)}`

export function useUsuariosApi() {
  const { requisitarAutenticado: requisitar } = useAutenticacao()

  async function listarTodas<T>(caminho: string) {
    const primeira = await requisitar<Pagina<T>>(`${caminho}?pagina=0&tamanho=100`)
    const itens = [...primeira.itens]
    for (let pagina = 1; pagina * primeira.tamanho < primeira.total; pagina++) {
      const proxima = await requisitar<Pagina<T>>(`${caminho}?pagina=${pagina}&tamanho=100`)
      itens.push(...proxima.itens)
    }
    return itens
  }

  return {
    pesquisar(filtros: FiltrosUsuarios, pagina: number) {
      const consulta = new URLSearchParams({ pagina: String(pagina) })
      for (const [campo, valor] of Object.entries(filtros)) {
        if (valor) consulta.set(campo, valor)
      }
      return requisitar<Pagina<UsuarioListagem>>(`${base}?${consulta}`)
    },
    consultar(id: string) {
      return requisitar<UsuarioListagem>(`${base}/${encodeURIComponent(id)}`)
    },
    criar(dados: NovoUsuario) {
      return requisitar<Usuario>(base, { metodo: 'POST', corpo: dados })
    },
    atualizar(id: string, dados: AtualizarUsuario) {
      return requisitar<Usuario>(`${base}/${encodeURIComponent(id)}`, {
        metodo: 'PUT',
        corpo: dados,
      })
    },
    excluir(id: string) {
      return requisitar<void>(`${base}/${encodeURIComponent(id)}`, { metodo: 'DELETE' })
    },
    dashboard(meses: number) {
      return requisitar<DashboardUsuarios>(`${base}/dashboard?meses=${meses}`)
    },
    listarPermissoesPlataforma() {
      return requisitar<PermissaoPlataforma[]>('/admin/permissoes-plataforma')
    },
    listarAplicacoes() {
      return listarTodas<AplicacaoAcesso>('/admin/aplicacoes')
    },
    listarPerfis(slug: string) {
      return listarTodas<PerfilAcesso>(`${aplicacao(slug)}/perfis`)
    },
    listarPerfisDoUsuario(slug: string, id: string) {
      return requisitar<PerfilAcesso[]>(`${vinculo(slug, id)}/perfis`)
    },
    listarPermissoes(slug: string) {
      return requisitar<PermissaoAcesso[]>(`${aplicacao(slug)}/permissoes`)
    },
    listarPermissoesDoPerfil(slug: string, idPerfil: string) {
      return requisitar<PermissaoAcesso[]>(
        `${aplicacao(slug)}/perfis/${encodeURIComponent(idPerfil)}/permissoes`,
      )
    },
    vincular(slug: string, id: string) {
      return requisitar<void>(vinculo(slug, id), { metodo: 'PUT' })
    },
    alterarPerfil(slug: string, id: string, idPerfil: string, atribuir: boolean) {
      return requisitar<void>(`${vinculo(slug, id)}/perfis/${encodeURIComponent(idPerfil)}`, {
        metodo: atribuir ? 'PUT' : 'DELETE',
      })
    },
  }
}
