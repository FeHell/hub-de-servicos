package br.com.gerenciamentoplataforma.persistencia;

import org.springframework.test.context.DynamicPropertyRegistry;

public final class ConexaoPostgresTeste {

    private ConexaoPostgresTeste() {
    }

    public static void configurar(DynamicPropertyRegistry propriedades) {
        String usuario = System.getenv("DATABASE_USERNAME");
        if (usuario == null || usuario.isBlank()) {
            throw new IllegalStateException("Defina e exporte DATABASE_USERNAME antes dos testes PostgreSQL.");
        }
        String esquema = System.getenv().getOrDefault("TESTE_POSTGRES_SCHEMA", "public");
        if (!esquema.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalStateException("Informe um esquema de teste válido.");
        }
        propriedades.add("spring.datasource.url",
                () -> "jdbc:postgresql://127.0.0.1:5432/gerenciamento_usuarios?currentSchema=" + esquema);
        propriedades.add("spring.datasource.username", () -> usuario);
        propriedades.add("spring.datasource.password", () -> System.getenv().getOrDefault("DATABASE_PASSWORD", ""));
        propriedades.add("spring.datasource.hikari.schema", () -> esquema);
        propriedades.add("spring.jpa.properties.hibernate.default_schema", () -> esquema);
    }
}
