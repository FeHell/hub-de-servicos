package br.com.hubdeservicos.service.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity.Operacao;
import br.com.hubdeservicos.repository.credenciamento.TentativaRecebedorRepository;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
class SituacaoCredenciamento {
    private final TentativaRecebedorRepository tentativas;
    private final DiagnosticoRecebedorService diagnosticos;
    private final ComparacaoCredenciamento comparacao;

    SituacaoCredenciamento(TentativaRecebedorRepository tentativas, DiagnosticoRecebedorService diagnosticos,
            ComparacaoCredenciamento comparacao) {
        this.tentativas = tentativas;
        this.diagnosticos = diagnosticos;
        this.comparacao = comparacao;
    }

    Operacao operacaoDeReenvio(RecebedorExternoEntity registro, long versao, DadosBean dados,
            Function<String, DadosBean> decifrar) {
        if (dados.getModalidade() != Modalidade.INDIVIDUAL || registro.tentativa() == null) {
            return null;
        }
        if (registro.idRecebedor() == null) {
            return registro.situacao() == SituacaoRecebedor.FALHA && (registro.erro() == ErroRecebedor.AUTENTICACAO
                    || registro.erro() == ErroRecebedor.DADOS_RECUSADOS
                            && diagnosticos.confirmaRecusaDeDados(registro.tentativa())) ? Operacao.CRIACAO : null;
        }

        // Aguarda a decisão atual da Stone antes de alterar um recebedor existente.
        if (registro.situacao() != SituacaoRecebedor.CRIADO || registro.representa(versao)
                || registro.statusProvedor() != StatusPagarme.ACTIVE || !aprovacaoConfirmada(registro)) {
            return null;
        }
        var anterior = tentativas
                .findFirstByRecebedorAndSituacaoOrderByTentativaEmDesc(registro.chave(), SituacaoRecebedor.CRIADO)
                .filter(t -> t.dadosProtegidos() != null).orElse(null);
        if (anterior == null) {
            return null;
        }
        DadosBean enviado;
        try {
            enviado = decifrar.apply(anterior.dadosProtegidos());
        } catch (ResponseStatusException erro) {
            return null;
        }
        if (enviado.getModalidade() != Modalidade.INDIVIDUAL || enviado.getPessoaFisica() == null
                || dados.getPessoaFisica() == null) {
            return null;
        }
        boolean mesmaConta = comparacao.iguais(enviado.getContaBancaria(), dados.getContaBancaria());
        var pessoa = dados.getPessoaFisica();
        if (!mesmaConta && comparacao.iguais(enviado.getPessoaFisica(), pessoa)) {
            return Operacao.ATUALIZACAO_BANCARIA;
        }
        return null;
    }

    RecebedorBean resposta(RecebedorExternoEntity registro, long versao, Operacao operacao, boolean apto) {
        boolean emProcessamento = registro.situacao() == SituacaoRecebedor.EM_ANALISE;
        boolean podeReenviar = operacao != null && apto;
        boolean podeConsultar = registro.situacao() != SituacaoRecebedor.FALHA || registro.idRecebedor() != null
                || registro.erro() == ErroRecebedor.DADOS_RECUSADOS && operacao == null;
        boolean versaoAtual = registro.representa(versao);
        var status = versaoAtual ? registro.statusProvedor() : null;
        String orientacao = orientacao(registro, versaoAtual, operacao, apto);
        if (status == StatusPagarme.ACTIVE && !aprovacaoConfirmada(registro)) {
            status = null;
            orientacao = "A Stone recebeu os dados alterados. A aprovação desta versão ainda precisa ser confirmada.";
        }
        var campos = registro.camposRecusados();
        var pendencias = !versaoAtual || campos == null || campos.isBlank() ? List.<PendenciaBean>of()
                : Arrays.stream(campos.split(",")).map(campo -> {
                    PendenciaBean pendenciaPreparado = new PendenciaBean();
                    pendenciaPreparado.setCampo(campo);
                    pendenciaPreparado.setMensagem("A Pagar.me recusou este campo. Confira o valor informado.");
                    return pendenciaPreparado;
                }).toList();
        var situacao = versaoAtual ? registro.situacao() : SituacaoRecebedor.AGUARDANDO_NOVA_SUBMISSAO;
        RecebedorBean recebedorPreparado = new RecebedorBean();
        recebedorPreparado.setAmbiente(registro.chave().ambiente());
        recebedorPreparado.setSituacao(situacao);
        recebedorPreparado.setStatusProvedor(status);
        recebedorPreparado.setErro(versaoAtual ? registro.erro() : null);
        recebedorPreparado.setPendencias(pendencias);
        recebedorPreparado.setVersao(versao);
        recebedorPreparado.setPodeEnviar(podeReenviar);
        recebedorPreparado.setPodeConsultar(podeConsultar);
        recebedorPreparado.setOrientacao(orientacao);
        recebedorPreparado.setPodeReenviar(podeReenviar);
        recebedorPreparado.setEmProcessamento(emProcessamento);
        recebedorPreparado.setEnvioAceito(versaoAtual && registro.situacao() == SituacaoRecebedor.CRIADO);
        recebedorPreparado.setTentativa(registro.tentativa());
        return recebedorPreparado;
    }

    private static String orientacao(RecebedorExternoEntity registro, boolean versaoAtual, Operacao operacao,
            boolean apto) {
        if (registro.situacao() == SituacaoRecebedor.EM_ANALISE) {
            return "Aguarde a resposta da Stone para esta tentativa.";
        }
        if (registro.situacao() == SituacaoRecebedor.INDETERMINADO) {
            return "Consulte o andamento antes de tentar um novo envio. Se a situação persistir, fale com o suporte.";
        }
        if (registro.statusProvedor() == StatusPagarme.REFUSED) {
            return "A Stone recusou o recebedor. Consulte o suporte para saber o motivo e as opções disponíveis.";
        }
        if (registro.statusProvedor() == StatusPagarme.REGISTRATION
                || registro.statusProvedor() == StatusPagarme.AFFILIATION) {
            return "Aguarde a análise da Stone. Acompanhe a tentativa em Consultar andamento.";
        }
        if (operacao != null) {
            return apto ? "Os dados salvos podem ser enviados novamente."
                    : "Complete e salve os dados para enviar novamente.";
        }
        if (registro.idRecebedor() != null && (!versaoAtual || registro.erro() == ErroRecebedor.ATUALIZACAO_RESTRITA)) {
            return "A Stone restringe alterações em dados já cadastrados. Procure o suporte para atualizar o recebedor existente.";
        }
        if (registro.erro() == ErroRecebedor.DADOS_RECUSADOS) {
            return "Consulte a tentativa anterior antes de enviar novamente.";
        }
        return null;
    }

    static RecebedorBean naoEnviado(Ambiente ambiente, Long versao) {
        RecebedorBean recebedorPreparado2 = new RecebedorBean();
        recebedorPreparado2.setAmbiente(ambiente);
        recebedorPreparado2.setSituacao(SituacaoRecebedor.NAO_ENVIADO);
        recebedorPreparado2.setStatusProvedor(null);
        recebedorPreparado2.setErro(null);
        recebedorPreparado2.setPendencias(List.of());
        recebedorPreparado2.setVersao(versao);
        recebedorPreparado2.setPodeEnviar(true);
        recebedorPreparado2.setPodeConsultar(false);
        recebedorPreparado2.setOrientacao(null);
        recebedorPreparado2.setPodeReenviar(false);
        recebedorPreparado2.setEmProcessamento(false);
        recebedorPreparado2.setEnvioAceito(false);
        recebedorPreparado2.setTentativa(null);
        return recebedorPreparado2;
    }

    static boolean requerConfirmacao(RecebedorExternoEntity registro, long versao) {
        if (!registro.representa(versao)) {
            return false;
        }
        return switch (registro.situacao()) {
        case EM_ANALISE, INDETERMINADO -> true;
        case CRIADO -> registro.statusProvedor() == null || registro.statusProvedor() == StatusPagarme.ACTIVE
                || registro.statusProvedor() == StatusPagarme.REGISTRATION
                || registro.statusProvedor() == StatusPagarme.AFFILIATION;
        default -> false;
        };
    }

    boolean aprovacaoConfirmada(RecebedorExternoEntity registro) {
        return registro.tentativa() == null || tentativas.findById(registro.tentativa())
                .map(tentativa -> tentativa.aprovacaoConfirmada()).orElse(false);
    }

}
