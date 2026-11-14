package br.com.hubdeservicos.repository.usuario;

import br.com.hubdeservicos.entity.usuario.DesafioTelefoneEntity;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface DesafioTelefoneRepository extends JpaRepository<DesafioTelefoneEntity, UUID> {
    Optional<DesafioTelefoneEntity> findByIdAndIdUsuario(UUID id, UUID idUsuario);
    boolean existsByIdUsuarioAndCriadoEmGreaterThan(UUID idUsuario, Instant desde);
    boolean existsByTelefoneAndCriadoEmGreaterThan(String telefone, Instant desde);
    long countByIdUsuarioAndCriadoEmGreaterThan(UUID idUsuario, Instant desde);
    long countByTelefoneAndCriadoEmGreaterThan(String telefone, Instant desde);

    @Modifying
    @Query("update DesafioTelefoneEntity d set d.codigoHash = null where d.idUsuario = :idUsuario and d.codigoHash is not null")
    void invalidar(UUID idUsuario);

    @Modifying
    @Query("delete from DesafioTelefoneEntity d where d.criadoEm < :antes")
    void removerAntigos(Instant antes);
}
