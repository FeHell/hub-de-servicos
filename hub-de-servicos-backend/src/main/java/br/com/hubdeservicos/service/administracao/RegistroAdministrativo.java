package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.service.autenticacao.IdentidadeJwtVO;

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
        return identidade().getIdUsuario();
    }

    private IdentidadeJwtVO identidade() {
        return (IdentidadeJwtVO) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    void registrarAlteracaoConfiguracao(UUID idConfiguracao, String chave, String anterior, String novo) {
        var antes = JsonNodeFactory.instance.objectNode().put("chave", chave).put("valor", anterior);
        var depois = JsonNodeFactory.instance.objectNode().put("chave", chave).put("valor", novo).put("idRecurso",
                idConfiguracao.toString());
        var operador = identidade();
        var auditoriaEntityPreparado = new AuditoriaEntity(operador.getIdUsuario(), operador.getIdAplicacao(),
                "ALTERAR_PROVEDOR_SMS", "configuracao_sistema", antes, depois, null);
        auditoria.save(auditoriaEntityPreparado);
    }

    void registrarAlteracaoServico(UUID id, String servico, String chave) {
        var dados = JsonNodeFactory.instance.objectNode().put("idRecurso", id.toString()).put("servico", servico)
                .put("chave", chave);
        var operador = identidade();
        var auditoriaEntityPreparado2 = new AuditoriaEntity(operador.getIdUsuario(), operador.getIdAplicacao(),
                "ALTERAR_CONFIGURACAO_SERVICO", "configuracao_sistema", null, dados, null);
        auditoria.save(auditoriaEntityPreparado2);
    }

    void registrar(String acao, String tabela, UUID idAplicacao, UUID idRecurso) {
        registrar(acao, tabela, idAplicacao, idRecurso, null);
    }

    void registrar(String acao, String tabela, UUID idAplicacao, UUID idRecurso, UUID idRelacionado) {
        var dados = JsonNodeFactory.instance.objectNode().put("idRecurso", idRecurso.toString());
        if (idRelacionado != null) {
            dados.put("idRelacionado", idRelacionado.toString());
        }
        var auditoriaEntityPreparado3 = new AuditoriaEntity(idOperador(), idAplicacao, acao, tabela, null, dados, null);
        auditoria.save(auditoriaEntityPreparado3);
    }
}
