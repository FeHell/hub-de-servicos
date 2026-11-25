package br.com.hubdeservicos.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.autenticacao.TokensAutenticacaoBean;
import br.com.hubdeservicos.dto.autenticacao.UsuarioAutenticadoBean;
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

    @Schema(name = "CadastroEntrada")
    public static final class CadastroEntradaBean {
        @EmailUsuario
        private String email;

        @SenhaNova
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String senha;

        public CadastroEntradaBean() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof CadastroEntradaBean outro)) {
                return false;
            }
            return Objects.equals(email, outro.email) && Objects.equals(senha, outro.senha);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(senha);
            return resultado;
        }

        @Override
        public String toString() {
            return "CadastroEntrada";
        }
    }

    @Schema(name = "LoginAppEntrada")
    public static final class LoginAppEntradaBean {
        @EmailUsuario
        private String email;

        @NotBlank
        @Size(max = 72)
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String senha;

        public LoginAppEntradaBean() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof LoginAppEntradaBean outro)) {
                return false;
            }
            return Objects.equals(email, outro.email) && Objects.equals(senha, outro.senha);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(senha);
            return resultado;
        }

        @Override
        public String toString() {
            return "LoginAppEntrada";
        }
    }

    @Schema(name = "ContaApp")
    public static final class ContaAppBean {
        private UUID id;

        private String email;

        public ContaAppBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ContaAppBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(email, outro.email);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(email);
            return resultado;
        }

        @Override
        public String toString() {
            return "ContaApp[" + "id=" + id + ", email=" + email + "]";
        }

        public static ContaAppBean de(UsuarioAutenticadoBean usuario) {
            ContaAppBean contaAppPreparado = new ContaAppBean();
            contaAppPreparado.setId(usuario.getId());
            contaAppPreparado.setEmail(usuario.getEmail());
            return contaAppPreparado;
        }
    }

    @Schema(name = "AcessoApp")
    public static final class AcessoAppBean {
        private String tokenAcesso;

        private String tokenAtualizacao;

        private Instant expiracaoTokenAcesso;

        private ContaAppBean usuario;

        public AcessoAppBean() {
        }

        public String getTokenAcesso() {
            return tokenAcesso;
        }

        public void setTokenAcesso(String tokenAcesso) {
            this.tokenAcesso = tokenAcesso;
        }

        public String getTokenAtualizacao() {
            return tokenAtualizacao;
        }

        public void setTokenAtualizacao(String tokenAtualizacao) {
            this.tokenAtualizacao = tokenAtualizacao;
        }

        public Instant getExpiracaoTokenAcesso() {
            return expiracaoTokenAcesso;
        }

        public void setExpiracaoTokenAcesso(Instant expiracaoTokenAcesso) {
            this.expiracaoTokenAcesso = expiracaoTokenAcesso;
        }

        public ContaAppBean getUsuario() {
            return usuario;
        }

        public void setUsuario(ContaAppBean usuario) {
            this.usuario = usuario;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AcessoAppBean outro)) {
                return false;
            }
            return Objects.equals(tokenAcesso, outro.tokenAcesso)
                    && Objects.equals(tokenAtualizacao, outro.tokenAtualizacao)
                    && Objects.equals(expiracaoTokenAcesso, outro.expiracaoTokenAcesso)
                    && Objects.equals(usuario, outro.usuario);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(tokenAcesso);
            resultado = 31 * resultado + Objects.hashCode(tokenAtualizacao);
            resultado = 31 * resultado + Objects.hashCode(expiracaoTokenAcesso);
            resultado = 31 * resultado + Objects.hashCode(usuario);
            return resultado;
        }

        public static AcessoAppBean de(TokensAutenticacaoBean tokens, UsuarioAutenticadoBean usuario) {
            AcessoAppBean acessoAppPreparado = new AcessoAppBean();
            acessoAppPreparado.setTokenAcesso(tokens.getTokenAcesso());
            acessoAppPreparado.setTokenAtualizacao(tokens.getTokenAtualizacao());
            acessoAppPreparado.setExpiracaoTokenAcesso(tokens.getExpiracaoTokenAcesso());
            acessoAppPreparado.setUsuario(ContaAppBean.de(usuario));
            return acessoAppPreparado;
        }

        @Override
        public String toString() {
            return "AcessoApp";
        }
    }
}
