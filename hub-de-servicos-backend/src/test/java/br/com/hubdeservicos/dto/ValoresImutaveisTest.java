package br.com.hubdeservicos.dto;

import br.com.hubdeservicos.config.autenticacao.ConfiguracaoAutenticacao;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

class ValoresImutaveisTest {
    private static final List<String> TIPOS = List.of("br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO",
            "br.com.hubdeservicos.service.autenticacao.AutenticacaoService$LoginComIdentidadeVO",
            "br.com.hubdeservicos.service.app.LimiteTentativasApp$ChaveVO",
            "br.com.hubdeservicos.service.app.LimiteTentativasApp$TentativasVO",
            "br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService$StoneVO",
            "br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService$InfobipVO");

    @Test
    void excecoesAprovadasMantemGettersImutabilidadeEChavesPorValor() throws Exception {
        for (String nome : TIPOS) {
            Class<?> tipo = Class.forName(nome);
            assertThat(tipo.isRecord()).isTrue();
            assertThat(tipo.getDeclaredMethods()).noneMatch(m -> m.getName().startsWith("set"));
            for (var campo : tipo.getDeclaredFields()) {
                if (!Modifier.isStatic(campo.getModifiers())) {
                    assertThat(Modifier.isFinal(campo.getModifiers())).isTrue();
                }
            }
            var campos = tipo.getRecordComponents();
            Class<?>[] tipos = new Class<?>[campos.length];
            Object[] valores = new Object[campos.length];
            for (int i = 0; i < campos.length; i++) {
                tipos[i] = campos[i].getType();
                valores[i] = exemplo(tipos[i]);
                String sufixo = Character.toUpperCase(campos[i].getName().charAt(0)) + campos[i].getName().substring(1);
                assertThat(tipo.getDeclaredMethod("get" + sufixo)).isNotNull();
            }
            var construtor = tipo.getDeclaredConstructor(tipos);
            construtor.setAccessible(true);
            Object original = construtor.newInstance(valores);
            Object equivalente = construtor.newInstance(valores);
            HashSet<Object> chaves = new HashSet<>();
            chaves.add(original);
            assertThat(chaves).contains(equivalente);
        }
    }

    @Test
    void contextoPagarmePreservaConstrucaoPrivadaESemSetterDeCredencial() throws Exception {
        Class<?> tipo = Class.forName("br.com.hubdeservicos.service.credenciamento.AutenticacaoPagarme$ContaVO");
        assertThat(tipo.getDeclaredConstructors()).allMatch(c -> Modifier.isPrivate(c.getModifiers()));
        assertThat(tipo.getDeclaredMethods())
                .noneMatch(m -> m.getName().startsWith("set") || m.getName().equals("getSecretKey"));
    }

    @Test
    void configuracaoSpringMantemConstructorBindingECopiaImutavelDosClientes() {
        String id = "11111111-1111-4111-8111-111111111111";
        Map<String, String> propriedades = Map.of("hub.autenticacao.emissor", "https://exemplo.invalid",
                "hub.autenticacao.chave-privada", "/tmp/teste-privada.pem", "hub.autenticacao.chave-publica",
                "/tmp/teste-publica.pem", "hub.autenticacao.clientes-confidenciais." + id, "a".repeat(64));
        MapConfigurationPropertySource fonte = new MapConfigurationPropertySource(propriedades);
        Binder binder = new Binder(fonte);
        ConfiguracaoAutenticacao configuracao = binder
                .bind("hub.autenticacao", Bindable.of(ConfiguracaoAutenticacao.class)).get();

        assertThat(configuracao.emissor().toString()).isEqualTo("https://exemplo.invalid");
        assertThat(configuracao.chavePrivada().toString()).isEqualTo("/tmp/teste-privada.pem");
        assertThat(configuracao.chavePublica().toString()).isEqualTo("/tmp/teste-publica.pem");
        assertThat(configuracao.clientesConfidenciais()).containsEntry(UUID.fromString(id), "a".repeat(64));
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> configuracao.clientesConfidenciais().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static Object exemplo(Class<?> tipo) throws Exception {
        if (tipo == String.class) {
            return "exemplo-sintetico";
        }
        if (tipo == int.class) {
            return 1;
        }
        if (tipo == boolean.class) {
            return true;
        }
        if (tipo == UUID.class) {
            return UUID.fromString("11111111-1111-4111-8111-111111111111");
        }
        if (tipo == Instant.class) {
            return Instant.parse("2026-01-01T00:00:00Z");
        }
        if (tipo.isEnum()) {
            return tipo.getEnumConstants()[0];
        }
        Object valor = tipo.getDeclaredConstructor().newInstance();
        return valor;
    }
}
