package br.com.gerenciamentoplataforma.administracao.service;

import br.com.gerenciamentoplataforma.auditoria.entity.AuditoriaEntity;
import br.com.gerenciamentoplataforma.auditoria.repository.AuditoriaRepository;
import br.com.gerenciamentoplataforma.autenticacao.service.IdentidadeJwt;

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
        var identidade = (IdentidadeJwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return identidade.idUsuario();
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
