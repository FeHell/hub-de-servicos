package br.com.hubdeservicos.aplicacao.repository;

import br.com.hubdeservicos.aplicacao.entity.AplicacaoEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AplicacaoRepository extends JpaRepository<AplicacaoEntity, UUID> {

    Optional<AplicacaoEntity> findBySlug(String slug);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select a from AplicacaoEntity a where a.slug = :slug")
    Optional<AplicacaoEntity> buscarSlugParaLogin(@Param("slug") String slug);

    // Compatível com os bloqueios de FK usados pela auditoria durante o encerramento de sessões.
    @Query(value = "select a.* from aplicacao a where a.slug = :slug for no key update", nativeQuery = true)
    Optional<AplicacaoEntity> bloquearPorSlug(@Param("slug") String slug);

    @Query(value = "select a.* from aplicacao a where a.id = :id for no key update", nativeQuery = true)
    Optional<AplicacaoEntity> bloquearPorId(@Param("id") UUID id);
}
