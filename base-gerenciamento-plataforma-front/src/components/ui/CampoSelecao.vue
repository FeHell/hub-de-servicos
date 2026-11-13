<script setup lang="ts" generic="T extends string">
defineOptions({ inheritAttrs: false })
defineProps<{ id: string; rotulo: string; opcoes: readonly { valor: T; texto: string }[] }>()
const valor = defineModel<T>({ required: true })
</script>

<template>
  <div class="campo-selecao">
    <label :for="id">{{ rotulo }}</label>
    <select :id="id" v-model="valor" v-bind="$attrs">
      <option v-for="opcao in opcoes" :key="opcao.valor" :value="opcao.valor">
        {{ opcao.texto }}
      </option>
    </select>
  </div>
</template>

<style scoped>
.campo-selecao {
  display: grid;
  gap: var(--espaco-2);
  min-width: 0;
}
label {
  font-size: var(--fonte-sm);
  font-weight: var(--peso-forte);
}
select {
  width: 100%;
  min-height: var(--altura-controle);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-sm);
  background: var(--cor-superficie);
  color: var(--cor-texto);
  padding: var(--espaco-3) var(--espaco-4);
}
select:hover:not(:disabled) {
  border-color: var(--cor-borda-hover);
}
</style>
