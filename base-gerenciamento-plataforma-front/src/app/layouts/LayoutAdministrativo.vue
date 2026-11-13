<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useTelaCompacta } from '@/composables/useTelaCompacta'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import { ambiente } from '@/app/config/ambiente'
import IconeBase from '@/components/ui/IconeBase.vue'
import MenuLateral from '@/components/navigation/MenuLateral.vue'

const compacto = useTelaCompacta()
const menuAberto = ref(false)
const botaoMenu = ref<HTMLButtonElement>()
const autenticacao = useAutenticacao()
const router = useRouter()

watch(compacto, () => (menuAberto.value = false))

function fecharAoClicarFora(evento: MouseEvent) {
  const acionador = botaoMenu.value
  if (!acionador || !evento.composedPath().includes(acionador)) menuAberto.value = false
}

async function sair() {
  menuAberto.value = false
  if (await autenticacao.sair()) await router.replace({ name: 'login' })
}
</script>

<template>
  <div class="layout" :class="{ 'layout--compacto': compacto }">
    <a href="#conteudo-principal" class="pular-conteudo" @click="menuAberto = false">
      Pular para o conteúdo
    </a>
    <MenuLateral
      :compacto="compacto"
      :aberto="menuAberto"
      :saindo="autenticacao.saindo"
      :permissoes="autenticacao.permissoes"
      @fechar="menuAberto = false"
      @sair="sair"
    />
    <div
      class="layout__pagina"
      :inert.attr="compacto && menuAberto ? true : undefined"
      @click.capture="fecharAoClicarFora"
    >
      <header class="cabecalho">
        <div class="cabecalho__esquerda">
          <button
            ref="botaoMenu"
            type="button"
            class="cabecalho__menu"
            aria-label="Alternar menu principal"
            aria-controls="menu-principal"
            :aria-expanded="menuAberto"
            @click="menuAberto = !menuAberto"
          >
            <IconeBase nome="menu" :tamanho="21" />
          </button>
          <span class="cabecalho__produto">{{ ambiente.nomeAplicacao }}</span>
        </div>
        <div class="cabecalho__perfil">
          <span class="cabecalho__nome">{{ autenticacao.usuario?.login }}</span>
          <span class="avatar" aria-hidden="true">{{
            autenticacao.usuario?.login.slice(0, 2).toUpperCase()
          }}</span>
        </div>
      </header>
      <main id="conteudo-principal" class="conteudo" tabindex="-1">
        <p v-if="autenticacao.aviso" class="aviso-sessao" role="alert">{{ autenticacao.aviso }}</p>
        <RouterView />
      </main>
      <footer class="rodape">Plataforma de gerenciamento <span>Área administrativa</span></footer>
    </div>
  </div>
</template>

<style scoped>
.layout {
  min-height: 100dvh;
}
.layout__pagina {
  min-height: 100dvh;
  margin-left: var(--largura-menu-recolhido);
  display: flex;
  flex-direction: column;
}
.layout--compacto .layout__pagina {
  margin-left: 0;
}
.cabecalho {
  min-height: var(--altura-cabecalho);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--espaco-4);
  padding: var(--espaco-4) var(--espaco-8);
  background: var(--cor-superficie);
  border-bottom: var(--borda-fina) solid var(--cor-borda);
}
.cabecalho__esquerda,
.cabecalho__perfil {
  display: flex;
  align-items: center;
  gap: var(--espaco-3);
  min-width: 0;
}
.cabecalho__menu {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: var(--raio-sm);
  background: transparent;
  color: var(--cor-texto-secundario);
  flex-shrink: 0;
}
.cabecalho__menu:hover {
  background: var(--cor-primaria-suave);
}
.cabecalho__produto {
  font-size: var(--fonte-sm);
  font-weight: var(--peso-medio);
}
.cabecalho__nome {
  font-size: var(--fonte-sm);
  color: var(--cor-texto-secundario);
  max-width: 15rem;
  text-overflow: ellipsis;
  overflow: hidden;
}
.avatar {
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  background: var(--cor-primaria-suave);
  color: var(--cor-primaria-texto);
  border-radius: 50%;
  font-size: var(--fonte-xs);
  font-weight: var(--peso-forte);
  flex-shrink: 0;
}
.conteudo {
  width: 100%;
  max-width: 1300px;
  margin: 0 auto;
  padding: var(--espaco-12) var(--espaco-10);
  flex: 1;
}
.rodape {
  display: flex;
  justify-content: space-between;
  gap: var(--espaco-4);
  padding: var(--espaco-6) var(--espaco-10);
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-xs);
}
.aviso-sessao {
  margin-bottom: var(--espaco-5);
  padding: var(--espaco-3) var(--espaco-4);
  border-radius: var(--raio-sm);
  background: var(--cor-erro-fundo);
  color: var(--cor-erro);
}
.layout--compacto .cabecalho {
  padding: var(--espaco-3) var(--espaco-4);
}
.layout--compacto .cabecalho__nome {
  display: none;
}
.layout--compacto .conteudo {
  padding: var(--espaco-8) var(--espaco-5);
}
.layout--compacto .rodape {
  padding: var(--espaco-5);
  flex-wrap: wrap;
}
.pular-conteudo {
  position: fixed;
  top: var(--espaco-3);
  left: var(--espaco-3);
  transform: translateY(-200%);
  z-index: 50;
  border-radius: var(--raio-sm);
  padding: var(--espaco-3);
  color: var(--cor-sobre-primaria);
  background: var(--cor-primaria);
}
.pular-conteudo:focus {
  transform: translateY(0);
}
</style>
