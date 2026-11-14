package br.com.hubdeservicos.administracao.service;

import br.com.hubdeservicos.auditoria.entity.AuditoriaEntity;
import br.com.hubdeservicos.auditoria.repository.AuditoriaRepository;
import br.com.hubdeservicos.autenticacao.service.IdentidadeJwt;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.UUID;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
class RegistroAdministrativo {

    private final AuditoriaRepository auditoria;

    RegistroAdministrativo(AuditoriaRepository auditoria) {
        this.auditoria = auditoria;
    }

    UUID idOperador() {
        return identidade().idUsuario();
    }

    private IdentidadeJwt identidade() {
        return (IdentidadeJwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    void registrarAlteracaoConfiguracao(UUID idConfiguracao, String chave, String anterior, String novo) {
        var antes = JsonNodeFactory.instance.objectNode().put("chave", chave).put("valor", anterior);
        var depois = JsonNodeFactory.instance.objectNode().put("chave", chave).put("valor", novo)
                .put("idRecurso", idConfiguracao.toString());
        var operador = identidade();
        auditoria.save(new AuditoriaEntity(operador.idUsuario(), operador.idAplicacao(),
                "ALTERAR_PROVEDOR_SMS", "configuracao_sistema", antes, depois, null));
    }

    void registrar(String acao, String tabela, UUID idAplicacao, UUID idRecurso) {
        registrar(acao, tabela, idAplicacao, idRecurso, null);
    }

    void registrar(String acao, String tabela, UUID idAplicacao, UUID idRecurso, UUID idRelacionado) {
        var dados = JsonNodeFactory.instance.objectNode().put("idRecurso", idRecurso.toString());
        if (idRelacionado != null) {
            dados.put("idRelacionado", idRelacionado.toString());
        }
        auditoria.save(new AuditoriaEntity(idOperador(), idAplicacao, acao, tabela, null, dados, null));
    }
}
