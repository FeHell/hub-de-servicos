<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CampoTexto from '@/components/ui/CampoTexto.vue'
import { ErroApi } from '@/services/clienteHttp'
import { useConfiguracaoApi, type ServicoExterno } from '../services/configuracaoApi'

const props = defineProps<{
  servico: string
  titulo: string
  rotulos: Record<string, string>
}>()
const api = useConfiguracaoApi()
const detalhe = ref<ServicoExterno>()
const campos = computed(
  () => detalhe.value?.configuracoes.filter(({ chave }) => props.rotulos[chave]) ?? [],
)
const grupos = computed(() =>
  props.servico === 'STONE'
    ? [
        {
          titulo: 'Desenvolvimento (DEV)',
          campos: campos.value.filter(({ chave }) => chave.startsWith('stone.dev.')),
        },
        {
          titulo: 'Produção (PROD)',
          campos: campos.value.filter(({ chave }) => chave.startsWith('stone.prod.')),
        },
      ]
    : [{ titulo: '', campos: campos.value }],
)
const entradas = ref<Record<string, string>>({})
const carregando = ref(false)
const erroConsulta = ref('')
const salvando = ref(false)
const erro = ref('')
const aviso = ref('')
const id = (chave: string) => `servico-${chave.toLowerCase().replace(/[_.]/g, '-')}`

async function carregar() {
  carregando.value = true
  erroConsulta.value = ''
  detalhe.value = undefined
  entradas.value = {}
  try {
    const servico = await api.consultarServico(props.servico)
    entradas.value = Object.fromEntries(
      servico.configuracoes.map(({ chave, valor }) => [chave, valor ?? '']),
    )
    detalhe.value = servico
  } catch {
    erroConsulta.value = 'Não foi possível consultar as configurações deste serviço.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)

const alteracoes = computed(() => {
  const valores: Record<string, string> = {}
  for (const campo of campos.value) {
    if (campo.status !== 'ATIVO') continue
    const valor = entradas.value[campo.chave]?.trim() ?? ''
    if ((!campo.sensivel || valor !== '') && valor !== (campo.valor ?? '')) {
      valores[campo.chave] = valor
    }
  }
  return valores
})

async function salvar() {
  if (salvando.value || Object.keys(alteracoes.value).length === 0) return
  aviso.value = ''
  erro.value = ''
  salvando.value = true
  try {
    await api.atualizarServico(props.servico, alteracoes.value)
    aviso.value = 'Configurações salvas.'
    await carregar()
  } catch (falha) {
    erro.value =
      falha instanceof ErroApi && falha.status === 503
        ? 'Configure a chave mestra no backend antes de salvar a credencial.'
        : falha instanceof ErroApi && falha.status === 400
          ? props.servico === 'INFOBIP'
            ? 'Confira os dados informados. Use a URL HTTPS da conta, sem caminho.'
            : 'Confira os dados informados para este ambiente.'
          : 'Não foi possível salvar as configurações do serviço.'
  } finally {
    salvando.value = false
  }
}
</script>

<template>
  <form
    class="servico"
    :aria-labelledby="id(servico)"
    :aria-busy="carregando || salvando"
    @submit.prevent="salvar"
  >
    <h3 :id="id(servico)">{{ titulo }}</h3>
    <p v-if="carregando" role="status">Carregando configurações…</p>
    <p v-if="erroConsulta" class="erro" role="alert">{{ erroConsulta }}</p>
    <div v-if="erroConsulta">
      <BotaoBase variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
    </div>
    <div v-for="grupo in grupos" v-show="grupo.campos.length" :key="grupo.titulo" class="grupo">
      <h4 v-if="grupo.titulo">{{ grupo.titulo }}</h4>
      <div v-for="campo in grupo.campos" :key="campo.chave" class="configuracao-campo">
        <CampoTexto
          :id="id(campo.chave)"
          v-model="entradas[campo.chave]"
          :rotulo="rotulos[campo.chave] ?? 'Configuração'"
          tipo="text"
          autocomplete="off"
          autocapitalize="off"
          autocorrect="off"
          :descricao="campo.sensivel ? id(campo.chave) + '-estado' : undefined"
          :disabled="salvando"
          :readonly="campo.status !== 'ATIVO'"
          maxlength="2048"
          spellcheck="false"
        />
        <p v-if="campo.sensivel" :id="id(campo.chave) + '-estado'" class="estado">
          {{ campo.configurada ? 'Credencial configurada' : 'Credencial não configurada' }}
        </p>
      </div>
    </div>
    <p
      v-if="
        servico === 'STONE' &&
        detalhe &&
        !campos.some(({ chave }) => chave === 'stone.prod.secretKey')
      "
      class="estado"
    >
      A chave secreta de produção ficará disponível quando o servidor liberar essa configuração.
    </p>
    <p v-if="servico === 'STONE' && detalhe" class="estado">
      Para substituir uma chave secreta, informe a nova chave. Deixar o campo vazio mantém a chave
      salva.
    </p>
    <p v-if="erro" class="erro" role="alert">{{ erro }}</p>
    <p v-if="aviso" role="status">{{ aviso }}</p>
    <div v-if="detalhe">
      <BotaoBase
        tipo="submit"
        :desabilitado="Object.keys(alteracoes).length === 0"
        :carregando="salvando"
      >
        Salvar configurações
      </BotaoBase>
    </div>
  </form>
</template>

<style scoped>
.servico,
.grupo,
.configuracao-campo {
  display: grid;
  gap: var(--espaco-2);
}
.servico {
  gap: var(--espaco-4);
  padding: var(--espaco-6);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
}
.estado {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
}
.erro {
  color: var(--cor-erro);
}
</style>
