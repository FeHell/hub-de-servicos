package br.com.hubdeservicos.perfil.repository;

import br.com.hubdeservicos.perfil.entity.PerfilEntity;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PerfilRepository extends JpaRepository<PerfilEntity, UUID> {

    Optional<PerfilEntity> findByIdAndIdAplicacao(UUID id, UUID idAplicacao);

    Page<PerfilEntity> findByIdAplicacao(UUID idAplicacao, Pageable pagina);
}
