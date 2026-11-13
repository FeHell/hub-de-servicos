<script setup lang="ts">
withDefaults(
  defineProps<{
    tipo?: 'button' | 'submit'
    carregando?: boolean
    desabilitado?: boolean
    variante?: 'primario' | 'discreto' | 'perigo'
  }>(),
  { tipo: 'button', carregando: false, desabilitado: false, variante: 'primario' },
)
</script>

<template>
  <button
    :type="tipo"
    class="botao"
    :class="'botao--' + variante"
    :disabled="carregando || desabilitado"
    :aria-busy="carregando"
  >
    <span v-if="carregando" class="botao__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>

<style scoped>
.botao {
  min-height: var(--altura-controle);
  padding: var(--espaco-3) var(--espaco-5);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: var(--espaco-3);
  border: var(--borda-fina) solid transparent;
  border-radius: var(--raio-sm);
  font-size: var(--fonte-sm);
  font-weight: var(--peso-forte);
  transition:
    background var(--transicao-rapida),
    color var(--transicao-rapida),
    opacity var(--transicao-rapida);
}
.botao--primario {
  color: var(--cor-sobre-primaria);
  background: var(--cor-primaria);
}
.botao--primario:hover:not(:disabled) {
  background: var(--cor-primaria-hover);
}
.botao--discreto {
  background: transparent;
  border-color: var(--cor-borda);
}
.botao--discreto:hover:not(:disabled) {
  background: var(--cor-primaria-suave);
}
.botao--perigo {
  background: var(--cor-erro);
  color: var(--cor-sobre-erro);
}
.botao--perigo:hover:not(:disabled) {
  filter: brightness(0.9);
}
.botao:disabled {
  opacity: 0.65;
}
.botao__spinner {
  width: 1rem;
  height: 1rem;
  border: 2px solid currentColor;
  border-right-color: transparent;
  border-radius: 50%;
  animation: girar 700ms linear infinite;
}
@keyframes girar {
  to {
    transform: rotate(360deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .botao__spinner {
    animation: none;
  }
}
</style>
