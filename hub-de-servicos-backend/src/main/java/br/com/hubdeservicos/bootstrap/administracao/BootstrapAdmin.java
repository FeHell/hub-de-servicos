package br.com.hubdeservicos.bootstrap.administracao;

import br.com.hubdeservicos.entity.usuario.StatusUsuario;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuarioBean;
import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;

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
        NovoUsuarioBean novoUsuarioPreparado = new NovoUsuarioBean();
        novoUsuarioPreparado.setLogin(ambiente.getRequiredProperty("HUB_BOOTSTRAP_LOGIN"));
        novoUsuarioPreparado.setEmail(ambiente.getRequiredProperty("HUB_BOOTSTRAP_EMAIL"));
        novoUsuarioPreparado.setSenha(ambiente.getRequiredProperty("HUB_BOOTSTRAP_SENHA"));
        novoUsuarioPreparado.setStatus(StatusUsuario.ATIVO);

        String banco = jdbc.queryForObject("select current_database()", String.class);
        if (!"hub_de_servicos".equals(banco)) {
            throw new IllegalStateException("O bootstrap exige uma conexão ao banco hub_de_servicos.");
        }
        inicializacao.criarPrimeiroAdministrador(idAplicacao, novoUsuarioPreparado);
        log.info("Primeiro administrador criado. Aplicação: {}. UUID: {}.", InicializacaoAdministrativa.SLUG,
                idAplicacao);
    }
}
