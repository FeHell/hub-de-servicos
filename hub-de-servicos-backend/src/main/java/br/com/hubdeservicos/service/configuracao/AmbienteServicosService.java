package br.com.hubdeservicos.service.configuracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AmbienteServicosService {
    public static final String DEV_MODE = "DEV_MODE";

    public enum Ambiente {
        DEV, PROD
    }

    private final ConfiguracaoSistemaRepository configuracoes;

    public AmbienteServicosService(ConfiguracaoSistemaRepository configuracoes) {
        this.configuracoes = configuracoes;
    }

    public Ambiente atual() {
        return interpretar(configuracoes.buscarValor(DEV_MODE, StatusCadastro.ATIVO).orElse(""));
    }

    public static Ambiente interpretar(String valor) {
        return switch (valor) {
        case "true" -> Ambiente.DEV;
        case "false" -> Ambiente.PROD;
        default ->
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Configure a seleção global DEV_MODE.");
        };
    }
}
