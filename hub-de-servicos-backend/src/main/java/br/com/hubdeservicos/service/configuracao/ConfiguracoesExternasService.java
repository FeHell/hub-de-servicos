package br.com.hubdeservicos.service.configuracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ConfiguracaoExternaInfo;
import br.com.hubdeservicos.dto.configuracao.ContratosConfiguracao.ServicoInfo;
import br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity;
import br.com.hubdeservicos.entity.configuracao.ServicoExterno;
import br.com.hubdeservicos.repository.configuracao.ConfiguracaoSistemaRepository;
import br.com.hubdeservicos.service.configuracao.CriptografiaCredenciais.CredencialIndisponivel;
import java.time.Clock;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ConfiguracoesExternasService {
    private static final String INFOBIP_API_KEY = "INFOBIP_API_KEY";
    private static final String INFOBIP_BASE_URL = "INFOBIP_BASE_URL";
    private static final String STONE_API_KEY = "STONE_API_KEY";
    private final ConfiguracaoSistemaRepository repositorio;
    private final CriptografiaCredenciais criptografia;
    private final Clock relogio;

    public ConfiguracoesExternasService(ConfiguracaoSistemaRepository repositorio,
                                         CriptografiaCredenciais criptografia, Clock relogio) {
        this.repositorio = repositorio;
        this.criptografia = criptografia;
        this.relogio = relogio;
    }

    public ServicoInfo consultar(ServicoExterno servico) {
        return descrever(servico, false);
    }

    public ServicoInfo consultarComCredenciais(ServicoExterno servico) {
        try {
            return descrever(servico, true);
        } catch (CredencialIndisponivel erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível consultar a credencial protegida.");
        }
    }

    @Transactional
    public Set<ConfiguracaoSistemaEntity> atualizar(ServicoExterno servico, Map<String, String> valores) {
        var entradas = repositorio.findForUpdateByServicoOrderByChave(servico.name()).stream()
                .collect(Collectors.toMap(ConfiguracaoSistemaEntity::getChave, Function.identity()));
        var alteradas = new LinkedHashSet<ConfiguracaoSistemaEntity>();
        for (var atualizacao : valores.entrySet()) {
            String chave = atualizacao.getKey();
            String valor = atualizacao.getValue() == null ? "" : atualizacao.getValue().strip();
            var entrada = entradas.get(chave);
            if (!chaves(servico).contains(chave) || entrada == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Configuração inválida para este serviço.");
            }
            boolean sensivel = !INFOBIP_BASE_URL.equals(chave);
            if (entrada.isSensivel() != sensivel || entrada.getStatus() != StatusCadastro.ATIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A configuração solicitada está indisponível.");
            }
            if (sensivel && valor.isEmpty()) continue;
            if (sensivel) {
                if (valor.chars().anyMatch(Character::isISOControl)
                        || INFOBIP_API_KEY.equals(chave) && !valor.matches("[!-~]+")) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma API Key válida.");
                }
                try {
                    valor = criptografia.criptografar(contexto(servico, chave), valor);
                } catch (CredencialIndisponivel erro) {
                    throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                            "Proteção de credenciais indisponível. Verifique a chave mestra do backend.");
                }
            } else {
                valor = ServicoExterno.origemInfobip(valor).orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST, "Informe a URL HTTPS da conta Infobip, sem caminho.")).toString();
            }
            if (!entrada.getValor().equals(valor)) {
                entrada.atualizarValor(valor, relogio.instant());
                alteradas.add(entrada);
            }
        }
        return alteradas;
    }

    public Infobip lerInfobip() {
        var entradas = repositorio.findByServicoOrderByChave(ServicoExterno.INFOBIP.name());
        String url = valorAtivo(entradas, INFOBIP_BASE_URL, false);
        String selado = valorAtivo(entradas, INFOBIP_API_KEY, true);
        String chave = selado.isEmpty() || ServicoExterno.origemInfobip(url).isEmpty() ? ""
                : criptografia.descriptografar(contexto(ServicoExterno.INFOBIP, INFOBIP_API_KEY), selado);
        return new Infobip(url, chave, !selado.isEmpty());
    }

    private static String valorAtivo(List<ConfiguracaoSistemaEntity> entradas, String chave, boolean sensivel) {
        return entradas.stream().filter(e -> e.getChave().equals(chave) && e.isSensivel() == sensivel
                        && e.getStatus() == StatusCadastro.ATIVO)
                .map(ConfiguracaoSistemaEntity::getValor).findFirst().orElse("");
    }

    private ServicoInfo descrever(ServicoExterno servico, boolean incluirCredenciais) {
        var configuracoes = repositorio.findByServicoOrderByChave(servico.name()).stream()
                .filter(e -> chaves(servico).contains(e.getChave()))
                .map(e -> {
                    boolean sensivel = e.isSensivel() || !INFOBIP_BASE_URL.equals(e.getChave());
                    boolean cadastrada = !e.getValor().isBlank();
                    String valor = null;
                    if (cadastrada && !sensivel) {
                        valor = e.getValor();
                    } else if (cadastrada && incluirCredenciais) {
                        valor = criptografia.descriptografar(contexto(servico, e.getChave()), e.getValor());
                    }
                    return new ConfiguracaoExternaInfo(e.getChave(), cadastrada, sensivel, e.getStatus(), valor);
                }).toList();
        return new ServicoInfo(servico, configuracoes);
    }

    private static List<String> chaves(ServicoExterno servico) {
        return switch (servico) {
            case INFOBIP -> List.of(INFOBIP_API_KEY, INFOBIP_BASE_URL);
            case STONE -> List.of(STONE_API_KEY);
        };
    }

    private static String contexto(ServicoExterno servico, String chave) {
        return servico.name() + ":" + chave;
    }

    public record Infobip(String url, @com.fasterxml.jackson.annotation.JsonIgnore String chave, boolean chaveCadastrada) {
        @Override public String toString() { return "Infobip"; }
    }
}
