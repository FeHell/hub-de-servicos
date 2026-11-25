package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoInfoBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ServicoInfoBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ModoDesenvolvimentoBean;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService;
import static br.com.hubdeservicos.service.configuracao.AmbienteServicosService.DEV_MODE;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.service.sms.SmsProviderResolver;
import br.com.hubdeservicos.service.sms.SmsProviderResolver.ProvedorSmsBean;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity.SMS_PROVEDOR_ATIVO;

@Service
@PreAuthorize("@acessoAdministrativo.plataforma(authentication)")
@Transactional(readOnly = true)
public class AdministracaoConfiguracaoService {

    private final ConfiguracaoSistemaRepository configuracoes;
    private final SmsProviderResolver provedores;
    private final RegistroAdministrativo registro;
    private final Clock relogio;
    private final ConfiguracoesExternasService externas;

    public AdministracaoConfiguracaoService(ConfiguracaoSistemaRepository configuracoes, SmsProviderResolver provedores,
            RegistroAdministrativo registro, Clock relogio, ConfiguracoesExternasService externas) {
        this.configuracoes = configuracoes;
        this.provedores = provedores;
        this.registro = registro;
        this.relogio = relogio;
        this.externas = externas;
    }

    public List<ConfiguracaoInfoBean> listarConfiguracoes() {
        // Cada configuração exposta pela API precisa ser autorizada explicitamente.
        return configuracoes.findByChave(SMS_PROVEDOR_ATIVO).map(ConfiguracaoInfoBean::de).stream().toList();
    }

    public List<ProvedorSmsBean> listarProvedoresSms() {
        return provedores.listar();
    }

    public ConfiguracaoInfoBean consultarProvedorSms() {
        return ConfiguracaoInfoBean.de(configuracoes.findByChave(SMS_PROVEDOR_ATIVO)
                .orElseThrow(AdministracaoConfiguracaoService::configuracaoAusente));
    }

    @Transactional
    public ConfiguracaoInfoBean alterarProvedorSms(String codigo) {
        if (provedores.localizar(codigo).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Provedor de SMS indisponível.");
        }
        var configuracao = configuracoes.findForUpdateByChave(SMS_PROVEDOR_ATIVO)
                .orElseThrow(AdministracaoConfiguracaoService::configuracaoAusente);
        if (configuracao.getStatus() != StatusCadastro.ATIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A configuração de SMS está inativa.");
        }
        String valorAnterior = configuracao.getValor();
        if (!valorAnterior.equals(codigo)) {
            configuracao.atualizarValor(codigo, relogio.instant());
            registro.registrarAlteracaoConfiguracao(configuracao.getId(), SMS_PROVEDOR_ATIVO, valorAnterior, codigo);
        }
        return ConfiguracaoInfoBean.de(configuracao);
    }

    public ModoDesenvolvimentoBean consultarModoDesenvolvimento() {
        String valor = configuracoes.buscarValor(DEV_MODE, StatusCadastro.ATIVO).orElse("");
        ModoDesenvolvimentoBean modoDesenvolvimentoPreparado = new ModoDesenvolvimentoBean();
        modoDesenvolvimentoPreparado
                .setDevMode(AmbienteServicosService.interpretar(valor) == AmbienteServicosService.Ambiente.DEV);
        return modoDesenvolvimentoPreparado;
    }

    @Transactional
    public ModoDesenvolvimentoBean alterarModoDesenvolvimento(boolean dev) {
        var entrada = configuracoes.findForUpdateByChave(DEV_MODE)
                .filter(item -> item.getStatus() == StatusCadastro.ATIVO).orElseThrow(() -> {
                    var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "DEV_MODE indisponível.");
                    return responseStatusExceptionPreparado;
                });
        String valor = Boolean.toString(dev);
        if (!valor.equals(entrada.getValor())) {
            entrada.atualizarValor(valor, relogio.instant());
            registro.registrarAlteracaoServico(entrada.getId(), "SISTEMA", DEV_MODE);
        }
        ModoDesenvolvimentoBean modoDesenvolvimentoPreparado2 = new ModoDesenvolvimentoBean();
        modoDesenvolvimentoPreparado2.setDevMode(dev);
        return modoDesenvolvimentoPreparado2;
    }

    public List<ServicoInfoBean> listarServicos() {
        return Arrays.stream(ServicoExterno.values()).map(externas::consultar).toList();
    }

    public ServicoInfoBean consultarServico(ServicoExterno servico) {
        return externas.consultarComCredenciais(servico);
    }

    @Transactional
    public ServicoInfoBean atualizarServico(ServicoExterno servico, Map<String, String> valores) {
        for (var alterada : externas.atualizar(servico, valores)) {
            registro.registrarAlteracaoServico(alterada.getId(), servico.name(), alterada.getChave());
        }
        return externas.consultar(servico);
    }

    private static ResponseStatusException configuracaoAusente() {
        var responseStatusExceptionPreparado2 = new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Configuração de SMS não encontrada.");
        return responseStatusExceptionPreparado2;
    }
}
