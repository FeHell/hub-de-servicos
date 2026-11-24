package br.com.hubdeservicos.dto.credenciamento;

import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ContratosCredenciamento {
    private ContratosCredenciamento() {}

    public enum Modalidade { INDIVIDUAL, CORPORATION }
    public enum TipoConta { CORRENTE, POUPANCA }
    public enum TipoTitular { INDIVIDUAL, COMPANY }
    public enum TipoTelefone { CELULAR, FIXO }

    public record Dados(Modalidade modalidade, @Valid PessoaFisica pessoaFisica, @Valid Empresa empresa,
                        @Valid Representante representante, @Valid ContaBancaria contaBancaria) {
        // A estrutura legada contém somente os blocos empresa e representante.
        public Dados {
            if (modalidade == null && pessoaFisica == null && (empresa != null || representante != null)) {
                modalidade = Modalidade.CORPORATION;
            }
        }
        @Override public String toString() { return "DadosCredenciamento"; }
    }

    @JsonIgnoreProperties({"nomeMae", "mother_name"})
    public record PessoaFisica(@Size(max = 160) String nome, @Size(max = 16) String cpf,
                                @Size(max = 254) String email, @Size(max = 24) String telefone,
                                TipoTelefone tipoTelefone, @Size(max = 2048) String siteUrl, LocalDate nascimento,
                                @Size(max = 100) String profissao,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal rendaMensal,
                                @Valid Endereco endereco) {
        @Override public String toString() { return "PessoaFisica"; }
    }

    public record Empresa(@Size(max = 160) String razaoSocial, @Size(max = 160) String nomeFantasia,
                          @Size(max = 20) String cnpj, @Size(max = 254) String email,
                          @Size(max = 24) String telefone, TipoTelefone tipoTelefone,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal faturamento,
                          @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal faturamentoAnual,
                          @Size(max = 2048) String siteUrl, @Size(max = 100) String tipoSocietario,
                          LocalDate dataFundacao, @Valid Endereco endereco) {
        @Override public String toString() { return "Empresa"; }
    }

    @JsonIgnoreProperties({"nomeMae", "mother_name"})
    public record Representante(@Size(max = 160) String nome, @Size(max = 16) String cpf,
                                @Size(max = 254) String email, @Size(max = 24) String telefone,
                                TipoTelefone tipoTelefone, LocalDate nascimento,
                                @Size(max = 100) String profissao,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal renda,
                                @JsonFormat(shape = JsonFormat.Shape.STRING) BigDecimal rendaMensal,
                                Boolean representanteLegal, Boolean socioQsa, @Valid Endereco endereco) {
        @Override public String toString() { return "Representante"; }
    }

    public record Endereco(@Size(max = 9) String cep, @Size(max = 160) String logradouro,
                           @Size(max = 20) String numero, @Size(max = 100) String complemento,
                           @Size(max = 100) String bairro, @Size(max = 120) String cidade,
                           @Size(max = 2) String uf, @Size(max = 160) String pontoReferencia) {
        @Override public String toString() { return "Endereco"; }
    }

    public record ContaBancaria(@Size(max = 160) String titular, @Size(max = 20) String documento,
                                TipoTitular tipoTitular, TipoConta tipo, @Size(max = 3) String banco,
                                @Size(max = 10) String agencia, @Size(max = 2) String digitoAgencia,
                                @Size(max = 20) String conta, @Size(max = 2) String digitoConta) {
        @Override public String toString() { return "ContaBancaria"; }
    }

    public record Pendencia(String campo, String mensagem) {}

    public record Completude(boolean apto, List<Pendencia> faltantes, List<Pendencia> invalidos,
                             List<Pendencia> confirmacoes) {}

    public record Credenciamento(UUID id, ProvedorCredenciamento provedor, Dados dados, Instant atualizadoEm,
                                 Completude completude, long versao, Recebedor recebedor) {}

    public enum ConfirmacaoAlteracao { SUBMISSAO, APROVACAO }
    public enum SituacaoRecebedor { NAO_ENVIADO, EM_ANALISE, FALHA, INDETERMINADO, CRIADO, AGUARDANDO_NOVA_SUBMISSAO }
    public enum ErroRecebedor { DADOS_RECUSADOS, AUTENTICACAO, RESPOSTA_INDETERMINADA, ATUALIZACAO_RESTRITA }
    public enum StatusPagarme { REGISTRATION, AFFILIATION, ACTIVE, REFUSED, SUSPENDED, INACTIVE, BLOCKED }

    public record EnviarRecebedor(Ambiente ambiente, Dados dados) {}
    public record ReenviarRecebedor(@NotNull Ambiente ambiente, @NotNull Long versao, @NotNull UUID tentativa) {}
    public record Recebedor(Ambiente ambiente, SituacaoRecebedor situacao, StatusPagarme statusProvedor,
                             ErroRecebedor erro, List<Pendencia> pendencias, Long versao,
                             boolean podeEnviar, boolean podeConsultar, String orientacao,
                             boolean podeReenviar, boolean emProcessamento, boolean envioAceito, UUID tentativa) {}
}
