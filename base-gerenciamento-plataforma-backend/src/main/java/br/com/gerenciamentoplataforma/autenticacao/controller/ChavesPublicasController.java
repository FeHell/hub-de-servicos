package br.com.gerenciamentoplataforma.autenticacao.controller;

import br.com.gerenciamentoplataforma.autenticacao.service.TokenJwt;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChavesPublicasController {

    private final TokenJwt jwt;

    public ChavesPublicasController(TokenJwt jwt) {
        this.jwt = jwt;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> consultar() {
        return jwt.chavesPublicas();
    }
}
