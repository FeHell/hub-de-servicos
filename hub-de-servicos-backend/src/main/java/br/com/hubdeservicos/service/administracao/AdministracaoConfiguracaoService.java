package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoInfo;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ServicoInfo;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ModoDesenvolvimento;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService;
import static br.com.hubdeservicos.service.configuracao.AmbienteServicosService.DEV_MODE;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.service.configuracao.ConfiguracoesExternasService;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.service.sms.SmsProviderResolver;
import br.com.hubdeservicos.service.sms.SmsProviderResolver.ProvedorSms;

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

    public List<ConfiguracaoInfo> listarConfiguracoes() {
        // Cada configuração exposta pela API precisa ser autorizada explicitamente.
        return configuracoes.findByChave(SMS_PROVEDOR_ATIVO).map(ConfiguracaoInfo::de).stream().toList();
    }

    public List<ProvedorSms> listarProvedoresSms() {
        return provedores.listar();
    }

    public ConfiguracaoInfo consultarProvedorSms() {
        return ConfiguracaoInfo.de(configuracoes.findByChave(SMS_PROVEDOR_ATIVO)
                .orElseThrow(AdministracaoConfiguracaoService::configuracaoAusente));
    }

    @Transactional
    public ConfiguracaoInfo alterarProvedorSms(String codigo) {
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
        return ConfiguracaoInfo.de(configuracao);
    }

    public ModoDesenvolvimento consultarModoDesenvolvimento() {
        String valor = configuracoes.buscarValor(DEV_MODE, StatusCadastro.ATIVO).orElse("");
        return new ModoDesenvolvimento(AmbienteServicosService.interpretar(valor) == AmbienteServicosService.Ambiente.DEV);
    }

    @Transactional
    public ModoDesenvolvimento alterarModoDesenvolvimento(boolean dev) {
        var entrada = configuracoes.findForUpdateByChave(DEV_MODE)
                .filter(item -> item.getStatus() == StatusCadastro.ATIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "DEV_MODE indisponível."));
        String valor = Boolean.toString(dev);
        if (!valor.equals(entrada.getValor())) {
            entrada.atualizarValor(valor, relogio.instant());
            registro.registrarAlteracaoServico(entrada.getId(), "SISTEMA", DEV_MODE);
        }
        return new ModoDesenvolvimento(dev);
    }

    public List<ServicoInfo> listarServicos() {
        return Arrays.stream(ServicoExterno.values()).map(externas::consultar).toList();
    }

    public ServicoInfo consultarServico(ServicoExterno servico) {
        return externas.consultarComCredenciais(servico);
    }

    @Transactional
    public ServicoInfo atualizarServico(ServicoExterno servico, Map<String, String> valores) {
        for (var alterada : externas.atualizar(servico, valores)) {
            registro.registrarAlteracaoServico(alterada.getId(), servico.name(), alterada.getChave());
        }
        return externas.consultar(servico);
    }

    private static ResponseStatusException configuracaoAusente() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Configuração de SMS não encontrada.");
    }
}
