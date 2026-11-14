package br.com.hubdeservicos.dto.autenticacao;

import java.time.Instant;

public record TokensAutenticacao(String tokenAcesso, String tokenAtualizacao, Instant expiracaoTokenAcesso) {
    @Override
    public String toString() {
        return "TokensAutenticacao";
    }
}
