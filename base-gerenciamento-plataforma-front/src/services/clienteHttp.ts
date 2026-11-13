import { ambiente } from '@/app/config/ambiente'

export class ErroApi extends Error {
  constructor(
    mensagem: string,
    readonly status: number,
  ) {
    super(mensagem)
    this.name = 'ErroApi'
  }
}

export interface OpcoesRequisicao {
  metodo?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  corpo?: unknown
  token?: string
  sessaoNavegador?: boolean
}

export async function requisitar<T>(
  caminho: string,
  { metodo = 'GET', corpo, token, sessaoNavegador = false }: OpcoesRequisicao = {},
): Promise<T> {
  let resposta: Response
  try {
    resposta = await fetch(ambiente.apiBase + caminho, {
      method: metodo,
      headers: {
        Accept: 'application/json',
        ...(corpo === undefined ? {} : { 'Content-Type': 'application/json' }),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(sessaoNavegador ? { 'X-Sessao-Navegador': '1' } : {}),
      },
      body: corpo === undefined ? undefined : JSON.stringify(corpo),
      credentials: sessaoNavegador ? 'same-origin' : 'omit',
      cache: 'no-store',
      signal: AbortSignal.timeout(15_000),
    })
  } catch {
    throw new ErroApi('Não foi possível conectar. Confira sua conexão e tente novamente.', 0)
  }

  if (!resposta.ok) {
    const detalhe: unknown = await resposta.json().catch(() => null)
    const mensagem =
      detalhe !== null &&
      typeof detalhe === 'object' &&
      'detail' in detalhe &&
      typeof detalhe.detail === 'string'
        ? detalhe.detail
        : 'Não foi possível concluir a solicitação.'
    throw new ErroApi(mensagem, resposta.status)
  }

  if (resposta.status === 204) return undefined as T
  return (await resposta.json()) as T
}
