<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CabecalhoPagina from '@/components/ui/CabecalhoPagina.vue'
import CampoSelecao from '@/components/ui/CampoSelecao.vue'
import { ErroApi } from '@/services/clienteHttp'
import { traduzirEnum } from '@/shared/traducoes/enums'
import AmbienteGlobalFormulario from '../components/AmbienteGlobalFormulario.vue'
import ServicoExternoFormulario from '../components/ServicoExternoFormulario.vue'
import { useConfiguracaoApi, type ServicoExterno } from '../services/configuracaoApi'

const api = useConfiguracaoApi()
const opcoes = ref<{ valor: string; texto: string }[]>([])
const ativo = ref<string>()
const configuracaoAtiva = ref(false)
const selecionado = ref('')
const carregando = ref(false)
const salvando = ref(false)
const erro = ref('')
const aviso = ref('')
const nomeAtivo = computed(() => opcoes.value.find(({ valor }) => valor === ativo.value)?.texto)
const podeSalvar = computed(
  () =>
    configuracaoAtiva.value &&
    selecionado.value !== ativo.value &&
    opcoes.value.some(({ valor }) => valor === selecionado.value),
)

async function carregar() {
  carregando.value = true
  erro.value = ''
  try {
    const [provedores, configuracao] = await Promise.all([
      api.listarProvedoresSms(),
      api.consultarProvedorSms(),
    ])
    opcoes.value = provedores.map(({ codigo, nome }) => ({
      valor: codigo,
      texto: traduzirEnum('provedorSms', codigo, nome),
    }))
    ativo.value = configuracao.valor
    configuracaoAtiva.value = configuracao.status === 'ATIVO'
    selecionado.value = nomeAtivo.value ? configuracao.valor : ''
  } catch (falha) {
    erro.value =
      falha instanceof ErroApi ? falha.message : 'Não foi possível carregar as configurações.'
  } finally {
    carregando.value = false
  }
}

async function salvar() {
  if (!podeSalvar.value || salvando.value) return
  salvando.value = true
  erro.value = ''
  aviso.value = ''
  try {
    const configuracao = await api.alterarProvedorSms(selecionado.value)
    ativo.value = configuracao.valor
    configuracaoAtiva.value = configuracao.status === 'ATIVO'
    selecionado.value = configuracao.valor
    aviso.value = 'Provedor de SMS atualizado.'
  } catch (falha) {
    erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível salvar o provedor.'
  } finally {
    salvando.value = false
  }
}

const servicos = ref<ServicoExterno[]>([])
const carregandoServicos = ref(false)
const erroServicos = ref('')
const titulos: Record<string, string> = {
  INFOBIP: 'Infobip — Envio de SMS',
  STONE: 'Stone / Pagar.me — Integração financeira',
}
const campos: Record<string, string> = {
  INFOBIP_API_KEY: 'API Key',
  INFOBIP_BASE_URL: 'URL Base da API',
  'stone.dev.accountId': 'ID da conta',
  'stone.dev.publicKey': 'Chave pública',
  'stone.dev.secretKey': 'Chave secreta de testes',
  'stone.dev.recipientMaster': 'ID do recebedor principal',
  'stone.prod.accountId': 'ID da conta',
  'stone.prod.publicKey': 'Chave pública',
  'stone.prod.secretKey': 'Chave secreta',
  'stone.prod.recipientMaster': 'ID do recebedor principal',
}

async function carregarServicos() {
  carregandoServicos.value = true
  erroServicos.value = ''
  try {
    servicos.value = await api.listarServicos()
  } catch {
    erroServicos.value = 'Não foi possível carregar os serviços externos.'
  } finally {
    carregandoServicos.value = false
  }
}

onMounted(() => {
  void carregar()
  void carregarServicos()
})
</script>

<template>
  <CabecalhoPagina
    titulo="Sistema"
    descricao="Gerencie o provedor de SMS e as configurações dos serviços externos."
  />

  <AmbienteGlobalFormulario />

  <section class="configuracao" aria-labelledby="titulo-sms" :aria-busy="carregando || salvando">
    <h2 id="titulo-sms">Envio de SMS</h2>
    <p v-if="carregando" role="status">Carregando configuração…</p>
    <p v-if="erro" class="erro" role="alert">{{ erro }}</p>
    <p v-if="aviso" role="status">{{ aviso }}</p>
    <BotaoBase v-if="!carregando && ativo === undefined" variante="discreto" @click="carregar">
      Tentar novamente
    </BotaoBase>
    <form v-if="ativo !== undefined" class="formulario" @submit.prevent="salvar">
      <p v-if="!configuracaoAtiva" role="status">O envio de SMS está inativo.</p>
      <p v-else-if="!nomeAtivo" role="status">O provedor configurado está indisponível.</p>
      <p v-else>
        Provedor ativo <strong>{{ nomeAtivo }}</strong>
      </p>
      <CampoSelecao
        id="provedor-sms"
        v-model="selecionado"
        rotulo="Provedor de SMS"
        :opcoes="opcoes"
        :disabled="salvando || !configuracaoAtiva || opcoes.length === 0"
        @change="aviso = ''"
      />
      <BotaoBase tipo="submit" :desabilitado="!podeSalvar" :carregando="salvando">
        Salvar
      </BotaoBase>
    </form>
  </section>
  <section class="servicos" aria-labelledby="titulo-servicos" :aria-busy="carregandoServicos">
    <h2 id="titulo-servicos">Serviços externos</h2>
    <p v-if="carregandoServicos" role="status">Carregando serviços…</p>
    <p v-if="erroServicos" class="erro" role="alert">{{ erroServicos }}</p>
    <BotaoBase v-if="erroServicos" variante="discreto" @click="carregarServicos">
      Tentar novamente
    </BotaoBase>
    <ServicoExternoFormulario
      v-for="servico in servicos"
      :key="servico.servico"
      :servico="servico.servico"
      :titulo="titulos[servico.servico] ?? 'Serviço externo'"
      :rotulos="campos"
    />
  </section>
</template>

<style scoped>
.configuracao,
.formulario {
  display: grid;
  gap: var(--espaco-4);
  justify-items: start;
}
.configuracao {
  max-width: 40rem;
  padding: var(--espaco-6);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
}
.formulario {
  width: 100%;
}
.formulario :deep(.campo-selecao) {
  width: 100%;
}
.servicos {
  display: grid;
  gap: var(--espaco-4);
  margin-top: var(--espaco-6);
  max-width: 40rem;
}
.erro {
  color: var(--cor-erro);
}
</style>
