package br.com.hubdeservicos.sms.service;

import br.com.hubdeservicos.sms.provider.SmsProvider;

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

    public List<ProvedorSms> listar() {
        return provedores.values().stream()
                .map(provedor -> new ProvedorSms(provedor.getProviderCode(), provedor.getDisplayName()))
                .sorted(Comparator.comparing(ProvedorSms::codigo))
                .toList();
    }

    public record ProvedorSms(String codigo, String nome) {
    }
}
