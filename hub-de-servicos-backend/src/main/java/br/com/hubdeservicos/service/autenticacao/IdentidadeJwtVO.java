package br.com.hubdeservicos.service.autenticacao;

import java.util.UUID;

public record IdentidadeJwtVO(UUID idUsuario, UUID idAplicacao, UUID idSessao, String audiencia) {

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public UUID getIdSessao() {
        return idSessao;
    }

    public String getAudiencia() {
        return audiencia;
    }
}
