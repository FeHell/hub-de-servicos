package br.com.hubdeservicos.persistencia;

import br.com.hubdeservicos.HubDeServicosApplication;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoId;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoId;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPerfilId;
import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaId;

import jakarta.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.AbstractDataSource;

import static org.assertj.core.api.Assertions.assertThat;

class MapeamentoPersistenciaTest {

    @Test
    void inicializaEntidadesERepositoriosSemConectarAoBanco() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        HibernateJpaAutoConfiguration.class, JpaRepositoriesAutoConfiguration.class))
                .withUserConfiguration(ConfiguracaoSemBanco.class)
                .withPropertyValues(
                        "spring.jpa.hibernate.ddl-auto=none",
                        "spring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect",
                        "spring.jpa.properties.hibernate.temp.use_jdbc_metadata_defaults=false")
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    var metamodelo = contexto.getBean(EntityManagerFactory.class).getMetamodel();
                    assertThat(metamodelo.getEntities()).hasSize(14);
                    assertThat(metamodelo.getEntities()).allSatisfy(entidade ->
                            assertThat(entidade.getJavaType().getSimpleName()).endsWith("Entity"));
                    assertThat(metamodelo.entity(UsuarioAplicacaoEntity.class).getIdType().getJavaType())
                            .isEqualTo(UsuarioAplicacaoId.class);
                    assertThat(metamodelo.entity(AplicacaoPermissaoEntity.class).getIdType().getJavaType())
                            .isEqualTo(AplicacaoPermissaoId.class);
                    assertThat(metamodelo.entity(PerfilPermissaoEntity.class).getIdType().getJavaType())
                            .isEqualTo(PerfilPermissaoId.class);
                    assertThat(metamodelo.entity(UsuarioPerfilEntity.class).getIdType().getJavaType())
                            .isEqualTo(UsuarioPerfilId.class);
                    assertThat(metamodelo.entity(UsuarioPermissaoPlataformaEntity.class).getIdType().getJavaType())
                            .isEqualTo(UsuarioPermissaoPlataformaId.class);
                });
    }

    @TestConfiguration(proxyBeanMethods = false)
    @AutoConfigurationPackage(basePackageClasses = HubDeServicosApplication.class)
    static class ConfiguracaoSemBanco {

        @Bean
        DataSource dataSource() {
            return new AbstractDataSource() {
                @Override
                public Connection getConnection() {
                    throw new AssertionError("O teste de mapeamento não deve abrir conexão.");
                }

                @Override
                public Connection getConnection(String usuario, String senha) {
                    return getConnection();
                }
            };
        }
    }
}
