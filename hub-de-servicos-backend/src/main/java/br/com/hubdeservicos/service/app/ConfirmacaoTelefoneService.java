package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.CadastroBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.ConfirmarTelefoneBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.DesafioBean;
import br.com.hubdeservicos.dto.sms.SmsSendRequestBean;
import br.com.hubdeservicos.entity.usuario.DesafioTelefoneEntity;
import br.com.hubdeservicos.repository.usuario.DesafioTelefoneRepository;
import br.com.hubdeservicos.service.sms.SmsService;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ConfirmacaoTelefoneService {
    private static final Duration VALIDADE = Duration.ofMinutes(5);
    private static final Duration INTERVALO = Duration.ofSeconds(60);
    private static final int MAX_TENTATIVAS = 5;
    private static final int MAX_ENVIOS_DIARIOS = 5;

    private final PerfilAppService perfil;
    private final TelefoneUsuarioService telefones;
    private final DesafioTelefoneRepository desafios;
    private final SmsService sms;
    private final PasswordEncoder senhas;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transacao;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    public ConfirmacaoTelefoneService(PerfilAppService perfil, TelefoneUsuarioService telefones,
            DesafioTelefoneRepository desafios, SmsService sms, PasswordEncoder senhas, JdbcTemplate jdbc,
            PlatformTransactionManager gerenciador, Clock relogio) {
        this.perfil = perfil;
        this.telefones = telefones;
        this.desafios = desafios;
        this.sms = sms;
        this.senhas = senhas;
        this.jdbc = jdbc;
        var transactionTemplatePreparado = new TransactionTemplate(gerenciador);
        this.transacao = transactionTemplatePreparado;
        this.relogio = relogio;
    }

    public DesafioBean enviar(UUID idUsuario, String entrada) {
        UUID id = UUID.randomUUID();
        String codigo = String.format(Locale.ROOT, "%06d", aleatorio.nextInt(1_000_000));
        String numero = Objects.requireNonNull(transacao.execute(status -> preparar(idUsuario, entrada, id, codigo)));

        // A reserva de número e a cota já estão confirmadas. Nenhum bloqueio fica
        // aberto durante o gateway.
        SmsSendRequestBean smsSendRequestPreparado = new SmsSendRequestBean();
        smsSendRequestPreparado.setDestinatario(numero);
        smsSendRequestPreparado.setMensagem("Conecta Cheff: seu código é " + codigo + ".");
        var envio = sms.send(smsSendRequestPreparado);
        DesafioBean resultado = transacao.execute(status -> {
            var usuario = perfil.bloquear(idUsuario);
            var desafio = buscar(id, idUsuario);
            if (!numero.equals(usuario.getTelefone()) || desafio.getCodigoHash() == null) {
                throw indisponivel();
            }
            if (!envio.getAceito()) {
                desafio.consumir();
                return null;
            }
            desafio.registrarEnvio();
            DesafioBean desafioPreparado = new DesafioBean();
            desafioPreparado.setIdDesafio(id);
            desafioPreparado.setTelefoneMascarado("+55 •• •••••-" + numero.substring(numero.length() - 4));
            desafioPreparado.setExpiraEm(desafio.getExpiraEm());
            desafioPreparado.setCadastro(perfil.cadastro(usuario));
            return desafioPreparado;
        });
        if (resultado == null) {
            String mensagem = switch (envio.getErro()) {
            case DESTINATARIO_INVALIDO -> "Confira se o telefone informado pode receber SMS.";
            case DESTINATARIO_NAO_AUTORIZADO -> "O serviço de SMS não está autorizado a enviar para este número.";
            default -> "Não foi possível enviar o SMS. Tente novamente mais tarde.";
            };
            throw erro(HttpStatus.BAD_GATEWAY, mensagem);
        }
        return resultado;
    }

    private String preparar(UUID idUsuario, String entrada, UUID id, String codigo) {
        var usuario = perfil.bloquear(idUsuario);
        String numero = entrada == null ? usuario.getTelefone() : TelefoneUsuarioService.normalizar(entrada);
        if (numero == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe o telefone antes de pedir um código.");
        }
        if (numero.equals(usuario.getTelefone()) && usuario.getTelefoneVerificadoEm() != null) {
            throw erro(HttpStatus.CONFLICT, "Este telefone já foi confirmado.");
        }
        // Serializa a cota do destinatário mesmo após trocar o número ou removê-lo de
        // uma conta.
        jdbc.query("select pg_advisory_xact_lock(hashtextextended(?, 70421))", rs -> {
        }, numero);
        Instant agora = relogio.instant();
        Instant ontem = agora.minus(Duration.ofDays(1));
        Instant limiteIntervalo = agora.minus(INTERVALO);
        if (desafios.existsByIdUsuarioAndCriadoEmGreaterThan(idUsuario, limiteIntervalo)
                || desafios.existsByTelefoneAndCriadoEmGreaterThan(numero, limiteIntervalo)) {
            throw erro(HttpStatus.TOO_MANY_REQUESTS, "Aguarde um minuto antes de pedir outro código.");
        }
        if (desafios.countByIdUsuarioAndCriadoEmGreaterThan(idUsuario, ontem) >= MAX_ENVIOS_DIARIOS
                || desafios.countByTelefoneAndCriadoEmGreaterThan(numero, ontem) >= MAX_ENVIOS_DIARIOS) {
            throw erro(HttpStatus.TOO_MANY_REQUESTS,
                    "Limite diário de solicitações atingido. Tente novamente mais tarde.");
        }
        telefones.atualizar(usuario, numero);
        desafios.invalidar(idUsuario);
        desafios.removerAntigos(agora.minus(Duration.ofDays(2)));
        var desafioTelefoneEntityPreparado = new DesafioTelefoneEntity(id, idUsuario, numero, senhas.encode(codigo),
                agora, agora.plus(VALIDADE));
        desafios.save(desafioTelefoneEntityPreparado);
        return numero;
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public CadastroBean confirmar(UUID idUsuario, ConfirmarTelefoneBean dados) {
        var usuario = perfil.bloquear(idUsuario);
        var desafio = buscar(dados.getIdDesafio(), idUsuario);
        if (!desafio.getTelefone().equals(usuario.getTelefone()) || !desafio.isEnviado()) {
            throw indisponivel();
        }
        Instant agora = relogio.instant();
        if (!agora.isBefore(desafio.getExpiraEm())) {
            desafio.consumir();
            throw erro(HttpStatus.GONE, "O código expirou. Solicite outro.");
        }
        if (desafio.getTentativas() >= MAX_TENTATIVAS) {
            throw limiteTentativas();
        }
        if (desafio.getCodigoHash() == null) {
            throw indisponivel();
        }
        telefones.verificarDisponibilidade(idUsuario, desafio.getTelefone());
        if (!senhas.matches(dados.getCodigo(), desafio.getCodigoHash())) {
            desafio.registrarErro();
            if (desafio.getTentativas() >= MAX_TENTATIVAS) {
                desafio.consumir();
                throw limiteTentativas();
            }
            throw erro(HttpStatus.BAD_REQUEST, "Código incorreto. Confira o SMS e tente novamente.");
        }
        usuario.verificarTelefone(desafio.getTelefone(), agora);
        desafio.consumir();
        return perfil.cadastro(usuario);
    }

    private DesafioTelefoneEntity buscar(UUID id, UUID usuario) {
        return desafios.findByIdAndIdUsuario(id, usuario).orElseThrow(ConfirmacaoTelefoneService::indisponivel);
    }

    private static ResponseStatusException indisponivel() {
        return erro(HttpStatus.CONFLICT, "Este código não está mais disponível. Solicite outro.");
    }

    private static ResponseStatusException limiteTentativas() {
        return erro(HttpStatus.TOO_MANY_REQUESTS, "Limite de tentativas atingido. Solicite outro código.");
    }

    private static ResponseStatusException erro(HttpStatus status, String mensagem) {
        var responseStatusExceptionPreparado = new ResponseStatusException(status, mensagem);
        return responseStatusExceptionPreparado;
    }
}
