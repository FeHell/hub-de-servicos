package br.com.hubdeservicos.autenticacao.service;

import java.util.UUID;

public record IdentidadeJwt(UUID idUsuario, UUID idAplicacao, UUID idSessao, String audiencia) {
}
