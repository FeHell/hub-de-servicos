package br.com.gerenciamentoplataforma.autenticacao.service;

import java.util.UUID;

public record IdentidadeJwt(UUID idUsuario, UUID idAplicacao, UUID idSessao, String audiencia) {
}
