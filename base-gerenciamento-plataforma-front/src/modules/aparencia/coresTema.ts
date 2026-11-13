export const papeisTema = ['primaria', 'secundaria', 'terciaria'] as const
export type PapelTema = (typeof papeisTema)[number]
export type CoresTema = Record<PapelTema, string>
export type CoresPersonalizadas = Partial<CoresTema>

export function normalizarHex(valor: unknown) {
  if (typeof valor === 'string' && valor.length === 7 && /^#[\da-f]{6}$/i.test(valor)) {
    return valor.toUpperCase()
  }
}

function canais(cor: string) {
  return [1, 3, 5].map((inicio) => parseInt(cor.slice(inicio, inicio + 2), 16))
}

export function misturar(cor: string, outra: string, proporcao: number) {
  const destino = canais(outra)
  return (
    '#' +
    canais(cor)
      .map((canal, indice) =>
        Math.round(canal + (destino[indice]! - canal) * proporcao)
          .toString(16)
          .padStart(2, '0'),
      )
      .join('')
      .toUpperCase()
  )
}

function luminancia(cor: string) {
  const [r, g, b] = canais(cor).map((canal) => {
    const s = canal / 255
    return s <= 0.04045 ? s / 12.92 : ((s + 0.055) / 1.055) ** 2.4
  })
  return 0.2126 * r! + 0.7152 * g! + 0.0722 * b!
}

export function contraste(cor: string, fundo: string) {
  const a = luminancia(cor)
  const b = luminancia(fundo)
  return (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05)
}

const preto = '#000000'
const branco = '#FFFFFF'

export function textoSobreCor(fundo: string) {
  return contraste(preto, fundo) >= contraste(branco, fundo) ? preto : branco
}

export function corLegivel(cor: string, fundos: string[], destino: string, minimo = 4.5) {
  const atende = (valor: string) => fundos.every((fundo) => contraste(valor, fundo) >= minimo)
  if (atende(cor)) return cor

  // Aproxima a cor escolhida do tom legível sem perder mais da tonalidade que o necessário.
  let inicio = 0
  let fim = 1
  let legivel = destino
  for (let i = 0; i < 8; i++) {
    const meio = (inicio + fim) / 2
    const candidata = misturar(cor, destino, meio)
    if (atende(candidata)) {
      legivel = candidata
      fim = meio
    } else {
      inicio = meio
    }
  }
  return legivel
}

export function corHover(fundo: string, texto: string) {
  const variante = misturar(fundo, texto, 0.12)
  if (contraste(variante, texto) >= 4.5) return variante
  return misturar(fundo, texto === preto ? branco : preto, 0.12)
}
