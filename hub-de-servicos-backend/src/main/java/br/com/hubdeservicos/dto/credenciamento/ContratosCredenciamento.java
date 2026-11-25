package br.com.hubdeservicos.dto.credenciamento;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.fasterxml.jackson.annotation.JsonCreator;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

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
    private ContratosCredenciamento() {
    }

    public enum Modalidade {
        INDIVIDUAL, CORPORATION
    }

    public enum TipoConta {
        CORRENTE, POUPANCA
    }

    public enum TipoTitular {
        INDIVIDUAL, COMPANY
    }

    public enum TipoTelefone {
        CELULAR, FIXO
    }

    @Schema(name = "Dados")
    public static final class DadosBean {
        private Modalidade modalidade;

        @Valid
        private PessoaFisicaBean pessoaFisica;

        @Valid
        private EmpresaBean empresa;

        @Valid
        private RepresentanteBean representante;

        @Valid
        private ContaBancariaBean contaBancaria;

        public DadosBean() {
        }

        public Modalidade getModalidade() {
            return modalidade;
        }

        public void setModalidade(Modalidade modalidade) {
            this.modalidade = modalidade;
        }

        public PessoaFisicaBean getPessoaFisica() {
            return pessoaFisica;
        }

        public void setPessoaFisica(PessoaFisicaBean pessoaFisica) {
            this.pessoaFisica = pessoaFisica;
        }

        public EmpresaBean getEmpresa() {
            return empresa;
        }

        public void setEmpresa(EmpresaBean empresa) {
            this.empresa = empresa;
        }

        public RepresentanteBean getRepresentante() {
            return representante;
        }

        public void setRepresentante(RepresentanteBean representante) {
            this.representante = representante;
        }

        public ContaBancariaBean getContaBancaria() {
            return contaBancaria;
        }

        public void setContaBancaria(ContaBancariaBean contaBancaria) {
            this.contaBancaria = contaBancaria;
        }

        @JsonCreator
        public DadosBean(@JsonProperty("modalidade") Modalidade modalidade,
                @JsonProperty("pessoaFisica") PessoaFisicaBean pessoaFisica,
                @JsonProperty("empresa") EmpresaBean empresa,
                @JsonProperty("representante") RepresentanteBean representante,
                @JsonProperty("contaBancaria") ContaBancariaBean contaBancaria) {
            this.modalidade = modalidade;
            this.pessoaFisica = pessoaFisica;
            this.empresa = empresa;
            this.representante = representante;
            this.contaBancaria = contaBancaria;
            normalizarLegado();
        }

        public void normalizarLegado() {
            // A modalidade é inferida somente após receber todos os blocos do cadastro
            // legado.
            if (modalidade == null && pessoaFisica == null && (empresa != null || representante != null)) {
                modalidade = Modalidade.CORPORATION;
            }
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof DadosBean outro)) {
                return false;
            }
            return Objects.equals(modalidade, outro.modalidade) && Objects.equals(pessoaFisica, outro.pessoaFisica)
                    && Objects.equals(empresa, outro.empresa) && Objects.equals(representante, outro.representante)
                    && Objects.equals(contaBancaria, outro.contaBancaria);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(modalidade);
            resultado = 31 * resultado + Objects.hashCode(pessoaFisica);
            resultado = 31 * resultado + Objects.hashCode(empresa);
            resultado = 31 * resultado + Objects.hashCode(representante);
            resultado = 31 * resultado + Objects.hashCode(contaBancaria);
            return resultado;
        }

        @Override
        public String toString() {
            return "DadosCredenciamento";
        }
    }

    @Schema(name = "PessoaFisica")
    @JsonIgnoreProperties({ "nomeMae", "mother_name" })
    public static final class PessoaFisicaBean {
        @Size(max = 160)
        private String nome;

        @Size(max = 16)
        private String cpf;

        @Size(max = 254)
        private String email;

        @Size(max = 24)
        private String telefone;

        private TipoTelefone tipoTelefone;

        @Size(max = 2048)
        private String siteUrl;

        private LocalDate nascimento;

        @Size(max = 100)
        private String profissao;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal rendaMensal;

        @Valid
        private EnderecoBean endereco;

        public PessoaFisicaBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getCpf() {
            return cpf;
        }

        public void setCpf(String cpf) {
            this.cpf = cpf;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public TipoTelefone getTipoTelefone() {
            return tipoTelefone;
        }

        public void setTipoTelefone(TipoTelefone tipoTelefone) {
            this.tipoTelefone = tipoTelefone;
        }

        public String getSiteUrl() {
            return siteUrl;
        }

        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        public LocalDate getNascimento() {
            return nascimento;
        }

        public void setNascimento(LocalDate nascimento) {
            this.nascimento = nascimento;
        }

        public String getProfissao() {
            return profissao;
        }

        public void setProfissao(String profissao) {
            this.profissao = profissao;
        }

        public BigDecimal getRendaMensal() {
            return rendaMensal;
        }

        public void setRendaMensal(BigDecimal rendaMensal) {
            this.rendaMensal = rendaMensal;
        }

        public EnderecoBean getEndereco() {
            return endereco;
        }

        public void setEndereco(EnderecoBean endereco) {
            this.endereco = endereco;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PessoaFisicaBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(cpf, outro.cpf)
                    && Objects.equals(email, outro.email) && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(tipoTelefone, outro.tipoTelefone) && Objects.equals(siteUrl, outro.siteUrl)
                    && Objects.equals(nascimento, outro.nascimento) && Objects.equals(profissao, outro.profissao)
                    && Objects.equals(rendaMensal, outro.rendaMensal) && Objects.equals(endereco, outro.endereco);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(cpf);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(tipoTelefone);
            resultado = 31 * resultado + Objects.hashCode(siteUrl);
            resultado = 31 * resultado + Objects.hashCode(nascimento);
            resultado = 31 * resultado + Objects.hashCode(profissao);
            resultado = 31 * resultado + Objects.hashCode(rendaMensal);
            resultado = 31 * resultado + Objects.hashCode(endereco);
            return resultado;
        }

        @Override
        public String toString() {
            return "PessoaFisica";
        }
    }

    @Schema(name = "Empresa")
    public static final class EmpresaBean {
        @Size(max = 160)
        private String razaoSocial;

        @Size(max = 160)
        private String nomeFantasia;

        @Size(max = 20)
        private String cnpj;

        @Size(max = 254)
        private String email;

        @Size(max = 24)
        private String telefone;

        private TipoTelefone tipoTelefone;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal faturamento;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal faturamentoAnual;

        @Size(max = 2048)
        private String siteUrl;

        @Size(max = 100)
        private String tipoSocietario;

        private LocalDate dataFundacao;

        @Valid
        private EnderecoBean endereco;

        public EmpresaBean() {
        }

        public String getRazaoSocial() {
            return razaoSocial;
        }

        public void setRazaoSocial(String razaoSocial) {
            this.razaoSocial = razaoSocial;
        }

        public String getNomeFantasia() {
            return nomeFantasia;
        }

        public void setNomeFantasia(String nomeFantasia) {
            this.nomeFantasia = nomeFantasia;
        }

        public String getCnpj() {
            return cnpj;
        }

        public void setCnpj(String cnpj) {
            this.cnpj = cnpj;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public TipoTelefone getTipoTelefone() {
            return tipoTelefone;
        }

        public void setTipoTelefone(TipoTelefone tipoTelefone) {
            this.tipoTelefone = tipoTelefone;
        }

        public BigDecimal getFaturamento() {
            return faturamento;
        }

        public void setFaturamento(BigDecimal faturamento) {
            this.faturamento = faturamento;
        }

        public BigDecimal getFaturamentoAnual() {
            return faturamentoAnual;
        }

        public void setFaturamentoAnual(BigDecimal faturamentoAnual) {
            this.faturamentoAnual = faturamentoAnual;
        }

        public String getSiteUrl() {
            return siteUrl;
        }

        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        public String getTipoSocietario() {
            return tipoSocietario;
        }

        public void setTipoSocietario(String tipoSocietario) {
            this.tipoSocietario = tipoSocietario;
        }

        public LocalDate getDataFundacao() {
            return dataFundacao;
        }

        public void setDataFundacao(LocalDate dataFundacao) {
            this.dataFundacao = dataFundacao;
        }

        public EnderecoBean getEndereco() {
            return endereco;
        }

        public void setEndereco(EnderecoBean endereco) {
            this.endereco = endereco;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EmpresaBean outro)) {
                return false;
            }
            return Objects.equals(razaoSocial, outro.razaoSocial) && Objects.equals(nomeFantasia, outro.nomeFantasia)
                    && Objects.equals(cnpj, outro.cnpj) && Objects.equals(email, outro.email)
                    && Objects.equals(telefone, outro.telefone) && Objects.equals(tipoTelefone, outro.tipoTelefone)
                    && Objects.equals(faturamento, outro.faturamento)
                    && Objects.equals(faturamentoAnual, outro.faturamentoAnual)
                    && Objects.equals(siteUrl, outro.siteUrl) && Objects.equals(tipoSocietario, outro.tipoSocietario)
                    && Objects.equals(dataFundacao, outro.dataFundacao) && Objects.equals(endereco, outro.endereco);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(razaoSocial);
            resultado = 31 * resultado + Objects.hashCode(nomeFantasia);
            resultado = 31 * resultado + Objects.hashCode(cnpj);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(tipoTelefone);
            resultado = 31 * resultado + Objects.hashCode(faturamento);
            resultado = 31 * resultado + Objects.hashCode(faturamentoAnual);
            resultado = 31 * resultado + Objects.hashCode(siteUrl);
            resultado = 31 * resultado + Objects.hashCode(tipoSocietario);
            resultado = 31 * resultado + Objects.hashCode(dataFundacao);
            resultado = 31 * resultado + Objects.hashCode(endereco);
            return resultado;
        }

        @Override
        public String toString() {
            return "Empresa";
        }
    }

    @Schema(name = "Representante")
    @JsonIgnoreProperties({ "nomeMae", "mother_name" })
    public static final class RepresentanteBean {
        @Size(max = 160)
        private String nome;

        @Size(max = 16)
        private String cpf;

        @Size(max = 254)
        private String email;

        @Size(max = 24)
        private String telefone;

        private TipoTelefone tipoTelefone;

        private LocalDate nascimento;

        @Size(max = 100)
        private String profissao;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal renda;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        private BigDecimal rendaMensal;

        private Boolean representanteLegal;

        private Boolean socioQsa;

        @Valid
        private EnderecoBean endereco;

        public RepresentanteBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getCpf() {
            return cpf;
        }

        public void setCpf(String cpf) {
            this.cpf = cpf;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public TipoTelefone getTipoTelefone() {
            return tipoTelefone;
        }

        public void setTipoTelefone(TipoTelefone tipoTelefone) {
            this.tipoTelefone = tipoTelefone;
        }

        public LocalDate getNascimento() {
            return nascimento;
        }

        public void setNascimento(LocalDate nascimento) {
            this.nascimento = nascimento;
        }

        public String getProfissao() {
            return profissao;
        }

        public void setProfissao(String profissao) {
            this.profissao = profissao;
        }

        public BigDecimal getRenda() {
            return renda;
        }

        public void setRenda(BigDecimal renda) {
            this.renda = renda;
        }

        public BigDecimal getRendaMensal() {
            return rendaMensal;
        }

        public void setRendaMensal(BigDecimal rendaMensal) {
            this.rendaMensal = rendaMensal;
        }

        public Boolean getRepresentanteLegal() {
            return representanteLegal;
        }

        public void setRepresentanteLegal(Boolean representanteLegal) {
            this.representanteLegal = representanteLegal;
        }

        public Boolean getSocioQsa() {
            return socioQsa;
        }

        public void setSocioQsa(Boolean socioQsa) {
            this.socioQsa = socioQsa;
        }

        public EnderecoBean getEndereco() {
            return endereco;
        }

        public void setEndereco(EnderecoBean endereco) {
            this.endereco = endereco;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof RepresentanteBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(cpf, outro.cpf)
                    && Objects.equals(email, outro.email) && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(tipoTelefone, outro.tipoTelefone) && Objects.equals(nascimento, outro.nascimento)
                    && Objects.equals(profissao, outro.profissao) && Objects.equals(renda, outro.renda)
                    && Objects.equals(rendaMensal, outro.rendaMensal)
                    && Objects.equals(representanteLegal, outro.representanteLegal)
                    && Objects.equals(socioQsa, outro.socioQsa) && Objects.equals(endereco, outro.endereco);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(cpf);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(tipoTelefone);
            resultado = 31 * resultado + Objects.hashCode(nascimento);
            resultado = 31 * resultado + Objects.hashCode(profissao);
            resultado = 31 * resultado + Objects.hashCode(renda);
            resultado = 31 * resultado + Objects.hashCode(rendaMensal);
            resultado = 31 * resultado + Objects.hashCode(representanteLegal);
            resultado = 31 * resultado + Objects.hashCode(socioQsa);
            resultado = 31 * resultado + Objects.hashCode(endereco);
            return resultado;
        }

        @Override
        public String toString() {
            return "Representante";
        }
    }

    @Schema(name = "Endereco")
    public static final class EnderecoBean {
        @Size(max = 9)
        private String cep;

        @Size(max = 160)
        private String logradouro;

        @Size(max = 20)
        private String numero;

        @Size(max = 100)
        private String complemento;

        @Size(max = 100)
        private String bairro;

        @Size(max = 120)
        private String cidade;

        @Size(max = 2)
        private String uf;

        @Size(max = 160)
        private String pontoReferencia;

        public EnderecoBean() {
        }

        public String getCep() {
            return cep;
        }

        public void setCep(String cep) {
            this.cep = cep;
        }

        public String getLogradouro() {
            return logradouro;
        }

        public void setLogradouro(String logradouro) {
            this.logradouro = logradouro;
        }

        public String getNumero() {
            return numero;
        }

        public void setNumero(String numero) {
            this.numero = numero;
        }

        public String getComplemento() {
            return complemento;
        }

        public void setComplemento(String complemento) {
            this.complemento = complemento;
        }

        public String getBairro() {
            return bairro;
        }

        public void setBairro(String bairro) {
            this.bairro = bairro;
        }

        public String getCidade() {
            return cidade;
        }

        public void setCidade(String cidade) {
            this.cidade = cidade;
        }

        public String getUf() {
            return uf;
        }

        public void setUf(String uf) {
            this.uf = uf;
        }

        public String getPontoReferencia() {
            return pontoReferencia;
        }

        public void setPontoReferencia(String pontoReferencia) {
            this.pontoReferencia = pontoReferencia;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EnderecoBean outro)) {
                return false;
            }
            return Objects.equals(cep, outro.cep) && Objects.equals(logradouro, outro.logradouro)
                    && Objects.equals(numero, outro.numero) && Objects.equals(complemento, outro.complemento)
                    && Objects.equals(bairro, outro.bairro) && Objects.equals(cidade, outro.cidade)
                    && Objects.equals(uf, outro.uf) && Objects.equals(pontoReferencia, outro.pontoReferencia);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(cep);
            resultado = 31 * resultado + Objects.hashCode(logradouro);
            resultado = 31 * resultado + Objects.hashCode(numero);
            resultado = 31 * resultado + Objects.hashCode(complemento);
            resultado = 31 * resultado + Objects.hashCode(bairro);
            resultado = 31 * resultado + Objects.hashCode(cidade);
            resultado = 31 * resultado + Objects.hashCode(uf);
            resultado = 31 * resultado + Objects.hashCode(pontoReferencia);
            return resultado;
        }

        @Override
        public String toString() {
            return "Endereco";
        }
    }

    @Schema(name = "ContaBancaria")
    public static final class ContaBancariaBean {
        @Size(max = 160)
        private String titular;

        @Size(max = 20)
        private String documento;

        private TipoTitular tipoTitular;

        private TipoConta tipo;

        @Size(max = 3)
        private String banco;

        @Size(max = 10)
        private String agencia;

        @Size(max = 2)
        private String digitoAgencia;

        @Size(max = 20)
        private String conta;

        @Size(max = 2)
        private String digitoConta;

        public ContaBancariaBean() {
        }

        public String getTitular() {
            return titular;
        }

        public void setTitular(String titular) {
            this.titular = titular;
        }

        public String getDocumento() {
            return documento;
        }

        public void setDocumento(String documento) {
            this.documento = documento;
        }

        public TipoTitular getTipoTitular() {
            return tipoTitular;
        }

        public void setTipoTitular(TipoTitular tipoTitular) {
            this.tipoTitular = tipoTitular;
        }

        public TipoConta getTipo() {
            return tipo;
        }

        public void setTipo(TipoConta tipo) {
            this.tipo = tipo;
        }

        public String getBanco() {
            return banco;
        }

        public void setBanco(String banco) {
            this.banco = banco;
        }

        public String getAgencia() {
            return agencia;
        }

        public void setAgencia(String agencia) {
            this.agencia = agencia;
        }

        public String getDigitoAgencia() {
            return digitoAgencia;
        }

        public void setDigitoAgencia(String digitoAgencia) {
            this.digitoAgencia = digitoAgencia;
        }

        public String getConta() {
            return conta;
        }

        public void setConta(String conta) {
            this.conta = conta;
        }

        public String getDigitoConta() {
            return digitoConta;
        }

        public void setDigitoConta(String digitoConta) {
            this.digitoConta = digitoConta;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ContaBancariaBean outro)) {
                return false;
            }
            return Objects.equals(titular, outro.titular) && Objects.equals(documento, outro.documento)
                    && Objects.equals(tipoTitular, outro.tipoTitular) && Objects.equals(tipo, outro.tipo)
                    && Objects.equals(banco, outro.banco) && Objects.equals(agencia, outro.agencia)
                    && Objects.equals(digitoAgencia, outro.digitoAgencia) && Objects.equals(conta, outro.conta)
                    && Objects.equals(digitoConta, outro.digitoConta);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(titular);
            resultado = 31 * resultado + Objects.hashCode(documento);
            resultado = 31 * resultado + Objects.hashCode(tipoTitular);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(banco);
            resultado = 31 * resultado + Objects.hashCode(agencia);
            resultado = 31 * resultado + Objects.hashCode(digitoAgencia);
            resultado = 31 * resultado + Objects.hashCode(conta);
            resultado = 31 * resultado + Objects.hashCode(digitoConta);
            return resultado;
        }

        @Override
        public String toString() {
            return "ContaBancaria";
        }
    }

    @Schema(name = "Pendencia")
    public static final class PendenciaBean {
        private String campo;

        private String mensagem;

        public PendenciaBean() {
        }

        public String getCampo() {
            return campo;
        }

        public void setCampo(String campo) {
            this.campo = campo;
        }

        public String getMensagem() {
            return mensagem;
        }

        public void setMensagem(String mensagem) {
            this.mensagem = mensagem;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PendenciaBean outro)) {
                return false;
            }
            return Objects.equals(campo, outro.campo) && Objects.equals(mensagem, outro.mensagem);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(campo);
            resultado = 31 * resultado + Objects.hashCode(mensagem);
            return resultado;
        }

        @Override
        public String toString() {
            return "Pendencia[" + "campo=" + campo + ", mensagem=" + mensagem + "]";
        }
    }

    @Schema(name = "Completude")
    public static final class CompletudeBean {
        private boolean apto;

        private List<PendenciaBean> faltantes;

        private List<PendenciaBean> invalidos;

        private List<PendenciaBean> confirmacoes;

        public CompletudeBean() {
        }

        public boolean getApto() {
            return apto;
        }

        public void setApto(boolean apto) {
            this.apto = apto;
        }

        public List<PendenciaBean> getFaltantes() {
            return faltantes;
        }

        public void setFaltantes(List<PendenciaBean> faltantes) {
            this.faltantes = faltantes;
        }

        public List<PendenciaBean> getInvalidos() {
            return invalidos;
        }

        public void setInvalidos(List<PendenciaBean> invalidos) {
            this.invalidos = invalidos;
        }

        public List<PendenciaBean> getConfirmacoes() {
            return confirmacoes;
        }

        public void setConfirmacoes(List<PendenciaBean> confirmacoes) {
            this.confirmacoes = confirmacoes;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof CompletudeBean outro)) {
                return false;
            }
            return Objects.equals(apto, outro.apto) && Objects.equals(faltantes, outro.faltantes)
                    && Objects.equals(invalidos, outro.invalidos) && Objects.equals(confirmacoes, outro.confirmacoes);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(apto);
            resultado = 31 * resultado + Objects.hashCode(faltantes);
            resultado = 31 * resultado + Objects.hashCode(invalidos);
            resultado = 31 * resultado + Objects.hashCode(confirmacoes);
            return resultado;
        }

        @Override
        public String toString() {
            return "Completude[" + "apto=" + apto + ", faltantes=" + faltantes + ", invalidos=" + invalidos
                    + ", confirmacoes=" + confirmacoes + "]";
        }
    }

    @Schema(name = "Credenciamento")
    public static final class CredenciamentoBean {
        private UUID id;

        private ProvedorCredenciamento provedor;

        private DadosBean dados;

        private Instant atualizadoEm;

        private CompletudeBean completude;

        private long versao;

        private RecebedorBean recebedor;

        public CredenciamentoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public ProvedorCredenciamento getProvedor() {
            return provedor;
        }

        public void setProvedor(ProvedorCredenciamento provedor) {
            this.provedor = provedor;
        }

        public DadosBean getDados() {
            return dados;
        }

        public void setDados(DadosBean dados) {
            this.dados = dados;
        }

        public Instant getAtualizadoEm() {
            return atualizadoEm;
        }

        public void setAtualizadoEm(Instant atualizadoEm) {
            this.atualizadoEm = atualizadoEm;
        }

        public CompletudeBean getCompletude() {
            return completude;
        }

        public void setCompletude(CompletudeBean completude) {
            this.completude = completude;
        }

        public long getVersao() {
            return versao;
        }

        public void setVersao(long versao) {
            this.versao = versao;
        }

        public RecebedorBean getRecebedor() {
            return recebedor;
        }

        public void setRecebedor(RecebedorBean recebedor) {
            this.recebedor = recebedor;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof CredenciamentoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(provedor, outro.provedor)
                    && Objects.equals(dados, outro.dados) && Objects.equals(atualizadoEm, outro.atualizadoEm)
                    && Objects.equals(completude, outro.completude) && Objects.equals(versao, outro.versao)
                    && Objects.equals(recebedor, outro.recebedor);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(provedor);
            resultado = 31 * resultado + Objects.hashCode(dados);
            resultado = 31 * resultado + Objects.hashCode(atualizadoEm);
            resultado = 31 * resultado + Objects.hashCode(completude);
            resultado = 31 * resultado + Objects.hashCode(versao);
            resultado = 31 * resultado + Objects.hashCode(recebedor);
            return resultado;
        }

        @Override
        public String toString() {
            return "Credenciamento[" + "id=" + id + ", provedor=" + provedor + ", dados=" + dados + ", atualizadoEm="
                    + atualizadoEm + ", completude=" + completude + ", versao=" + versao + ", recebedor=" + recebedor
                    + "]";
        }
    }

    public enum ConfirmacaoAlteracao {
        SUBMISSAO, APROVACAO
    }

    public enum SituacaoRecebedor {
        NAO_ENVIADO, EM_ANALISE, FALHA, INDETERMINADO, CRIADO, AGUARDANDO_NOVA_SUBMISSAO
    }

    public enum ErroRecebedor {
        DADOS_RECUSADOS, AUTENTICACAO, RESPOSTA_INDETERMINADA, ATUALIZACAO_RESTRITA
    }

    public enum StatusPagarme {
        REGISTRATION, AFFILIATION, ACTIVE, REFUSED, SUSPENDED, INACTIVE, BLOCKED
    }

    @Schema(name = "EnviarRecebedor")
    public static final class EnviarRecebedorBean {
        private Ambiente ambiente;

        private DadosBean dados;

        public EnviarRecebedorBean() {
        }

        public Ambiente getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(Ambiente ambiente) {
            this.ambiente = ambiente;
        }

        public DadosBean getDados() {
            return dados;
        }

        public void setDados(DadosBean dados) {
            this.dados = dados;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EnviarRecebedorBean outro)) {
                return false;
            }
            return Objects.equals(ambiente, outro.ambiente) && Objects.equals(dados, outro.dados);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(ambiente);
            resultado = 31 * resultado + Objects.hashCode(dados);
            return resultado;
        }

        @Override
        public String toString() {
            return "EnviarRecebedor[" + "ambiente=" + ambiente + ", dados=" + dados + "]";
        }
    }

    @Schema(name = "ReenviarRecebedor")
    public static final class ReenviarRecebedorBean {
        @NotNull
        private Ambiente ambiente;

        @NotNull
        private Long versao;

        @NotNull
        private UUID tentativa;

        public ReenviarRecebedorBean() {
        }

        public Ambiente getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(Ambiente ambiente) {
            this.ambiente = ambiente;
        }

        public Long getVersao() {
            return versao;
        }

        public void setVersao(Long versao) {
            this.versao = versao;
        }

        public UUID getTentativa() {
            return tentativa;
        }

        public void setTentativa(UUID tentativa) {
            this.tentativa = tentativa;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ReenviarRecebedorBean outro)) {
                return false;
            }
            return Objects.equals(ambiente, outro.ambiente) && Objects.equals(versao, outro.versao)
                    && Objects.equals(tentativa, outro.tentativa);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(ambiente);
            resultado = 31 * resultado + Objects.hashCode(versao);
            resultado = 31 * resultado + Objects.hashCode(tentativa);
            return resultado;
        }

        @Override
        public String toString() {
            return "ReenviarRecebedor[" + "ambiente=" + ambiente + ", versao=" + versao + ", tentativa=" + tentativa
                    + "]";
        }
    }

    @Schema(name = "Recebedor")
    public static final class RecebedorBean {
        private Ambiente ambiente;

        private SituacaoRecebedor situacao;

        private StatusPagarme statusProvedor;

        private ErroRecebedor erro;

        private List<PendenciaBean> pendencias;

        private Long versao;

        private boolean podeEnviar;

        private boolean podeConsultar;

        private String orientacao;

        private boolean podeReenviar;

        private boolean emProcessamento;

        private boolean envioAceito;

        private UUID tentativa;

        public RecebedorBean() {
        }

        public Ambiente getAmbiente() {
            return ambiente;
        }

        public void setAmbiente(Ambiente ambiente) {
            this.ambiente = ambiente;
        }

        public SituacaoRecebedor getSituacao() {
            return situacao;
        }

        public void setSituacao(SituacaoRecebedor situacao) {
            this.situacao = situacao;
        }

        public StatusPagarme getStatusProvedor() {
            return statusProvedor;
        }

        public void setStatusProvedor(StatusPagarme statusProvedor) {
            this.statusProvedor = statusProvedor;
        }

        public ErroRecebedor getErro() {
            return erro;
        }

        public void setErro(ErroRecebedor erro) {
            this.erro = erro;
        }

        public List<PendenciaBean> getPendencias() {
            return pendencias;
        }

        public void setPendencias(List<PendenciaBean> pendencias) {
            this.pendencias = pendencias;
        }

        public Long getVersao() {
            return versao;
        }

        public void setVersao(Long versao) {
            this.versao = versao;
        }

        public boolean getPodeEnviar() {
            return podeEnviar;
        }

        public void setPodeEnviar(boolean podeEnviar) {
            this.podeEnviar = podeEnviar;
        }

        public boolean getPodeConsultar() {
            return podeConsultar;
        }

        public void setPodeConsultar(boolean podeConsultar) {
            this.podeConsultar = podeConsultar;
        }

        public String getOrientacao() {
            return orientacao;
        }

        public void setOrientacao(String orientacao) {
            this.orientacao = orientacao;
        }

        public boolean getPodeReenviar() {
            return podeReenviar;
        }

        public void setPodeReenviar(boolean podeReenviar) {
            this.podeReenviar = podeReenviar;
        }

        public boolean getEmProcessamento() {
            return emProcessamento;
        }

        public void setEmProcessamento(boolean emProcessamento) {
            this.emProcessamento = emProcessamento;
        }

        public boolean getEnvioAceito() {
            return envioAceito;
        }

        public void setEnvioAceito(boolean envioAceito) {
            this.envioAceito = envioAceito;
        }

        public UUID getTentativa() {
            return tentativa;
        }

        public void setTentativa(UUID tentativa) {
            this.tentativa = tentativa;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof RecebedorBean outro)) {
                return false;
            }
            return Objects.equals(ambiente, outro.ambiente) && Objects.equals(situacao, outro.situacao)
                    && Objects.equals(statusProvedor, outro.statusProvedor) && Objects.equals(erro, outro.erro)
                    && Objects.equals(pendencias, outro.pendencias) && Objects.equals(versao, outro.versao)
                    && Objects.equals(podeEnviar, outro.podeEnviar)
                    && Objects.equals(podeConsultar, outro.podeConsultar)
                    && Objects.equals(orientacao, outro.orientacao) && Objects.equals(podeReenviar, outro.podeReenviar)
                    && Objects.equals(emProcessamento, outro.emProcessamento)
                    && Objects.equals(envioAceito, outro.envioAceito) && Objects.equals(tentativa, outro.tentativa);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(ambiente);
            resultado = 31 * resultado + Objects.hashCode(situacao);
            resultado = 31 * resultado + Objects.hashCode(statusProvedor);
            resultado = 31 * resultado + Objects.hashCode(erro);
            resultado = 31 * resultado + Objects.hashCode(pendencias);
            resultado = 31 * resultado + Objects.hashCode(versao);
            resultado = 31 * resultado + Objects.hashCode(podeEnviar);
            resultado = 31 * resultado + Objects.hashCode(podeConsultar);
            resultado = 31 * resultado + Objects.hashCode(orientacao);
            resultado = 31 * resultado + Objects.hashCode(podeReenviar);
            resultado = 31 * resultado + Objects.hashCode(emProcessamento);
            resultado = 31 * resultado + Objects.hashCode(envioAceito);
            resultado = 31 * resultado + Objects.hashCode(tentativa);
            return resultado;
        }

        @Override
        public String toString() {
            return "Recebedor[" + "ambiente=" + ambiente + ", situacao=" + situacao + ", statusProvedor="
                    + statusProvedor + ", erro=" + erro + ", pendencias=" + pendencias + ", versao=" + versao
                    + ", podeEnviar=" + podeEnviar + ", podeConsultar=" + podeConsultar + ", orientacao=" + orientacao
                    + ", podeReenviar=" + podeReenviar + ", emProcessamento=" + emProcessamento + ", envioAceito="
                    + envioAceito + ", tentativa=" + tentativa + "]";
        }
    }
}
