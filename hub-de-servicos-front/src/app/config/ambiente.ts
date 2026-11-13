export const slugAdministrativo = 'hub-de-servicos-admin'
const slug = import.meta.env.VITE_APLICACAO_SLUG || slugAdministrativo

if (!/^[a-z0-9]+(?:-[a-z0-9]+)*$/.test(slug)) {
  throw new Error('VITE_APLICACAO_SLUG deve ser um identificador válido de aplicação.')
}

export const ambiente = Object.freeze({
  slugAplicacao: slug,
  nomeAplicacao: import.meta.env.VITE_APLICACAO_NOME || 'Hub de Serviços',
  apiBase: '/api/v1',
})
