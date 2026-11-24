package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedor.UnidadeMonetaria;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponseException;

@Component
public class MapeadorPagarme {
	private static final DateTimeFormatter NASCIMENTO_PAGARME = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private final ValidacaoCredenciamento validacao;

    public MapeadorPagarme(ValidacaoCredenciamento validacao) {
        this.validacao = validacao;
    }

    public PagarmeRecebedor preparar(Dados dados, UnidadeMonetaria unidade, UUID usuario, String codigo) {
        var completude = validacao.avaliar(dados);
        if (!completude.apto()) {
            var status = HttpStatus.UNPROCESSABLE_ENTITY;
            var problema = ProblemDetail.forStatusAndDetail(status, "Resolva as pendências do credenciamento.");
            problema.setProperty("completude", completude);
            throw new ErrorResponseException(status, problema, null);
        }
        java.util.Objects.requireNonNull(unidade);
        PagarmeRecebedor.Registro registro = switch (dados.modalidade()) {
            case INDIVIDUAL -> individual(dados.pessoaFisica(), unidade);
            case CORPORATION -> corporation(dados.empresa(), dados.representante(), unidade);
        };
        var conta = dados.contaBancaria();
        return new PagarmeRecebedor(codigo, registro, new PagarmeRecebedor.Banco(conta.titular().strip(),
                conta.tipoTitular() == TipoTitular.COMPANY ? "company" : "individual",
                ValidacaoCredenciamento.documento(conta.documento()), conta.banco(), conta.agencia(),
                digito(conta.digitoAgencia()), conta.conta(), digito(conta.digitoConta()), "checking"), usuario);
    }

    private static PagarmeRecebedor.Individual individual(PessoaFisica pessoa, UnidadeMonetaria unidade) {
    	
    	String aniversario = pessoa.nascimento().format(NASCIMENTO_PAGARME);
    	
    	
    	
        return new PagarmeRecebedor.Individual("individual", pessoa.nome().strip(), pessoa.email().strip(),
                ValidacaoCredenciamento.documento(pessoa.cpf()), pessoa.siteUrl().strip(),
                aniversario, monetario(pessoa.rendaMensal(), unidade), pessoa.profissao().strip(),
                endereco(pessoa.endereco()), telefone(pessoa.telefone(), pessoa.tipoTelefone()));
    }

    private static PagarmeRecebedor.Corporation corporation(Empresa empresa, Representante pessoa, UnidadeMonetaria unidade) {
        var representante = new PagarmeRecebedor.Representante("individual", pessoa.nome().strip(), pessoa.email().strip(),
                ValidacaoCredenciamento.documento(pessoa.cpf()), pessoa.nascimento().format(NASCIMENTO_PAGARME),
                monetario(pessoa.rendaMensal(), unidade), pessoa.profissao().strip(), pessoa.representanteLegal(),
                endereco(pessoa.endereco()), telefone(pessoa.telefone(), pessoa.tipoTelefone()));
        return new PagarmeRecebedor.Corporation("corporation", empresa.nomeFantasia().strip(), empresa.razaoSocial().strip(),
                empresa.email().strip(), ValidacaoCredenciamento.documento(empresa.cnpj()), empresa.siteUrl().strip(),
                monetario(empresa.faturamentoAnual(), unidade), opcional(empresa.tipoSocietario()),
                empresa.dataFundacao() == null ? null : empresa.dataFundacao().toString(), endereco(empresa.endereco()),
                telefone(empresa.telefone(), empresa.tipoTelefone()), List.of(representante));
    }

    private static PagarmeRecebedor.Endereco endereco(Endereco endereco) {
        return new PagarmeRecebedor.Endereco(endereco.logradouro().strip(), endereco.complemento().strip(),
                endereco.numero().strip(), endereco.bairro().strip(), endereco.cidade().strip(), endereco.uf(),
                endereco.cep().replace("-", ""), endereco.pontoReferencia().strip());
    }

    private static List<PagarmeRecebedor.Telefone> telefone(String valor, TipoTelefone tipo) {
        String tipoPagarme = switch (tipo) {
            case CELULAR -> "mobile";
            case FIXO -> throw new IllegalStateException("Tipo de telefone sem mapeamento confirmado.");
        };
        String numero = TelefoneUsuarioService.normalizar(valor).substring(3);
        return List.of(new PagarmeRecebedor.Telefone(numero.substring(0, 2), numero.substring(2), tipoPagarme));
    }

    private static String opcional(String valor) {
        return valor == null || valor.isBlank() ? null : valor.strip();
    }

    private static String digito(String valor) {
        String digito = opcional(valor);
        return digito == null ? null : digito.toUpperCase(Locale.ROOT);
    }

    private static BigDecimal monetario(BigDecimal valor, UnidadeMonetaria unidade) {
        return unidade == UnidadeMonetaria.CENTAVOS ? valor.movePointRight(2).setScale(0) : valor;
    }
}
