package br.com.hubdeservicos.dto.configuracao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

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

    @Schema(name = "ConfiguracaoInfo")
    public static final class ConfiguracaoInfoBean {
        private String chave;

        private String valor;

        private String descricao;

        private String tipo;

        private StatusCadastro status;

        private Instant dataCriacao;

        private Instant dataAtualizacao;

        public ConfiguracaoInfoBean() {
        }

        public String getChave() {
            return chave;
        }

        public void setChave(String chave) {
            this.chave = chave;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        public Instant getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(Instant dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        public Instant getDataAtualizacao() {
            return dataAtualizacao;
        }

        public void setDataAtualizacao(Instant dataAtualizacao) {
            this.dataAtualizacao = dataAtualizacao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ConfiguracaoInfoBean outro)) {
                return false;
            }
            return Objects.equals(chave, outro.chave) && Objects.equals(valor, outro.valor)
                    && Objects.equals(descricao, outro.descricao) && Objects.equals(tipo, outro.tipo)
                    && Objects.equals(status, outro.status) && Objects.equals(dataCriacao, outro.dataCriacao)
                    && Objects.equals(dataAtualizacao, outro.dataAtualizacao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(chave);
            resultado = 31 * resultado + Objects.hashCode(valor);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(dataCriacao);
            resultado = 31 * resultado + Objects.hashCode(dataAtualizacao);
            return resultado;
        }

        @Override
        public String toString() {
            return "ConfiguracaoInfo[" + "chave=" + chave + ", valor=" + valor + ", descricao=" + descricao + ", tipo="
                    + tipo + ", status=" + status + ", dataCriacao=" + dataCriacao + ", dataAtualizacao="
                    + dataAtualizacao + "]";
        }

        public static ConfiguracaoInfoBean de(ConfiguracaoSistemaEntity configuracao) {
            ConfiguracaoInfoBean configuracaoInfoPreparado = new ConfiguracaoInfoBean();
            configuracaoInfoPreparado.setChave(configuracao.getChave());
            configuracaoInfoPreparado.setValor(configuracao.getValor());
            configuracaoInfoPreparado.setDescricao(configuracao.getDescricao());
            configuracaoInfoPreparado.setTipo(configuracao.getTipo());
            configuracaoInfoPreparado.setStatus(configuracao.getStatus());
            configuracaoInfoPreparado.setDataCriacao(configuracao.getDataCriacao());
            configuracaoInfoPreparado.setDataAtualizacao(configuracao.getDataAtualizacao());
            return configuracaoInfoPreparado;
        }
    }

    @Schema(name = "ModoDesenvolvimento")
    public static final class ModoDesenvolvimentoBean {
        @NotNull
        private Boolean devMode;

        public ModoDesenvolvimentoBean() {
        }

        public Boolean getDevMode() {
            return devMode;
        }

        public void setDevMode(Boolean devMode) {
            this.devMode = devMode;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ModoDesenvolvimentoBean outro)) {
                return false;
            }
            return Objects.equals(devMode, outro.devMode);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(devMode);
            return resultado;
        }

        @Override
        public String toString() {
            return "ModoDesenvolvimento[" + "devMode=" + devMode + "]";
        }
    }

    @Schema(name = "AlterarProvedorSms")
    public static final class AlterarProvedorSmsBean {
        @NotBlank
        @Size(max = 100)
        private String codigo;

        public AlterarProvedorSmsBean() {
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AlterarProvedorSmsBean outro)) {
                return false;
            }
            return Objects.equals(codigo, outro.codigo);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigo);
            return resultado;
        }

        @Override
        public String toString() {
            return "AlterarProvedorSms[" + "codigo=" + codigo + "]";
        }
    }

    @Schema(name = "ServicoInfo")
    public static final class ServicoInfoBean {
        private ServicoExterno servico;

        private List<ConfiguracaoExternaInfoBean> configuracoes;

        public ServicoInfoBean() {
        }

        public ServicoExterno getServico() {
            return servico;
        }

        public void setServico(ServicoExterno servico) {
            this.servico = servico;
        }

        public List<ConfiguracaoExternaInfoBean> getConfiguracoes() {
            return configuracoes;
        }

        public void setConfiguracoes(List<ConfiguracaoExternaInfoBean> configuracoes) {
            this.configuracoes = configuracoes;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ServicoInfoBean outro)) {
                return false;
            }
            return Objects.equals(servico, outro.servico) && Objects.equals(configuracoes, outro.configuracoes);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(servico);
            resultado = 31 * resultado + Objects.hashCode(configuracoes);
            return resultado;
        }

        @Override
        public String toString() {
            return "ServicoInfo[" + "servico=" + servico + ", configuracoes=" + configuracoes + "]";
        }
    }

    @Schema(name = "ConfiguracaoExternaInfo")
    public static final class ConfiguracaoExternaInfoBean {
        private String chave;

        private boolean configurada;

        private boolean sensivel;

        private StatusCadastro status;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private String valor;

        public ConfiguracaoExternaInfoBean() {
        }

        public String getChave() {
            return chave;
        }

        public void setChave(String chave) {
            this.chave = chave;
        }

        public boolean getConfigurada() {
            return configurada;
        }

        public void setConfigurada(boolean configurada) {
            this.configurada = configurada;
        }

        public boolean getSensivel() {
            return sensivel;
        }

        public void setSensivel(boolean sensivel) {
            this.sensivel = sensivel;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ConfiguracaoExternaInfoBean outro)) {
                return false;
            }
            return Objects.equals(chave, outro.chave) && Objects.equals(configurada, outro.configurada)
                    && Objects.equals(sensivel, outro.sensivel) && Objects.equals(status, outro.status)
                    && Objects.equals(valor, outro.valor);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(chave);
            resultado = 31 * resultado + Objects.hashCode(configurada);
            resultado = 31 * resultado + Objects.hashCode(sensivel);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(valor);
            return resultado;
        }

        @Override
        public String toString() {
            return "ConfiguracaoExternaInfo[chave=" + chave + ", configurada=" + configurada + ", sensivel=" + sensivel
                    + ", status=" + status + "]";
        }
    }

    @Schema(name = "AtualizarServico")
    public static final class AtualizarServicoBean {
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @NotNull
        @Size(max = 10)
        private Map<String, String> valores;

        public AtualizarServicoBean() {
        }

        public Map<@NotBlank @Size(max = 100) String, @Size(max = 2048) String> getValores() {
            return valores;
        }

        public void setValores(Map<String, String> valores) {
            this.valores = valores;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarServicoBean outro)) {
                return false;
            }
            return Objects.equals(valores, outro.valores);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(valores);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarServico";
        }
    }
}
