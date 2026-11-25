package br.com.hubdeservicos.bootstrap.administracao;

import br.com.hubdeservicos.dto.administracao.ContratosAdministrativos.NovoUsuarioBean;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.service.administracao.AcessoAdministrativo;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BootstrapAdminTest {
    @Test
    void montagemPorSettersPreservaAdministradorAtivoDoConstrutorAnterior() {
        UUID aplicacao = UUID.fromString("11111111-1111-4111-8111-111111111111");
        Environment ambiente = mock(Environment.class);
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        InicializacaoAdministrativa inicializacao = mock(InicializacaoAdministrativa.class);
        when(ambiente.getRequiredProperty(AcessoAdministrativo.PROPRIEDADE_APLICACAO, UUID.class))
                .thenReturn(aplicacao);
        when(ambiente.getRequiredProperty("HUB_BOOTSTRAP_LOGIN")).thenReturn("exemplo");
        when(ambiente.getRequiredProperty("HUB_BOOTSTRAP_EMAIL")).thenReturn("exemplo@example.test");
        when(ambiente.getRequiredProperty("HUB_BOOTSTRAP_SENHA")).thenReturn("senha-sintetica");
        when(jdbc.queryForObject("select current_database()", String.class)).thenReturn("hub_de_servicos");

        BootstrapAdmin bootstrap = new BootstrapAdmin(ambiente, jdbc, inicializacao);
        bootstrap.run(null);

        ArgumentCaptor<NovoUsuarioBean> capturado = ArgumentCaptor.forClass(NovoUsuarioBean.class);
        verify(inicializacao).criarPrimeiroAdministrador(eq(aplicacao), capturado.capture());
        NovoUsuarioBean dados = capturado.getValue();
        assertThat(dados.getLogin()).isEqualTo("exemplo");
        assertThat(dados.getEmail()).isEqualTo("exemplo@example.test");
        assertThat(dados.getSenha()).isEqualTo("senha-sintetica");
        assertThat(dados.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(dados.getPermissoesPlataforma()).isNull();
    }
}
