<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { ambiente } from '@/app/config/ambiente'
import IconeBase from '@/components/ui/IconeBase.vue'
import { useAutenticacao } from '@/modules/autenticacao/stores/autenticacao'

const autenticacao = useAutenticacao()
</script>

<template>
  <section class="inicio" aria-labelledby="titulo-inicio">
    <p class="inicio__caminho">Visão geral</p>
    <h1 id="titulo-inicio">Olá, {{ autenticacao.usuario?.login }}.</h1>
    <p class="inicio__descricao">Bem-vindo à sua área de gerenciamento.</p>

    <article class="acesso" aria-labelledby="titulo-acesso">
      <div class="acesso__topo">
        <span class="acesso__icone"><IconeBase nome="escudo" :tamanho="26" /></span>
        <span class="status"><span></span> Conectado</span>
      </div>
      <h2 id="titulo-acesso">Seu acesso está pronto</h2>
      <p class="acesso__descricao">Você está conectado à {{ ambiente.nomeAplicacao }}.</p>
      <dl>
        <div>
          <dt>Usuário</dt>
          <dd>{{ autenticacao.usuario?.login }}</dd>
        </div>
        <div>
          <dt>E-mail</dt>
          <dd>{{ autenticacao.usuario?.email }}</dd>
        </div>
        <div>
          <dt>Perfil de acesso</dt>
          <dd>
            {{
              autenticacao.permissoes.includes('PLATAFORMA_ADMINISTRAR')
                ? traduzirEnum('permissao', 'PLATAFORMA_ADMINISTRAR')
                : 'Usuário da aplicação'
            }}
          </dd>
        </div>
      </dl>
      <p class="acesso__nota">
        <IconeBase nome="cadeado" :tamanho="16" /> As ações disponíveis seguem suas permissões nesta
        aplicação.
      </p>
    </article>
  </section>
</template>

<style scoped>
.inicio__caminho {
  font-size: var(--fonte-xs);
  color: var(--cor-texto-secundario);
  margin-bottom: var(--espaco-5);
}
h1 {
  font-size: var(--fonte-titulo);
  font-weight: var(--peso-forte);
  letter-spacing: -0.04em;
}
.inicio__descricao {
  margin-top: var(--espaco-3);
  color: var(--cor-texto-secundario);
  line-height: 1.7;
}
.acesso {
  margin-top: var(--espaco-10);
  max-width: 700px;
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-lg);
  background: var(--cor-superficie);
  padding: clamp(1.5rem, 3vw, 2.5rem);
  box-shadow: var(--sombra-card);
}
.acesso__topo {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--espaco-4);
  margin-bottom: var(--espaco-6);
}
.acesso__icone {
  display: grid;
  place-items: center;
  width: 54px;
  height: 54px;
  border-radius: var(--raio-md);
  background: var(--cor-primaria-suave);
  color: var(--cor-primaria-texto);
}
.status {
  display: inline-flex;
  align-items: center;
  gap: var(--espaco-2);
  font-size: var(--fonte-xs);
  color: var(--cor-sucesso);
  background: var(--cor-primaria-suave);
  border-radius: var(--raio-sm);
  padding: var(--espaco-2) var(--espaco-3);
}
.status span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: currentColor;
}
h2 {
  font-size: var(--fonte-lg);
  font-weight: var(--peso-forte);
  letter-spacing: -0.025em;
}
.acesso__descricao {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
  line-height: 1.75;
  margin-top: var(--espaco-2);
}
dl {
  display: grid;
  gap: var(--espaco-4);
  margin: var(--espaco-8) 0;
}
dl div {
  display: flex;
  flex-wrap: wrap;
  gap: var(--espaco-2) var(--espaco-6);
  padding-bottom: var(--espaco-4);
  border-bottom: var(--borda-fina) solid var(--cor-borda);
}
dt {
  min-width: 120px;
  font-size: var(--fonte-sm);
  color: var(--cor-texto-secundario);
}
dd {
  margin: 0;
  font-size: var(--fonte-sm);
  overflow-wrap: anywhere;
}
.acesso__nota {
  display: flex;
  align-items: flex-start;
  gap: var(--espaco-2);
  font-size: var(--fonte-xs);
  color: var(--cor-texto-secundario);
  line-height: 1.7;
}
.acesso__nota svg {
  flex-shrink: 0;
  margin-top: 2px;
}
</style>
