import { afterEach, describe, expect, it, vi } from 'vitest'
import { requisitar } from './clienteHttp'

afterEach(() => vi.unstubAllGlobals())

describe('cliente HTTP', () => {
  it('envia JSON e a autorização para a API configurada', async () => {
    const buscar = vi.fn().mockResolvedValue(new Response(JSON.stringify({ ok: true })))
    vi.stubGlobal('fetch', buscar)
    await expect(
      requisitar('/teste', {
        metodo: 'POST',
        corpo: { exemplo: true },
        token: 'token-ficticio',
      }),
    ).resolves.toEqual({ ok: true })
    expect(buscar).toHaveBeenCalledWith(
      '/api/v1/teste',
      expect.objectContaining({
        method: 'POST',
        body: JSON.stringify({ exemplo: true }),
        credentials: 'omit',
        headers: expect.objectContaining({ Authorization: 'Bearer token-ficticio' }),
      }),
    )
  })

  it('preserva o status e a mensagem padronizada de erro', async () => {
    vi.stubGlobal(
      'fetch',
      vi
        .fn()
        .mockResolvedValue(
          new Response(JSON.stringify({ detail: 'Acesso negado.' }), { status: 403 }),
        ),
    )
    await expect(requisitar('/teste')).rejects.toMatchObject({
      status: 403,
      message: 'Acesso negado.',
    })
  })

  it('transforma indisponibilidade em mensagem de conexão sem expor detalhes internos', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('detalhe interno')))
    await expect(requisitar('/teste')).rejects.toMatchObject({
      status: 0,
      message: 'Não foi possível conectar. Confira sua conexão e tente novamente.',
    })
  })
})
