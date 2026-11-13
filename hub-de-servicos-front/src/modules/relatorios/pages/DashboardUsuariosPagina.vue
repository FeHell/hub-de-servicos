<script setup lang="ts">
import { traduzirEnum } from '@/shared/traducoes/enums'
import { computed, ref, watch } from 'vue'
import {
  ArcElement,
  BarElement,
  CategoryScale,
  Chart,
  LinearScale,
  Tooltip,
  type ChartOptions,
} from 'chart.js'
import { Bar, Doughnut } from 'vue-chartjs'
import BotaoBase from '@/components/ui/BotaoBase.vue'
import CabecalhoPagina from '@/components/ui/CabecalhoPagina.vue'
import CampoSelecao from '@/components/ui/CampoSelecao.vue'
import { useTema } from '@/modules/aparencia/stores/tema'
import { ErroApi } from '@/services/clienteHttp'
import { useRotulosAcesso } from '@/modules/usuarios/composables/useRotulosAcesso'
import type { DashboardUsuarios } from '@/modules/usuarios/contratos'
import { useUsuariosApi } from '@/modules/usuarios/services/usuariosApi'

Chart.register(ArcElement, BarElement, CategoryScale, LinearScale, Tooltip)

const api = useUsuariosApi()
const { nomeAplicacao, nomePermissao } = useRotulosAcesso()
const periodo = ref('0')
const opcoes = [
  { valor: '0', texto: 'Todo o período' },
  { valor: '3', texto: 'Últimos 3 meses' },
  { valor: '6', texto: 'Últimos 6 meses' },
  { valor: '12', texto: 'Últimos 12 meses' },
]
const dados = ref<DashboardUsuarios>()
const carregando = ref(true)
const erro = ref('')
let consultaAtual = 0

const numeros = new Intl.NumberFormat('pt-BR')
const percentuais = new Intl.NumberFormat('pt-BR', { style: 'percent', maximumFractionDigits: 1 })
const meses = new Intl.DateTimeFormat('pt-BR', { month: 'short', year: '2-digit', timeZone: 'UTC' })
const css = getComputedStyle(document.documentElement)
const cor = (token: string) => css.getPropertyValue(token).trim()
const tema = useTema()
const baseGrafico = {
  responsive: true,
  maintainAspectRatio: false,
  animation: false as const,
  color: cor('--cor-texto-secundario'),
}
const graficoTipos: ChartOptions<'doughnut'> = { ...baseGrafico, cutout: '64%' }
const graficoMeses: ChartOptions<'bar'> = {
  ...baseGrafico,
  scales: { y: { beginAtZero: true, ticks: { precision: 0 } } },
}
const graficoPermissoes: ChartOptions<'bar'> = {
  ...baseGrafico,
  indexAxis: 'y',
  scales: { x: { beginAtZero: true, ticks: { precision: 0 } } },
  plugins: {
    tooltip: {
      callbacks: {
        title: ([item]) => {
          const permissao = permissoesNoGrafico.value[item?.dataIndex ?? -1]
          return permissao
            ? `${nomePermissao(permissao.codigo)} · ${nomeAplicacao(permissao.aplicacao)}`
            : ''
        },
      },
    },
  },
}

const indicadores = computed(() => {
  const atual = dados.value
  if (!atual) return []
  const totalStatus = (status: 'ATIVO' | 'INATIVO') =>
    atual.porStatus.find((item) => item.status === status)?.total ?? 0
  return [
    { titulo: 'Usuários cadastrados', valor: atual.total },
    { titulo: 'Usuários ativos', valor: totalStatus('ATIVO') },
    { titulo: 'Usuários inativos', valor: totalStatus('INATIVO') },
    { titulo: 'Cadastros nos últimos 30 dias', valor: atual.recentes },
  ]
})
const tipos = computed(() => dados.value?.porTipo ?? [])
const dadosTipos = computed(() => ({
  labels: tipos.value.map(({ tipo }) => traduzirEnum('tipoAcesso', tipo)),
  datasets: [
    {
      data: tipos.value.map(({ total }) => total),
      backgroundColor: tema.coresGrafico,
      borderWidth: 0,
    },
  ],
}))
const permissoesNoGrafico = computed(() => dados.value?.porPermissao.slice(0, 8) ?? [])
const abreviar = (texto: string) => (texto.length > 21 ? texto.slice(0, 20) + '…' : texto)
const dadosPermissoes = computed(() => ({
  labels: permissoesNoGrafico.value.map((item) => [
    abreviar(nomePermissao(item.codigo)),
    abreviar(nomeAplicacao(item.aplicacao)),
  ]),
  datasets: [
    {
      label: 'Usuários',
      data: permissoesNoGrafico.value.map(({ total }) => total),
      backgroundColor: tema.coresGrafico[0],
      borderRadius: 4,
      maxBarThickness: 26,
    },
  ],
}))
const dadosMeses = computed(() => ({
  labels: dados.value?.porMes.map(({ mes }) => formatarMes(mes)) ?? [],
  datasets: [
    {
      label: 'Cadastros',
      data: dados.value?.porMes.map(({ total }) => total) ?? [],
      backgroundColor: tema.coresGrafico[0],
      borderRadius: 4,
    },
  ],
}))

function formatarMes(mes: string) {
  return meses.format(new Date(mes + '-01T00:00:00Z'))
}

async function carregar() {
  const atual = ++consultaAtual
  carregando.value = true
  erro.value = ''
  try {
    const resposta = await api.dashboard(Number(periodo.value))
    if (atual === consultaAtual) dados.value = resposta
  } catch (falha) {
    if (atual === consultaAtual) {
      erro.value =
        falha instanceof ErroApi ? falha.message : 'Não foi possível carregar o dashboard.'
    }
  } finally {
    if (atual === consultaAtual) carregando.value = false
  }
}
watch(periodo, carregar, { immediate: true })
</script>

<template>
  <CabecalhoPagina
    titulo="Dashboard Usuários"
    descricao="Acompanhe os cadastros e os acessos dos usuários do Hub."
  >
    <CampoSelecao
      id="dashboard-periodo"
      v-model="periodo"
      rotulo="Período dos cadastros"
      :opcoes="opcoes"
    />
  </CabecalhoPagina>

  <div v-if="carregando" class="painel estado" role="status">Carregando indicadores…</div>
  <div v-else-if="erro" class="painel estado">
    <p role="alert">{{ erro }}</p>
    <BotaoBase variante="discreto" @click="carregar">Tentar novamente</BotaoBase>
  </div>
  <template v-else-if="dados">
    <section class="indicadores" aria-label="Indicadores de usuários">
      <article v-for="indicador in indicadores" :key="indicador.titulo" class="painel indicador">
        <h2>{{ indicador.titulo }}</h2>
        <p>{{ numeros.format(indicador.valor) }}</p>
      </article>
    </section>
    <p v-if="!dados.total" class="painel estado">
      Nenhum usuário cadastrado no período selecionado.
    </p>
    <div v-else class="graficos">
      <section class="painel">
        <h2>Usuários por tipo de acesso</h2>
        <div class="grafico grafico--tipos" aria-hidden="true">
          <Doughnut :data="dadosTipos" :options="graficoTipos" />
        </div>
        <ul class="totais">
          <li v-for="(item, i) in tipos" :key="item.tipo">
            <span class="tipo"
              ><i :style="{ backgroundColor: tema.coresGrafico[i] }" aria-hidden="true" />{{
                traduzirEnum('tipoAcesso', item.tipo)
              }}</span
            >
            <strong
              >{{ numeros.format(item.total) }} ·
              {{ percentuais.format(item.total / dados.total) }}</strong
            >
          </li>
        </ul>
      </section>
      <section class="painel">
        <h2>Usuários por permissão</h2>
        <p class="auxiliar">
          Usuários com acesso efetivo por aplicação. Um usuário pode aparecer em mais de uma
          permissão.
        </p>
        <p v-if="!dados.porPermissao.length" class="auxiliar">
          Nenhuma permissão efetiva no período.
        </p>
        <template v-else>
          <div class="grafico grafico--permissoes" aria-hidden="true">
            <Bar :data="dadosPermissoes" :options="graficoPermissoes" />
          </div>
          <details>
            <summary>
              {{
                dados.porPermissao.length > 8
                  ? 'Ver todas as permissões'
                  : 'Ver valores por permissão'
              }}
            </summary>
            <ul class="totais">
              <li v-for="item in dados.porPermissao" :key="item.aplicacao + ':' + item.codigo">
                <span
                  ><strong>{{ nomePermissao(item.codigo) }}</strong
                  ><br />{{ nomeAplicacao(item.aplicacao) }}</span
                >
                <strong>{{ numeros.format(item.total) }}</strong>
              </li>
            </ul>
          </details>
        </template>
      </section>
      <section class="painel painel--evolucao">
        <h2>Evolução dos cadastros</h2>
        <p v-if="periodo === '0'" class="auxiliar">Últimos 12 meses</p>
        <div class="grafico" aria-hidden="true">
          <Bar :data="dadosMeses" :options="graficoMeses" />
        </div>
        <details>
          <summary>Ver valores por mês</summary>
          <ul class="totais">
            <li v-for="item in dados.porMes" :key="item.mes">
              <span>{{ formatarMes(item.mes) }}</span
              ><strong>{{ numeros.format(item.total) }}</strong>
            </li>
          </ul>
        </details>
      </section>
    </div>
  </template>
</template>

<style scoped>
.painel {
  min-width: 0;
  padding: var(--espaco-5);
  border: var(--borda-fina) solid var(--cor-borda);
  border-radius: var(--raio-md);
  background: var(--cor-superficie);
  box-shadow: var(--sombra-card);
}
.estado {
  display: grid;
  justify-items: start;
  gap: var(--espaco-4);
}
.indicadores,
.graficos {
  display: grid;
  gap: var(--espaco-4);
}
.indicadores {
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 13rem), 1fr));
  margin-bottom: var(--espaco-6);
}
.indicador h2 {
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
  font-weight: var(--peso-forte);
}
.indicador p {
  margin-top: var(--espaco-2);
  font-size: var(--fonte-xl);
  font-weight: var(--peso-titulo);
}
.graficos {
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 25rem), 1fr));
}
.painel--evolucao {
  grid-column: 1 / -1;
}
.graficos h2 {
  font-size: var(--fonte-lg);
}
.auxiliar {
  margin-block: var(--espaco-2);
  color: var(--cor-texto-secundario);
  font-size: var(--fonte-sm);
}
.grafico {
  position: relative;
  height: 16rem;
  margin-block: var(--espaco-4);
}
.grafico--tipos {
  height: 12rem;
}
.grafico--permissoes {
  height: 17rem;
}
.totais {
  margin: 0;
  padding: 0;
  list-style: none;
  font-size: var(--fonte-sm);
}
.totais li {
  display: flex;
  justify-content: space-between;
  gap: var(--espaco-3);
  border-top: var(--borda-fina) solid var(--cor-borda);
  padding: var(--espaco-2) 0;
  overflow-wrap: anywhere;
}
.totais li > strong {
  flex-shrink: 0;
}
.tipo {
  display: flex;
  align-items: center;
  gap: var(--espaco-2);
}
.tipo i {
  width: 0.65rem;
  height: 0.65rem;
  border-radius: 50%;
  flex-shrink: 0;
}
summary {
  cursor: pointer;
  color: var(--cor-primaria-texto);
}
details[open] summary {
  margin-bottom: var(--espaco-3);
}
</style>
