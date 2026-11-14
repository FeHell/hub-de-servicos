type CatalogoEnum = Readonly<Record<string, string>>

export const rotulosEnum = {
  statusUsuario: {
    ATIVO: 'Ativo',
    BLOQUEADO: 'Bloqueado',
    PENDENTE: 'Pendente',
    INATIVO: 'Inativo',
  },
  statusCadastro: {
    ATIVO: 'Ativo',
    INATIVO: 'Inativo',
  },
  statusPermissao: {
    ATIVO: 'Ativa',
    INATIVO: 'Inativa',
  },
  tipoAcesso: {
    PLATAFORMA: 'Administrador da plataforma',
    APLICACAO: 'Administrador de aplicação',
    USUARIO: 'Usuário',
  },
  provedorSms: {
    TEXTBEE: 'TextBee',
  },
  situacaoTelefone: {
    NAO_INFORMADO: 'Não informado',
    PENDENTE: 'Pendente de confirmação',
    VERIFICADO: 'Verificado',
  },
  permissao: {
    PLATAFORMA_EMPRESA: 'Empresa',
    PLATAFORMA_TRABALHADOR: 'Trabalhador',
    PLATAFORMA_ADMINISTRAR: 'Administrador da plataforma',
    PLATAFORMA_ANALISTA: 'Analista da plataforma',
    PLATAFORMA_VISITANTE: 'Visitante da plataforma',
    APLICACAO_ADMINISTRAR: 'Administrador de aplicação',
  },
} as const satisfies Record<string, CatalogoEnum>

type GrupoEnum = keyof typeof rotulosEnum
export type ValorEnum<G extends GrupoEnum> = Extract<keyof (typeof rotulosEnum)[G], string>

const ausentes = new Set<string>()

export function traduzirEnum(grupo: GrupoEnum, valor: string, nomeCatalogado?: string | null) {
  const catalogo: CatalogoEnum = rotulosEnum[grupo]
  if (Object.hasOwn(catalogo, valor)) return catalogo[valor]!

  const nome = nomeCatalogado?.trim()
  if (nome && nome.toUpperCase() !== valor.toUpperCase()) return nome

  const chave = `${grupo}.${valor}`
  if (import.meta.env.DEV && !ausentes.has(chave)) {
    ausentes.add(chave)
    console.warn(`Tradução de enum ausente: ${chave}`)
  }
  return grupo === 'permissao' ? 'Permissão sem rótulo' : 'Não identificado'
}

export function opcoesEnum<G extends GrupoEnum>(grupo: G) {
  const valores = Object.keys(rotulosEnum[grupo]) as ValorEnum<G>[]
  return valores.map((valor) => ({ valor, texto: traduzirEnum(grupo, valor) }))
}
