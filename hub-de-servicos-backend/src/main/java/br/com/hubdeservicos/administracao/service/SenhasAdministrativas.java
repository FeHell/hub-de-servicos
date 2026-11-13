package br.com.hubdeservicos.administracao.service;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SenhasAdministrativas {

    private final PasswordEncoder senhas;

    public SenhasAdministrativas(PasswordEncoder senhas) {
        this.senhas = senhas;
    }

    public String gerarHash(String senha) {
        if (senha == null || senha.isBlank() || senha.length() < 12
                || senha.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A senha deve ter pelo menos 12 caracteres e no máximo 72 bytes em UTF-8.");
        }
        return senhas.encode(senha);
    }
}
