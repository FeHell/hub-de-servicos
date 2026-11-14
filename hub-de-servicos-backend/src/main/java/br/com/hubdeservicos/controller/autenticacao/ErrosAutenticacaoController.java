package br.com.hubdeservicos.controller.autenticacao;

import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {AutenticacaoController.class, SessaoNavegadorController.class})
public class ErrosAutenticacaoController {

    @ExceptionHandler(AutenticacaoNegadaException.class)
    public ResponseEntity<ProblemDetail> autenticacaoNegada(AutenticacaoNegadaException erro) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, erro.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    public ProblemDetail entradaInvalida() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados de autenticação inválidos.");
    }
}
