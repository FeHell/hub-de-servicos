<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CabecalhoPagina from '@/components/ui/CabecalhoPagina.vue'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import { ErroApi } from '@/services/clienteHttp'
import UsuarioFormulario from '../components/UsuarioFormulario.vue'
import UsuarioPermissoes from '../components/UsuarioPermissoes.vue'
import type { Usuario, UsuarioListagem } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'

const rota = useRoute()
const router = useRouter()
const autenticacao = useAutenticacao()
const api = useUsuariosApi()
const id = computed(() => (typeof rota.params.id === 'string' ? rota.params.id : ''))
const usuario = ref<UsuarioListagem>()
const carregando = ref(false)
const erro = ref('')
const aviso = ref('')
let consultaAtual = 0

async function carregar() {
  const consulta = ++consultaAtual
  const alvo = id.value
  usuario.value = undefined
  erro.value = ''
  carregando.value = Boolean(alvo)
  if (!alvo) return
  try {
    const resposta = await api.consultar(alvo)
    if (consulta === consultaAtual) usuario.value = resposta
  } catch (falha) {
    if (consulta === consultaAtual) {
      erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível carregar o usuário.'
    }
  } finally {
    if (consulta === consultaAtual) carregando.value = false
  }
}
watch(
  id,
  () => {
    aviso.value = window.history.state?.usuarioCriado === true ? 'Usuário criado.' : ''
    void carregar()
  },
  { immediate: true },
)

function voltar() {
  return router.push({ name: 'usuarios', query: rota.query })
}

async function salvo(dados: Usuario) {
  if (!id.value) {
    await router.replace({
      name: 'usuarios-editar',
      params: { id: dados.id },
      query: rota.query,
      state: { usuarioCriado: true },
    })
    return
  }
  const permissoesAnteriores = usuario.value?.permissoesPlataforma ?? []
  const contaPropria = dados.id === autenticacao.usuario?.id
  if (usuario.value) usuario.value = { ...usuario.value, ...dados }
  aviso.value = 'Usuário atualizado.'
  if (
    contaPropria &&
    [...permissoesAnteriores].sort().join(',') !== [...dados.permissoesPlataforma].sort().join(',')
  ) {
    try {
      await autenticacao.renovarSessao()
    } catch {
      await router.replace({ name: 'login' })
      return
    }
    if (!autenticacao.temPermissao('PLATAFORMA_ADMINISTRAR')) {
      await router.replace({ name: 'inicio' })
      return
    }
  }
  await atualizarPermissoes()
}

async function atualizarPermissoes() {
  const alvo = id.value
  if (!alvo) return
  const consulta = ++consultaAtual
  try {
    const resposta = await api.consultar(alvo)
    if (id.value === alvo && consulta === consultaAtual) usuario.value = resposta
  } catch {
    if (id.value === alvo && consulta === consultaAtual)
      aviso.value = 'Alterações salvas. Reabra a página para atualizar as permissões efetivas.'
  }
}
</script>

<template>
  <CabecalhoPagina
    :titulo="id ? 'Editar usuário' : 'Novo usuário'"
    :descricao="
      id
        ? 'Atualize os dados e as permissões do usuário.'
        : 'Preencha os dados para criar uma conta.'
    "
  >
    <BotaoBase variante="discreto" @click="voltar">Voltar à pesquisa</BotaoBase>
  </CabecalhoPagina>
  <p v-if="aviso" class="aviso" role="status">{{ aviso }}</p>
  <p v-if="carregando" role="status">Carregando usuário…</p>
  <div v-else-if="erro" class="estado">
    <p role="alert">{{ erro }}</p>
    <BotaoBase variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
  </div>
  <div v-else class="secoes">
    <UsuarioFormulario
      :key="id"
      :usuario="usuario"
      :conta-propria="usuario?.id === autenticacao.usuario?.id"
      @cancelar="voltar"
      @salvo="salvo"
    />
    <!-- <UsuarioPermissoes
      v-if="usuario"
      :key="usuario.id"
      :usuario="usuario"
      @alterado="atualizarPermissoes"
    /> -->
  </div>
</template>

<style scoped>
.secoes {
  display: grid;
  gap: var(--espaco-6);
}
.estado {
  display: grid;
  justify-items: start;
  gap: var(--espaco-3);
  padding: var(--espaco-5);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
  box-shadow: var(--sombra-card);
}
.estado p {
  color: var(--cor-texto-secundario);
}
.aviso {
  margin-bottom: var(--espaco-4);
  padding: var(--espaco-3) var(--espaco-4);
  border-radius: var(--raio-sm);
  color: var(--cor-sucesso);
  background: var(--cor-primaria-suave);
}
</style>
