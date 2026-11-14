package br.com.hubdeservicos.configuracao.dto;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.configuracao.entity.ConfiguracaoSistemaEntity;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class ContratosConfiguracao {

    private ContratosConfiguracao() {
    }

    public record ConfiguracaoInfo(String chave, String valor, String descricao, String tipo, StatusCadastro status,
                                   Instant dataCriacao, Instant dataAtualizacao) {
        public static ConfiguracaoInfo de(ConfiguracaoSistemaEntity configuracao) {
            return new ConfiguracaoInfo(configuracao.getChave(), configuracao.getValor(), configuracao.getDescricao(),
                    configuracao.getTipo(), configuracao.getStatus(), configuracao.getDataCriacao(),
                    configuracao.getDataAtualizacao());
        }
    }

    public record AlterarProvedorSms(@NotBlank @Size(max = 100) String codigo) {
    }
}
