package br.com.hubdeservicos.service.credenciamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Email;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ValidacaoCredenciamento {
    private static final String UF = "AC|AL|AP|AM|BA|CE|DF|ES|GO|MA|MT|MS|MG|PA|PB|PR|PE|PI|RJ|RN|RS|RO|RR|SC|SP|SE|TO";
    private final Clock relogio;
    private final Validator validator;

    public ValidacaoCredenciamento(Clock relogio, Validator validator) {
        this.relogio = relogio;
        this.validator = validator;
    }

    public CompletudeBean avaliar(DadosBean dados) {
        var resultado = new Resultado();
        validator.validate(dados).forEach(
                violacao -> resultado.invalido(violacao.getPropertyPath().toString(), "Confira o tamanho do campo."));
        String documento = null;
        if (dados.getModalidade() == Modalidade.INDIVIDUAL) {
            documento = pessoaFisica(dados.getPessoaFisica(), resultado);
            if (dados.getEmpresa() != null || dados.getRepresentante() != null) {
                resultado.invalido("modalidade", "Envie os dados da modalidade selecionada.");
            }
        } else {
            if (dados.getModalidade() == Modalidade.CORPORATION) {
                documento = empresa(dados.getEmpresa(), resultado);
                representante(dados.getRepresentante(), resultado);
                if (dados.getPessoaFisica() != null) {
                    resultado.invalido("modalidade", "Envie os dados da modalidade selecionada.");
                }
            } else {
                resultado.falta("modalidade", "Selecione Pessoa física ou Pessoa jurídica.");
            }
        }
        conta(dados.getContaBancaria(), documento, resultado);
        return resultado.completude();
    }

    private String pessoaFisica(PessoaFisicaBean pessoa, Resultado r) {
        String p = "pessoaFisica.";
        if (pessoa == null) {
            r.falta("pessoaFisica", "Preencha os dados da pessoa física.");
            return null;
        }
        r.texto(p + "nome", pessoa.getNome());
        cpf(p + "cpf", pessoa.getCpf(), r);
        contato(p, pessoa.getEmail(), pessoa.getTelefone(), pessoa.getTipoTelefone(), r);
        site(p + "siteUrl", pessoa.getSiteUrl(), r);
        nascimento(p + "nascimento", pessoa.getNascimento(), r);
        r.texto(p + "profissao", pessoa.getProfissao());
        dinheiro(p + "rendaMensal", pessoa.getRendaMensal(), r);
        endereco(p, pessoa.getEndereco(), r);
        return pessoa.getCpf();
    }

    private String empresa(EmpresaBean empresa, Resultado r) {
        String p = "empresa.";
        if (empresa == null) {
            r.falta("empresa", "Preencha os dados da empresa.");
            return null;
        }
        r.texto(p + "razaoSocial", empresa.getRazaoSocial());
        r.texto(p + "nomeFantasia", empresa.getNomeFantasia());
        if (r.texto(p + "cnpj", empresa.getCnpj())) {
            if (!DocumentosCredenciamento.cnpjValido(empresa.getCnpj())) {
                r.invalido(p + "cnpj", "Confira o CNPJ.");
            } else {
                if (documento(empresa.getCnpj()).matches(".*[A-Z].*")) {
                    r.confirmar(p + "cnpj", "Confirme com a Pagar.me a aceitação do CNPJ alfanumérico.");
                }
            }
        }
        contato(p, empresa.getEmail(), empresa.getTelefone(), empresa.getTipoTelefone(), r);
        site(p + "siteUrl", empresa.getSiteUrl(), r);
        if (empresa.getFaturamentoAnual() == null && empresa.getFaturamento() != null) {
            r.confirmar(p + "faturamentoAnual",
                    "Informe o faturamento anual. O valor anterior não tem período definido.");
        } else {
            dinheiro(p + "faturamentoAnual", empresa.getFaturamentoAnual(), r);
        }
        if (empresa.getDataFundacao() != null && (!anoValido(empresa.getDataFundacao())
                || empresa.getDataFundacao().isAfter(LocalDate.now(relogio)))) {
            r.invalido(p + "dataFundacao", "Confira a data de fundação.");
        }
        endereco(p, empresa.getEndereco(), r);
        return empresa.getCnpj();
    }

    private void representante(RepresentanteBean pessoa, Resultado r) {
        String p = "representante.";
        if (pessoa == null) {
            r.falta("representante", "Preencha os dados do representante legal.");
            return;
        }
        r.texto(p + "nome", pessoa.getNome());
        cpf(p + "cpf", pessoa.getCpf(), r);
        contato(p, pessoa.getEmail(), pessoa.getTelefone(), pessoa.getTipoTelefone(), r);
        nascimento(p + "nascimento", pessoa.getNascimento(), r);
        r.texto(p + "profissao", pessoa.getProfissao());
        if (pessoa.getRendaMensal() == null && pessoa.getRenda() != null) {
            r.confirmar(p + "rendaMensal", "Informe a renda mensal. O valor anterior não tem período definido.");
        } else {
            dinheiro(p + "rendaMensal", pessoa.getRendaMensal(), r);
        }
        if (!Boolean.TRUE.equals(pessoa.getRepresentanteLegal())) {
            r.confirmar(p + "representanteLegal", "Confirme a declaração de representação legal.");
        }
        if (!Boolean.TRUE.equals(pessoa.getSocioQsa())) {
            r.confirmar(p + "socioQsa", "Confirme que o representante é sócio qualificado no QSA.");
        }
        endereco(p, pessoa.getEndereco(), r);
    }

    private void contato(String p, String email, String telefone, TipoTelefone tipo, Resultado r) {
        boolean emailPreenchido = r.texto(p + "email", email);
        if (emailPreenchido) {
            EmailCadastralBean emailCadastral = new EmailCadastralBean();
            emailCadastral.setEmail(email.strip());

            boolean emailInvalido = !validator.validate(emailCadastral).isEmpty();
            if (emailInvalido) {
                r.invalido(p + "email", "Confira o e-mail.");
            }
        }
        if (tipo == null) {
            r.falta(p + "tipoTelefone", "Selecione o tipo de telefone.");
        }
        if (!r.texto(p + "telefone", telefone)) {
            return;
        }
        try {
            boolean celular = TelefoneUsuarioService.normalizar(telefone).length() == 14;
            if (tipo != null && celular != (tipo == TipoTelefone.CELULAR)) {
                r.invalido(p + "telefone", "O telefone não corresponde ao tipo escolhido.");
            } else {
                if (tipo == TipoTelefone.FIXO) {
                    r.confirmar(p + "tipoTelefone", "Confirme com a Pagar.me o código para telefone fixo.");
                }
            }
        } catch (ResponseStatusException erro) {
            r.invalido(p + "telefone", "Informe um telefone brasileiro válido com DDD.");
        }
    }

    @Schema(name = "EmailCadastral")
    private static final class EmailCadastralBean {
        @Email
        private String email;

        public EmailCadastralBean() {
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EmailCadastralBean outro)) {
                return false;
            }
            return Objects.equals(email, outro.email);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(email);
            return resultado;
        }

        @Override
        public String toString() {
            return "EmailCadastral[" + "email=" + email + "]";
        }
    }

    private static void cpf(String campo, String cpf, Resultado r) {
        if (r.texto(campo, cpf) && !DocumentosCredenciamento.cpfValido(cpf)) {
            r.invalido(campo, "Confira o CPF.");
        }
    }

    private void nascimento(String campo, LocalDate data, Resultado r) {
        if (data == null) {
            r.falta(campo, "Informe a data de nascimento.");
        } else {
            if (!data.isBefore(LocalDate.now(relogio)) || !anoValido(data)) {
                r.invalido(campo, "Confira a data de nascimento.");
            }
        }
    }

    private static boolean anoValido(LocalDate data) {
        return data.getYear() >= 1 && data.getYear() <= 9999;
    }

    private static void site(String campo, String valor, Resultado r) {
        if (!r.texto(campo, valor)) {
            return;
        }
        try {
            URI uri = URI.create(valor.strip());
            if (uri.getHost() != null && uri.getUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) {
                return;
            }
        } catch (IllegalArgumentException ignorado) {
            // A mensagem não reproduz a URL.
        }
        r.invalido(campo, "Informe uma URL HTTP ou HTTPS válida.");
    }

    private static void dinheiro(String campo, BigDecimal valor, Resultado r) {
        if (valor == null) {
            r.falta(campo, "Informe o valor em reais.");
        } else {
            if (valor.signum() < 0 || valor.scale() > 2 || (long) valor.precision() - valor.scale() > 13) {
                r.invalido(campo, "Informe um valor não negativo com até 13 dígitos inteiros e duas casas decimais.");
            }
        }
    }

    private static void endereco(String p, EnderecoBean e, Resultado r) {
        p += "endereco.";
        if (e == null) {
            r.falta(p.substring(0, p.length() - 1), "Preencha o endereço.");
            return;
        }
        r.formato(p + "cep", e.getCep(), "[0-9]{5}-?[0-9]{3}");
        r.texto(p + "logradouro", e.getLogradouro());
        r.texto(p + "numero", e.getNumero());
        r.texto(p + "complemento", e.getComplemento());
        r.texto(p + "bairro", e.getBairro());
        r.texto(p + "cidade", e.getCidade());
        r.formato(p + "uf", e.getUf(), UF);
        r.texto(p + "pontoReferencia", e.getPontoReferencia());
    }

    private static void conta(ContaBancariaBean conta, String documentoRecebedor, Resultado r) {
        String p = "contaBancaria.";
        if (conta == null) {
            r.falta("contaBancaria", "Preencha os dados bancários.");
            return;
        }
        r.texto(p + "titular", conta.getTitular());
        if (conta.getTipoTitular() == null) {
            r.falta(p + "tipoTitular", "Selecione o tipo de titular.");
        }
        if (conta.getTipo() == null) {
            r.falta(p + "tipo", "Selecione o tipo de conta.");
        } else {
            if (conta.getTipo() == TipoConta.POUPANCA) {
                r.confirmar(p + "tipo", "Confirme com a Pagar.me a aceitação de conta poupança.");
            }
        }
        if (r.texto(p + "documento", conta.getDocumento())) {
            boolean cpf = DocumentosCredenciamento.cpfValido(conta.getDocumento());
            boolean cnpj = DocumentosCredenciamento.cnpjValido(conta.getDocumento());
            if ((!cpf && !cnpj) || (conta.getTipoTitular() == TipoTitular.INDIVIDUAL && !cpf)
                    || (conta.getTipoTitular() == TipoTitular.COMPANY && !cnpj)) {
                r.invalido(p + "documento", "Confira o documento e o tipo do titular da conta.");
            } else {
                if (documentoRecebedor != null
                        && !documento(conta.getDocumento()).equals(documento(documentoRecebedor))) {
                    r.confirmar(p + "documento",
                            "O documento difere do recebedor. Confirme a elegibilidade dessa titularidade com a Pagar.me.");
                }
            }
        }
        r.formato(p + "banco", conta.getBanco(), "[0-9]{3}");
        r.formato(p + "agencia", conta.getAgencia(), "[0-9]{1,10}");
        r.formato(p + "conta", conta.getConta(), "[0-9]{1,20}");
        if (!vazio(conta.getDigitoAgencia())) {
            r.formato(p + "digitoAgencia", conta.getDigitoAgencia(), "[0-9Xx]{1,2}");
        }
        r.formato(p + "digitoConta", conta.getDigitoConta(), "[0-9Xx]{1,2}");
    }

    static String documento(String valor) {
        return valor.replaceAll("[./-]", "").toUpperCase(Locale.ROOT);
    }

    private static boolean vazio(String valor) {
        return valor == null || valor.isBlank();
    }

    private static class Resultado {
        private final List<PendenciaBean> faltantes = new ArrayList<>();
        private final List<PendenciaBean> invalidos = new ArrayList<>();
        private final List<PendenciaBean> confirmacoes = new ArrayList<>();

        void falta(String campo, String mensagem) {
            PendenciaBean pendenciaPreparado = new PendenciaBean();
            pendenciaPreparado.setCampo(campo);
            pendenciaPreparado.setMensagem(mensagem);
            faltantes.add(pendenciaPreparado);
        }

        void invalido(String campo, String mensagem) {
            PendenciaBean pendenciaPreparado2 = new PendenciaBean();
            pendenciaPreparado2.setCampo(campo);
            pendenciaPreparado2.setMensagem(mensagem);
            invalidos.add(pendenciaPreparado2);
        }

        void confirmar(String campo, String mensagem) {
            PendenciaBean pendenciaPreparado3 = new PendenciaBean();
            pendenciaPreparado3.setCampo(campo);
            pendenciaPreparado3.setMensagem(mensagem);
            confirmacoes.add(pendenciaPreparado3);
        }

        boolean texto(String campo, String valor) {
            if (vazio(valor)) {
                falta(campo, "Preencha este campo.");
                return false;
            }
            if (valor.chars().anyMatch(Character::isISOControl)) {
                invalido(campo, "Confira os caracteres do campo.");
                return false;
            }
            return true;
        }

        void formato(String campo, String valor, String regex) {
            if (texto(campo, valor) && !valor.matches(regex)) {
                invalido(campo, "Confira o formato do campo.");
            }
        }

        CompletudeBean completude() {
            CompletudeBean completudePreparado = new CompletudeBean();
            completudePreparado.setApto(faltantes.isEmpty() && invalidos.isEmpty() && confirmacoes.isEmpty());
            completudePreparado.setFaltantes(List.copyOf(faltantes));
            completudePreparado.setInvalidos(List.copyOf(invalidos));
            completudePreparado.setConfirmacoes(List.copyOf(confirmacoes));
            return completudePreparado;
        }
    }
}
