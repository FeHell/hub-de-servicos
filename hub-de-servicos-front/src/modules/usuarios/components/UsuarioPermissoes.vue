<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { computed } from 'vue'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CampoSelecao from '@/components/ui/CampoSelecao.vue'
import { usePermissoesUsuario } from '../composables/usePermissoesUsuario'
import type { UsuarioListagem } from '../contratos'

const props = defineProps<{ usuario: UsuarioListagem }>()
const emit = defineEmits<{ alterado: [] }>()
const {
  slug,
  perfis,
  permissoes,
  vinculado,
  editavel,
  opcoesAplicacao,
  carregandoAplicacoes,
  carregando,
  erroAplicacoes,
  erro,
  aviso,
  operacao,
  detalhes,
  carregarAplicacoes,
  carregarAcessos,
  atribuido,
  vincular,
  alterarPerfil,
  mostrarDetalhes,
} = usePermissoesUsuario(props.usuario.id, () => emit('alterado'))
const codigosEfetivos = computed(
  () =>
    new Set(
      props.usuario.permissoes
        .filter((item) => item.aplicacao === slug.value)
        .map((item) => item.codigo),
    ),
)
</script>

<template>
  <section
    class="acessos"
    aria-labelledby="permissoes-usuario"
    :aria-busy="carregando || !!operacao"
  >
    <h2 id="permissoes-usuario">Acessos por aplicação</h2>
    <p class="auxiliar">
      As permissões de outras aplicações vêm dos perfis. Atribuições e remoções são salvas nesta
      seção.
    </p>
    <p v-if="carregandoAplicacoes" role="status">Carregando aplicações…</p>
    <div v-else-if="erroAplicacoes" class="mensagem">
      <p role="alert">{{ erroAplicacoes }}</p>
      <BotaoBase variante="discreto" @click="carregarAplicacoes">Tentar novamente</BotaoBase>
    </div>
    <p v-else-if="opcoesAplicacao.length === 1">Nenhuma aplicação disponível.</p>
    <template v-else>
      <CampoSelecao
        id="usuario-aplicacao"
        v-model="slug"
        rotulo="Aplicação"
        :opcoes="opcoesAplicacao"
        :disabled="!!operacao"
      />
      <p v-if="carregando" role="status">Carregando perfis e permissões…</p>
      <template v-else-if="slug">
        <p v-if="aviso" class="sucesso" role="status">{{ aviso }}</p>
        <div v-if="erro" class="mensagem">
          <p role="alert">{{ erro }}</p>
          <BotaoBase variante="discreto" :desabilitado="!!operacao" @click="carregarAcessos"
            >Atualizar acessos</BotaoBase
          >
        </div>
        <p v-if="!editavel" class="auxiliar">
          Os perfis desta aplicação estão disponíveis para consulta.
        </p>
        <div v-else-if="!vinculado && !erro" class="mensagem">
          <p>O usuário precisa estar vinculado a esta aplicação para receber perfis.</p>
          <BotaoBase variante="discreto" :carregando="operacao === 'vincular'" @click="vincular"
            >Vincular usuário</BotaoBase
          >
        </div>

        <div v-if="perfis.length || permissoes.length || !erro" class="acessos__colunas">
          <div>
            <h3>Perfis da aplicação</h3>
            <p v-if="!perfis.length" class="auxiliar">Nenhum perfil cadastrado.</p>
            <ul v-else class="lista">
              <li v-for="perfil in perfis" :key="perfil.id">
                <div class="perfil">
                  <div>
                    <strong>{{ perfil.nome }}</strong>
                    <p class="auxiliar">
                      {{ atribuido(perfil) ? 'Atribuído' : 'Não atribuído'
                      }}{{
                        perfil.status === 'INATIVO'
                          ? ' · ' + traduzirEnum('statusCadastro', perfil.status)
                          : ''
                      }}
                    </p>
                    <p v-if="perfil.descricao" class="auxiliar">{{ perfil.descricao }}</p>
                  </div>
                  <BotaoBase
                    v-if="editavel && vinculado"
                    variante="discreto"
                    :carregando="operacao === perfil.id"
                    :desabilitado="!!operacao || (!atribuido(perfil) && perfil.status !== 'ATIVO')"
                    :aria-label="
                      (atribuido(perfil) ? 'Remover perfil ' : 'Atribuir perfil ') + perfil.nome
                    "
                    @click="alterarPerfil(perfil)"
                  >
                    {{ atribuido(perfil) ? 'Remover' : 'Atribuir' }}
                  </BotaoBase>
                </div>
                <button
                  type="button"
                  class="ver-permissoes"
                  :aria-expanded="detalhes?.id === perfil.id"
                  @click="mostrarDetalhes(perfil)"
                >
                  Permissões do perfil
                </button>
                <div v-if="detalhes?.id === perfil.id" class="perfil-detalhes">
                  <p v-if="detalhes.erro" role="alert">{{ detalhes.erro }}</p>
                  <p v-else-if="!detalhes.dados" role="status">Carregando permissões…</p>
                  <p v-else-if="!detalhes.dados.length">Nenhuma permissão neste perfil.</p>
                  <ul v-else>
                    <li v-for="permissao in detalhes.dados" :key="permissao.id">
                      {{ traduzirEnum('permissao', permissao.codigo) }}
                      <span v-if="permissao.status === 'INATIVO'">
                        · {{ traduzirEnum('statusPermissao', permissao.status) }}</span
                      >
                    </li>
                  </ul>
                </div>
              </li>
            </ul>
          </div>
          <div>
            <h3>Permissões habilitadas</h3>
            <p v-if="!permissoes.length" class="auxiliar">
              Nenhuma permissão habilitada nesta aplicação.
            </p>
            <ul v-else class="lista">
              <li v-for="permissao in permissoes" :key="permissao.id">
                <div class="permissao">
                  <strong>{{ traduzirEnum('permissao', permissao.codigo) }}</strong>
                  <span
                    class="selo"
                    :class="{ 'selo--concedido': codigosEfetivos.has(permissao.codigo) }"
                  >
                    {{
                      permissao.status === 'INATIVO'
                        ? traduzirEnum('statusPermissao', permissao.status)
                        : codigosEfetivos.has(permissao.codigo)
                          ? 'Efetiva'
                          : 'Sem acesso efetivo'
                    }}
                  </span>
                </div>
                <p v-if="permissao.descricao" class="auxiliar">{{ permissao.descricao }}</p>
              </li>
            </ul>
          </div>
        </div>
      </template>
    </template>
  </section>
</template>

<style scoped>
.acessos {
  display: grid;
  gap: var(--espaco-4);
  padding: var(--espaco-5);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
  box-shadow: var(--sombra-card);
}
h2 {
  font-size: var(--fonte-lg);
}
h3 {
  margin: 0 0 var(--espaco-3);
  font-size: var(--fonte-md);
}
.auxiliar {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
  line-height: 1.5;
}
.acessos__colunas {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 23rem), 1fr));
  gap: var(--espaco-5);
}
.lista {
  list-style: none;
  padding: 0;
  margin: 0;
}
.lista > li {
  padding: var(--espaco-3) 0;
  border-top: var(--borda-fina) solid var(--cor-borda);
  overflow-wrap: anywhere;
}
.perfil,
.permissao {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--espaco-2);
}
.selo {
  font-size: var(--fonte-xs);
  background: var(--cor-fundo);
  color: var(--cor-texto-secundario);
  padding: var(--espaco-1) var(--espaco-2);
  border-radius: 2rem;
}
.selo--concedido {
  background: var(--cor-primaria-suave);
  color: var(--cor-sucesso);
}
.mensagem {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: var(--espaco-3);
}
[role='alert'] {
  color: var(--cor-erro);
}
.sucesso {
  color: var(--cor-sucesso);
}
.ver-permissoes {
  border: 0;
  padding: var(--espaco-2) 0;
  color: var(--cor-primaria-texto);
  background: transparent;
  text-decoration: underline;
  font-size: var(--fonte-sm);
}
.perfil-detalhes {
  font-size: var(--fonte-sm);
}
</style>
