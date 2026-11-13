package br.com.hubdeservicos.usuario.repository;

import br.com.hubdeservicos.usuario.entity.UsuarioEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, UUID> {

    @Query("select u from UsuarioEntity u where lower(u.login) = lower(:login)")
    Optional<UsuarioEntity> buscarPorLogin(@Param("login") String login);

    @Query("select u from UsuarioEntity u where lower(u.email) = lower(:email)")
    Optional<UsuarioEntity> buscarPorEmail(@Param("email") String email);

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select u from UsuarioEntity u where lower(u.login) = lower(:login)")
    Optional<UsuarioEntity> buscarLoginParaAutenticacao(@Param("login") String login);

    // Compatível com os bloqueios de FK usados pela auditoria durante o encerramento de sessões.
    @Query(value = "select u.* from usuario u where u.id = :id for no key update", nativeQuery = true)
    Optional<UsuarioEntity> bloquearPorId(@Param("id") UUID id);
}
