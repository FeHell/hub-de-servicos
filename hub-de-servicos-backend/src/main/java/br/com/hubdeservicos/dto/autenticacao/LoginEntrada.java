package br.com.hubdeservicos.dto.autenticacao;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginEntrada(
        @NotBlank @Size(max = 50) String login,
        @NotBlank @Size(max = 72) @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha
) {
    @Override
    public String toString() {
        return "LoginEntrada";
    }
}
