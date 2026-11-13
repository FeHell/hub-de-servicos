package br.com.gerenciamentoplataforma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BaseGerenciamentoPlataformaApplication {

    public static void main(String[] args) {
        SpringApplication.run(BaseGerenciamentoPlataformaApplication.class, args);
    }

}
