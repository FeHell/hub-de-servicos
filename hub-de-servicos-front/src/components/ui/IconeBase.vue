<script setup lang="ts">
const caminhos = {
  marca: 'M4 4h6v6H4z M14 4h6v6h-6z M4 14h6v6H4z M14 14h6v6h-6z',
  seta: 'M5 12h14 M13 6l6 6-6 6',
  olho: 'M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12z M15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  ocultar:
    'M3 3l18 18 M10 5.2A12 12 0 0 1 12 5c6.5 0 10 7 10 7a20 20 0 0 1-3.5 4.3 M6.4 6.4A23 23 0 0 0 2 12s3.5 7 10 7a12 12 0 0 0 4.3-.8',
  cadeado: 'M7 10V7a5 5 0 0 1 10 0v3 M5 10h14v11H5z M12 14v3',
  inicio: 'M3 10l9-7 9 7 M5 9v12h5v-7h4v7h5V9',
  menu: 'M4 6h16 M4 12h16 M4 18h16',
  fechar: 'M6 6l12 12 M18 6L6 18',
  sair: 'M9 4H4v16h5 M13 8l4 4-4 4 M8 12h12',
  escudo: 'M12 3l8 3v6c0 5-8 9-8 9s-8-4-8-9V6z M8 12l3 3 5-6',
  usuario: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0 M4 21v-2a8 8 0 0 1 16 0v2',
  check: 'M5 12l4 4L19 6',
  cadastros: 'M3 6h7l2 3h9v11H3z M3 6V4h7l2 2h9v3',
  grafico: 'M4 4v16h16 M9 15v-4 M14 15V7 M19 15v-8',
  chevron: 'M6 9l6 6 6-6',
  mais: 'M12 5v14 M5 12h14',
  buscar: 'M15 15l5 5 M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0',
  editar: 'M4 20l4-1 12-12-3-3L5 16z M14 7l3 3',
  paleta:
    'M12 3a9 9 0 1 0 0 18h1a2 2 0 0 0 1.5-3.3 1.6 1.6 0 0 1 1.2-2.7H18a3 3 0 0 0 3-3c0-5-4-9-9-9z M7 11h.01 M9 7h.01 M14 6h.01 M17 9h.01',
  lixeira: 'M4 7h16 M9 7V4h6v3 M6 7l1 13h10l1-13 M10 11v5 M14 11v5',
} as const

defineProps<{ nome: keyof typeof caminhos; tamanho?: number }>()
</script>

<template>
  <svg
    :width="tamanho || 22"
    :height="tamanho || 22"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.7"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
    focusable="false"
  >
    <path :d="caminhos[nome]" />
  </svg>
</template>
