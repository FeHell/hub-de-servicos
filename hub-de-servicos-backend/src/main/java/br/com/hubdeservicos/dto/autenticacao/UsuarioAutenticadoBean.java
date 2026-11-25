package br.com.hubdeservicos.dto.autenticacao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import java.util.UUID;

@Schema(name = "UsuarioAutenticado")
public final class UsuarioAutenticadoBean {
    private UUID id;

    private String login;

    private String email;

    private UUID idAplicacao;

    private String aplicacao;

    public UsuarioAutenticadoBean() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public void setIdAplicacao(UUID idAplicacao) {
        this.idAplicacao = idAplicacao;
    }

    public String getAplicacao() {
        return aplicacao;
    }

    public void setAplicacao(String aplicacao) {
        this.aplicacao = aplicacao;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof UsuarioAutenticadoBean outro)) {
            return false;
        }
        return Objects.equals(id, outro.id) && Objects.equals(login, outro.login) && Objects.equals(email, outro.email)
                && Objects.equals(idAplicacao, outro.idAplicacao) && Objects.equals(aplicacao, outro.aplicacao);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(id);
        resultado = 31 * resultado + Objects.hashCode(login);
        resultado = 31 * resultado + Objects.hashCode(email);
        resultado = 31 * resultado + Objects.hashCode(idAplicacao);
        resultado = 31 * resultado + Objects.hashCode(aplicacao);
        return resultado;
    }

    @Override
    public String toString() {
        return "UsuarioAutenticado[" + "id=" + id + ", login=" + login + ", email=" + email + ", idAplicacao="
                + idAplicacao + ", aplicacao=" + aplicacao + "]";
    }
}
