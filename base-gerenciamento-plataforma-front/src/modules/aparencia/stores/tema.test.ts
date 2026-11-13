import { readFileSync } from 'node:fs'
import { createPinia, disposePinia, type Pinia } from 'pinia'
import { afterEach, beforeEach, describe, expect, it } from 'vitest'
import { contraste } from '../coresTema'
import { useTema } from './tema'

const chave = 'plataforma.tema.cores'
const raiz = document.documentElement
const padrao = { primaria: '#247366', secundaria: '#173B38', terciaria: '#C9DBC1' }
const claro = readFileSync('src/styles/themes/claro.css', 'utf8')
let estilo: HTMLStyleElement
let pinia: Pinia

function iniciarTema() {
  pinia = createPinia()
  return useTema(pinia)
}

beforeEach(() => {
  localStorage.removeItem(chave)
  estilo = document.createElement('style')
  estilo.textContent = claro
  document.head.append(estilo)
})

afterEach(() => {
  disposePinia(pinia)
  raiz.removeAttribute('style')
  estilo.remove()
  localStorage.removeItem(chave)
})

describe('tema personalizado', () => {
  it('inicia com os padrões e aplica cada papel de forma independente', () => {
    const tema = iniciarTema()
    expect(tema.cores).toEqual(padrao)
    expect(raiz.style.length).toBe(0)
    tema.escolher('primaria', '#2555b5')
    expect(raiz.style.getPropertyValue('--cor-primaria')).toBe('#2555B5')
    expect(tema.cores).toEqual({ ...padrao, primaria: '#2555B5' })
    expect(JSON.parse(localStorage.getItem(chave)!)).toEqual({ primaria: '#2555B5' })

    tema.escolher('secundaria', '#522343')
    expect(raiz.style.getPropertyValue('--cor-secundaria')).toBe('#522343')
    expect(tema.cores).toEqual({ ...padrao, primaria: '#2555B5', secundaria: '#522343' })

    tema.escolher('terciaria', '#ffc688')
    expect(raiz.style.getPropertyValue('--cor-terciaria')).toBe('#FFC688')
    expect(tema.cores).toEqual({ primaria: '#2555B5', secundaria: '#522343', terciaria: '#FFC688' })
    expect(tema.coresGrafico).toEqual(['#2555B5', '#522343', expect.any(String)])
  })

  it('restaura as escolhas em uma nova inicialização e limpa os tokens ao voltar ao padrão', () => {
    const anterior = iniciarTema()
    anterior.escolher('terciaria', '#e6b45c')
    disposePinia(pinia)
    raiz.removeAttribute('style')

    const atual = iniciarTema()
    expect(atual.cores).toEqual({ ...padrao, terciaria: '#E6B45C' })
    expect(raiz.style.getPropertyValue('--cor-terciaria')).toBe('#E6B45C')
    expect(raiz.style.getPropertyValue('--cor-primaria')).toBe('')
    atual.restaurar()
    expect(atual.cores).toEqual(padrao)
    expect(raiz.style.length).toBe(0)
    expect(localStorage.getItem(chave)).toBeNull()
  })

  it('descarta JSON inválido e recupera cores válidas de preferências parciais', () => {
    localStorage.setItem(chave, '{inválido')
    expect(iniciarTema().cores).toEqual(padrao)
    disposePinia(pinia)

    localStorage.setItem(
      chave,
      JSON.stringify({ primaria: 'red', secundaria: '#412f88', terciaria: 3 }),
    )
    const tema = iniciarTema()
    expect(tema.cores).toEqual({ ...padrao, secundaria: '#412F88' })
    expect(tema.escolher('terciaria', 'rgb(0, 0, 0)')).toBe(false)
    expect(tema.escolher('terciaria', '#000000\n')).toBe(false)
    expect(tema.cores.terciaria).toBe(padrao.terciaria)
  })

  it.each([
    ['#FFFFFF', '#FFFFFF', '#FFFFFF'],
    ['#000000', '#000000', '#000000'],
    ['#FFFF00', '#777777', '#888888'],
    ['#333333', '#757575', '#0000FF'],
  ])('preserva contraste com %s, %s e %s', (primaria, secundaria, terciaria) => {
    const tema = iniciarTema()
    tema.escolher('primaria', primaria)
    tema.escolher('secundaria', secundaria)
    tema.escolher('terciaria', terciaria)

    const token = (nome: string) => raiz.style.getPropertyValue('--cor-' + nome)
    for (const fundo of [primaria, token('primaria-hover')]) {
      expect(contraste(token('sobre-primaria'), fundo)).toBeGreaterThanOrEqual(4.5)
    }
    for (const fundo of [secundaria, token('menu-hover')]) {
      for (const cor of ['menu-texto', 'menu-secundario', 'detalhe']) {
        expect(contraste(token(cor), fundo)).toBeGreaterThanOrEqual(4.5)
      }
      expect(contraste(token('foco-menu'), fundo)).toBeGreaterThanOrEqual(3)
    }
    for (const fundo of ['#FFFFFF', '#F5F7F4', token('primaria-suave')]) {
      expect(contraste(token('primaria-texto'), fundo)).toBeGreaterThanOrEqual(4.5)
    }
    expect(contraste(token('foco'), '#FFFFFF')).toBeGreaterThanOrEqual(3)
    for (const cor of tema.coresGrafico) {
      expect(contraste(cor, '#FFFFFF')).toBeGreaterThanOrEqual(3)
    }
  })
})
