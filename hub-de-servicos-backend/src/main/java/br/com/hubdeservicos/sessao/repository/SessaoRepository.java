package br.com.hubdeservicos.sessao.repository;

import br.com.hubdeservicos.sessao.entity.SessaoEntity;
import br.com.hubdeservicos.sessao.entity.StatusSessao;

import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SessaoRepository extends JpaRepository<SessaoEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SessaoEntity s where s.id = :id")
    Optional<SessaoEntity> bloquearPorId(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SessaoEntity> findByIdAndIdAplicacao(UUID id, UUID idAplicacao);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SessaoEntity> findByIdUsuarioAndStatusOrderById(UUID idUsuario, StatusSessao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SessaoEntity> findByIdAplicacaoAndStatusOrderById(UUID idAplicacao, StatusSessao status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<SessaoEntity> findByIdUsuarioAndIdAplicacaoAndStatusOrderById(UUID idUsuario, UUID idAplicacao,
                                                               StatusSessao status);

    Page<SessaoEntity> findByIdAplicacao(UUID idAplicacao, Pageable pagina);
}
