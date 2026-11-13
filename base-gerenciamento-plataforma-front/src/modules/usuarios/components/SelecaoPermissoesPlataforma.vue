<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { onMounted, ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import { ErroApi } from '@/services/clienteHttp'
import type { PermissaoPlataforma } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'

defineProps<{ desabilitada: boolean }>()
const selecionadas = defineModel<string[]>({ required: true })
const api = useUsuariosApi()
const opcoes = ref<PermissaoPlataforma[]>([])
const carregando = ref(true)
const erro = ref('')

async function carregar() {
  carregando.value = true
  erro.value = ''
  try {
    opcoes.value = await api.listarPermissoesPlataforma()
  } catch (falha) {
    erro.value =
      falha instanceof ErroApi ? falha.message : 'Não foi possível carregar as permissões.'
  } finally {
    carregando.value = false
  }
}

onMounted(carregar)
</script>

<template>
  <fieldset class="permissoes" :disabled="desabilitada" :aria-busy="carregando">
    <legend>Permissões da Plataforma</legend>
    <p v-if="carregando" role="status">Carregando permissões…</p>
    <div v-else-if="erro" class="permissoes__erro">
      <p role="alert">{{ erro }}</p>
      <BotaoBase variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
    </div>
    <p v-else-if="!opcoes.length">Nenhuma permissão da plataforma disponível.</p>
    <div v-else class="permissoes__opcoes">
      <label
        v-for="opcao in opcoes"
        :key="opcao.id"
        class="permissoes__opcao"
        :class="{ 'permissoes__opcao--selecionada': selecionadas.includes(opcao.codigo) }"
      >
        <input
          v-model="selecionadas"
          type="checkbox"
          :value="opcao.codigo"
          :disabled="opcao.status === 'INATIVO' && !selecionadas.includes(opcao.codigo)"
        />
        <span>
          <strong>{{ traduzirEnum('permissao', opcao.codigo, opcao.nome) }}</strong>
          <span v-if="opcao.status === 'INATIVO'">
            · {{ traduzirEnum('statusPermissao', opcao.status) }}</span
          >
          <small v-if="opcao.descricao">{{ opcao.descricao }}</small>
        </span>
      </label>
    </div>
  </fieldset>
</template>

<style scoped>
.permissoes {
  min-inline-size: 0;
  border: 0;
  padding: 0;
  margin: 0;
}
.permissoes legend {
  font-size: var(--fonte-lg);
  font-weight: var(--peso-forte);
  padding: 0;
  margin-bottom: var(--espaco-3);
}
.permissoes__opcoes {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 14rem), 1fr));
  gap: var(--espaco-3);
}
.permissoes__opcao {
  display: flex;
  gap: var(--espaco-3);
  align-items: flex-start;
  padding: var(--espaco-3);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-sm);
}
.permissoes__opcao--selecionada {
  border-color: var(--cor-primaria-texto);
  background: var(--cor-primaria-suave);
}
.permissoes__opcao input {
  flex-shrink: 0;
  width: 1rem;
  height: 1rem;
  margin: 0.2rem 0 0;
  accent-color: var(--cor-primaria-texto);
}
.permissoes__opcao small {
  display: block;
  margin-top: var(--espaco-1);
  color: var(--cor-texto-secundario);
}
.permissoes__erro {
  display: grid;
  justify-items: start;
  gap: var(--espaco-2);
  color: var(--cor-erro);
}
</style>
