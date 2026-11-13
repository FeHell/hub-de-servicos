import { computed, onMounted, ref, watch } from 'vue'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import { ErroApi } from '@/services/clienteHttp'
import type { AplicacaoAcesso, PerfilAcesso, PermissaoAcesso } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'

export function usePermissoesUsuario(id: string, aoAlterar: () => void) {
  const api = useUsuariosApi()
  const autenticacao = useAutenticacao()
  const aplicacoes = ref<AplicacaoAcesso[]>([])
  const slug = ref('')
  const perfis = ref<PerfilAcesso[]>([])
  const permissoes = ref<PermissaoAcesso[]>([])
  const atribuidos = ref<PerfilAcesso[]>([])
  const vinculado = ref(false)
  const carregandoAplicacoes = ref(true)
  const carregando = ref(false)
  const erroAplicacoes = ref('')
  const erro = ref('')
  const aviso = ref('')
  const operacao = ref('')
  const detalhes = ref<{ id: string; dados?: PermissaoAcesso[]; erro?: string }>()
  let consulta = 0

  const aplicacao = computed(() => aplicacoes.value.find((item) => item.slug === slug.value))
  const editavel = computed(
    () =>
      aplicacao.value?.status === 'ATIVO' &&
      aplicacao.value.id !== autenticacao.usuario?.idAplicacao,
  )
  const opcoesAplicacao = computed(() => [
    { valor: '', texto: 'Selecione uma aplicação' },
    ...aplicacoes.value.map((app) => ({ valor: app.slug, texto: app.nome })),
  ])

  async function carregarAplicacoes() {
    carregandoAplicacoes.value = true
    erroAplicacoes.value = ''
    try {
      aplicacoes.value = (await api.listarAplicacoes()).filter(
        (app) => app.id !== autenticacao.usuario?.idAplicacao && app.slug !== 'plataforma-admin',
      )
    } catch (falha) {
      erroAplicacoes.value =
        falha instanceof ErroApi ? falha.message : 'Não foi possível listar as aplicações.'
    } finally {
      carregandoAplicacoes.value = false
    }
  }

  async function carregarAcessos() {
    const atual = ++consulta
    const alvo = slug.value
    detalhes.value = undefined
    perfis.value = []
    atribuidos.value = []
    vinculado.value = false
    permissoes.value = []
    erro.value = ''
    carregando.value = Boolean(alvo)
    if (!alvo) return
    try {
      const [perfisDisponiveis, catalogo, perfisDoUsuario] = await Promise.all([
        api.listarPerfis(alvo),
        api.listarPermissoes(alvo),
        api.listarPerfisDoUsuario(alvo, id).catch((falha) => {
          if (falha instanceof ErroApi && falha.status === 404) return null
          throw falha
        }),
      ])
      if (atual !== consulta) return
      perfis.value = perfisDisponiveis
      permissoes.value = catalogo
      vinculado.value = perfisDoUsuario !== null
      atribuidos.value = perfisDoUsuario ?? []
    } catch (falha) {
      if (atual === consulta)
        erro.value =
          falha instanceof ErroApi ? falha.message : 'Não foi possível consultar os perfis.'
    } finally {
      if (atual === consulta) carregando.value = false
    }
  }

  function atribuido(perfil: PerfilAcesso) {
    return atribuidos.value.some((item) => item.id === perfil.id)
  }

  async function vincular() {
    if (operacao.value || !editavel.value || !slug.value) return
    operacao.value = 'vincular'
    erro.value = ''
    aviso.value = ''
    try {
      await api.vincular(slug.value, id)
      aviso.value = 'Usuário vinculado. Agora você pode atribuir perfis.'
      await carregarAcessos()
      aoAlterar()
    } catch (falha) {
      erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível vincular o usuário.'
    } finally {
      operacao.value = ''
    }
  }

  async function alterarPerfil(perfil: PerfilAcesso) {
    if (operacao.value || !vinculado.value || !editavel.value) return
    const atribuir = !atribuido(perfil)
    operacao.value = perfil.id
    erro.value = ''
    aviso.value = ''
    try {
      await api.alterarPerfil(slug.value, id, perfil.id, atribuir)
      atribuidos.value = atribuir
        ? [...atribuidos.value, perfil]
        : atribuidos.value.filter((item) => item.id !== perfil.id)
      aviso.value = atribuir ? 'Perfil atribuído.' : 'Perfil removido.'
      aoAlterar()
    } catch (falha) {
      erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível alterar o perfil.'
    } finally {
      operacao.value = ''
    }
  }

  async function mostrarDetalhes(perfil: PerfilAcesso) {
    if (detalhes.value?.id === perfil.id) {
      detalhes.value = undefined
      return
    }
    const alvo = { id: perfil.id }
    detalhes.value = alvo
    const aplicacaoAtual = slug.value
    try {
      const dados = await api.listarPermissoesDoPerfil(aplicacaoAtual, perfil.id)
      if (slug.value === aplicacaoAtual && detalhes.value?.id === perfil.id) {
        detalhes.value = { ...alvo, dados }
      }
    } catch (falha) {
      if (slug.value === aplicacaoAtual && detalhes.value?.id === perfil.id) {
        detalhes.value = {
          ...alvo,
          erro: falha instanceof ErroApi ? falha.message : 'Não foi possível consultar o perfil.',
        }
      }
    }
  }

  watch(slug, () => {
    aviso.value = ''
    void carregarAcessos()
  })
  onMounted(carregarAplicacoes)

  return {
    slug,
    perfis,
    permissoes,
    vinculado,
    editavel,
    opcoesAplicacao,
    carregandoAplicacoes,
    carregando,
    erroAplicacoes,
    erro,
    aviso,
    operacao,
    detalhes,
    carregarAplicacoes,
    carregarAcessos,
    atribuido,
    vincular,
    alterarPerfil,
    mostrarDetalhes,
  }
}
