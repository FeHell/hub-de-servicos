package br.com.hubdeservicos.administracao.bootstrap;

import br.com.hubdeservicos.administracao.dto.ContratosAdministrativos.NovoUsuario;
import br.com.hubdeservicos.administracao.service.AcessoAdministrativo;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnNotWebApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("bootstrap-admin")
@ConditionalOnNotWebApplication
class BootstrapAdmin implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BootstrapAdmin.class);

    private final Environment ambiente;
    private final JdbcTemplate jdbc;
    private final InicializacaoAdministrativa inicializacao;

    BootstrapAdmin(Environment ambiente, JdbcTemplate jdbc, InicializacaoAdministrativa inicializacao) {
        this.ambiente = ambiente;
        this.jdbc = jdbc;
        this.inicializacao = inicializacao;
    }

    @Override
    public void run(ApplicationArguments argumentos) {
        UUID idAplicacao = ambiente.getRequiredProperty(AcessoAdministrativo.PROPRIEDADE_APLICACAO, UUID.class);
        var dados = new NovoUsuario(ambiente.getRequiredProperty("HUB_BOOTSTRAP_LOGIN"),
                ambiente.getRequiredProperty("HUB_BOOTSTRAP_EMAIL"),
                ambiente.getRequiredProperty("HUB_BOOTSTRAP_SENHA"));
        String banco = jdbc.queryForObject("select current_database()", String.class);
        if (!"hub_de_servicos".equals(banco)) {
            throw new IllegalStateException("O bootstrap exige uma conexão ao banco hub_de_servicos.");
        }
        inicializacao.criarPrimeiroAdministrador(idAplicacao, dados);
        log.info("Primeiro administrador criado. Aplicação: {}. UUID: {}.", InicializacaoAdministrativa.SLUG, idAplicacao);
    }
}
