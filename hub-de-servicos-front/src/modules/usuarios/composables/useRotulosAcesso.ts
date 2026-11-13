import { onMounted, ref } from 'vue'
import { ambiente, slugAdministrativo } from '@/app/config/ambiente'
import { traduzirEnum } from '@/shared/traducoes/enums'
import { useUsuariosApi } from '../services/usuariosApi'

export function useRotulosAcesso() {
  const api = useUsuariosApi()
  const aplicacoes = ref(new Map([[slugAdministrativo, ambiente.nomeAplicacao]]))
  const permissoes = ref(new Map<string, string>())

  onMounted(() => {
    // As consultas complementam os rótulos. A listagem continua disponível se o catálogo falhar.
    void api.listarAplicacoes().then(
      (itens) => {
        aplicacoes.value = new Map([
          ...aplicacoes.value,
          ...itens.map(({ slug, nome }) => [slug, nome] as const),
        ])
      },
      () => undefined,
    )
    void api.listarPermissoesPlataforma().then(
      (itens) => {
        permissoes.value = new Map(itens.map(({ codigo, nome }) => [codigo, nome]))
      },
      () => undefined,
    )
  })

  return {
    nomeAplicacao: (slug: string) => aplicacoes.value.get(slug) || 'Aplicação sem nome',
    nomePermissao: (codigo: string) =>
      traduzirEnum('permissao', codigo, permissoes.value.get(codigo)),
  }
}
