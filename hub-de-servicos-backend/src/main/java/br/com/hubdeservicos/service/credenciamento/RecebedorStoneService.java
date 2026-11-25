package br.com.hubdeservicos.service.credenciamento;

import io.swagger.v3.oas.annotations.media.Schema;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.UnidadeMonetaria;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import br.com.hubdeservicos.entity.credenciamento.CredenciamentoEntity;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity.Operacao;
import br.com.hubdeservicos.repository.credenciamento.TentativaRecebedorRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import br.com.hubdeservicos.repository.credenciamento.CredenciamentoRepository;
import br.com.hubdeservicos.repository.credenciamento.RecebedorExternoRepository;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecebedorStoneService {
    private static final Logger log = LoggerFactory.getLogger(RecebedorStoneService.class);
    private final CredenciamentoService credenciamentos;
    private final CredenciamentoRepository rascunhos;
    private final RecebedorExternoRepository recebedores;
    private final AmbienteServicosService ambientes;
    private final AutenticacaoPagarme autenticacao;
    private final MapeadorPagarme mapeador;
    private final ClienteRecebedorPagarme cliente;
    private final DiagnosticoRecebedorService diagnosticos;
    private final CriptografiaCredenciais criptografia;
    private final Clock relogio;
    private final TransactionTemplate transacao;
    private final TentativaRecebedorRepository tentativas;
    private final SituacaoCredenciamento situacoes;
    @PersistenceContext
    private EntityManager entidades;

    public RecebedorStoneService(CredenciamentoService credenciamentos, CredenciamentoRepository rascunhos,
            RecebedorExternoRepository recebedores, AmbienteServicosService ambientes, AutenticacaoPagarme autenticacao,
            MapeadorPagarme mapeador, ClienteRecebedorPagarme cliente, CriptografiaCredenciais criptografia,
            Clock relogio, PlatformTransactionManager transacoes, DiagnosticoRecebedorService diagnosticos,
            TentativaRecebedorRepository tentativas, SituacaoCredenciamento situacoes) {
        this.credenciamentos = credenciamentos;
        this.rascunhos = rascunhos;
        this.recebedores = recebedores;
        this.ambientes = ambientes;
        this.autenticacao = autenticacao;
        this.mapeador = mapeador;
        this.cliente = cliente;
        this.diagnosticos = diagnosticos;
        this.tentativas = tentativas;
        this.situacoes = situacoes;
        this.criptografia = criptografia;
        this.relogio = relogio;
        var transactionTemplatePreparado = new TransactionTemplate(transacoes);
        transacao = transactionTemplatePreparado;
        transacao.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public RecebedorBean consultar(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor) {
        UUID usuario = credenciamentos.autorizarTrabalhador(identidade, false);
        Ambiente ambiente = ambientes.atual();
        return rascunhos.findBySolicitanteAndProvedor(usuario, provedor).map(cadastro -> {
            var chavePreparado = new Chave(cadastro.getId(), provedor, ambiente);
            return recebedores.findById(chavePreparado).map(registro -> resposta(registro, cadastro))
                    .orElseGet(() -> SituacaoCredenciamento.naoEnviado(ambiente, cadastro.versao()));
        }).orElseGet(() -> SituacaoCredenciamento.naoEnviado(ambiente, null));
    }

    public RecebedorBean enviar(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor, EnviarRecebedorBean pedido,
            Long versaoEsperada, UUID idEsperado) {
        return submeter(identidade, provedor, pedido.getAmbiente(), versaoEsperada, pedido.getDados(), null,
                idEsperado);
    }

    public RecebedorBean reenviar(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor,
            ReenviarRecebedorBean pedido) {
        return submeter(identidade, provedor, pedido.getAmbiente(), pedido.getVersao(), null, pedido.getTentativa(),
                null);
    }

    private RecebedorBean submeter(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor, Ambiente ambiente,
            Long versaoEsperada, DadosBean dadosIniciais, UUID tentativaEsperada, UUID idEsperado) {
        credenciamentos.autorizarTrabalhador(identidade, false);
        var conta = autenticacao.selecionarConta();
        confirmarAmbiente(ambiente, conta.getAmbiente());
        String contaDigest = contaDigest(conta);
        OperacaoEnvioBean operacao;
        try {
            operacao = transacao.execute(ignorado -> iniciar(identidade, provedor, dadosIniciais, ambiente, contaDigest,
                    versaoEsperada, tentativaEsperada, idEsperado));
        } catch (DataIntegrityViolationException erro) {
            if (erro.getCause() instanceof ConstraintViolationException violacao
                    && "23505".equals(violacao.getSQLState()) && violacao.getConstraintName() != null
                    && violacao.getConstraintName().contains("ambiente_documento")) {
                throw documentoJaVinculado();
            }
            throw erro;
        }
        var registro = operacao.getRegistro();
        if (operacao.getEstado() != null) {
            return operacao.getEstado();
        }
        var chave = registro.chave();
        UUID referencia = registro.tentativa();
        // A tentativa e o snapshot já estão registrados antes da chamada de rede.
        var resultado = operacao.getTipo() == Operacao.CRIACAO ? cliente.criar(conta, operacao.getPayload(), referencia)
                : cliente.alterar(conta, registro.idRecebedor(), operacao.getPayload(), operacao.getTipo(), referencia);
        if (resultado.getDiagnostico() != null) {
            try {
                diagnosticos.arquivar(chave, referencia, resultado.getDiagnostico());
            } catch (DataAccessException | ResponseStatusException erro) {
                log.warn("Não foi possível arquivar a resposta da tentativa {}", referencia);
            }
        }
        return finalizar(chave, referencia, resultado, true);
    }

    public RecebedorBean reconciliar(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor) {
        UUID usuario = credenciamentos.autorizarTrabalhador(identidade, false);
        var conta = autenticacao.selecionarConta();
        var cadastro = rascunhos.findBySolicitanteAndProvedor(usuario, provedor).orElse(null);
        if (cadastro == null) {
            return SituacaoCredenciamento.naoEnviado(conta.getAmbiente(), null);
        }
        var chavePreparado2 = new Chave(cadastro.getId(), provedor, conta.getAmbiente());
        var tentativa = recebedores.findById(chavePreparado2).orElse(null);
        if (tentativa == null) {
            return SituacaoCredenciamento.naoEnviado(conta.getAmbiente(), cadastro.versao());
        }
        if (tentativa.situacao() == SituacaoRecebedor.FALHA && !resposta(tentativa, cadastro).getPodeConsultar()) {
            return resposta(tentativa, cadastro);
        }
        confirmarConta(tentativa, contaDigest(conta));
        var historico = historico(tentativa);
        var chave = tentativa.chave();
        UUID referencia = tentativa.tentativa();
        var codigo = chave.codigo();
        var resultado = historico == null || historico.operacao() == Operacao.CRIACAO
                ? cliente.consultar(conta, codigo, tentativa.idRecebedor())
                : cliente.consultarAtualizacao(conta, tentativa.idRecebedor(),
                        prepararSnapshot(cadastro, historico, codigo), historico.operacao());
        return finalizar(chave, referencia, resultado, false);
    }

    private OperacaoEnvioBean iniciar(IdentidadeJwtVO identidade, ProvedorCredenciamento provedor,
            DadosBean dadosIniciais, Ambiente ambiente, String contaDigest, Long versaoEsperada, UUID tentativaEsperada,
            UUID idEsperado) {
        UUID usuario = credenciamentos.autorizarTrabalhador(identidade, true);
        confirmarAmbiente(ambiente, ambientes.atual());
        var atual = rascunhos.findBySolicitanteAndProvedor(usuario, provedor);
        if (atual.isPresent()) {
            CredenciamentoService.conferirIdentidade(atual.get(), idEsperado);
            CredenciamentoService.conferirVersao(atual.get(), versaoEsperada);
        } else {
            if (versaoEsperada != null || idEsperado != null) {
                throw CredenciamentoService.conflitoVersao();
            }
        }

        UUID id = atual.map(CredenciamentoEntity::getId).orElseGet(UUID::randomUUID);
        var chave = new Chave(id, provedor, ambiente);
        var anterior = recebedores.bloquear(chave);
        if (tentativaEsperada == null && anterior.isPresent()) {
            OperacaoEnvioBean operacaoEnvioPreparado = new OperacaoEnvioBean();
            operacaoEnvioPreparado.setRegistro(null);
            operacaoEnvioPreparado.setPayload(null);
            operacaoEnvioPreparado.setTipo(null);
            operacaoEnvioPreparado.setEstado(resposta(anterior.get(), atual.orElseThrow()));
            return operacaoEnvioPreparado;
        }

        DadosBean dados;
        Operacao tipo;
        var registro = anterior.orElseGet(() -> {
            var recebedorExternoEntityPreparado = new RecebedorExternoEntity(chave, contaDigest);
            return recebedorExternoEntityPreparado;
        });
        confirmarConta(registro, contaDigest);
        if (tentativaEsperada != null) {
            var cadastro = atual.orElseThrow(CredenciamentoService::conflitoVersao);
            if (anterior.isEmpty()) {
                throw CredenciamentoService.conflitoVersao();
            }
            dados = credenciamentos.ler(cadastro);
            if (!tentativaEsperada.equals(registro.tentativa())) {
                OperacaoEnvioBean operacaoEnvioPreparado2 = new OperacaoEnvioBean();
                operacaoEnvioPreparado2.setRegistro(null);
                operacaoEnvioPreparado2.setPayload(null);
                operacaoEnvioPreparado2.setTipo(null);
                operacaoEnvioPreparado2.setEstado(credenciamentos.respostaRecebedor(registro, cadastro, dados));
                return operacaoEnvioPreparado2;
            }
            tipo = situacoes.operacaoDeReenvio(registro, cadastro.versao(), dados,
                    protegido -> credenciamentos.lerProtegidos(cadastro, protegido));
            if (tipo == null) {
                OperacaoEnvioBean operacaoEnvioPreparado3 = new OperacaoEnvioBean();
                operacaoEnvioPreparado3.setRegistro(null);
                operacaoEnvioPreparado3.setPayload(null);
                operacaoEnvioPreparado3.setTipo(null);
                operacaoEnvioPreparado3.setEstado(credenciamentos.respostaRecebedor(registro, cadastro, dados));
                return operacaoEnvioPreparado3;
            }
        } else {
            dados = atual.filter(cadastro -> credenciamentos.iguais(cadastro, dadosIniciais)).map(credenciamentos::ler)
                    .orElse(dadosIniciais);
            if (dados == null || dados.getModalidade() != Modalidade.INDIVIDUAL) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Selecione Pessoa física para enviar os dados à Pagar.me.");
            }
            tipo = Operacao.CRIACAO;
        }

        var payload = mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, usuario, chave.codigo());
        String documentoDigest = identificador("recebedor-stone:documento",
                payload.getDefaultBankAccount().getHolderDocument());
        // O bloqueio inclui envios de contas diferentes e dura até o commit da
        // tentativa.
        recebedores.bloquearDocumento(Long.parseUnsignedLong(documentoDigest.substring(0, 16), 16));
        if (recebedores.documentoVinculado(provedor, ambiente, documentoDigest, id)) {
            throw documentoJaVinculado();
        }
        var cadastro = tentativaEsperada == null
                ? credenciamentos.gravar(identidade, provedor, dados, usuario, id, atual)
                : atual.orElseThrow();
        registro.iniciar(dados.getModalidade(), documentoDigest, UUID.randomUUID(), cadastro.versao(),
                relogio.instant());
        registro = recebedores.saveAndFlush(registro);
        var tentativaRecebedorEntityPreparado = new TentativaRecebedorEntity(registro, cadastro, tipo);
        tentativas.save(tentativaRecebedorEntityPreparado);
        OperacaoEnvioBean operacaoEnvioPreparado4 = new OperacaoEnvioBean();
        operacaoEnvioPreparado4.setRegistro(registro);
        operacaoEnvioPreparado4.setPayload(payload);
        operacaoEnvioPreparado4.setTipo(tipo);
        operacaoEnvioPreparado4.setEstado(null);
        return operacaoEnvioPreparado4;
    }

    private RecebedorBean resposta(RecebedorExternoEntity registro, CredenciamentoEntity cadastro) {
        return credenciamentos.respostaRecebedor(registro, cadastro, credenciamentos.ler(cadastro));
    }

    private PagarmeRecebedorBean prepararSnapshot(CredenciamentoEntity cadastro, TentativaRecebedorEntity tentativa,
            String codigo) {
        var dados = credenciamentos.lerProtegidos(cadastro, tentativa.dadosProtegidos());
        if (dados.getModalidade() != Modalidade.INDIVIDUAL) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Não foi possível identificar os dados da atualização. Procure o suporte.");
        }
        return mapeador.preparar(dados, UnidadeMonetaria.CENTAVOS, cadastro.getSolicitante(), codigo);
    }

    private TentativaRecebedorEntity historico(RecebedorExternoEntity registro) {
        return registro.tentativa() == null ? null : tentativas.findById(registro.tentativa()).orElse(null);
    }

    private RecebedorBean finalizar(Chave chave, UUID referencia, ClienteRecebedorPagarme.ResultadoBean resultado,
            boolean envio) {
        return transacao.execute(ignorado -> {
            var registro = recebedores.bloquear(chave).orElseThrow();
            entidades.refresh(registro);
            var cadastro = rascunhos.findById(registro.chave().credenciamento()).orElseThrow();
            entidades.refresh(cadastro);
            if (!Objects.equals(referencia, registro.tentativa())) {
                return cadastro.excluido() ? SituacaoCredenciamento.naoEnviado(chave.ambiente(), null)
                        : resposta(registro, cadastro);
            }
            if (envio && registro.situacao() == SituacaoRecebedor.CRIADO) {
                return cadastro.excluido() ? SituacaoCredenciamento.naoEnviado(chave.ambiente(), null)
                        : resposta(registro, cadastro);
            }
            boolean mudou = false;
            if (resultado.getId() != null) {
                if (registro.idRecebedor() == null || registro.idRecebedor().equals(resultado.getId())) {
                    registro.confirmar(resultado.getId(), resultado.getStatus(), relogio.instant());
                    mudou = true;
                }
            } else {
                if (envio) {
                    boolean incerto = resultado.getErro() == ErroRecebedor.RESPOSTA_INDETERMINADA;
                    registro.registrarFalha(incerto ? SituacaoRecebedor.INDETERMINADO : SituacaoRecebedor.FALHA,
                            resultado.getErro(), String.join(",", resultado.getCampos()));
                    mudou = true;
                }
            }
            if (mudou) {
                var snapshot = historico(registro);
                if (snapshot != null) {
                    snapshot.registrar(registro, relogio.instant());
                }
            }
            if (cadastro.excluido()) {
                return SituacaoCredenciamento.naoEnviado(chave.ambiente(), null);
            }
            return resposta(registro, cadastro);
        });
    }

    private static ResponseStatusException documentoJaVinculado() {
        var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.CONFLICT,
                "Já existe uma tentativa de credenciamento para este CPF neste ambiente. Consulte a tentativa anterior ou procure o suporte antes de enviar novamente.");
        return responseStatusExceptionPreparado;
    }

    private String contaDigest(AutenticacaoPagarme.ContaVO conta) {
        return identificador("recebedor-stone:conta", conta.getAccountId());
    }

    private String identificador(String contexto, String valor) {
        try {
            return criptografia.identificadorProtegido(contexto, valor);
        } catch (CriptografiaCredenciais.CredencialIndisponivel erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Proteção dos dados do credenciamento indisponível.");
        }
    }

    private static void confirmarAmbiente(Ambiente esperado, Ambiente atual) {
        if (esperado != atual) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "O ambiente de envio mudou. Abra a conta de recebimento novamente antes de enviar.");
        }
    }

    private static void confirmarConta(RecebedorExternoEntity registro, String digest) {
        if (!registro.daConta(digest)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "A conta Pagar.me mudou após o envio. Entre em contato com o suporte.");
        }
    }

    @Schema(name = "OperacaoEnvio")
    private static final class OperacaoEnvioBean {
        private RecebedorExternoEntity registro;

        private PagarmeRecebedorBean payload;

        private Operacao tipo;

        private RecebedorBean estado;

        public OperacaoEnvioBean() {
        }

        public RecebedorExternoEntity getRegistro() {
            return registro;
        }

        public void setRegistro(RecebedorExternoEntity registro) {
            this.registro = registro;
        }

        public PagarmeRecebedorBean getPayload() {
            return payload;
        }

        public void setPayload(PagarmeRecebedorBean payload) {
            this.payload = payload;
        }

        public Operacao getTipo() {
            return tipo;
        }

        public void setTipo(Operacao tipo) {
            this.tipo = tipo;
        }

        public RecebedorBean getEstado() {
            return estado;
        }

        public void setEstado(RecebedorBean estado) {
            this.estado = estado;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof OperacaoEnvioBean outro)) {
                return false;
            }
            return Objects.equals(registro, outro.registro) && Objects.equals(payload, outro.payload)
                    && Objects.equals(tipo, outro.tipo) && Objects.equals(estado, outro.estado);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(registro);
            resultado = 31 * resultado + Objects.hashCode(payload);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(estado);
            return resultado;
        }

        @Override
        public String toString() {
            return "OperacaoEnvio[" + "registro=" + registro + ", payload=" + payload + ", tipo=" + tipo + ", estado="
                    + estado + "]";
        }
    }
}
