package br.com.hubdeservicos.dto.autenticacao;

import java.util.UUID;

public record UsuarioAutenticado(UUID id, String login, String email, UUID idAplicacao, String aplicacao) {
}
