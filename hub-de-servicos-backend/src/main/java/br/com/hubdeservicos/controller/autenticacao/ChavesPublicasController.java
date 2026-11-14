package br.com.hubdeservicos.controller.autenticacao;

import br.com.hubdeservicos.service.autenticacao.TokenJwt;

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
