package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.service.administracao.AdministracaoConfiguracaoService;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AlterarProvedorSmsBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoInfoBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AtualizarServicoBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ServicoInfoBean;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ModoDesenvolvimentoBean;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.service.sms.SmsProviderResolver.ProvedorSmsBean;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/configuracoes")
@SecurityRequirement(name = "bearer")
@Tag(name = "Configurações do sistema")
public class AdministracaoConfiguracaoController {

    private final AdministracaoConfiguracaoService administracao;

    public AdministracaoConfiguracaoController(AdministracaoConfiguracaoService administracao) {
        this.administracao = administracao;
    }

    @GetMapping
    public List<ConfiguracaoInfoBean> listarConfiguracoes() {
        return administracao.listarConfiguracoes();
    }

    @GetMapping("/sms/provedores")
    public List<ProvedorSmsBean> listarProvedoresSms() {
        return administracao.listarProvedoresSms();
    }

    @GetMapping("/sms/provedor-ativo")
    public ConfiguracaoInfoBean consultarProvedorSms() {
        return administracao.consultarProvedorSms();
    }

    @PutMapping("/sms/provedor-ativo")
    public ConfiguracaoInfoBean alterarProvedorSms(@Valid @RequestBody AlterarProvedorSmsBean dados) {
        return administracao.alterarProvedorSms(dados.getCodigo());
    }

    @GetMapping("/dev-mode")
    public ResponseEntity<ModoDesenvolvimentoBean> consultarModoDesenvolvimento() {
        return semCache(administracao.consultarModoDesenvolvimento());
    }

    @PutMapping("/dev-mode")
    public ResponseEntity<ModoDesenvolvimentoBean> alterarModoDesenvolvimento(
            @Valid @RequestBody ModoDesenvolvimentoBean dados) {
        return semCache(administracao.alterarModoDesenvolvimento(dados.getDevMode()));
    }

    @GetMapping("/servicos")
    public ResponseEntity<List<ServicoInfoBean>> listarServicos() {
        return semCache(administracao.listarServicos());
    }

    @GetMapping("/servicos/{servico}")
    public ResponseEntity<ServicoInfoBean> consultarServico(@PathVariable ServicoExterno servico) {
        return semCache(administracao.consultarServico(servico));
    }

    @PutMapping("/servicos/{servico}")
    public ResponseEntity<ServicoInfoBean> atualizarServico(@PathVariable ServicoExterno servico,
            @Valid @RequestBody AtualizarServicoBean dados) {
        return semCache(administracao.atualizarServico(servico, dados.getValores()));
    }

    private static <T> ResponseEntity<T> semCache(T dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
