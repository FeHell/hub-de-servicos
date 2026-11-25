package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastroBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.CadastroBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IdentificarMunicipioBean;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.OpcaoMunicipioBean;
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
    public CadastroBean consultar(UUID id) {
        return cadastro(usuarios.findById(id).orElseThrow(AutenticacaoNegadaException::new));
    }

    @Transactional
    public CadastroBean atualizar(UUID id, AtualizarCadastroBean dados) {
        var usuario = bloquear(id);
        if (dados.getNome() != null) {
            usuario.atualizarNome(dados.getNome());
        }
        if (dados.getRemuneracao() != null) {
            if (!permissoes.consultarPapeis(id).contains(PapelUsuario.TRABALHADOR)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Selecione Trabalhador para editar a remuneração pretendida.");
            }
            usuario.atualizarRemuneracao(dados.getRemuneracao().getValorHora(),
                    dados.getRemuneracao().getValorDiaria());
        }
        if (dados.getTelefone() != null) {
            telefones.atualizar(usuario, dados.getTelefone());
        }
        return cadastro(usuario);
    }

    @Transactional
    public CadastroBean selecionarPapel(UUID id, UUID idAplicacao, PapelUsuario papel, boolean selecionado) {
        var usuario = bloquear(id);
        permissoes.selecionarPapel(id, idAplicacao, papel, selecionado);
        return cadastro(usuario);
    }

    @Transactional
    public CadastroBean registrarMunicipio(UUID id, IdentificarMunicipioBean dados) {
        String estado = normalizar(dados.getEstado());
        var municipio = municipios.buscar(normalizar(dados.getCidade()), estado.toUpperCase(Locale.ROOT), estado)
                .orElseThrow(() -> {
                    var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                            "Não foi possível identificar o município brasileiro.");
                    return responseStatusExceptionPreparado;
                });
        var usuario = bloquear(id);
        if (!usuario.isMunicipioManual() || dados.getConfirmada()) {
            usuario.atualizarMunicipio(municipio, relogio.instant(), false);
        }
        return cadastro(usuario);
    }

    @Transactional
    public CadastroBean selecionarMunicipio(UUID id, int codigoIbge) {
        var municipio = municipios.findById(codigoIbge).orElseThrow(() -> {
            var responseStatusExceptionPreparado2 = new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Município não encontrado.");
            return responseStatusExceptionPreparado2;
        });
        var usuario = bloquear(id);
        usuario.atualizarMunicipio(municipio, relogio.instant(), true);
        return cadastro(usuario);
    }

    @Transactional(readOnly = true)
    public List<OpcaoMunicipioBean> pesquisarMunicipios(String nome) {
        String termo = normalizar(nome);
        if (termo.length() < 2) {
            return List.of();
        }
        String literal = termo.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return municipios.pesquisar("%" + literal + "%", literal + "%", PageRequest.of(0, 25)).stream()
                .map(OpcaoMunicipioBean::de).toList();
    }

    CadastroBean cadastro(UsuarioEntity usuario) {
        return CadastroBean.de(usuario, permissoes.consultarPapeis(usuario.getId()));
    }

    private static String normalizar(String texto) {
        return ACENTOS.matcher(Normalizer.normalize(texto.strip().toLowerCase(Locale.ROOT), Normalizer.Form.NFD))
                .replaceAll("");
    }

    UsuarioEntity bloquear(UUID id) {
        var usuario = usuarios.bloquearPorId(id).orElseThrow(AutenticacaoNegadaException::new);
        // A validação da sessão pode ter carregado a conta antes da aquisição do
        // bloqueio.
        persistencia.refresh(usuario);
        return usuario;
    }
}
