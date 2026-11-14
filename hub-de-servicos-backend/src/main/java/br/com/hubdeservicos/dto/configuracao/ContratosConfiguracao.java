package br.com.hubdeservicos.dto.configuracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity;

import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;

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

    public record ServicoInfo(ServicoExterno servico, List<ConfiguracaoExternaInfo> configuracoes) {
    }

    public record ConfiguracaoExternaInfo(String chave, boolean configurada, boolean sensivel,
                                         StatusCadastro status, @JsonInclude(JsonInclude.Include.NON_NULL) String valor) {
        @Override
        public String toString() {
            return "ConfiguracaoExternaInfo[chave=" + chave + ", configurada=" + configurada
                    + ", sensivel=" + sensivel + ", status=" + status + "]";
        }
    }

    public record AtualizarServico(@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
                                   @NotNull @Size(max = 10) Map<@NotBlank @Size(max = 100) String, @Size(max = 2048) String> valores) {
        @Override public String toString() { return "AtualizarServico"; }
    }
}
