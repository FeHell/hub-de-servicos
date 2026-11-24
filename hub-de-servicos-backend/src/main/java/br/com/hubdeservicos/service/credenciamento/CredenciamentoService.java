package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Credenciamento;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Dados;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Recebedor;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.Completude;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.ConfirmacaoAlteracao;
import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.StatusPagarme;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor.UnidadeMonetaria;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.entity.credenciamento.CredenciamentoEntity;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.repository.credenciamento.CredenciamentoRepository;
import br.com.hubdeservicos.repository.credenciamento.RecebedorExternoRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioPermissaoPlataformaRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.app.AutenticacaoAppService;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.ErrorResponseException;

@Service
public class CredenciamentoService {
    private final CredenciamentoRepository registros;
    private final UsuarioRepository usuarios;
    private final UsuarioPermissaoPlataformaRepository permissoes;
    private final AutenticacaoService autenticacao;
    private final AuditoriaRepository auditoria;
    private final CriptografiaCredenciais criptografia;
    private final ObjectMapper json;
    private final Clock relogio;
    private final ValidacaoCredenciamento validacao;
    private final MapeadorPagarme mapeador;
    private final AutenticacaoPagarme pagarme;
    private final ComparacaoCredenciamento comparacao;
    private final RecebedorExternoRepository recebedores;
    private final AmbienteServicosService ambientes;
    private final SituacaoCredenciamento situacoes;

    public CredenciamentoService(CredenciamentoRepository registros, UsuarioRepository usuarios,
            UsuarioPermissaoPlataformaRepository permissoes, AutenticacaoService autenticacao,
            AuditoriaRepository auditoria, CriptografiaCredenciais criptografia, ObjectMapper json, Clock relogio,
            ValidacaoCredenciamento validacao, MapeadorPagarme mapeador, AutenticacaoPagarme pagarme,
            ComparacaoCredenciamento comparacao, RecebedorExternoRepository recebedores,
            AmbienteServicosService ambientes, SituacaoCredenciamento situacoes) {
        this.registros = registros;
        this.usuarios = usuarios;
        this.permissoes = permissoes;
        this.autenticacao = autenticacao;
        this.auditoria = auditoria;
        this.criptografia = criptografia;
        this.json = json;
        this.relogio = relogio;
        this.validacao = validacao;
        this.mapeador = mapeador;
        this.pagarme = pagarme;
        this.comparacao = comparacao;
        this.recebedores = recebedores;
        this.ambientes = ambientes;
        this.situacoes = situacoes;
    }

    @Transactional(readOnly = true)
    public Credenciamento consultar(IdentidadeJwt identidade, ProvedorCredenciamento provedor) {
        UUID usuario = autorizar(identidade, false);
        var registro = registros.findBySolicitanteAndProvedor(usuario, provedor).orElseThrow(CredenciamentoService::ausente);
        return resposta(registro, ler(registro));
    }

    @Transactional(readOnly = true)
    public Completude validar(IdentidadeJwt identidade, Dados dados) {
        autorizar(identidade, false);
        return validacao.avaliar(dados);
    }

    @PreAuthorize("@acessoAdministrativo.plataforma(authentication)")
    @Transactional(readOnly = true)
    public Previa preparar(UUID id, UnidadeMonetaria unidade) {
        var registro = registros.findById(id).filter(cadastro -> !cadastro.excluido()).orElseThrow(CredenciamentoService::ausente);
        var ambiente = pagarme.selecionarConta().ambiente();
        var chave = new RecebedorExternoEntity.Chave(registro.getId(), registro.getProvedor(), ambiente);
        var payload = mapeador.preparar(ler(registro), unidade, registro.getSolicitante(), chave.codigo());
        return new Previa(ambiente, unidade, payload);
    }

    public record Previa(Ambiente ambiente, UnidadeMonetaria unidadeMonetaria, PagarmeRecebedor payload) {}

    Dados ler(CredenciamentoEntity registro) {
        return lerProtegidos(registro, registro.getDadosProtegidos());
    }

    Dados lerProtegidos(CredenciamentoEntity registro, String protegido) {
        try {
            return json.readValue(criptografia.descriptografar(
                    contexto(registro.getId(), registro.getSolicitante()), protegido), Dados.class);
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw dadosIndisponiveis();
        }
    }

    @Transactional
    public Credenciamento salvar(IdentidadeJwt identidade, ProvedorCredenciamento provedor, Dados dados, boolean criar,
            Long versaoEsperada, ConfirmacaoAlteracao confirmacao, UUID idEsperado) {
        UUID usuario = autorizar(identidade, true);
        var atual = registros.findBySolicitanteAndProvedor(usuario, provedor);
        if (criar && atual.isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe um credenciamento para este provedor.");
        }
        if (!criar && atual.isEmpty()) throw ausente();
        if (atual.isPresent()) {
            var cadastro = atual.get();
            conferirIdentidade(cadastro, idEsperado);
            conferirVersao(cadastro, versaoEsperada);
            var anterior = ler(cadastro);
            if (comparacao.iguais(anterior, dados)) return resposta(cadastro, anterior);
            var historico = recebedores.findByIdCredenciamentoOrderByIdAmbiente(cadastro.getId());
            if (!historico.isEmpty() && versaoEsperada == null) throw conflitoVersao();
            var protegidos = historico.stream().filter(r -> SituacaoCredenciamento.requerConfirmacao(r, cadastro.versao())).toList();
            boolean aprovado = protegidos.stream().anyMatch(r -> r.statusProvedor() == StatusPagarme.ACTIVE
                    && situacoes.aprovacaoConfirmada(r));
            if (!protegidos.isEmpty() && (confirmacao == null || aprovado && confirmacao != ConfirmacaoAlteracao.APROVACAO)) {
                var status = HttpStatus.CONFLICT;
                var problema = ProblemDetail.forStatusAndDetail(status,
                        "Confirme a alteração dos dados já submetidos à Stone.");
                problema.setProperty("codigo", "CONFIRMAR_ALTERACAO_CREDENCIAMENTO");
                problema.setProperty("aprovado", aprovado);
                throw new ErrorResponseException(status, problema, null);
            }
        }
        UUID id = atual.map(CredenciamentoEntity::getId).orElseGet(UUID::randomUUID);
        return resposta(gravar(identidade, provedor, dados, usuario, id, atual), dados);
    }

    @Transactional
    public void excluir(IdentidadeJwt identidade, ProvedorCredenciamento provedor, UUID idEsperado, Long versaoEsperada) {
        UUID usuario = autorizar(identidade, true);
        var cadastro = registros.findBySolicitanteAndProvedor(usuario, provedor).orElse(null);
        if (cadastro == null) return;
        if (idEsperado == null || versaoEsperada == null) throw conflitoVersao();
        if (!idEsperado.equals(cadastro.getId())) {
            boolean jaExcluido = registros.findById(idEsperado)
                    .filter(registro -> usuario.equals(registro.getSolicitante()) && provedor == registro.getProvedor()
                            && registro.excluido()).isPresent();
            if (jaExcluido) return;
            throw conflitoVersao();
        }
        conferirVersao(cadastro, versaoEsperada);
        recebedores.findByIdCredenciamentoOrderByIdAmbiente(cadastro.getId());
        boolean preservarHistorico = registros.possuiHistorico(cadastro.getId());
        if (!preservarHistorico) registros.delete(cadastro);
        else {
            try {
                cadastro.excluir(criptografia.criptografar(contexto(cadastro.getId(), usuario), "{}"), relogio.instant());
            } catch (CredencialIndisponivel erro) {
                throw dadosIndisponiveis();
            }
        }
        auditoria.save(new AuditoriaEntity(usuario, identidade.idAplicacao(), "EXCLUIR_CREDENCIAMENTO",
                "credenciamento_empresarial", null, json.createObjectNode().put("idRecurso", cadastro.getId().toString())
                        .put("historicoPreservado", preservarHistorico), null));
        registros.flush();
    }

    static void conferirIdentidade(CredenciamentoEntity cadastro, UUID idEsperado) {
        if (idEsperado != null && !idEsperado.equals(cadastro.getId())) throw conflitoVersao();
    }

    CredenciamentoEntity gravar(IdentidadeJwt identidade, ProvedorCredenciamento provedor, Dados dados,
            UUID usuario, UUID id, Optional<CredenciamentoEntity> atual) {
        if (atual.isPresent() && iguais(atual.get(), dados)) return atual.get();
        String protegido;
        try {
            protegido = criptografia.criptografar(contexto(id, usuario), json.writeValueAsString(dados));
        } catch (JsonProcessingException | CredencialIndisponivel erro) {
            throw dadosIndisponiveis();
        }
        var agora = relogio.instant();
        var registro = atual.orElseGet(() -> new CredenciamentoEntity(id, usuario, provedor, protegido, agora));
        if (atual.isEmpty()) registros.save(registro);
        else registro.atualizar(protegido, agora);
        var metadados = json.createObjectNode().put("idRecurso", id.toString()).put("provedor", provedor.name());
        auditoria.save(new AuditoriaEntity(usuario, identidade.idAplicacao(),
                atual.isEmpty() ? "CRIAR_CREDENCIAMENTO" : "ATUALIZAR_CREDENCIAMENTO",
                "credenciamento_empresarial", null, metadados, null));
        return registro;
    }

    boolean iguais(CredenciamentoEntity registro, Dados novos) { return comparacao.iguais(ler(registro), novos); }

    static void conferirVersao(CredenciamentoEntity cadastro, Long versaoEsperada) {
        if (versaoEsperada != null && cadastro.versao() != versaoEsperada) throw conflitoVersao();
    }

    static ErrorResponseException conflitoVersao() {
        var status = HttpStatus.CONFLICT;
        var problema = ProblemDetail.forStatusAndDetail(status, "Os dados do credenciamento mudaram. Recarregue o formulário.");
        problema.setProperty("codigo", "VERSAO_CREDENCIAMENTO_DESATUALIZADA");
        return new ErrorResponseException(status, problema, null);
    }

    private UUID autorizar(IdentidadeJwt identidade, boolean escrita) {
        return autorizar(identidade, escrita, false);
    }

    UUID autorizarTrabalhador(IdentidadeJwt identidade, boolean escrita) {
        return autorizar(identidade, escrita, true);
    }

    private UUID autorizar(IdentidadeJwt identidade, boolean escrita, boolean somenteTrabalhador) {
        UUID id = autenticacao.consultarUsuario(AutenticacaoAppService.SLUG, identidade).id();
        if (escrita) usuarios.bloquearPorId(id).orElseThrow(CredenciamentoService::ausente);
        var codigos = permissoes.listarCodigosAtivos(id);
        if (!codigos.contains(PapelUsuario.EMPRESA.codigoPermissao())
                && !codigos.contains(PapelUsuario.TRABALHADOR.codigoPermissao())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Habilite o perfil Empresa ou Trabalhador para acessar o credenciamento.");
        }
        if (somenteTrabalhador && !codigos.contains(PapelUsuario.TRABALHADOR.codigoPermissao())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Habilite o perfil Trabalhador para enviar o credenciamento.");
        }
        return id;
    }

    private Credenciamento resposta(CredenciamentoEntity registro, Dados dados) {
        var ambiente = ambientes.atual();
        var estado = recebedores.findById(new Chave(registro.getId(), registro.getProvedor(), ambiente))
                .map(recebedor -> respostaRecebedor(recebedor, registro, dados))
                .orElseGet(() -> SituacaoCredenciamento.naoEnviado(ambiente, registro.versao()));
        return new Credenciamento(registro.getId(), registro.getProvedor(), dados, registro.getAtualizadoEm(),
                validacao.avaliar(dados), registro.versao(), estado);
    }

    Recebedor respostaRecebedor(RecebedorExternoEntity recebedor, CredenciamentoEntity cadastro, Dados dados) {
        var operacao = situacoes.operacaoDeReenvio(recebedor, cadastro.versao(), dados,
                protegido -> lerProtegidos(cadastro, protegido));
        return situacoes.resposta(recebedor, cadastro.versao(), operacao, operacao != null && validacao.avaliar(dados).apto());
    }

    private static String contexto(UUID id, UUID usuario) {
        return "credenciamento:" + id + ":" + usuario;
    }

    private static ResponseStatusException ausente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Credenciamento ainda não cadastrado.");
    }

    private static ResponseStatusException dadosIndisponiveis() {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Dados protegidos do credenciamento indisponíveis.");
    }
}
