<script setup lang="ts">
import { ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import DialogoBase from '@/components/ui/DialogoBase.vue'
import { ErroApi } from '@/services/clienteHttp'
import type { Usuario } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'

const props = defineProps<{ usuario: Usuario }>()
const emit = defineEmits<{ fechar: []; excluido: [] }>()
const api = useUsuariosApi()
const excluindo = ref(false)
const erro = ref('')

async function confirmar() {
  if (excluindo.value) return
  excluindo.value = true
  erro.value = ''
  try {
    await api.excluir(props.usuario.id)
    emit('excluido')
  } catch (falha) {
    erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível excluir o usuário.'
  } finally {
    excluindo.value = false
  }
}
</script>

<template>
  <DialogoBase titulo="Excluir usuário" :ocupado="excluindo" @fechar="emit('fechar')">
    <p class="confirmacao__texto">
      O usuário <strong>{{ usuario.login }}</strong> ({{ usuario.email }}) ficará inativo e suas
      sessões serão encerradas. O cadastro será mantido no histórico.
    </p>
    <p v-if="erro" class="confirmacao__erro" role="alert">{{ erro }}</p>
    <div class="confirmacao__acoes">
      <BotaoBase variante="discreto" autofocus :desabilitado="excluindo" @click="emit('fechar')"
        >Cancelar</BotaoBase
      >
      <BotaoBase variante="perigo" :carregando="excluindo" @click="confirmar">
        {{ excluindo ? 'Excluindo…' : 'Excluir usuário' }}
      </BotaoBase>
    </div>
  </DialogoBase>
</template>

<style scoped>
.confirmacao__texto {
  line-height: 1.6;
  overflow-wrap: anywhere;
}
.confirmacao__erro {
  color: var(--cor-erro);
  margin-top: var(--espaco-4);
}
.confirmacao__acoes {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--espaco-3);
  margin-top: var(--espaco-6);
}
</style>
