<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, useId } from 'vue'
import IconeBase from './IconeBase.vue'

defineProps<{ titulo: string; ocupado?: boolean }>()
const emit = defineEmits<{ fechar: [] }>()
const dialogo = ref<HTMLDialogElement>()
const idTitulo = useId()

onMounted(() => dialogo.value?.showModal())
onBeforeUnmount(() => dialogo.value?.close())
</script>

<template>
  <dialog
    ref="dialogo"
    class="dialogo"
    :aria-labelledby="idTitulo"
    @cancel.prevent="!ocupado && emit('fechar')"
  >
    <div class="dialogo__cabecalho">
      <h2 :id="idTitulo">{{ titulo }}</h2>
      <button type="button" aria-label="Fechar" :disabled="ocupado" @click="emit('fechar')">
        <IconeBase nome="fechar" :tamanho="20" />
      </button>
    </div>
    <slot />
  </dialog>
</template>

<style scoped>
.dialogo {
  width: min(34rem, calc(100% - var(--espaco-8)));
  max-height: calc(100dvh - var(--espaco-8));
  padding: var(--espaco-6);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-lg);
  background: var(--cor-superficie);
  color: var(--cor-texto);
  box-shadow: var(--sombra-card);
}
.dialogo::backdrop {
  background: var(--cor-overlay);
}
.dialogo__cabecalho {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--espaco-4);
  margin-bottom: var(--espaco-5);
}
h2 {
  font-size: var(--fonte-lg);
}
button {
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--raio-sm);
  width: 2.5rem;
  height: 2.5rem;
  flex-shrink: 0;
  background: transparent;
}
button:hover:not(:disabled) {
  background: var(--cor-primaria-suave);
}
</style>
