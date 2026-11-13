package br.com.hubdeservicos.autenticacao.exception;

public class AutenticacaoNegadaException extends RuntimeException {

    public AutenticacaoNegadaException() {
        super("Credenciais inválidas ou sessão indisponível.");
    }
}
