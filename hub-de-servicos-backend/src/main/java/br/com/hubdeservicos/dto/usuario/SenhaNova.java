package br.com.hubdeservicos.dto.usuario;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@NotBlank
@Size(min = 12, max = 72)
@Constraint(validatedBy = {})
@Retention(RUNTIME)
@Target({FIELD, METHOD, PARAMETER, TYPE_USE, ANNOTATION_TYPE})
public @interface SenhaNova {
    String message() default "Senha inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
