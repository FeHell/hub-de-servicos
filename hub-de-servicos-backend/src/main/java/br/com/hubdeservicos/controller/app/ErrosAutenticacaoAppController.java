package br.com.hubdeservicos.controller.app;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {AutenticacaoAppController.class, PerfilAppController.class})
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrosAutenticacaoAppController {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail dadosInvalidos(MethodArgumentNotValidException erro) {
        var problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Confira os dados informados.");
        var campos = erro.getBindingResult().getFieldErrors().stream().map(item -> item.getField()).distinct().toList();
        problema.setProperty("camposInvalidos", campos);
        return problema;
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> falhaConhecida(ResponseStatusException erro) {
        return ResponseEntity.status(erro.getStatusCode()).body(erro.getBody());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail conflitoDeCadastro(DataIntegrityViolationException erro) {
        if (erro.getCause() instanceof ConstraintViolationException violacao
                && "idx_usuario_email_lower".equals(violacao.getConstraintName())) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "Não foi possível concluir o cadastro.");
        }
        throw erro;
    }
}
