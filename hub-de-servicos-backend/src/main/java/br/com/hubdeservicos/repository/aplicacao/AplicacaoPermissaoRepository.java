package br.com.hubdeservicos.repository.aplicacao;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoEntity;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoPermissaoId;
import br.com.hubdeservicos.entity.permissao.PermissaoEntity;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AplicacaoPermissaoRepository extends JpaRepository<AplicacaoPermissaoEntity, AplicacaoPermissaoId> {

    @Query("select ap.permissao from AplicacaoPermissaoEntity ap where ap.id.idAplicacao = :id order by ap.permissao.codigo")
    List<PermissaoEntity> listarPermissoes(@Param("id") UUID idAplicacao);
}
