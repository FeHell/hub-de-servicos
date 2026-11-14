import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import type { ValorEnum } from '@/shared/traducoes/enums'

interface ConfiguracaoSms {
  valor: string
  status: ValorEnum<'statusCadastro'>
}

interface ProvedorSms {
  codigo: string
  nome: string
}

export interface ConfiguracaoExterna {
  chave: string
  configurada: boolean
  sensivel: boolean
  status: ValorEnum<'statusCadastro'>
  valor?: string
}

export interface ServicoExterno {
  servico: string
  configuracoes: ConfiguracaoExterna[]
}

const base = '/admin/configuracoes'
const sms = `${base}/sms`
const servicos = `${base}/servicos`
const detalheServico = (servico: string) => `${servicos}/${encodeURIComponent(servico)}`

export function useConfiguracaoApi() {
  const { requisitarAutenticado: requisitar } = useAutenticacao()
  return {
    listarServicos: () => requisitar<ServicoExterno[]>(servicos),
    consultarServico: (servico: string) => requisitar<ServicoExterno>(detalheServico(servico)),
    atualizarServico: (servico: string, valores: Record<string, string>) =>
      requisitar<ServicoExterno>(detalheServico(servico), {
        metodo: 'PUT',
        corpo: { valores },
      }),
    listarProvedoresSms: () => requisitar<ProvedorSms[]>(`${sms}/provedores`),
    consultarProvedorSms: () => requisitar<ConfiguracaoSms>(`${sms}/provedor-ativo`),
    alterarProvedorSms: (codigo: string) =>
      requisitar<ConfiguracaoSms>(`${sms}/provedor-ativo`, {
        metodo: 'PUT',
        corpo: { codigo },
      }),
  }
}
