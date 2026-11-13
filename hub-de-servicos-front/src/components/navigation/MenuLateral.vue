<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ambiente } from '@/app/config/ambiente'
import { atalhosMenu, gruposMenu } from '@/app/router/menu'
import IconeBase from '@/components/ui/IconeBase.vue'

const props = defineProps<{
  compacto: boolean
  aberto: boolean
  saindo: boolean
  permissoes?: readonly string[]
}>()
const emit = defineEmits<{ fechar: []; sair: [] }>()
const rota = useRoute()
const painel = ref<HTMLElement>()
const recolhidoPeloItem = ref(false)
const gruposAbertos = ref<Record<string, boolean>>({})
const gruposVisiveis = computed(() =>
  gruposMenu
    .map((grupo) => ({
      ...grupo,
      telas: grupo.telas.filter(
        (tela) => !tela.permissao || props.permissoes?.includes(tela.permissao),
      ),
    }))
    .filter((grupo) => grupo.telas.length > 0),
)
const telaAtiva = computed(() => rota.meta.itemMenu ?? rota.name)
const grupoAtivo = computed(
  () =>
    gruposVisiveis.value.find((grupo) => grupo.telas.some((tela) => tela.nome === telaAtiva.value))
      ?.id,
)
let focoAnterior: HTMLElement | null = null
let rolagemAnterior: string | undefined

function liberarRolagem() {
  if (rolagemAnterior !== undefined) {
    document.body.style.overflow = rolagemAnterior
    rolagemAnterior = undefined
  }
}

function fechar() {
  emit('fechar')
}

function selecionarItem() {
  recolhidoPeloItem.value = true
  fechar()
}

function sair() {
  selecionarItem()
  emit('sair')
}

function grupoAberto(id: string) {
  return grupoAtivo.value === id || !!gruposAbertos.value[id]
}

function alternarGrupo(id: string) {
  recolhidoPeloItem.value = false
  if (grupoAtivo.value !== id) gruposAbertos.value[id] = !gruposAbertos.value[id]
}

function tratarTeclado(evento: KeyboardEvent) {
  if (!props.compacto || !props.aberto) return
  if (evento.key === 'Escape') {
    evento.preventDefault()
    fechar()
  }
  if (evento.key === 'Tab') {
    const alvos = Array.from(
      painel.value?.querySelectorAll<HTMLElement>('a[href], button:not([disabled])') ?? [],
    ).filter((alvo) => !alvo.closest('[inert]'))
    if (!alvos?.length) return
    const primeiro = alvos[0]
    const ultimo = alvos[alvos.length - 1]
    if (evento.shiftKey && document.activeElement === primeiro) {
      evento.preventDefault()
      ultimo?.focus()
    } else if (!evento.shiftKey && document.activeElement === ultimo) {
      evento.preventDefault()
      primeiro?.focus()
    }
  }
}

watch(
  () => props.compacto && props.aberto,
  async (aberto) => {
    if (aberto) {
      rolagemAnterior = document.body.style.overflow
      document.body.style.overflow = 'hidden'
      focoAnterior = document.activeElement instanceof HTMLElement ? document.activeElement : null
      document.addEventListener('keydown', tratarTeclado)
      await nextTick()
      if (!props.compacto || !props.aberto) return
      painel.value?.querySelector<HTMLButtonElement>('button')?.focus()
    } else {
      liberarRolagem()
      document.removeEventListener('keydown', tratarTeclado)
      const restaurarFoco = focoAnterior
      focoAnterior = null
      await nextTick()
      if (!props.compacto || !props.aberto) restaurarFoco?.focus()
    }
  },
  { immediate: true },
)

watch(() => rota.path, fechar)
onBeforeUnmount(() => {
  document.removeEventListener('keydown', tratarTeclado)
  liberarRolagem()
})
</script>

<template>
  <Transition name="fundo">
    <div v-if="compacto && aberto" class="menu-fundo" aria-hidden="true" @click="fechar"></div>
  </Transition>
  <aside
    id="menu-principal"
    ref="painel"
    class="menu-lateral"
    :class="{
      'menu-lateral--compacto': compacto,
      'menu-lateral--aberto': aberto,
      'menu-lateral--recolhido-pelo-item': recolhidoPeloItem,
    }"
    :role="compacto ? 'dialog' : undefined"
    :aria-modal="compacto && aberto ? true : undefined"
    :aria-hidden="compacto && !aberto ? true : undefined"
    :inert.attr="compacto && !aberto ? true : undefined"
    aria-label="Menu principal"
    @mouseleave="recolhidoPeloItem = false"
    @keydown.tab="recolhidoPeloItem = false"
  >
    <div class="menu__marca">
      <span class="menu__simbolo"><IconeBase nome="marca" :tamanho="25" /></span>
      <span class="menu__texto marca-texto">{{ ambiente.nomeAplicacao }}</span>
      <button
        v-if="compacto"
        type="button"
        class="menu__fechar"
        aria-label="Fechar menu"
        @click="fechar"
      >
        <IconeBase nome="fechar" :tamanho="20" />
      </button>
    </div>

    <nav class="menu__navegacao" aria-label="Navegação principal">
      <span class="menu__secao menu__texto">PRINCIPAL</span>
      <RouterLink
        v-for="atalho in atalhosMenu"
        :key="atalho.nome"
        :to="{ name: atalho.nome }"
        class="menu__item"
        :aria-label="atalho.titulo"
        :title="atalho.titulo"
        @click="selecionarItem"
      >
        <span class="menu__icone"><IconeBase :nome="atalho.icone" /></span>
        <span class="menu__texto">{{ atalho.titulo }}</span>
      </RouterLink>
      <div v-for="grupo in gruposVisiveis" :key="grupo.id" class="menu__grupo">
        <button
          type="button"
          class="menu__item menu__grupo-botao"
          :class="{ 'menu__grupo-botao--ativo': grupoAtivo === grupo.id }"
          :title="grupo.titulo"
          :aria-label="grupo.titulo"
          :aria-controls="'submenu-' + grupo.id"
          :aria-expanded="grupoAberto(grupo.id)"
          :aria-disabled="grupoAtivo === grupo.id || undefined"
          @click="alternarGrupo(grupo.id)"
        >
          <span class="menu__icone"><IconeBase :nome="grupo.icone" /></span>
          <span class="menu__texto menu__grupo-titulo">{{ grupo.titulo }}</span>
          <IconeBase nome="chevron" :tamanho="16" class="menu__texto menu__seta" />
        </button>
        <div
          :id="'submenu-' + grupo.id"
          class="menu__submenu"
          :class="{ 'menu__submenu--aberto': grupoAberto(grupo.id) }"
          :inert.attr="!grupoAberto(grupo.id) ? true : undefined"
        >
          <div class="menu__subitens">
            <RouterLink
              v-for="tela in grupo.telas"
              :key="tela.nome"
              :to="{ name: tela.nome, query: tela.nome === telaAtiva ? rota.query : undefined }"
              class="menu__item menu__subitem"
              :class="{ 'menu__subitem--ativo': tela.nome === telaAtiva }"
              @click="selecionarItem"
            >
              <IconeBase :nome="tela.icone" :tamanho="17" />
              {{ tela.titulo }}
            </RouterLink>
          </div>
        </div>
      </div>
    </nav>

    <div class="menu__rodape">
      <p class="menu__nota menu__texto">Administração de acessos</p>
      <button
        type="button"
        class="menu__item menu__sair"
        :disabled="saindo"
        aria-label="Sair da conta"
        title="Sair"
        @click="sair"
      >
        <span class="menu__icone"><IconeBase nome="sair" /></span>
        <span class="menu__texto">{{ saindo ? 'Saindo…' : 'Sair da conta' }}</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.menu-lateral {
  --cor-foco: var(--cor-foco-menu);
  position: fixed;
  z-index: 30;
  inset: 0 auto 0 0;
  width: var(--largura-menu-recolhido);
  background: var(--cor-menu);
  color: var(--cor-menu-texto);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition:
    background-color var(--transicao-normal),
    color var(--transicao-normal),
    width var(--transicao-normal) var(--curva-transicao),
    transform var(--transicao-normal) var(--curva-transicao);
}
.menu-lateral:not(.menu-lateral--compacto, .menu-lateral--recolhido-pelo-item):is(
    :hover,
    :has(:focus-visible)
  ),
.menu-lateral--aberto:not(.menu-lateral--compacto) {
  width: var(--largura-menu-expandido);
  box-shadow: var(--sombra-menu);
}
.menu-lateral {
  --submenu-linhas: 0fr;
  --submenu-visibilidade: hidden;
}
.menu-lateral:not(.menu-lateral--compacto, .menu-lateral--recolhido-pelo-item):is(
    :hover,
    :has(:focus-visible)
  ),
.menu-lateral--aberto {
  --submenu-linhas: 1fr;
  --submenu-visibilidade: visible;
  --menu-item-padding-left: 10px;
}
.menu__marca {
  height: var(--altura-cabecalho);
  display: flex;
  align-items: center;
  flex-shrink: 0;
  white-space: nowrap;
  border-bottom: var(--borda-fina) solid var(--cor-menu-hover);
}
.menu__simbolo {
  width: var(--largura-menu-recolhido);
  flex-shrink: 0;
  display: flex;
  justify-content: center;
  color: var(--cor-detalhe);
}
.menu__texto {
  opacity: 0;
  transition: opacity var(--transicao-rapida);
  white-space: nowrap;
}
.menu-lateral:not(.menu-lateral--compacto, .menu-lateral--recolhido-pelo-item):is(
    :hover,
    :has(:focus-visible)
  )
  .menu__texto,
.menu-lateral--aberto .menu__texto {
  opacity: 1;
}
.marca-texto {
  font-size: var(--fonte-lg);
  font-weight: var(--peso-forte);
  letter-spacing: -0.04em;
}
.menu__navegacao {
  padding: var(--espaco-8) var(--espaco-3);
  display: grid;
  grid-template-columns: minmax(0, 1fr);
  align-content: start;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  gap: var(--espaco-3);
}
.menu__grupo-botao {
  font-weight: var(--peso-forte);
}
.menu__grupo-botao--ativo {
  color: var(--cor-detalhe);
}
.menu__grupo-titulo {
  flex: 1;
}
.menu__seta {
  margin: 0 var(--espaco-3);
  flex-shrink: 0;
  transition: transform var(--transicao-normal);
}
.menu__grupo-botao[aria-expanded='true'] .menu__seta {
  transform: rotate(180deg);
}
.menu__submenu {
  display: grid;
  grid-template-rows: 0fr;
  visibility: hidden;
  transition:
    grid-template-rows var(--transicao-normal) var(--curva-transicao),
    visibility var(--transicao-normal);
}
.menu__submenu--aberto {
  grid-template-rows: var(--submenu-linhas);
  visibility: var(--submenu-visibilidade);
}
.menu__subitens {
  overflow: hidden;
  min-height: 0;
  padding-left: 20px;
}
.menu__subitem {
  margin-top: var(--espaco-1);
  min-height: 2.5rem;
  padding: var(--espaco-2) var(--espaco-3) var(--espaco-2) var(--espaco-4);
  gap: var(--espaco-2);
  color: var(--cor-menu-secundario);
  white-space: nowrap;
}
.menu__subitem--ativo {
  background: var(--cor-menu-hover);
  color: var(--cor-detalhe);
  font-weight: var(--peso-forte);
  box-shadow: inset 3px 0 var(--cor-detalhe);
}
.menu__secao {
  color: var(--cor-menu-secundario);
  font-size: var(--fonte-xs);
  letter-spacing: 0.12em;
  padding-left: var(--espaco-4);
  margin-bottom: var(--espaco-2);
}
.menu__item {
  display: flex;
  align-items: center;
  min-height: var(--altura-controle);
  border: 0;
  border-radius: var(--raio-sm);
  background: transparent;
  color: var(--cor-menu-texto);
  text-decoration: none;
  text-align: left;
  font-size: var(--fonte-sm);
  width: 100%;
  padding: 0;
  padding-left: var(--menu-item-padding-left, 0px);
}
.menu__icone {
  display: flex;
  justify-content: center;
  width: calc(var(--largura-menu-recolhido) - var(--espaco-6));
  flex-shrink: 0;
}
.menu__item:hover,
.menu__item.router-link-exact-active {
  background: var(--cor-menu-hover);
}
.menu__item.router-link-exact-active {
  color: var(--cor-detalhe);
}
.menu__rodape {
  margin-top: auto;
  padding: var(--espaco-5) var(--espaco-3);
  border-top: var(--borda-fina) solid var(--cor-menu-hover);
}
.menu__nota {
  color: var(--cor-menu-secundario);
  font-size: var(--fonte-xs);
  padding: var(--espaco-3) var(--espaco-4);
}
.menu__fechar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  margin-left: auto;
  margin-right: var(--espaco-3);
  border: 0;
  border-radius: var(--raio-sm);
  background: transparent;
  color: var(--cor-menu-texto);
}
.menu__fechar:hover {
  background: var(--cor-menu-hover);
}
.menu-lateral--compacto {
  width: min(var(--largura-menu-expandido), calc(100vw - var(--espaco-8)));
  transform: translateX(-100%);
}
.menu-lateral--compacto.menu-lateral--aberto {
  transform: translateX(0);
  box-shadow: var(--sombra-menu);
}
.menu-fundo {
  position: fixed;
  inset: 0;
  z-index: 20;
  background: var(--cor-overlay);
}
.fundo-enter-active,
.fundo-leave-active {
  transition: opacity var(--transicao-normal);
}
.fundo-enter-from,
.fundo-leave-to {
  opacity: 0;
}
</style>
