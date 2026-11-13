<script setup lang="ts">
import { computed, ref } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CabecalhoPagina from '@/components/ui/CabecalhoPagina.vue'
import CampoTexto from '@/components/ui/CampoTexto.vue'
import DialogoBase from '@/components/ui/DialogoBase.vue'
import IconeBase from '@/components/ui/IconeBase.vue'
import { textoSobreCor, type PapelTema } from '../coresTema'
import { useTema } from '../stores/tema'

const tema = useTema()
const papeis = [
  { id: 'primaria', titulo: 'Cor primária', descricao: 'Botões, links e destaques.' },
  {
    id: 'secundaria',
    titulo: 'Cor secundária',
    descricao: 'Fundo do menu e do painel de entrada.',
  },
  {
    id: 'terciaria',
    titulo: 'Cor terciária',
    descricao: 'Ícones e detalhes do menu e do painel de entrada.',
  },
] as const
const paleta = [
  '#194D43',
  '#138575',
  '#0086A5',
  '#2555B5',
  '#4338CA',
  '#774BB1',
  '#A8437C',
  '#A8343E',
  '#C65B2A',
  '#D89C20',
  '#66742E',
  '#535D6C',
  '#E2ECF1',
  '#F8E3D7',
  '#EEE5F4',
  '#F4E8B8',
  '#252D37',
  '#F8FAFC',
]
const aberta = ref<PapelTema>()
const hex = ref('')
const erroHex = ref('')
const aviso = ref('')
const papelAtivo = computed(() => papeis.find((papel) => papel.id === aberta.value))
const corAtual = computed(() => (aberta.value ? tema.cores[aberta.value] : ''))
const amostras = computed(() =>
  aberta.value ? [...new Set([tema.padrao[aberta.value], ...paleta, corAtual.value])] : [],
)

function abrir(papel: PapelTema) {
  aberta.value = papel
  hex.value = tema.cores[papel]
  erroHex.value = ''
  aviso.value = ''
}

function aplicar(cor: string) {
  if (!aberta.value) return
  if (!tema.escolher(aberta.value, cor.trim())) {
    erroHex.value = 'Digite uma cor no formato #RRGGBB.'
    return
  }
  hex.value = tema.cores[aberta.value]
  erroHex.value = ''
  aviso.value = `${papelAtivo.value?.titulo ?? 'Cor'} atualizada.`
}

function escolherPersonalizada(evento: Event) {
  if (evento.target instanceof HTMLInputElement) aplicar(evento.target.value)
}

function restaurar() {
  tema.restaurar()
  aviso.value = 'Cores padrão restauradas.'
}
</script>

<template>
  <CabecalhoPagina titulo="Aparência" descricao="Personalize as cores do Hub neste navegador.">
    <BotaoBase
      variante="discreto"
      :desabilitado="!tema.personalizado && !tema.erroPersistencia"
      @click="restaurar"
    >
      Restaurar cores padrão
    </BotaoBase>
  </CabecalhoPagina>

  <div class="cores" aria-label="Cores do tema">
    <button
      v-for="papel in papeis"
      :key="papel.id"
      type="button"
      class="cor"
      aria-haspopup="dialog"
      :aria-label="`Personalizar ${papel.titulo.toLowerCase()}, ${tema.cores[papel.id]}`"
      @click="abrir(papel.id)"
    >
      <span
        class="cor__amostra"
        :style="{ backgroundColor: tema.cores[papel.id] }"
        aria-hidden="true"
      />
      <span class="cor__info">
        <strong>{{ papel.titulo }}</strong>
        <span class="cor__descricao">{{ papel.descricao }}</span>
        <span class="cor__hex">{{ tema.cores[papel.id] }}</span>
      </span>
      <IconeBase nome="editar" :tamanho="17" class="cor__editar" />
    </button>
  </div>

  <p v-if="tema.erroPersistencia && !aberta" class="mensagem erro" role="alert">
    O navegador não permitiu salvar a preferência. As cores foram aplicadas nesta página.
  </p>
  <p v-else-if="aviso && !aberta" class="mensagem" role="status">{{ aviso }}</p>

  <DialogoBase v-if="papelAtivo" :titulo="papelAtivo.titulo" @fechar="aberta = undefined">
    <div class="paleta">
      <div class="paleta__atual">
        <span class="cor__amostra" :style="{ backgroundColor: corAtual }" aria-hidden="true" />
        <span
          >Cor atual <strong class="cor__hex">{{ corAtual }}</strong></span
        >
      </div>
      <div class="paleta__opcoes" role="group" aria-label="Tonalidades disponíveis">
        <button
          v-for="cor in amostras"
          :key="cor"
          type="button"
          class="paleta__opcao"
          :style="{ backgroundColor: cor, color: textoSobreCor(cor) }"
          :aria-label="`Cor ${cor}${cor === tema.padrao[papelAtivo.id] ? ' padrão' : ''}`"
          :aria-pressed="corAtual === cor"
          :title="cor"
          @click="aplicar(cor)"
        >
          <IconeBase v-if="corAtual === cor" nome="check" :tamanho="20" />
        </button>
      </div>

      <div class="personalizada">
        <label for="cor-personalizada">Seletor de cor personalizada</label>
        <input
          id="cor-personalizada"
          type="color"
          :value="corAtual"
          @input="escolherPersonalizada"
        />
      </div>
      <form class="hex" @submit.prevent="aplicar(hex)">
        <CampoTexto
          id="cor-hexadecimal"
          v-model="hex"
          rotulo="Código hexadecimal"
          maxlength="7"
          placeholder="#RRGGBB"
          autocomplete="off"
          spellcheck="false"
          :erro="erroHex"
        />
        <BotaoBase tipo="submit" variante="discreto">Aplicar código</BotaoBase>
      </form>
      <p v-if="tema.erroPersistencia" class="erro" role="alert">
        O navegador não permitiu salvar a preferência.
      </p>
      <p v-else class="somente-leitor" role="status">{{ aviso }}</p>
      <div class="paleta__rodape">
        <BotaoBase @click="aberta = undefined">Concluir</BotaoBase>
      </div>
    </div>
  </DialogoBase>
</template>

<style scoped>
.cores {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 17rem), 1fr));
  gap: var(--espaco-4);
}
.cor {
  display: flex;
  align-items: center;
  gap: var(--espaco-3);
  padding: var(--espaco-4);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
  box-shadow: var(--sombra-card);
  text-align: left;
  transition:
    border-color var(--transicao-rapida),
    background-color var(--transicao-rapida);
}
.cor:hover {
  border-color: var(--cor-primaria-texto);
  background: var(--cor-primaria-suave);
}
.cor__amostra {
  width: 2.75rem;
  height: 2.75rem;
  border: var(--borda-fina) solid var(--cor-borda-hover);
  border-radius: var(--raio-sm);
  flex-shrink: 0;
  transition: background-color var(--transicao-normal);
}
.cor__info {
  display: grid;
  flex: 1;
  gap: var(--espaco-1);
  min-width: 0;
}
.cor__descricao {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
  line-height: 1.45;
}
.cor__hex {
  font-family: ui-monospace, monospace;
  font-size: var(--fonte-sm);
}
.cor__editar {
  color: var(--cor-primaria-texto);
  flex-shrink: 0;
}
.mensagem {
  margin-top: var(--espaco-4);
  color: var(--cor-sucesso);
}
.erro {
  color: var(--cor-erro);
  font-size: var(--fonte-sm);
}
.paleta {
  display: grid;
  gap: var(--espaco-4);
}
.paleta__atual,
.personalizada {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--espaco-3);
  font-size: var(--fonte-sm);
}
.paleta__atual strong {
  display: block;
  margin-top: var(--espaco-1);
}
.paleta__opcoes {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(2.75rem, 1fr));
  gap: var(--espaco-2);
}
.paleta__opcao {
  display: grid;
  place-items: center;
  height: 2.75rem;
  border: var(--borda-fina) solid var(--cor-borda-hover);
  border-radius: var(--raio-sm);
}
.paleta__opcao[aria-pressed='true'] {
  box-shadow: 0 0 0 2px var(--cor-primaria-texto);
}
.personalizada {
  justify-content: space-between;
}
.personalizada input {
  width: 3.5rem;
  min-height: 2.75rem;
  background: var(--cor-superficie);
  border: var(--borda-fina) solid var(--cor-borda-hover);
  border-radius: var(--raio-sm);
  cursor: pointer;
}
.hex {
  display: flex;
  flex-wrap: wrap;
  align-items: end;
  gap: var(--espaco-3);
}
.hex > :first-child {
  flex: 1 1 10rem;
}
.paleta__rodape {
  display: flex;
  justify-content: flex-end;
}
</style>
