package br.com.hubdeservicos.service.autorizacao;

import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
import br.com.hubdeservicos.repository.usuario.UsuarioPerfilRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioPermissaoPlataformaRepository;

import java.util.List;
import java.util.UUID;

import org.springframework.core.env.Environment;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("autorizacao")
public class AutorizacaoService {

    private final AutenticacaoService autenticacao;
    private final UsuarioPerfilRepository perfis;

    private final UsuarioPermissaoPlataformaRepository permissoesPlataforma;
    private final UUID idAplicacaoPlataforma;

    public AutorizacaoService(AutenticacaoService autenticacao, UsuarioPerfilRepository perfis,
                              UsuarioPermissaoPlataformaRepository permissoesPlataforma, Environment ambiente) {
        this.autenticacao = autenticacao;
        this.perfis = perfis;
        this.permissoesPlataforma = permissoesPlataforma;
        this.idAplicacaoPlataforma = ambiente.getProperty(AcessoAdministrativo.PROPRIEDADE_APLICACAO, UUID.class);
    }

    @Transactional(readOnly = true)
    public List<String> listarPermissoes(String slug, IdentidadeJwt identidade) {
        return consultarPermissoes(slug, identidade);
    }

    @Transactional(readOnly = true)
    public boolean temPermissao(Authentication autenticacaoAtual, String slug, String codigo) {
        if (autenticacaoAtual == null || !autenticacaoAtual.isAuthenticated()
                || !(autenticacaoAtual.getPrincipal() instanceof IdentidadeJwt identidade)
                || codigo == null || codigo.isBlank()) {
            return false;
        }
        try {
            return consultarPermissoes(slug, identidade).contains(codigo);
        } catch (AutenticacaoNegadaException erro) {
            return false;
        }
    }

    private List<String> consultarPermissoes(String slug, IdentidadeJwt identidade) {
        if (slug == null || identidade == null) {
            throw new AutenticacaoNegadaException();
        }
        var usuario = autenticacao.consultarUsuario(slug, identidade);
        if (AcessoAdministrativo.SLUG_PLATAFORMA.equals(slug) || usuario.idAplicacao().equals(idAplicacaoPlataforma)) {
            return permissoesPlataforma.listarCodigosAtivos(usuario.id());
        }
        return perfis.buscarPermissoesEfetivas(usuario.id(), usuario.idAplicacao());
    }
}
