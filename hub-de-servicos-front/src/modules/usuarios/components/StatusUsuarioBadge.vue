<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import type { StatusUsuario } from '../contratos'
defineProps<{ status: StatusUsuario }>()
</script>

<template>
  <span class="status" :class="'status--' + status.toLowerCase()">
    {{ traduzirEnum('statusUsuario', status) }}
  </span>
</template>

<style scoped>
.status {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  gap: var(--espaco-2);
  border-radius: 2rem;
  padding: var(--espaco-1) var(--espaco-3);
  color: var(--cor-texto-secundario);
  background: var(--cor-fundo);
  font-size: var(--fonte-xs);
  font-weight: var(--peso-forte);
  white-space: nowrap;
}
.status::before {
  content: '';
  width: 0.4rem;
  height: 0.4rem;
  border-radius: 50%;
  background: currentColor;
}
.status--ativo {
  color: var(--cor-sucesso);
  background: var(--cor-primaria-suave);
}
.status--pendente {
  color: var(--cor-aviso);
  background: var(--cor-aviso-fundo);
}
.status--bloqueado {
  color: var(--cor-erro);
  background: var(--cor-erro-fundo);
}
</style>
