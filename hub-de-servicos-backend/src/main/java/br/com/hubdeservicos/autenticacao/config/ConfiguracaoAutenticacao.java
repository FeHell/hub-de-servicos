package br.com.hubdeservicos.autenticacao.config;

import java.net.URI;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ConfigurationProperties("hub.autenticacao")
public record ConfiguracaoAutenticacao(
        URI emissor,
        Path chavePrivada,
        Path chavePublica,
        Map<UUID, String> clientesConfidenciais
) {
    public ConfiguracaoAutenticacao {
        clientesConfidenciais = clientesConfidenciais == null ? Map.of() : Map.copyOf(clientesConfidenciais);
        if (clientesConfidenciais.values().stream().anyMatch(hash -> !hash.matches("[0-9a-fA-F]{64}"))) {
            throw new IllegalArgumentException("Configure cada cliente confidencial com o hash SHA-256 hexadecimal do segredo.");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ConfiguracaoAutenticacao.class)
    static class Componentes {

        @Bean
        Clock relogio() {
            return Clock.systemUTC();
        }

        @Bean
        PasswordEncoder codificadorSenha() {
            return new BCryptPasswordEncoder(12);
        }
    }
}
