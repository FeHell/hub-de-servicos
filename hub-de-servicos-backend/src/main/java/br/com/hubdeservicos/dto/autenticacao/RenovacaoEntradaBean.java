package br.com.hubdeservicos.dto.autenticacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

@Schema(name = "RenovacaoEntrada")
public final class RenovacaoEntradaBean {
    @NotNull
    @Pattern(regexp = "[A-Za-z0-9_-]{43}")
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String tokenAtualizacao;

    public RenovacaoEntradaBean() {
    }

    public String getTokenAtualizacao() {
        return tokenAtualizacao;
    }

    public void setTokenAtualizacao(String tokenAtualizacao) {
        this.tokenAtualizacao = tokenAtualizacao;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof RenovacaoEntradaBean outro)) {
            return false;
        }
        return Objects.equals(tokenAtualizacao, outro.tokenAtualizacao);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(tokenAtualizacao);
        return resultado;
    }

    @Override
    public String toString() {
        return "RenovacaoEntrada";
    }
}
