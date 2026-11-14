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

const base = '/admin/configuracoes/sms'

export function useConfiguracaoApi() {
  const { requisitarAutenticado: requisitar } = useAutenticacao()
  return {
    listarProvedoresSms: () => requisitar<ProvedorSms[]>(`${base}/provedores`),
    consultarProvedorSms: () => requisitar<ConfiguracaoSms>(`${base}/provedor-ativo`),
    alterarProvedorSms: (codigo: string) =>
      requisitar<ConfiguracaoSms>(`${base}/provedor-ativo`, {
        metodo: 'PUT',
        corpo: { codigo },
      }),
  }
}
