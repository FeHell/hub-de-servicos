package br.com.gerenciamentoplataforma.autenticacao.dto;

import java.util.UUID;

public record UsuarioAutenticado(UUID id, String login, String email, UUID idAplicacao, String aplicacao) {
}
