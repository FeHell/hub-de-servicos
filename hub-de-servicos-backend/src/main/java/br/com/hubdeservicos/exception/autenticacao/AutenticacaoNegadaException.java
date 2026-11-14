package br.com.hubdeservicos.exception.autenticacao;

public class AutenticacaoNegadaException extends RuntimeException {

    public AutenticacaoNegadaException() {
        super("Credenciais inválidas ou sessão indisponível.");
    }
}
