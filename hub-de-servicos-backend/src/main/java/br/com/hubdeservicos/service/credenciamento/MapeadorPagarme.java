package br.com.hubdeservicos.service.credenciamento;

import java.util.Objects;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.UnidadeMonetaria;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.AntecipacaoBean;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.MetadataBean;
import br.com.hubdeservicos.dto.credenciamento.PagarmeRecebedorBean.TransferenciaBean;
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

    public PagarmeRecebedorBean preparar(DadosBean dados, UnidadeMonetaria unidade, UUID usuario, String codigo) {
        // A validação antecede a preparação para nunca montar uma requisição com
        // pendências.
        var completude = validacao.avaliar(dados);
        if (!completude.getApto()) {
            var status = HttpStatus.UNPROCESSABLE_ENTITY;
            var problema = ProblemDetail.forStatusAndDetail(status, "Resolva as pendências do credenciamento.");
            problema.setProperty("completude", completude);
            throw new ErrorResponseException(status, problema, null);
        }

        Objects.requireNonNull(unidade);

        PagarmeRecebedorBean.RegistroBean registro = switch (dados.getModalidade()) {
        case INDIVIDUAL -> individual(dados.getPessoaFisica(), unidade);
        case CORPORATION -> corporation(dados.getEmpresa(), dados.getRepresentante(), unidade);
        };

        PagarmeRecebedorBean.BancoBean banco = prepararBanco(dados.getContaBancaria());

        PagarmeRecebedorBean recebedor = new PagarmeRecebedorBean();
        recebedor.setCode(codigo);
        recebedor.setRegisterInformation(registro);
        recebedor.setDefaultBankAccount(banco);

        prepararPadroes(recebedor, usuario);

        return recebedor;
    }

    private static PagarmeRecebedorBean.BancoBean prepararBanco(ContaBancariaBean conta) {
        String titular = conta.getTitular().strip();
        String tipoTitular = conta.getTipoTitular() == TipoTitular.COMPANY ? "company" : "individual";
        String documento = ValidacaoCredenciamento.documento(conta.getDocumento());
        String digitoAgencia = digito(conta.getDigitoAgencia());
        String digitoConta = digito(conta.getDigitoConta());

        PagarmeRecebedorBean.BancoBean banco = new PagarmeRecebedorBean.BancoBean();
        banco.setHolderName(titular);
        banco.setHolderType(tipoTitular);
        banco.setHolderDocument(documento);
        banco.setBank(conta.getBanco());
        banco.setBranchNumber(conta.getAgencia());
        banco.setBranchCheckDigit(digitoAgencia);
        banco.setAccountNumber(conta.getConta());
        banco.setAccountCheckDigit(digitoConta);
        banco.setType("checking");

        return banco;
    }

    private static PagarmeRecebedorBean.IndividualBean individual(PessoaFisicaBean pessoa, UnidadeMonetaria unidade) {

        String aniversario = pessoa.getNascimento().format(NASCIMENTO_PAGARME);

        PagarmeRecebedorBean.IndividualBean individualPreparado = new PagarmeRecebedorBean.IndividualBean();
        individualPreparado.setType("individual");
        individualPreparado.setName(pessoa.getNome().strip());
        individualPreparado.setEmail(pessoa.getEmail().strip());
        individualPreparado.setDocument(ValidacaoCredenciamento.documento(pessoa.getCpf()));
        individualPreparado.setSiteUrl(pessoa.getSiteUrl().strip());
        individualPreparado.setBirthdate(aniversario);
        individualPreparado.setMonthlyIncome(monetario(pessoa.getRendaMensal(), unidade));
        individualPreparado.setProfessionalOccupation(pessoa.getProfissao().strip());
        individualPreparado.setAddress(endereco(pessoa.getEndereco()));
        individualPreparado.setPhoneNumbers(telefone(pessoa.getTelefone(), pessoa.getTipoTelefone()));
        return individualPreparado;
    }

    private static PagarmeRecebedorBean.CorporationBean corporation(EmpresaBean empresa, RepresentanteBean pessoa,
            UnidadeMonetaria unidade) {
        PagarmeRecebedorBean.RepresentanteBean representantePreparado = new PagarmeRecebedorBean.RepresentanteBean();
        representantePreparado.setType("individual");
        representantePreparado.setName(pessoa.getNome().strip());
        representantePreparado.setEmail(pessoa.getEmail().strip());
        representantePreparado.setDocument(ValidacaoCredenciamento.documento(pessoa.getCpf()));
        representantePreparado.setBirthdate(pessoa.getNascimento().format(NASCIMENTO_PAGARME));
        representantePreparado.setMonthlyIncome(monetario(pessoa.getRendaMensal(), unidade));
        representantePreparado.setProfessionalOccupation(pessoa.getProfissao().strip());
        representantePreparado.setRepresentanteLegal(pessoa.getRepresentanteLegal());
        representantePreparado.setAddress(endereco(pessoa.getEndereco()));
        representantePreparado.setPhoneNumbers(telefone(pessoa.getTelefone(), pessoa.getTipoTelefone()));

        PagarmeRecebedorBean.CorporationBean corporationPreparado = new PagarmeRecebedorBean.CorporationBean();
        corporationPreparado.setType("corporation");
        corporationPreparado.setCompanyName(empresa.getNomeFantasia().strip());
        corporationPreparado.setTradingName(empresa.getRazaoSocial().strip());
        corporationPreparado.setEmail(empresa.getEmail().strip());
        corporationPreparado.setDocument(ValidacaoCredenciamento.documento(empresa.getCnpj()));
        corporationPreparado.setSiteUrl(empresa.getSiteUrl().strip());
        corporationPreparado.setAnnualRevenue(monetario(empresa.getFaturamentoAnual(), unidade));
        corporationPreparado.setCorporationType(opcional(empresa.getTipoSocietario()));
        corporationPreparado
                .setFoundingDate(empresa.getDataFundacao() == null ? null : empresa.getDataFundacao().toString());
        corporationPreparado.setMainAddress(endereco(empresa.getEndereco()));
        corporationPreparado.setPhoneNumbers(telefone(empresa.getTelefone(), empresa.getTipoTelefone()));
        corporationPreparado.setManagingPartners(List.of(representantePreparado));
        return corporationPreparado;
    }

    private static PagarmeRecebedorBean.EnderecoBean endereco(EnderecoBean endereco) {
        PagarmeRecebedorBean.EnderecoBean enderecoPreparado = new PagarmeRecebedorBean.EnderecoBean();
        enderecoPreparado.setStreet(endereco.getLogradouro().strip());
        enderecoPreparado.setComplementary(endereco.getComplemento().strip());
        enderecoPreparado.setStreetNumber(endereco.getNumero().strip());
        enderecoPreparado.setNeighborhood(endereco.getBairro().strip());
        enderecoPreparado.setCity(endereco.getCidade().strip());
        enderecoPreparado.setState(endereco.getUf());
        enderecoPreparado.setZipCode(endereco.getCep().replace("-", ""));
        enderecoPreparado.setReferencePoint(endereco.getPontoReferencia().strip());
        return enderecoPreparado;
    }

    private static List<PagarmeRecebedorBean.TelefoneBean> telefone(String valor, TipoTelefone tipo) {
        String tipoPagarme = switch (tipo) {
        case CELULAR -> "mobile";
        case FIXO -> throw new IllegalStateException("Tipo de telefone sem mapeamento confirmado.");
        };
        String numero = TelefoneUsuarioService.normalizar(valor).substring(3);
        PagarmeRecebedorBean.TelefoneBean telefonePreparado = new PagarmeRecebedorBean.TelefoneBean();
        telefonePreparado.setDdd(numero.substring(0, 2));
        telefonePreparado.setNumber(numero.substring(2));
        telefonePreparado.setType(tipoPagarme);
        return List.of(telefonePreparado);
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

    static void prepararPadroes(PagarmeRecebedorBean recebedor, UUID usuario) {
        // Mantém os padrões da integração em um único ponto; o identificador vem do
        // usuário autorizado pelo serviço.
        AntecipacaoBean antecipacao = new AntecipacaoBean();
        antecipacao.setEnabled(false);

        MetadataBean metadados = new MetadataBean();
        metadados.setCompanyToken(Objects.requireNonNull(usuario).toString());

        TransferenciaBean transferencia = new TransferenciaBean();
        transferencia.setTransferEnabled(true);
        transferencia.setTransferInterval("Daily");
        transferencia.setTransferDay(0);

        recebedor.setAutomaticAnticipationSettings(antecipacao);
        recebedor.setMetadata(metadados);
        recebedor.setTransferSettings(transferencia);
    }

}
