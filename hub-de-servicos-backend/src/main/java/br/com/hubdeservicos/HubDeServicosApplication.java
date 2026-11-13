package br.com.hubdeservicos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class HubDeServicosApplication {

    public static void main(String[] args) {
        SpringApplication.run(HubDeServicosApplication.class, args);
    }

}
