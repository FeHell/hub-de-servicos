import { describe, expect, it, vi } from 'vitest'
import { opcoesEnum, traduzirEnum } from './enums'

describe('tradução de enums', () => {
  it.each([
    ['PLATAFORMA_ADMINISTRAR', 'Administrador da plataforma'],
    ['PLATAFORMA_ANALISTA', 'Analista da plataforma'],
    ['PLATAFORMA_VISITANTE', 'Visitante da plataforma'],
  ])('traduz %s e prioriza a tradução central', (codigo, texto) => {
    expect(traduzirEnum('permissao', codigo, 'Nome anterior')).toBe(texto)
  })

  it('preserva os valores originais nas opções', () => {
    expect(opcoesEnum('statusUsuario')).toEqual([
      { valor: 'ATIVO', texto: 'Ativo' },
      { valor: 'BLOQUEADO', texto: 'Bloqueado' },
      { valor: 'PENDENTE', texto: 'Pendente' },
      { valor: 'INATIVO', texto: 'Inativo' },
    ])
    expect(traduzirEnum('statusPermissao', 'INATIVO')).toBe('Inativa')
  })

  it('aproveita o nome amigável de uma permissão cadastrada', () => {
    expect(traduzirEnum('permissao', 'PLATAFORMA_SUPORTE', 'Equipe de suporte')).toBe(
      'Equipe de suporte',
    )
  })

  it('avisa uma vez e nunca exibe um código sem tradução', () => {
    const aviso = vi.spyOn(console, 'warn').mockImplementation(() => undefined)
    try {
      expect(traduzirEnum('permissao', 'SEM_ROTULO', 'SEM_ROTULO')).toBe('Permissão sem rótulo')
      expect(traduzirEnum('permissao', 'SEM_ROTULO')).toBe('Permissão sem rótulo')
      expect(aviso).toHaveBeenCalledExactlyOnceWith(
        'Tradução de enum ausente: permissao.SEM_ROTULO',
      )
      expect(traduzirEnum('statusUsuario', 'toString')).toBe('Não identificado')
    } finally {
      aviso.mockRestore()
    }
  })
})
