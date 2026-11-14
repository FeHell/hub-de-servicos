<script setup lang="ts">
import { ref } from 'vue'

defineOptions({ inheritAttrs: false })
defineProps<{
  id: string
  rotulo: string
  tipo?: 'text' | 'email' | 'password'
  erro?: string
  descricao?: string
  autocomplete?: string
}>()
const valor = defineModel<string>({ default: '' })
const entrada = ref<HTMLInputElement>()
defineExpose({ focar: () => entrada.value?.focus() })
</script>

<template>
  <div class="campo" :class="{ 'campo--erro': erro }">
    <label :for="id" class="campo__rotulo">{{ rotulo }}</label>
    <div class="campo__controle">
      <input
        :id="id"
        ref="entrada"
        v-model="valor"
        v-bind="$attrs"
        :type="tipo || 'text'"
        :autocomplete="autocomplete"
        :aria-invalid="!!erro"
        :aria-describedby="[descricao, erro && id + '-erro'].filter(Boolean).join(' ') || undefined"
      />
      <div v-if="$slots.acao" class="campo__acao"><slot name="acao" /></div>
    </div>
    <p v-if="erro" :id="id + '-erro'" class="campo__erro">{{ erro }}</p>
  </div>
</template>

<style scoped>
.campo {
  display: grid;
  gap: var(--espaco-2);
}
.campo__rotulo {
  font-size: var(--fonte-sm);
  font-weight: var(--peso-forte);
}
.campo__controle {
  display: flex;
  align-items: center;
  min-height: var(--altura-controle);
  background: var(--cor-superficie);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-sm);
  transition:
    border-color var(--transicao-rapida),
    box-shadow var(--transicao-rapida);
}
.campo__controle:hover {
  border-color: var(--cor-borda-hover);
}
.campo__controle:focus-within {
  border-color: var(--cor-primaria-texto);
  box-shadow: 0 0 0 3px var(--cor-primaria-suave);
}
.campo input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--cor-texto);
  padding: var(--espaco-4);
  font-size: var(--fonte-md);
}
.campo input::placeholder {
  color: var(--cor-texto-secundario);
}
.campo input:disabled {
  opacity: 0.65;
}
.campo__acao {
  padding-right: var(--espaco-2);
  display: flex;
}
.campo--erro .campo__controle {
  border-color: var(--cor-erro);
}
.campo__erro {
  color: var(--cor-erro);
  font-size: var(--fonte-xs);
  line-height: 1.5;
}
</style>
