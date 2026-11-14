import type { RouteRecordRaw } from 'vue-router'
import type IconeBase from '@/components/ui/IconeBase.vue'

type Icone = InstanceType<typeof IconeBase>['$props']['nome']
type Tela = {
  nome: string
  caminho: string
  titulo: string
  componente: NonNullable<RouteRecordRaw['component']>
}

type TelaMenu = Tela & {
  icone: Icone
  permissao?: string
  internas?: Tela[]
}

type GrupoMenu = {
  id: string
  titulo: string
  icone: Icone
  telas: TelaMenu[]
}

const administrar = 'PLATAFORMA_ADMINISTRAR'
const cadastroUsuario = () => import('@/modules/usuarios/pages/UsuarioEdicao.vue')

export const atalhosMenu: TelaMenu[] = [
  {
    nome: 'inicio',
    caminho: '',
    titulo: 'Início',
    icone: 'inicio',
    componente: () => import('@/modules/inicio/pages/InicioPagina.vue'),
  },
  {
    nome: 'aparencia',
    caminho: 'configuracoes/aparencia',
    titulo: 'Aparência',
    icone: 'paleta',
    componente: () => import('@/modules/aparencia/pages/AparenciaPagina.vue'),
  },
]

export const gruposMenu: GrupoMenu[] = [
  {
    id: 'cadastros',
    titulo: 'Cadastros',
    icone: 'cadastros',
    telas: [
      {
        nome: 'usuarios',
        caminho: 'cadastros/usuarios',
        titulo: 'Usuários',
        icone: 'usuario',
        componente: () => import('@/modules/usuarios/pages/UsuariosPesquisa.vue'),
        internas: [
          {
            nome: 'usuarios-novo',
            caminho: 'cadastros/usuarios/novo',
            titulo: 'Novo usuário',
            componente: cadastroUsuario,
          },
          {
            nome: 'usuarios-editar',
            caminho: 'cadastros/usuarios/:id/editar',
            titulo: 'Editar usuário',
            componente: cadastroUsuario,
          },
        ],
        permissao: administrar,
      },
    ],
  },
  {
    id: 'relatorios',
    titulo: 'Relatórios',
    icone: 'grafico',
    telas: [
      {
        nome: 'dashboard-usuarios',
        caminho: 'relatorios/dashboard-usuarios',
        titulo: 'Dashboard Usuários',
        icone: 'grafico',
        componente: () => import('@/modules/relatorios/pages/DashboardUsuariosPagina.vue'),
        permissao: administrar,
      },
    ],
  },
  {
    id: 'configuracoes',
    titulo: 'Configurações',
    icone: 'configuracoes',
    telas: [
      {
        nome: 'configuracoes-sistema',
        caminho: 'configuracoes/sistema',
        titulo: 'Sistema',
        icone: 'configuracoes',
        componente: () => import('@/modules/configuracao/pages/SistemaPagina.vue'),
        permissao: administrar,
      },
    ],
  },
]

export const rotasAdministrativas: RouteRecordRaw[] = [
  ...atalhosMenu,
  ...gruposMenu.flatMap((grupo) => grupo.telas),
].flatMap((tela) =>
  [tela, ...(tela.internas ?? [])].map((rota) => ({
    path: rota.caminho,
    name: rota.nome,
    component: rota.componente,
    meta: { titulo: rota.titulo, permissao: tela.permissao, itemMenu: tela.nome },
  })),
)
