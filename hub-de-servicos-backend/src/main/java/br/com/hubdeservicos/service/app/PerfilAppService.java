package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.Cadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.IdentificarMunicipio;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.repository.usuario.MunicipioRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import jakarta.persistence.EntityManager;
import java.text.Normalizer;
import java.time.Clock;
import java.util.Locale;
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

    public PerfilAppService(UsuarioRepository usuarios, TelefoneUsuarioService telefones, EntityManager persistencia,
                           MunicipioRepository municipios, Clock relogio) {
        this.usuarios = usuarios;
        this.telefones = telefones;
        this.persistencia = persistencia;
        this.municipios = municipios;
        this.relogio = relogio;
    }

    @Transactional(readOnly = true)
    public Cadastro consultar(UUID id) {
        return Cadastro.de(usuarios.findById(id).orElseThrow(AutenticacaoNegadaException::new));
    }

    @Transactional
    public Cadastro atualizar(UUID id, AtualizarCadastro dados) {
        var usuario = bloquear(id);
        if (dados.nome() != null) usuario.atualizarNome(dados.nome());
        if (dados.remuneracao() != null) {
            if (!usuario.isPapelTrabalhador()) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Selecione Trabalhador para editar a remuneração pretendida.");
            }
            usuario.atualizarRemuneracao(dados.remuneracao().valorHora(), dados.remuneracao().valorDiaria());
        }
        if (dados.telefone() != null) telefones.atualizar(usuario, dados.telefone());
        return Cadastro.de(usuario);
    }

    @Transactional
    public Cadastro selecionarPapel(UUID id, PapelUsuario papel, boolean selecionado) {
        var usuario = bloquear(id);
        usuario.atualizarPapel(papel, selecionado);
        return Cadastro.de(usuario);
    }

    @Transactional
    public Cadastro registrarMunicipio(UUID id, IdentificarMunicipio dados) {
        String estado = normalizar(dados.estado());
        var municipio = municipios.buscar(normalizar(dados.cidade()), estado.toUpperCase(Locale.ROOT), estado)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Não foi possível identificar o município brasileiro."));
        var usuario = bloquear(id);
        usuario.atualizarMunicipio(municipio, relogio.instant());
        return Cadastro.de(usuario);
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
