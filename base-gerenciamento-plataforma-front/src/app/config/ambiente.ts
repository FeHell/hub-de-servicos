const slug = import.meta.env.VITE_APLICACAO_SLUG || 'plataforma-admin'

if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
  throw new Error('VITE_APLICACAO_SLUG deve ser um identificador válido de aplicação.')
}

export const ambiente = Object.freeze({
  slugAplicacao: slug,
  nomeAplicacao: import.meta.env.VITE_APLICACAO_NOME || 'Administração da plataforma',
  apiBase: '/api/v1',
})
