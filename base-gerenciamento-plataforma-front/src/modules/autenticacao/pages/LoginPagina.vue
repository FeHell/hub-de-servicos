<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ambiente } from '@/app/config/ambiente'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CampoTexto from '@/components/ui/CampoTexto.vue'
import IconeBase from '@/components/ui/IconeBase.vue'
import { ErroApi } from '@/services/clienteHttp'
import { useAutenticacao } from '../stores/autenticacao'

const autenticacao = useAutenticacao()
const router = useRouter()
const login = ref('')
const senha = ref('')
const mostrarSenha = ref(false)
const erroLogin = ref('')
const erroSenha = ref('')
const erroGeral = ref('')
const campoLogin = ref<InstanceType<typeof CampoTexto>>()
const campoSenha = ref<InstanceType<typeof CampoTexto>>()

onMounted(() => campoLogin.value?.focar())

async function enviar() {
  if (autenticacao.enviando) return
  erroGeral.value = ''
  erroLogin.value = login.value.trim() ? '' : 'Informe seu usuário.'
  erroSenha.value = senha.value ? '' : 'Informe sua senha.'
  if (erroLogin.value || erroSenha.value) {
    if (erroLogin.value) campoLogin.value?.focar()
    else campoSenha.value?.focar()
    return
  }
  try {
    await autenticacao.entrar(login.value, senha.value)
    if (autenticacao.autenticado) {
      senha.value = ''
      await router.replace({ name: 'inicio' })
    }
  } catch (erro) {
    erroGeral.value =
      erro instanceof ErroApi && erro.status === 401
        ? 'Confira seu usuário e sua senha ou solicite acesso ao administrador.'
        : erro instanceof ErroApi
          ? erro.message
          : 'Não foi possível entrar. Tente novamente.'
  }
}
</script>

<template>
  <main class="login">
    <section class="login__apresentacao" aria-label="Plataforma de gerenciamento">
      <a class="marca marca--clara" href="/login" aria-label="Plataforma, página de acesso">
        <span class="marca__simbolo"><IconeBase nome="marca" :tamanho="25" /></span>
        <span>plataforma<span class="marca__ponto">.</span></span>
      </a>

      <div class="login__mensagem">
        <p class="sobrelinha">GERENCIAMENTO DE ACESSOS</p>
        <h1>Seu próximo<br />acesso começa<br /><span>aqui.</span></h1>
        <p class="login__descricao">
          Organize usuários, aplicações e permissões.<br />Tudo em um só lugar.
        </p>
        <div class="selo">
          <IconeBase nome="escudo" :tamanho="17" /><span
            >Acesso individual para cada aplicação</span
          >
        </div>
      </div>

      <div class="grafismo" aria-hidden="true">
        <div class="grafismo__circulo grafismo__circulo--externo"></div>
        <div class="grafismo__circulo grafismo__circulo--interno"></div>
        <div class="grafismo__cartao">
          <span class="grafismo__avatar"><IconeBase nome="usuario" :tamanho="30" /></span>
          <span class="grafismo__linhas"><span></span><span></span></span>
          <span class="grafismo__check"><IconeBase nome="check" :tamanho="15" /></span>
        </div>
        <span class="grafismo__ponto"></span>
      </div>
      <p class="login__rodape-marca">Uma base para todas as suas aplicações.</p>
    </section>

    <section class="login__acesso" aria-labelledby="titulo-login">
      <div class="login__formulario">
        <div class="login__marca-mobile">
          <span class="marca__simbolo"><IconeBase nome="marca" /></span>plataforma.
        </div>
        <span class="identificador"><span></span> {{ ambiente.nomeAplicacao }}</span>
        <h2 id="titulo-login">Bem-vindo de volta</h2>
        <p class="login__orientacao">Entre com suas credenciais para continuar.</p>

        <form novalidate :aria-busy="autenticacao.enviando" @submit.prevent="enviar">
          <p v-if="autenticacao.aviso && !erroGeral" class="aviso" role="status">
            {{ autenticacao.aviso }}
          </p>
          <p v-if="erroGeral" class="aviso aviso--erro" role="alert">{{ erroGeral }}</p>
          <CampoTexto
            id="login"
            ref="campoLogin"
            v-model="login"
            rotulo="Usuário"
            autocomplete="username"
            placeholder="Digite seu usuário"
            :erro="erroLogin"
            :disabled="autenticacao.enviando"
            maxlength="50"
            autocapitalize="none"
            spellcheck="false"
            required
            @input="erroLogin = ''"
          />
          <CampoTexto
            id="senha"
            ref="campoSenha"
            v-model="senha"
            rotulo="Senha"
            autocomplete="current-password"
            placeholder="Digite sua senha"
            :tipo="mostrarSenha ? 'text' : 'password'"
            :erro="erroSenha"
            :disabled="autenticacao.enviando"
            required
            @input="erroSenha = ''"
          >
            <template #acao>
              <button
                type="button"
                class="mostrar-senha"
                :aria-label="mostrarSenha ? 'Ocultar senha' : 'Mostrar senha'"
                :aria-pressed="mostrarSenha"
                :disabled="autenticacao.enviando"
                @click="mostrarSenha = !mostrarSenha"
              >
                <IconeBase :nome="mostrarSenha ? 'ocultar' : 'olho'" :tamanho="20" />
              </button>
            </template>
          </CampoTexto>
          <BotaoBase tipo="submit" :carregando="autenticacao.enviando" class="login__entrar">
            {{ autenticacao.enviando ? 'Entrando…' : 'Entrar na plataforma' }}
            <IconeBase v-if="!autenticacao.enviando" nome="seta" :tamanho="18" />
          </BotaoBase>
        </form>

        <p class="login__ajuda">
          Precisa de acesso? Entre em contato com o administrador da sua aplicação.
        </p>
        <p class="login__privacidade">
          <IconeBase nome="cadeado" :tamanho="14" /> Seu acesso respeita as permissões da aplicação.
        </p>
      </div>
      <footer class="login__rodape">
        Plataforma de gerenciamento <span>© {{ new Date().getFullYear() }}</span>
      </footer>
    </section>
  </main>
</template>

<style scoped>
.login {
  min-height: 100dvh;
  display: grid;
  grid-template-columns: var(--login-colunas);
  background: var(--cor-superficie);
}
.login__apresentacao {
  --cor-foco: var(--cor-foco-menu);
  position: relative;
  overflow: hidden;
  display: var(--login-painel);
  flex-direction: column;
  background: var(--cor-menu);
  color: var(--cor-menu-texto);
  padding: var(--login-espaco);
  min-height: 100dvh;
}
.marca {
  display: inline-flex;
  align-items: center;
  gap: var(--espaco-3);
  text-decoration: none;
  font-size: var(--fonte-lg);
  font-weight: var(--peso-forte);
  letter-spacing: -0.04em;
  width: fit-content;
}
.marca__simbolo {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: var(--cor-primaria-texto);
}
.marca--clara .marca__simbolo,
.marca__ponto {
  color: var(--cor-detalhe);
}
.login__mensagem {
  margin-top: auto;
  padding-top: var(--espaco-16);
  z-index: 1;
}
.sobrelinha {
  color: var(--cor-menu-secundario);
  font-size: var(--fonte-xs);
  letter-spacing: 0.16em;
  font-weight: var(--peso-forte);
  margin-bottom: var(--espaco-6);
}
h1 {
  font-size: clamp(2.75rem, 4vw, 4.25rem);
  font-weight: var(--peso-medio);
  line-height: 1.12;
  letter-spacing: -0.045em;
}
h1 span {
  color: var(--cor-detalhe);
}
.login__descricao {
  font-size: var(--fonte-sm);
  line-height: 1.85;
  color: var(--cor-menu-secundario);
  margin-top: var(--espaco-6);
}
.selo {
  display: flex;
  align-items: center;
  gap: var(--espaco-2);
  font-size: var(--fonte-xs);
  color: var(--cor-menu-secundario);
  margin-top: var(--espaco-6);
}
.grafismo {
  position: relative;
  width: min(100%, 360px);
  height: 170px;
  margin: var(--espaco-8) auto;
}
.grafismo__circulo {
  position: absolute;
  border: var(--borda-fina) solid var(--cor-menu-hover);
  border-radius: 50%;
}
.grafismo__circulo--externo {
  width: 320px;
  height: 320px;
  top: -35px;
  left: 30px;
}
.grafismo__circulo--interno {
  width: 230px;
  height: 230px;
  top: 10px;
  left: 75px;
}
.grafismo__cartao {
  position: absolute;
  display: flex;
  align-items: center;
  gap: var(--espaco-4);
  top: 48px;
  left: 30px;
  width: 270px;
  padding: var(--espaco-5);
  background: var(--cor-menu-hover);
  border: var(--borda-fina) solid var(--cor-menu-secundario);
  border-radius: var(--raio-md);
  transform: rotate(-5deg);
}
.grafismo__avatar {
  color: var(--cor-detalhe);
}
.grafismo__linhas {
  display: grid;
  gap: var(--espaco-2);
  flex: 1;
}
.grafismo__linhas span {
  height: 5px;
  background: var(--cor-menu-secundario);
  opacity: 0.65;
  border-radius: var(--raio-sm);
}
.grafismo__linhas span:last-child {
  width: 65%;
  opacity: 0.35;
}
.grafismo__check {
  width: 25px;
  height: 25px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--cor-detalhe);
  color: var(--cor-menu);
}
.grafismo__ponto {
  position: absolute;
  right: 20px;
  top: 20px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--cor-detalhe);
}
.login__rodape-marca {
  margin-top: auto;
  padding-top: var(--espaco-8);
  color: var(--cor-menu-secundario);
  font-size: var(--fonte-xs);
  position: relative;
  z-index: 1;
}
.login__acesso {
  min-width: 0;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: var(--login-espaco);
  position: relative;
}
.login__formulario {
  width: min(100%, 390px);
  margin: auto;
  padding: var(--espaco-10) 0;
}
.login__marca-mobile {
  display: var(--login-marca-mobile);
  align-items: center;
  gap: var(--espaco-2);
  margin-bottom: var(--espaco-10);
  font-weight: var(--peso-forte);
  font-size: var(--fonte-lg);
  letter-spacing: -0.04em;
}
.identificador {
  display: inline-flex;
  align-items: center;
  gap: var(--espaco-2);
  font-size: var(--fonte-xs);
  color: var(--cor-texto-secundario);
  margin-bottom: var(--espaco-5);
}
.identificador span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--cor-primaria);
}
h2 {
  font-size: var(--fonte-xl);
  font-weight: var(--peso-forte);
  letter-spacing: -0.035em;
  line-height: 1.25;
}
.login__orientacao {
  color: var(--cor-texto-secundario);
  margin-top: var(--espaco-3);
  font-size: var(--fonte-sm);
  line-height: 1.7;
}
form {
  margin-top: var(--espaco-8);
  display: grid;
  gap: var(--espaco-5);
}
.mostrar-senha {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border: 0;
  background: transparent;
  border-radius: var(--raio-sm);
  color: var(--cor-texto-secundario);
}
.mostrar-senha:hover {
  background: var(--cor-primaria-suave);
}
.login__entrar {
  width: 100%;
  margin-top: var(--espaco-2);
}
.login__ajuda {
  text-align: center;
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-xs);
  line-height: 1.8;
  margin: var(--espaco-6) var(--espaco-4) 0;
}
.login__privacidade {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: var(--espaco-2);
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-xs);
  line-height: 1.5;
  border-top: var(--borda-fina) solid var(--cor-borda);
  padding-top: var(--espaco-6);
  margin-top: var(--espaco-8);
}
.login__rodape {
  display: flex;
  justify-content: space-between;
  gap: var(--espaco-4);
  font-size: var(--fonte-xs);
  color: var(--cor-texto-secundario);
  padding-top: var(--espaco-6);
}
.aviso {
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-sm);
  padding: var(--espaco-3) var(--espaco-4);
  font-size: var(--fonte-sm);
  line-height: 1.6;
}
.aviso--erro {
  color: var(--cor-erro);
  background: var(--cor-erro-fundo);
  border-color: var(--cor-erro);
}
</style>
