package br.com.gerenciamentoplataforma.autenticacao.dto;

import java.time.Instant;

public record TokensAutenticacao(String tokenAcesso, String tokenAtualizacao, Instant expiracaoTokenAcesso) {
    @Override
    public String toString() {
        return "TokensAutenticacao";
    }
}
