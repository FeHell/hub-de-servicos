import { normalizarHex, papeisTema, type CoresPersonalizadas } from './coresTema'

const chave = 'hub-de-servicos.tema.cores'

export function lerTema(): CoresPersonalizadas {
  try {
    const valor: unknown = JSON.parse(localStorage.getItem(chave) ?? '{}')
    if (!valor || typeof valor !== 'object' || Array.isArray(valor)) return {}
    const dados = valor as Record<string, unknown>
    const cores: CoresPersonalizadas = {}
    for (const papel of papeisTema) {
      const cor = normalizarHex(dados[papel])
      if (cor) cores[papel] = cor
    }
    return cores
  } catch {
    return {}
  }
}

export function gravarTema(cores: CoresPersonalizadas) {
  try {
    if (Object.keys(cores).length === 0) localStorage.removeItem(chave)
    else localStorage.setItem(chave, JSON.stringify(cores))
    return true
  } catch {
    return false
  }
}
