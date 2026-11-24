package br.com.hubdeservicos.service.credenciamento;

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

    public Completude avaliar(Dados dados) {
        var resultado = new Resultado();
        validator.validate(dados).forEach(violacao ->
                resultado.invalido(violacao.getPropertyPath().toString(), "Confira o tamanho do campo."));
        String documento = null;
        if (dados.modalidade() == Modalidade.INDIVIDUAL) {
            documento = pessoaFisica(dados.pessoaFisica(), resultado);
            if (dados.empresa() != null || dados.representante() != null) {
                resultado.invalido("modalidade", "Envie os dados da modalidade selecionada.");
            }
        } else if (dados.modalidade() == Modalidade.CORPORATION) {
            documento = empresa(dados.empresa(), resultado);
            representante(dados.representante(), resultado);
            if (dados.pessoaFisica() != null) resultado.invalido("modalidade", "Envie os dados da modalidade selecionada.");
        } else {
            resultado.falta("modalidade", "Selecione Pessoa física ou Pessoa jurídica.");
        }
        conta(dados.contaBancaria(), documento, resultado);
        return resultado.completude();
    }

    private String pessoaFisica(PessoaFisica pessoa, Resultado r) {
        String p = "pessoaFisica.";
        if (pessoa == null) {
            r.falta("pessoaFisica", "Preencha os dados da pessoa física.");
            return null;
        }
        r.texto(p + "nome", pessoa.nome());
        cpf(p + "cpf", pessoa.cpf(), r);
        contato(p, pessoa.email(), pessoa.telefone(), pessoa.tipoTelefone(), r);
        site(p + "siteUrl", pessoa.siteUrl(), r);
        nascimento(p + "nascimento", pessoa.nascimento(), r);
        r.texto(p + "profissao", pessoa.profissao());
        dinheiro(p + "rendaMensal", pessoa.rendaMensal(), r);
        endereco(p, pessoa.endereco(), r);
        return pessoa.cpf();
    }

    private String empresa(Empresa empresa, Resultado r) {
        String p = "empresa.";
        if (empresa == null) {
            r.falta("empresa", "Preencha os dados da empresa.");
            return null;
        }
        r.texto(p + "razaoSocial", empresa.razaoSocial());
        r.texto(p + "nomeFantasia", empresa.nomeFantasia());
        if (r.texto(p + "cnpj", empresa.cnpj())) {
            if (!DocumentosCredenciamento.cnpjValido(empresa.cnpj())) r.invalido(p + "cnpj", "Confira o CNPJ.");
            else if (documento(empresa.cnpj()).matches(".*[A-Z].*")) {
                r.confirmar(p + "cnpj", "Confirme com a Pagar.me a aceitação do CNPJ alfanumérico.");
            }
        }
        contato(p, empresa.email(), empresa.telefone(), empresa.tipoTelefone(), r);
        site(p + "siteUrl", empresa.siteUrl(), r);
        if (empresa.faturamentoAnual() == null && empresa.faturamento() != null) {
            r.confirmar(p + "faturamentoAnual", "Informe o faturamento anual. O valor anterior não tem período definido.");
        } else dinheiro(p + "faturamentoAnual", empresa.faturamentoAnual(), r);
        if (empresa.dataFundacao() != null && (!anoValido(empresa.dataFundacao())
                || empresa.dataFundacao().isAfter(LocalDate.now(relogio)))) {
            r.invalido(p + "dataFundacao", "Confira a data de fundação.");
        }
        endereco(p, empresa.endereco(), r);
        return empresa.cnpj();
    }

    private void representante(Representante pessoa, Resultado r) {
        String p = "representante.";
        if (pessoa == null) {
            r.falta("representante", "Preencha os dados do representante legal.");
            return;
        }
        r.texto(p + "nome", pessoa.nome());
        cpf(p + "cpf", pessoa.cpf(), r);
        contato(p, pessoa.email(), pessoa.telefone(), pessoa.tipoTelefone(), r);
        nascimento(p + "nascimento", pessoa.nascimento(), r);
        r.texto(p + "profissao", pessoa.profissao());
        if (pessoa.rendaMensal() == null && pessoa.renda() != null) {
            r.confirmar(p + "rendaMensal", "Informe a renda mensal. O valor anterior não tem período definido.");
        } else dinheiro(p + "rendaMensal", pessoa.rendaMensal(), r);
        if (!Boolean.TRUE.equals(pessoa.representanteLegal())) {
            r.confirmar(p + "representanteLegal", "Confirme a declaração de representação legal.");
        }
        if (!Boolean.TRUE.equals(pessoa.socioQsa())) {
            r.confirmar(p + "socioQsa", "Confirme que o representante é sócio qualificado no QSA.");
        }
        endereco(p, pessoa.endereco(), r);
    }

    private void contato(String p, String email, String telefone, TipoTelefone tipo, Resultado r) {
        if (r.texto(p + "email", email)
                && !validator.validate(new EmailCadastral(email.strip())).isEmpty()) {
            r.invalido(p + "email", "Confira o e-mail.");
        }
        if (tipo == null) r.falta(p + "tipoTelefone", "Selecione o tipo de telefone.");
        if (!r.texto(p + "telefone", telefone)) return;
        try {
            boolean celular = TelefoneUsuarioService.normalizar(telefone).length() == 14;
            if (tipo != null && celular != (tipo == TipoTelefone.CELULAR)) {
                r.invalido(p + "telefone", "O telefone não corresponde ao tipo escolhido.");
            } else if (tipo == TipoTelefone.FIXO) {
                r.confirmar(p + "tipoTelefone", "Confirme com a Pagar.me o código para telefone fixo.");
            }
        } catch (ResponseStatusException erro) {
            r.invalido(p + "telefone", "Informe um telefone brasileiro válido com DDD.");
        }
    }

    private record EmailCadastral(@Email String email) {}

    private static void cpf(String campo, String cpf, Resultado r) {
        if (r.texto(campo, cpf) && !DocumentosCredenciamento.cpfValido(cpf)) r.invalido(campo, "Confira o CPF.");
    }

    private void nascimento(String campo, LocalDate data, Resultado r) {
        if (data == null) r.falta(campo, "Informe a data de nascimento.");
        else if (!data.isBefore(LocalDate.now(relogio)) || !anoValido(data)) r.invalido(campo, "Confira a data de nascimento.");
    }

    private static boolean anoValido(LocalDate data) {
        return data.getYear() >= 1 && data.getYear() <= 9999;
    }

    private static void site(String campo, String valor, Resultado r) {
        if (!r.texto(campo, valor)) return;
        try {
            URI uri = URI.create(valor.strip());
            if (uri.getHost() != null && uri.getUserInfo() == null
                    && ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) return;
        } catch (IllegalArgumentException ignorado) {
            // A mensagem não reproduz a URL.
        }
        r.invalido(campo, "Informe uma URL HTTP ou HTTPS válida.");
    }

    private static void dinheiro(String campo, BigDecimal valor, Resultado r) {
        if (valor == null) r.falta(campo, "Informe o valor em reais.");
        else if (valor.signum() < 0 || valor.scale() > 2 || (long) valor.precision() - valor.scale() > 13) {
            r.invalido(campo, "Informe um valor não negativo com até 13 dígitos inteiros e duas casas decimais.");
        }
    }

    private static void endereco(String p, Endereco e, Resultado r) {
        p += "endereco.";
        if (e == null) {
            r.falta(p.substring(0, p.length() - 1), "Preencha o endereço.");
            return;
        }
        r.formato(p + "cep", e.cep(), "[0-9]{5}-?[0-9]{3}");
        r.texto(p + "logradouro", e.logradouro());
        r.texto(p + "numero", e.numero());
        r.texto(p + "complemento", e.complemento());
        r.texto(p + "bairro", e.bairro());
        r.texto(p + "cidade", e.cidade());
        r.formato(p + "uf", e.uf(), UF);
        r.texto(p + "pontoReferencia", e.pontoReferencia());
    }

    private static void conta(ContaBancaria conta, String documentoRecebedor, Resultado r) {
        String p = "contaBancaria.";
        if (conta == null) {
            r.falta("contaBancaria", "Preencha os dados bancários.");
            return;
        }
        r.texto(p + "titular", conta.titular());
        if (conta.tipoTitular() == null) r.falta(p + "tipoTitular", "Selecione o tipo de titular.");
        if (conta.tipo() == null) r.falta(p + "tipo", "Selecione o tipo de conta.");
        else if (conta.tipo() == TipoConta.POUPANCA) {
            r.confirmar(p + "tipo", "Confirme com a Pagar.me a aceitação de conta poupança.");
        }
        if (r.texto(p + "documento", conta.documento())) {
            boolean cpf = DocumentosCredenciamento.cpfValido(conta.documento());
            boolean cnpj = DocumentosCredenciamento.cnpjValido(conta.documento());
            if ((!cpf && !cnpj) || (conta.tipoTitular() == TipoTitular.INDIVIDUAL && !cpf)
                    || (conta.tipoTitular() == TipoTitular.COMPANY && !cnpj)) {
                r.invalido(p + "documento", "Confira o documento e o tipo do titular da conta.");
            } else if (documentoRecebedor != null && !documento(conta.documento()).equals(documento(documentoRecebedor))) {
                r.confirmar(p + "documento", "O documento difere do recebedor. Confirme a elegibilidade dessa titularidade com a Pagar.me.");
            }
        }
        r.formato(p + "banco", conta.banco(), "[0-9]{3}");
        r.formato(p + "agencia", conta.agencia(), "[0-9]{1,10}");
        r.formato(p + "conta", conta.conta(), "[0-9]{1,20}");
        if (!vazio(conta.digitoAgencia())) r.formato(p + "digitoAgencia", conta.digitoAgencia(), "[0-9Xx]{1,2}");
        r.formato(p + "digitoConta", conta.digitoConta(), "[0-9Xx]{1,2}");
    }

    static String documento(String valor) {
        return valor.replaceAll("[./-]", "").toUpperCase(Locale.ROOT);
    }

    private static boolean vazio(String valor) { return valor == null || valor.isBlank(); }

    private static class Resultado {
        private final List<Pendencia> faltantes = new ArrayList<>();
        private final List<Pendencia> invalidos = new ArrayList<>();
        private final List<Pendencia> confirmacoes = new ArrayList<>();

        void falta(String campo, String mensagem) { faltantes.add(new Pendencia(campo, mensagem)); }
        void invalido(String campo, String mensagem) { invalidos.add(new Pendencia(campo, mensagem)); }
        void confirmar(String campo, String mensagem) { confirmacoes.add(new Pendencia(campo, mensagem)); }

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
            if (texto(campo, valor) && !valor.matches(regex)) invalido(campo, "Confira o formato do campo.");
        }

        Completude completude() {
            return new Completude(faltantes.isEmpty() && invalidos.isEmpty() && confirmacoes.isEmpty(),
                    List.copyOf(faltantes), List.copyOf(invalidos), List.copyOf(confirmacoes));
        }
    }
}
