<script setup lang="ts">
defineProps<{ titulo: string; descricao: string }>()
</script>

<template>
  <header class="cabecalho-pagina">
    <div>
      <h1>{{ titulo }}</h1>
      <p>{{ descricao }}</p>
    </div>
    <slot />
  </header>
</template>

<style scoped>
.cabecalho-pagina {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--espaco-4);
  margin-bottom: var(--espaco-8);
}
h1 {
  font-size: var(--fonte-titulo);
  line-height: 1.14;
  letter-spacing: -0.035em;
}
p {
  margin-top: var(--espaco-2);
  color: var(--cor-texto-secundario);
  line-height: 1.5;
}
</style>
