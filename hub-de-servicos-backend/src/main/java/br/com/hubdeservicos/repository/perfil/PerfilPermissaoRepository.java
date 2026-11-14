package br.com.hubdeservicos.repository.perfil;

import br.com.hubdeservicos.entity.perfil.PerfilPermissaoEntity;
import br.com.hubdeservicos.entity.perfil.PerfilPermissaoId;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PerfilPermissaoRepository extends JpaRepository<PerfilPermissaoEntity, PerfilPermissaoId> {

    List<PerfilPermissaoEntity> findByIdIdAplicacaoAndIdIdPerfilOrderByIdIdPermissao(UUID idAplicacao, UUID idPerfil);

    @Modifying
    @Query("delete from PerfilPermissaoEntity pp where pp.id.idAplicacao = :app and pp.id.idPermissao = :permissao")
    void removerPermissaoDaAplicacao(@Param("app") UUID idAplicacao, @Param("permissao") UUID idPermissao);
}
