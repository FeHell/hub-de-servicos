<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CabecalhoPagina from '@/components/ui/CabecalhoPagina.vue'
import CampoSelecao from '@/components/ui/CampoSelecao.vue'
import CampoTexto from '@/components/ui/CampoTexto.vue'
import IconeBase from '@/components/ui/IconeBase.vue'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'
import ConfirmarExclusaoUsuario from '../components/ConfirmarExclusaoUsuario.vue'
import StatusUsuarioBadge from '../components/StatusUsuarioBadge.vue'
import { opcoesStatus, opcoesTipo, type UsuarioListagem } from '../contratos'
import { usePesquisaUsuarios } from '../composables/usePesquisaUsuarios'
import { useRotulosAcesso } from '../composables/useRotulosAcesso'

const router = useRouter()
const rota = useRoute()
const autenticacao = useAutenticacao()
const { nomeAplicacao, nomePermissao } = useRotulosAcesso()
const {
  filtros,
  dados,
  carregando,
  erro,
  paginas,
  paginaAtual,
  carregar,
  pesquisar,
  limpar,
  mudarPagina,
} = usePesquisaUsuarios()
const usuarioParaExcluir = ref<UsuarioListagem>()
const aviso = ref('')
const formatarData = new Intl.DateTimeFormat('pt-BR')

function abrirExclusao(usuario: UsuarioListagem) {
  aviso.value = ''
  usuarioParaExcluir.value = usuario
}

function aposExcluir() {
  aviso.value = `Usuário ${usuarioParaExcluir.value?.login ?? ''} desativado.`
  usuarioParaExcluir.value = undefined
  void carregar()
}
</script>

<template>
  <CabecalhoPagina titulo="Usuários" descricao="Pesquise e gerencie os usuários do Hub.">
    <BotaoBase @click="router.push({ name: 'usuarios-novo', query: rota.query })">
      <IconeBase nome="mais" :tamanho="18" /> Novo Usuário
    </BotaoBase>
  </CabecalhoPagina>
  <form class="filtros" aria-label="Pesquisar usuários" @submit.prevent="pesquisar">
    <div class="filtros__campos">
      <CampoTexto
        id="filtro-login"
        v-model="filtros.login"
        rotulo="Nome do usuário"
        maxlength="50"
        placeholder="Buscar por login"
      />
      <CampoTexto
        id="filtro-email"
        v-model="filtros.email"
        rotulo="E-mail"
        maxlength="255"
        placeholder="Buscar por e-mail"
      />
      <CampoSelecao
        id="filtro-tipo"
        v-model="filtros.tipo"
        rotulo="Tipo de acesso"
        :opcoes="[{ valor: '', texto: 'Todos os tipos' }, ...opcoesTipo]"
      />
      <CampoSelecao
        id="filtro-status"
        v-model="filtros.status"
        rotulo="Status"
        :opcoes="[{ valor: '', texto: 'Todos os status' }, ...opcoesStatus]"
      />
    </div>
    <div class="filtros__acoes">
      <BotaoBase variante="discreto" @click="limpar">Limpar filtros</BotaoBase>
      <BotaoBase tipo="submit"><IconeBase nome="buscar" :tamanho="18" /> Pesquisar</BotaoBase>
    </div>
  </form>

  <p v-if="aviso" class="aviso" role="status">{{ aviso }}</p>
  <section class="resultados" aria-labelledby="usuarios-resultados" :aria-busy="carregando">
    <div class="resultados__cabecalho">
      <h2 id="usuarios-resultados">Resultados</h2>
      <span v-if="!carregando && !erro && dados"
        >{{ dados.total }} {{ dados.total === 1 ? 'usuário' : 'usuários' }}</span
      >
    </div>
    <p v-if="carregando" class="resultados__estado" role="status">Carregando usuários…</p>
    <div v-else-if="erro" class="resultados__estado">
      <p role="alert">{{ erro }}</p>
      <BotaoBase variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
    </div>
    <p v-else-if="!dados?.itens.length" class="resultados__estado">Nenhum usuário encontrado.</p>
    <template v-else>
      <table role="table">
        <caption class="somente-leitor">
          Usuários encontrados
        </caption>
        <thead role="rowgroup">
          <tr role="row">
            <th scope="col" role="columnheader">Nome do usuário</th>
            <th scope="col" role="columnheader">E-mail</th>
            <th scope="col" role="columnheader">Tipo de acesso</th>
            <th scope="col" role="columnheader">Permissões</th>
            <th scope="col" role="columnheader">Status</th>
            <th scope="col" role="columnheader">Data de cadastro</th>
            <th scope="col" role="columnheader">Ações</th>
          </tr>
        </thead>
        <tbody role="rowgroup">
          <tr v-for="usuario in dados.itens" :key="usuario.id" role="row">
            <th scope="row" role="rowheader" class="usuario-nome">
              <span class="usuario-avatar" aria-hidden="true">{{
                usuario.login.slice(0, 2).toUpperCase()
              }}</span>
              {{ usuario.login }}
            </th>
            <td role="cell" data-rotulo="E-mail">{{ usuario.email }}</td>
            <td role="cell" data-rotulo="Tipo de acesso">
              {{ traduzirEnum('tipoAcesso', usuario.tipo) }}
            </td>
            <td role="cell" data-rotulo="Permissões">
              <span v-if="!usuario.permissoes.length">Nenhuma efetiva</span>
              <details v-else>
                <summary>
                  {{ usuario.permissoes.length }}
                  {{ usuario.permissoes.length === 1 ? 'permissão' : 'permissões' }}
                </summary>
                <ul class="permissoes">
                  <li
                    v-for="permissao in usuario.permissoes"
                    :key="permissao.aplicacao + ':' + permissao.codigo"
                  >
                    <strong>{{ nomePermissao(permissao.codigo) }}</strong
                    ><span>{{ nomeAplicacao(permissao.aplicacao) }}</span>
                  </li>
                </ul>
              </details>
            </td>
            <td role="cell" data-rotulo="Status">
              <StatusUsuarioBadge :status="usuario.status" />
            </td>
            <td role="cell" data-rotulo="Data de cadastro">
              <time :datetime="usuario.dataCriacao">{{
                formatarData.format(new Date(usuario.dataCriacao))
              }}</time>
            </td>
            <td role="cell" data-rotulo="Ações">
              <div class="acoes">
                <RouterLink
                  class="acao"
                  :to="{ name: 'usuarios-editar', params: { id: usuario.id }, query: rota.query }"
                  :aria-label="'Editar ' + usuario.login"
                  title="Editar"
                  ><IconeBase nome="editar" :tamanho="17"
                /></RouterLink>
                <button
                  type="button"
                  class="acao acao--excluir"
                  :aria-label="'Excluir ' + usuario.login"
                  :title="
                    usuario.id === autenticacao.usuario?.id
                      ? 'Sua conta deve permanecer ativa'
                      : usuario.status === 'INATIVO'
                        ? 'Conta já inativa'
                        : 'Excluir'
                  "
                  :disabled="
                    usuario.id === autenticacao.usuario?.id || usuario.status === 'INATIVO'
                  "
                  @click="abrirExclusao(usuario)"
                >
                  <IconeBase nome="lixeira" :tamanho="17" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
      <nav v-if="paginas > 1" class="paginacao" aria-label="Paginação de usuários">
        <span>Página {{ paginaAtual + 1 }} de {{ paginas }}</span>
        <div class="acoes">
          <BotaoBase
            variante="discreto"
            :desabilitado="paginaAtual === 0"
            @click="mudarPagina(paginaAtual - 1)"
            >Anterior</BotaoBase
          >
          <BotaoBase
            variante="discreto"
            :desabilitado="paginaAtual + 1 >= paginas"
            @click="mudarPagina(paginaAtual + 1)"
            >Próxima</BotaoBase
          >
        </div>
      </nav>
    </template>
  </section>
  <ConfirmarExclusaoUsuario
    v-if="usuarioParaExcluir"
    :usuario="usuarioParaExcluir"
    @fechar="usuarioParaExcluir = undefined"
    @excluido="aposExcluir"
  />
</template>

<style scoped>
.filtros,
.resultados {
  background: var(--cor-superficie);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  box-shadow: var(--sombra-card);
}
.filtros {
  padding: var(--espaco-5);
  margin-bottom: var(--espaco-6);
}
.filtros__campos {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 12rem), 1fr));
  gap: var(--espaco-4);
}
.filtros__acoes,
.acoes {
  display: flex;
  flex-wrap: wrap;
  gap: var(--espaco-2);
}
.filtros__acoes {
  margin-top: var(--espaco-4);
  justify-content: flex-end;
}
.resultados__cabecalho,
.paginacao {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: var(--espaco-3);
  padding: var(--espaco-4) var(--espaco-5);
}
.resultados__cabecalho h2 {
  font-size: var(--fonte-lg);
}
.resultados__cabecalho span,
.paginacao span,
.resultados__estado {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
}
.resultados__estado {
  display: grid;
  justify-items: center;
  gap: var(--espaco-4);
  padding: var(--espaco-12) var(--espaco-5);
  text-align: center;
}
table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  font-size: var(--fonte-sm);
}
th,
td {
  padding: var(--espaco-3) var(--espaco-2);
  text-align: left;
  border-top: var(--borda-fina) solid var(--cor-borda);
  overflow-wrap: anywhere;
}
thead {
  background: var(--cor-fundo);
  color: var(--cor-texto-secundario);
}
thead th:nth-child(2) {
  width: 21%;
}
thead th:nth-child(3) {
  width: 16%;
}
thead th:last-child {
  width: 7rem;
}
tr > :first-child {
  padding-left: var(--espaco-5);
}
tr > :last-child {
  padding-right: var(--espaco-5);
}
.usuario-avatar {
  display: inline-grid;
  width: 2rem;
  height: 2rem;
  margin-right: var(--espaco-2);
  place-items: center;
  border-radius: 50%;
  background: var(--cor-primaria-suave);
  color: var(--cor-primaria-texto);
  font-size: var(--fonte-xs);
  vertical-align: middle;
}
summary {
  cursor: pointer;
  color: var(--cor-primaria-texto);
}
.permissoes {
  list-style: none;
  display: grid;
  gap: var(--espaco-2);
  padding: 0;
  margin: var(--espaco-2) 0 0;
  font-size: var(--fonte-xs);
}
.permissoes span {
  display: block;
  color: var(--cor-texto-secundario);
}
.acao {
  display: grid;
  width: 2.15rem;
  height: 2.15rem;
  place-items: center;
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-sm);
  color: var(--cor-primaria-texto);
  background: var(--cor-superficie);
}
.acao--excluir {
  color: var(--cor-erro);
}
.acao:hover:not(:disabled) {
  background: var(--cor-primaria-suave);
}
.acao--excluir:hover:not(:disabled) {
  background: var(--cor-erro-fundo);
}
.acao:disabled {
  opacity: 0.45;
  cursor: default;
}
.paginacao {
  border-top: var(--borda-fina) solid var(--cor-borda);
}
.aviso {
  margin-bottom: var(--espaco-4);
  padding: var(--espaco-3) var(--espaco-4);
  border-radius: var(--raio-sm);
  color: var(--cor-sucesso);
  background: var(--cor-primaria-suave);
}
@media (max-width: 63rem) {
  table,
  tbody {
    display: block;
  }
  thead {
    position: absolute;
    width: 1px;
    height: 1px;
    overflow: hidden;
    clip-path: inset(50%);
  }
  tbody tr {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: var(--espaco-3) var(--espaco-4);
    padding: var(--espaco-4) var(--espaco-5);
    border-top: var(--borda-fina) solid var(--cor-borda);
  }
  tbody tr > * {
    padding: 0;
    border: 0;
  }
  tbody tr > :first-child,
  tbody tr > :last-child {
    padding: 0;
  }
  .usuario-nome,
  td[data-rotulo='E-mail'] {
    grid-column: 1 / -1;
  }
  td::before {
    content: attr(data-rotulo);
    display: block;
    color: var(--cor-texto-secundario);
    font-size: var(--fonte-xs);
    margin-bottom: var(--espaco-1);
  }
  .acao {
    width: 2.75rem;
    height: 2.75rem;
  }
}
@media (max-width: 22.5rem) {
  tbody tr {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
