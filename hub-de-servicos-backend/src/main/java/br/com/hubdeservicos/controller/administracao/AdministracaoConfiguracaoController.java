package br.com.hubdeservicos.controller.administracao;

import br.com.hubdeservicos.service.administracao.AdministracaoConfiguracaoService;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.AlterarProvedorSms;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoInfo;
import br.com.hubdeservicos.service.sms.SmsProviderResolver.ProvedorSms;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
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
}
