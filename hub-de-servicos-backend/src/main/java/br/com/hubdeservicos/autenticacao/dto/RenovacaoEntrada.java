package br.com.hubdeservicos.autenticacao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record RenovacaoEntrada(
        @NotNull @Pattern(regexp = "[A-Za-z0-9_-]{43}")
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String tokenAtualizacao
) {
    @Override
    public String toString() {
        return "RenovacaoEntrada";
    }
}
