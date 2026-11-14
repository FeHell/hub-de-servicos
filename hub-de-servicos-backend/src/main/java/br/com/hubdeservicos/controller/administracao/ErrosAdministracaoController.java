package br.com.hubdeservicos.controller.administracao;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {AdministracaoPlataformaController.class,
        AdministracaoAplicacaoController.class, AdministracaoConfiguracaoController.class})
public class ErrosAdministracaoController {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> erroDeOperacao(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail dadosEmConflito() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Os dados conflitam com um registro existente.");
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    public ProblemDetail dadosInvalidos() {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Dados da operação inválidos.");
    }
}
