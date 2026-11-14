package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.service.autenticacao.IdentidadeJwt;
import br.com.hubdeservicos.service.autorizacao.AutorizacaoService;

import java.util.UUID;

import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component("acessoAdministrativo")
public class AcessoAdministrativo {

    public static final String PLATAFORMA_ADMINISTRAR = "PLATAFORMA_ADMINISTRAR";
    public static final String APLICACAO_ADMINISTRAR = "APLICACAO_ADMINISTRAR";
    public static final String SLUG_PLATAFORMA = "hub-de-servicos-admin";
    public static final String PROPRIEDADE_APLICACAO = "hub.administracao.aplicacao-id";

    private final AutorizacaoService autorizacao;
    private final UUID idAplicacaoPlataforma;

    public AcessoAdministrativo(AutorizacaoService autorizacao, Environment ambiente) {
        this.autorizacao = autorizacao;
        this.idAplicacaoPlataforma = ambiente.getProperty(PROPRIEDADE_APLICACAO, UUID.class);
    }

    public boolean plataforma(Authentication autenticacao) {
        return autenticacao != null && autenticacao.getPrincipal() instanceof IdentidadeJwt identidade
                && identidade.idAplicacao().equals(idAplicacaoPlataforma)
                && autorizacao.temPermissao(autenticacao, identidade.audiencia(), PLATAFORMA_ADMINISTRAR);
    }

    UUID idAplicacaoPlataforma() {
        return idAplicacaoPlataforma;
    }

    public boolean aplicacao(Authentication autenticacao, String slug) {
        return plataforma(autenticacao)
                || (autenticacao != null && autenticacao.getPrincipal() instanceof IdentidadeJwt identidade
                && !identidade.idAplicacao().equals(idAplicacaoPlataforma)
                && autorizacao.temPermissao(autenticacao, slug, APLICACAO_ADMINISTRAR));
    }

    public void verificarAplicacaoEditavel(UUID idAplicacao) {
        if (idAplicacao.equals(idAplicacaoPlataforma)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A aplicação administrativa é reservada.");
        }
    }
}
