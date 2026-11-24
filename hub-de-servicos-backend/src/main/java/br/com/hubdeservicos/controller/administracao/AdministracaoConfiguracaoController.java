package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.service.administracao.AdministracaoConfiguracaoService;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AlterarProvedorSms;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoInfo;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AtualizarServico;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ServicoInfo;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ModoDesenvolvimento;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.service.sms.SmsProviderResolver.ProvedorSms;

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
    public List<ConfiguracaoInfo> listarConfiguracoes() {
        return administracao.listarConfiguracoes();
    }

    @GetMapping("/sms/provedores")
    public List<ProvedorSms> listarProvedoresSms() {
        return administracao.listarProvedoresSms();
    }

    @GetMapping("/sms/provedor-ativo")
    public ConfiguracaoInfo consultarProvedorSms() {
        return administracao.consultarProvedorSms();
    }

    @PutMapping("/sms/provedor-ativo")
    public ConfiguracaoInfo alterarProvedorSms(@Valid @RequestBody AlterarProvedorSms dados) {
        return administracao.alterarProvedorSms(dados.codigo());
    }

    @GetMapping("/dev-mode")
    public ResponseEntity<ModoDesenvolvimento> consultarModoDesenvolvimento() {
        return semCache(administracao.consultarModoDesenvolvimento());
    }

    @PutMapping("/dev-mode")
    public ResponseEntity<ModoDesenvolvimento> alterarModoDesenvolvimento(@Valid @RequestBody ModoDesenvolvimento dados) {
        return semCache(administracao.alterarModoDesenvolvimento(dados.devMode()));
    }

    @GetMapping("/servicos")
    public ResponseEntity<List<ServicoInfo>> listarServicos() {
        return semCache(administracao.listarServicos());
    }

    @GetMapping("/servicos/{servico}")
    public ResponseEntity<ServicoInfo> consultarServico(@PathVariable ServicoExterno servico) {
        return semCache(administracao.consultarServico(servico));
    }

    @PutMapping("/servicos/{servico}")
    public ResponseEntity<ServicoInfo> atualizarServico(@PathVariable ServicoExterno servico,
                                                        @Valid @RequestBody AtualizarServico dados) {
        return semCache(administracao.atualizarServico(servico, dados.valores()));
    }

    private static <T> ResponseEntity<T> semCache(T dados) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(dados);
    }
}
