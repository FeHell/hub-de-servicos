package br.com.hubdeservicos.repository.sessao;

import br.com.hubdeservicos.entity.sessao.TokenAtualizacaoEntity;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TokenAtualizacaoRepository extends JpaRepository<TokenAtualizacaoEntity, UUID> {

    Optional<TokenAtualizacaoEntity> findByHashToken(String hashToken);

    @Query("select t.idSessao from TokenAtualizacaoEntity t where t.hashToken = :hashToken")
    Optional<UUID> buscarIdSessaoPorHash(@Param("hashToken") String hashToken);

    @Modifying(flushAutomatically = true)
    @Query("""
            update TokenAtualizacaoEntity t
               set t.revogado = true, t.dataRevogacao = :agora
             where t.idSessao = :idSessao and t.idFamilia = :idFamilia and t.revogado = false
            """)
    void revogarFamilia(@Param("idSessao") UUID idSessao, @Param("idFamilia") UUID idFamilia,
                       @Param("agora") Instant agora);
}
