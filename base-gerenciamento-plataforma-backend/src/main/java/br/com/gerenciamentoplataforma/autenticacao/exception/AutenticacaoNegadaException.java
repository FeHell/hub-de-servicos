package br.com.gerenciamentoplataforma.autenticacao.exception;

public class AutenticacaoNegadaException extends RuntimeException {

    public AutenticacaoNegadaException() {
        super("Credenciais inválidas ou sessão indisponível.");
    }
}
