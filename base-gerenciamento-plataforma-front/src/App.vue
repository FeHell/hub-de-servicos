<script setup lang="ts">
import { watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'

const autenticacao = useAutenticacao()
const rota = useRoute()
const router = useRouter()

watch(
  () => autenticacao.autenticado,
  (autenticado) => {
    if (!autenticado && rota.meta.exigeAutenticacao) void router.replace({ name: 'login' })
  },
)
</script>

<template>
  <p v-if="autenticacao.restaurando" class="sessao-carregando" role="status">
    Verificando sua sessão…
  </p>
  <RouterView />
</template>

<style scoped>
.sessao-carregando {
  display: grid;
  min-height: 100dvh;
  place-items: center;
  color: var(--cor-texto-secundario);
}
</style>
