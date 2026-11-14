function digitosNacionais(valor: string) {
  const digitos = valor.replace(/[^0-9]/g, '')
  return (valor.trim().startsWith('+55') || digitos.length > 11) && digitos.startsWith('55')
    ? digitos.slice(2)
    : digitos
}

export function normalizarTelefone(valor: string) {
  const numero = digitosNacionais(valor)
  return /^[1-9][0-9](?:[2-5][0-9]{7}|9[0-9]{8})$/.test(numero) &&
    /^[+]?[0-9(). -]+$/.test(valor.trim()) &&
    (!valor.trim().startsWith('+') || valor.trim().startsWith('+55'))
    ? '+55' + numero
    : null
}

export function mascararTelefone(valor: string) {
  if (!/^[+]?[0-9(). -]*$/.test(valor) || (valor.startsWith('+') && !valor.startsWith('+55')))
    return valor
  const numero = digitosNacionais(valor)
  if (numero.length > 11) return valor
  if (numero.length <= 2) return numero
  const ddd = numero.slice(0, 2)
  const corte = numero.startsWith('9', 2) ? 7 : 6
  const prefixo = numero.slice(2, corte)
  const sufixo = numero.slice(corte)
  return `(${ddd}) ${prefixo}${sufixo ? '-' + sufixo : ''}`
}
