package br.com.hubdeservicos.service.sms;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.provider.SmsProvider;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class SmsProviderResolver {

    private final Map<String, SmsProvider> provedores;

    public SmsProviderResolver(List<SmsProvider> implementacoes) {
        provedores = implementacoes.stream()
                .collect(Collectors.toUnmodifiableMap(SmsProvider::getProviderCode, Function.identity()));
    }

    public Optional<SmsProvider> localizar(String codigo) {
        return Optional.ofNullable(provedores.get(codigo));
    }

    public List<ProvedorSmsBean> listar() {
        return provedores.values().stream().map(provedor -> {
            ProvedorSmsBean provedorSmsPreparado = new ProvedorSmsBean();
            provedorSmsPreparado.setCodigo(provedor.getProviderCode());
            provedorSmsPreparado.setNome(provedor.getDisplayName());
            return provedorSmsPreparado;
        }).sorted(Comparator.comparing(ProvedorSmsBean::getCodigo)).toList();
    }

    @Schema(name = "ProvedorSms")
    public static final class ProvedorSmsBean {
        private String codigo;

        private String nome;

        public ProvedorSmsBean() {
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ProvedorSmsBean outro)) {
                return false;
            }
            return Objects.equals(codigo, outro.codigo) && Objects.equals(nome, outro.nome);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(nome);
            return resultado;
        }

        @Override
        public String toString() {
            return "ProvedorSms[" + "codigo=" + codigo + ", nome=" + nome + "]";
        }
    }
}
