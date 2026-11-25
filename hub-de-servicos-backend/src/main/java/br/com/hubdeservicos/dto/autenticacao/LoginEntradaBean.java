package br.com.hubdeservicos.dto.autenticacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(name = "LoginEntrada")
public final class LoginEntradaBean {
    @NotBlank
    @Size(max = 50)
    private String login;

    @NotBlank
    @Size(max = 72)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String senha;

    public LoginEntradaBean() {
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
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
        if (!(objeto instanceof LoginEntradaBean outro)) {
            return false;
        }
        return Objects.equals(login, outro.login) && Objects.equals(senha, outro.senha);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(login);
        resultado = 31 * resultado + Objects.hashCode(senha);
        return resultado;
    }

    @Override
    public String toString() {
        return "LoginEntrada";
    }
}
