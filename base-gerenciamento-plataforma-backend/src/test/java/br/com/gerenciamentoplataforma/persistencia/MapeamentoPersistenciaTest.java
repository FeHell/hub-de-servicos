package br.com.gerenciamentoplataforma.persistencia;

import br.com.gerenciamentoplataforma.BaseGerenciamentoPlataformaApplication;
import br.com.gerenciamentoplataforma.aplicacao.entity.AplicacaoPermissaoEntity;
import br.com.gerenciamentoplataforma.aplicacao.entity.AplicacaoPermissaoId;
import br.com.gerenciamentoplataforma.perfil.entity.PerfilPermissaoEntity;
import br.com.gerenciamentoplataforma.perfil.entity.PerfilPermissaoId;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoId;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPerfilEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPerfilId;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaId;

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
                    assertThat(metamodelo.getEntities()).hasSize(12);
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
    @AutoConfigurationPackage(basePackageClasses = BaseGerenciamentoPlataformaApplication.class)
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
