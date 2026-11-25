package br.com.hubdeservicos.dto.autenticacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import java.time.Instant;

@Schema(name = "TokensAutenticacao")
public final class TokensAutenticacaoBean {
    private String tokenAcesso;

    private String tokenAtualizacao;

    private Instant expiracaoTokenAcesso;

    public TokensAutenticacaoBean() {
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

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof TokensAutenticacaoBean outro)) {
            return false;
        }
        return Objects.equals(tokenAcesso, outro.tokenAcesso)
                && Objects.equals(tokenAtualizacao, outro.tokenAtualizacao)
                && Objects.equals(expiracaoTokenAcesso, outro.expiracaoTokenAcesso);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(tokenAcesso);
        resultado = 31 * resultado + Objects.hashCode(tokenAtualizacao);
        resultado = 31 * resultado + Objects.hashCode(expiracaoTokenAcesso);
        return resultado;
    }

    @Override
    public String toString() {
        return "TokensAutenticacao";
    }
}
