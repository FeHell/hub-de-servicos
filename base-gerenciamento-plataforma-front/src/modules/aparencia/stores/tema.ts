import { defineStore } from 'pinia'
import { computed, shallowRef, ref, watch } from 'vue'
import { gravarTema, lerTema } from '../armazenamentoTema'
import {
  corHover,
  corLegivel,
  misturar,
  normalizarHex,
  papeisTema,
  textoSobreCor,
  type CoresPersonalizadas,
  type CoresTema,
  type PapelTema,
} from '../coresTema'

export const useTema = defineStore('tema', () => {
  const raiz = document.documentElement
  const css = getComputedStyle(raiz)
  const ler = (nome: string) =>
    css
      .getPropertyValue('--cor-' + nome)
      .trim()
      .toUpperCase()
  const padrao: CoresTema = {
    primaria: ler('primaria-padrao'),
    secundaria: ler('secundaria-padrao'),
    terciaria: ler('terciaria-padrao'),
  }
  const superficie = ler('superficie')
  const fundo = ler('fundo')
  const menuHoverPadrao = ler('menu-hover')
  const focoPadrao = ler('foco')
  const personalizadas = shallowRef(lerTema())
  const erroPersistencia = ref(false)
  const cores = computed<CoresTema>(() => ({ ...padrao, ...personalizadas.value }))
  const personalizado = computed(() => Object.keys(personalizadas.value).length > 0)

  const padraoGrafico = [padrao.primaria, ler('aviso'), ler('borda-hover')]
  const coresGrafico = computed(() =>
    papeisTema.map((papel, i) =>
      personalizadas.value[papel]
        ? corLegivel(cores.value[papel], [superficie], '#000000', 3)
        : padraoGrafico[i]!,
    ),
  )

  const tokens = computed(() => {
    const { primaria, secundaria, terciaria } = cores.value
    const cor = personalizadas.value
    const resultado: Record<string, string> = {}
    if (cor.primaria) {
      const texto = textoSobreCor(primaria)
      const suave = misturar(primaria, superficie, 0.93)
      resultado['--cor-primaria'] = primaria
      resultado['--cor-sobre-primaria'] = texto
      resultado['--cor-primaria-hover'] = corHover(primaria, texto)
      resultado['--cor-primaria-suave'] = suave
      resultado['--cor-primaria-texto'] = corLegivel(
        primaria,
        [fundo, superficie, suave],
        '#000000',
      )
      resultado['--cor-foco'] = corLegivel(primaria, [fundo, superficie], '#000000', 3)
    }

    const textoMenu = textoSobreCor(secundaria)
    const menuHover = cor.secundaria ? corHover(secundaria, textoMenu) : menuHoverPadrao
    if (cor.secundaria) {
      resultado['--cor-secundaria'] = secundaria
      resultado['--cor-menu-hover'] = menuHover
      resultado['--cor-menu-texto'] = textoMenu
      resultado['--cor-menu-secundario'] = corLegivel(
        misturar(textoMenu, secundaria, 0.2),
        [secundaria, menuHover],
        textoMenu,
      )
    }
    if (cor.terciaria) resultado['--cor-terciaria'] = terciaria
    if (cor.secundaria || cor.terciaria) {
      resultado['--cor-detalhe'] = corLegivel(terciaria, [secundaria, menuHover], textoMenu)
    }
    if (cor.primaria || cor.secundaria) {
      resultado['--cor-foco-menu'] = corLegivel(
        cor.primaria ?? focoPadrao,
        [secundaria, menuHover],
        textoMenu,
        3,
      )
    }
    return resultado
  })

  watch(
    tokens,
    (novos, anteriores) => {
      for (const nome of Object.keys(anteriores ?? {})) {
        if (!(nome in novos)) raiz.style.removeProperty(nome)
      }
      for (const [nome, valor] of Object.entries(novos)) raiz.style.setProperty(nome, valor)
    },
    { immediate: true, flush: 'sync' },
  )

  function salvar(proximas: CoresPersonalizadas) {
    personalizadas.value = proximas
    erroPersistencia.value = !gravarTema(proximas)
  }

  function escolher(papel: PapelTema, valor: string) {
    const cor = normalizarHex(valor)
    if (!cor) return false
    const proximas = { ...personalizadas.value }
    if (cor === padrao[papel]) delete proximas[papel]
    else proximas[papel] = cor
    salvar(proximas)
    return true
  }

  function restaurar() {
    salvar({})
  }

  return { cores, padrao, coresGrafico, personalizado, erroPersistencia, escolher, restaurar }
})
