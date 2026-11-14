package br.com.hubdeservicos.dto.app;

import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacao;
import br.com.hubdeservicos.dto.autenticacao.UsuarioAutenticado;
import br.com.hubdeservicos.dto.usuario.EmailUsuario;
import br.com.hubdeservicos.dto.usuario.SenhaNova;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ContratosApp {
    private ContratosApp() {
    }

    public record CadastroEntrada(@EmailUsuario String email,
                                  @SenhaNova @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha) {
        @Override
        public String toString() {
            return "CadastroEntrada";
        }
    }

    public record LoginAppEntrada(@EmailUsuario String email,
                                  @NotBlank @Size(max = 72)
                                  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha) {
        @Override
        public String toString() {
            return "LoginAppEntrada";
        }
    }

    public record ContaApp(UUID id, String email) {
        public static ContaApp de(UsuarioAutenticado usuario) {
            return new ContaApp(usuario.id(), usuario.email());
        }
    }

    public record AcessoApp(String tokenAcesso, String tokenAtualizacao, Instant expiracaoTokenAcesso, ContaApp usuario) {
        public static AcessoApp de(TokensAutenticacao tokens, UsuarioAutenticado usuario) {
            return new AcessoApp(tokens.tokenAcesso(), tokens.tokenAtualizacao(), tokens.expiracaoTokenAcesso(),
                    ContaApp.de(usuario));
        }

        @Override
        public String toString() {
            return "AcessoApp";
        }
    }
}
