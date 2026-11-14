<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { onBeforeRouteLeave, onBeforeRouteUpdate } from 'vue-router'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CampoSelecao from '@/components/ui/CampoSelecao.vue'
import CampoTexto from '@/components/ui/CampoTexto.vue'
import DialogoBase from '@/components/ui/DialogoBase.vue'
import SelecaoPermissoesPlataforma from './SelecaoPermissoesPlataforma.vue'
import { ErroApi } from '@/services/clienteHttp'
import { opcoesStatus, type StatusUsuario, type Usuario, type UsuarioListagem } from '../contratos'
import { useUsuariosApi } from '../services/usuariosApi'
import { mascararTelefone, normalizarTelefone } from '../telefone'

const props = defineProps<{ usuario?: UsuarioListagem; contaPropria?: boolean }>()
const emit = defineEmits<{ cancelar: []; salvo: [Usuario] }>()
const api = useUsuariosApi()
const login = ref(props.usuario?.login ?? '')
const email = ref(props.usuario?.email ?? '')
const status = ref<StatusUsuario>(props.usuario?.status ?? 'ATIVO')
const senha = ref('')
const telefone = ref(mascararTelefone(props.usuario?.telefone ?? ''))
const situacaoTelefone = ref(props.usuario?.situacaoTelefone)
const erroTelefone = ref('')
function editarTelefone(valor: string) {
  telefone.value = mascararTelefone(valor)
  erroTelefone.value = ''
}
const permissoes = ref([...(props.usuario?.permissoesPlataforma ?? [])])
const salvando = ref(false)
const erro = ref('')
const confirmandoSaida = ref(false)
let resolverSaida: ((descartar: boolean) => void) | undefined
const campos = computed(() => ({
  login: login.value,
  email: email.value,
  status: status.value,
  telefone: telefone.value,
  permissoesPlataforma: [...permissoes.value].sort(),
}))
const original = ref(campos.value)
const permissoesAlteradas = computed(
  () =>
    campos.value.permissoesPlataforma.join(',') !== original.value.permissoesPlataforma.join(','),
)
const alterado = computed(
  () =>
    login.value !== original.value.login ||
    email.value !== original.value.email ||
    status.value !== original.value.status ||
    telefone.value !== original.value.telefone ||
    permissoesAlteradas.value ||
    senha.value !== '',
)

function descarregar(evento: BeforeUnloadEvent) {
  evento.preventDefault()
  evento.returnValue = ''
}
watch(alterado, (pendente) => {
  if (pendente) window.addEventListener('beforeunload', descarregar)
  else window.removeEventListener('beforeunload', descarregar)
})
onBeforeUnmount(() => {
  window.removeEventListener('beforeunload', descarregar)
  decidirSaida(false)
})

function decidirSaida(descartar: boolean) {
  const resolver = resolverSaida
  resolverSaida = undefined
  confirmandoSaida.value = false
  resolver?.(descartar)
}

function podeSair() {
  if (salvando.value || resolverSaida) return false
  if (!alterado.value) return true
  return new Promise<boolean>((resolver) => {
    resolverSaida = resolver
    confirmandoSaida.value = true
  })
}
onBeforeRouteLeave((destino) => destino.name === 'login' || podeSair())
onBeforeRouteUpdate((destino, origem) => destino.params.id === origem.params.id || podeSair())

async function salvar() {
  if (salvando.value) return
  erro.value = ''
  erroTelefone.value = ''
  const numero = telefone.value.trim() ? normalizarTelefone(telefone.value) : ''
  if (numero === null) {
    erroTelefone.value = 'Informe um telefone brasileiro válido com DDD.'
    return
  }
  const dados = { login: login.value.trim(), email: email.value.trim(), status: status.value }
  if (!dados.login) {
    erro.value = 'Informe o nome de usuário.'
    return
  }
  if (
    !props.usuario &&
    (senha.value.length < 12 || new TextEncoder().encode(senha.value).length > 72)
  ) {
    erro.value = 'A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes.'
    return
  }
  salvando.value = true
  let usuario: Usuario
  try {
    usuario = props.usuario
      ? await api.atualizar(props.usuario.id, {
          ...dados,
          ...(telefone.value !== original.value.telefone && { telefone: numero }),
          ...(permissoesAlteradas.value && {
            permissoesPlataforma: campos.value.permissoesPlataforma,
          }),
        })
      : await api.criar({
          ...dados,
          senha: senha.value,
          ...(numero && { telefone: numero }),
          permissoesPlataforma: campos.value.permissoesPlataforma,
        })
  } catch (falha) {
    if (falha instanceof ErroApi && falha.status === 409 && falha.message.includes('telefone')) {
      erroTelefone.value = falha.message
    } else {
      erro.value = falha instanceof ErroApi ? falha.message : 'Não foi possível salvar o usuário.'
    }
    return
  } finally {
    salvando.value = false
  }
  login.value = usuario.login
  email.value = usuario.email
  status.value = usuario.status
  senha.value = ''
  telefone.value = mascararTelefone(usuario.telefone ?? '')
  situacaoTelefone.value = usuario.situacaoTelefone
  permissoes.value = [...usuario.permissoesPlataforma]
  original.value = campos.value
  emit('salvo', usuario)
}
</script>

<template>
  <form
    class="formulario"
    :aria-busy="salvando"
    aria-labelledby="dados-usuario"
    @submit.prevent="salvar"
  >
    <h2 id="dados-usuario">Dados do usuário</h2>
    <p v-if="erro" class="formulario__erro" role="alert">{{ erro }}</p>
    <div class="formulario__campos">
      <CampoTexto
        id="usuario-form-login"
        v-model="login"
        rotulo="Nome de usuário"
        maxlength="50"
        autocomplete="off"
        required
        :disabled="salvando"
      />
      <CampoTexto
        id="usuario-form-email"
        v-model="email"
        rotulo="E-mail"
        tipo="email"
        maxlength="255"
        autocomplete="off"
        required
        :disabled="salvando"
      />
      <div class="tipo-acesso">
        <CampoTexto
          id="usuario-form-telefone"
          :model-value="telefone"
          :erro="erroTelefone"
          rotulo="Telefone"
          inputmode="tel"
          autocomplete="tel"
          maxlength="32"
          placeholder="(34) 99999-9999"
          :disabled="salvando"
          @update:model-value="editarTelefone"
        />
        <span v-if="situacaoTelefone" role="status">
          {{ traduzirEnum('situacaoTelefone', situacaoTelefone) }}
        </span>
      </div>
      <CampoSelecao
        id="usuario-form-status"
        v-model="status"
        rotulo="Status"
        :opcoes="opcoesStatus"
        :disabled="salvando || contaPropria"
      />
      <CampoTexto
        v-if="!usuario"
        id="usuario-form-senha"
        v-model="senha"
        rotulo="Senha inicial"
        tipo="password"
        autocomplete="new-password"
        minlength="12"
        maxlength="72"
        placeholder="Mínimo de 12 caracteres"
        required
        :disabled="salvando"
      />
      <!-- <div class="tipo-acesso">
        <span>Tipo de acesso efetivo</span>
        <p>{{ traduzirEnum('tipoAcesso', usuario?.tipo ?? 'USUARIO') }}</p>
      </div> -->
    </div>
    <SelecaoPermissoesPlataforma v-model="permissoes" :desabilitada="salvando" />
    <div class="formulario__acoes">
      <BotaoBase variante="discreto" :desabilitado="salvando" @click="emit('cancelar')"
        >Cancelar</BotaoBase
      >
      <BotaoBase tipo="submit" :carregando="salvando">
        {{ salvando ? 'Salvando…' : 'Salvar usuário' }}
      </BotaoBase>
    </div>
  </form>
  <DialogoBase v-if="confirmandoSaida" titulo="Alterações não salvas" @fechar="decidirSaida(false)">
    <p>Deseja sair e descartar as alterações deste formulário?</p>
    <div class="formulario__acoes descarte-acoes">
      <BotaoBase variante="discreto" autofocus @click="decidirSaida(false)"
        >Continuar editando</BotaoBase
      >
      <BotaoBase variante="perigo" @click="decidirSaida(true)">Descartar alterações</BotaoBase>
    </div>
  </DialogoBase>
</template>

<style scoped>
.formulario {
  display: grid;
  gap: var(--espaco-5);
  padding: var(--espaco-5);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
  box-shadow: var(--sombra-card);
}
.formulario h2 {
  font-size: var(--fonte-lg);
}
.formulario__campos {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 18rem), 1fr));
  gap: var(--espaco-4);
}
.formulario__erro {
  color: var(--cor-erro);
}
.tipo-acesso {
  display: grid;
  gap: var(--espaco-2);
  align-content: start;
}
.tipo-acesso span {
  font-size: var(--fonte-sm);
  font-weight: var(--peso-forte);
}
.tipo-acesso p {
  padding-block: var(--espaco-3);
  color: var(--cor-texto-secundario);
}
.formulario__acoes {
  display: flex;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: var(--espaco-3);
}
.descarte-acoes {
  margin-top: var(--espaco-5);
}
</style>
