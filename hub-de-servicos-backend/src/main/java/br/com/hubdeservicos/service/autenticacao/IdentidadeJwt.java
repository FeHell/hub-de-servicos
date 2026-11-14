package br.com.hubdeservicos.service.autenticacao;

import java.util.UUID;

public record IdentidadeJwt(UUID idUsuario, UUID idAplicacao, UUID idSessao, String audiencia) {
}
