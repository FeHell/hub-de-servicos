import { computed, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ErroApi } from '@/services/clienteHttp'
import {
  opcoesStatus,
  opcoesTipo,
  type FiltrosUsuarios,
  type Pagina,
  type UsuarioListagem,
} from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'

const filtrosVazios: FiltrosUsuarios = { login: '', email: '', status: '', tipo: '' }

export function usePesquisaUsuarios() {
  const rota = useRoute()
  const router = useRouter()
  const api = useUsuariosApi()
  const filtros = reactive({ ...filtrosVazios })
  const dados = ref<Pagina<UsuarioListagem>>()
  const carregando = ref(true)
  const erro = ref('')
  let consultaAtual = 0

  const aplicados = computed<FiltrosUsuarios>(() => ({
    login: typeof rota.query.login === 'string' ? rota.query.login : '',
    email: typeof rota.query.email === 'string' ? rota.query.email : '',
    status: opcoesStatus.find(({ valor }) => valor === rota.query.status)?.valor ?? '',
    tipo: opcoesTipo.find(({ valor }) => valor === rota.query.tipo)?.valor ?? '',
  }))
  const paginaAtual = computed(() => {
    const pagina = Number(rota.query.pagina ?? 0)
    return Number.isInteger(pagina) && pagina >= 0 && pagina <= 2147483647 ? pagina : 0
  })
  const paginas = computed(() =>
    dados.value ? Math.max(1, Math.ceil(dados.value.total / dados.value.tamanho)) : 1,
  )

  async function carregar() {
    const consulta = ++consultaAtual
    carregando.value = true
    erro.value = ''
    try {
      const resposta = await api.pesquisar(aplicados.value, paginaAtual.value)
      if (consulta !== consultaAtual || rota.name !== 'usuarios') return
      const ultima = Math.max(0, Math.ceil(resposta.total / resposta.tamanho) - 1)
      if (resposta.pagina > ultima) {
        await navegar(aplicados.value, ultima)
        return
      }
      dados.value = resposta
    } catch (falha) {
      if (consulta === consultaAtual) {
        erro.value =
          falha instanceof ErroApi ? falha.message : 'Não foi possível consultar os usuários.'
      }
    } finally {
      if (consulta === consultaAtual) carregando.value = false
    }
  }

  async function navegar(consulta: FiltrosUsuarios, pagina = 0) {
    const destino = {
      name: 'usuarios',
      query: {
        login: consulta.login.trim() || undefined,
        email: consulta.email.trim() || undefined,
        tipo: consulta.tipo || undefined,
        status: consulta.status || undefined,
        pagina: pagina ? String(pagina) : undefined,
      },
    }
    if (router.resolve(destino).fullPath === rota.fullPath) {
      Object.assign(filtros, aplicados.value)
      await carregar()
    } else {
      await router.replace(destino)
    }
  }

  watch(
    () => rota.fullPath,
    () => {
      if (rota.name !== 'usuarios') return
      Object.assign(filtros, aplicados.value)
      void carregar()
    },
    { immediate: true },
  )

  return {
    filtros,
    dados,
    carregando,
    erro,
    paginas,
    paginaAtual,
    carregar,
    pesquisar: () => navegar(filtros),
    limpar: () => navegar(filtrosVazios),
    mudarPagina: (pagina: number) => navegar(aplicados.value, pagina),
  }
}
