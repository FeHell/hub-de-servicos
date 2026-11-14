package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.Cadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IdentificarMunicipio;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.OpcaoMunicipio;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.repository.usuario.MunicipioRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.administracao.PermissoesPlataformaService;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import jakarta.persistence.EntityManager;
import java.text.Normalizer;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PerfilAppService {
    private static final Pattern ACENTOS = Pattern.compile("\\p{M}+");
    private final UsuarioRepository usuarios;
    private final TelefoneUsuarioService telefones;
    private final EntityManager persistencia;
    private final MunicipioRepository municipios;
    private final Clock relogio;
    private final PermissoesPlataformaService permissoes;

    public PerfilAppService(UsuarioRepository usuarios, TelefoneUsuarioService telefones, EntityManager persistencia,
                           MunicipioRepository municipios, Clock relogio, PermissoesPlataformaService permissoes) {
        this.usuarios = usuarios;
        this.telefones = telefones;
        this.persistencia = persistencia;
        this.municipios = municipios;
        this.relogio = relogio;
        this.permissoes = permissoes;
    }

    @Transactional(readOnly = true)
    public Cadastro consultar(UUID id) {
        return cadastro(usuarios.findById(id).orElseThrow(AutenticacaoNegadaException::new));
    }

    @Transactional
    public Cadastro atualizar(UUID id, AtualizarCadastro dados) {
        var usuario = bloquear(id);
        if (dados.nome() != null) usuario.atualizarNome(dados.nome());
        if (dados.remuneracao() != null) {
            if (!permissoes.consultarPapeis(id).contains(PapelUsuario.TRABALHADOR)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Selecione Trabalhador para editar a remuneração pretendida.");
            }
            usuario.atualizarRemuneracao(dados.remuneracao().valorHora(), dados.remuneracao().valorDiaria());
        }
        if (dados.telefone() != null) telefones.atualizar(usuario, dados.telefone());
        return cadastro(usuario);
    }

    @Transactional
    public Cadastro selecionarPapel(UUID id, UUID idAplicacao, PapelUsuario papel, boolean selecionado) {
        var usuario = bloquear(id);
        permissoes.selecionarPapel(id, idAplicacao, papel, selecionado);
        return cadastro(usuario);
    }

    @Transactional
    public Cadastro registrarMunicipio(UUID id, IdentificarMunicipio dados) {
        String estado = normalizar(dados.estado());
        var municipio = municipios.buscar(normalizar(dados.cidade()), estado.toUpperCase(Locale.ROOT), estado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Não foi possível identificar o município brasileiro."));
        var usuario = bloquear(id);
        if (!usuario.isMunicipioManual() || dados.confirmada()) {
            usuario.atualizarMunicipio(municipio, relogio.instant(), false);
        }
        return cadastro(usuario);
    }

    @Transactional
    public Cadastro selecionarMunicipio(UUID id, int codigoIbge) {
        var municipio = municipios.findById(codigoIbge)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Município não encontrado."));
        var usuario = bloquear(id);
        usuario.atualizarMunicipio(municipio, relogio.instant(), true);
        return cadastro(usuario);
    }

    @Transactional(readOnly = true)
    public List<OpcaoMunicipio> pesquisarMunicipios(String nome) {
        String termo = normalizar(nome);
        if (termo.length() < 2) return List.of();
        String literal = termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return municipios.pesquisar("%" + literal + "%", literal + "%", PageRequest.of(0, 25))
                .stream().map(OpcaoMunicipio::de).toList();
    }

    Cadastro cadastro(UsuarioEntity usuario) {
        return Cadastro.de(usuario, permissoes.consultarPapeis(usuario.getId()));
    }

    private static String normalizar(String texto) {
        return ACENTOS.matcher(Normalizer.normalize(texto.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("");
    }

    UsuarioEntity bloquear(UUID id) {
        var usuario = usuarios.bloquearPorId(id).orElseThrow(AutenticacaoNegadaException::new);
        // A validação da sessão pode ter carregado a conta antes da aquisição do bloqueio.
        persistencia.refresh(usuario);
        return usuario;
    }
}
