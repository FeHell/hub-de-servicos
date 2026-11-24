<script setup lang="ts">
import { onMounted, ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import { useConfiguracaoApi } from '../services/configuracaoApi'

const api = useConfiguracaoApi()
const atual = ref<boolean>()
const dev = ref(false)
const ocupado = ref(false)
const erro = ref('')
const aviso = ref('')

async function carregar() {
  ocupado.value = true
  erro.value = ''
  try {
    atual.value = (await api.consultarDevMode()).devMode
    dev.value = atual.value
  } catch {
    erro.value = 'Não foi possível consultar o ambiente global.'
  } finally {
    ocupado.value = false
  }
}

async function salvar() {
  if (ocupado.value || atual.value === undefined || dev.value === atual.value) return
  ocupado.value = true
  erro.value = ''
  aviso.value = ''
  try {
    atual.value = (await api.alterarDevMode(dev.value)).devMode
    dev.value = atual.value
    aviso.value = 'Ambiente global atualizado.'
  } catch {
    dev.value = atual.value
    erro.value = 'Não foi possível atualizar o ambiente global.'
  } finally {
    ocupado.value = false
  }
}

function alterarToggle(evento: Event) {
  dev.value = (evento.target as HTMLInputElement).checked
  void salvar()
}

onMounted(carregar)
</script>

<template>
  <section class="ambiente" aria-labelledby="titulo-ambiente" :aria-busy="ocupado">
    <h2 id="titulo-ambiente">Ambiente global</h2>
    <p v-if="ocupado && atual === undefined" role="status">Carregando ambiente…</p>
    <p v-if="erro" role="alert">{{ erro }}</p>
    <p v-if="aviso" role="status">{{ aviso }}</p>
    <div v-if="atual !== undefined" class="controle">
      <label class="alternador" for="dev-mode">
        <input
          id="dev-mode"
          type="checkbox"
          role="switch"
          :checked="dev"
          :disabled="ocupado"
          @change="alterarToggle"
        />
        <span class="alternador-trilho" aria-hidden="true"></span>
        <span>Modo de desenvolvimento (DEV_MODE)</span>
        <strong aria-hidden="true">{{ dev ? 'Ativado' : 'Desativado' }}</strong>
      </label>
      <p>
        Ambiente em uso <strong>{{ atual ? 'Desenvolvimento (DEV)' : 'Produção (PROD)' }}</strong>
      </p>
      <p v-if="ocupado" role="status">Salvando ambiente…</p>
    </div>
    <BotaoBase v-else-if="erro" variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
  </section>
</template>

<style scoped>
.ambiente,
.controle {
  display: grid;
  gap: var(--espaco-4);
  justify-items: start;
}
.ambiente {
  max-width: 40rem;
  margin-bottom: var(--espaco-6);
  padding: var(--espaco-6);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
}
.alternador {
  position: relative;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--espaco-2);
  min-height: 2.75rem;
  cursor: pointer;
}
.alternador input {
  position: absolute;
  width: 2.75rem;
  height: 1.5rem;
  margin: 0;
  opacity: 0;
  cursor: inherit;
}
.alternador-trilho {
  display: grid;
  align-items: center;
  width: 2.75rem;
  height: 1.5rem;
  padding-inline: 0.1875rem;
  border-radius: 1rem;
  background: var(--cor-texto-secundario);
}
.alternador-trilho::before {
  width: 1.125rem;
  height: 1.125rem;
  border-radius: 50%;
  background: var(--cor-superficie);
  content: '';
}
.alternador input:checked + .alternador-trilho {
  background: var(--cor-primaria);
}
.alternador input:checked + .alternador-trilho::before {
  justify-self: end;
}
.alternador input:focus-visible + .alternador-trilho {
  outline: var(--foco-espessura) solid var(--cor-foco);
  outline-offset: 2px;
}
.alternador input:disabled + .alternador-trilho {
  opacity: 0.55;
}
</style>
