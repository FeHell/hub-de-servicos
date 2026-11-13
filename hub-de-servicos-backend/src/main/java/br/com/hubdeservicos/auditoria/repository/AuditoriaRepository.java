package br.com.hubdeservicos.auditoria.repository;

import br.com.hubdeservicos.auditoria.entity.AuditoriaEntity;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, Long> {

    Page<AuditoriaEntity> findByIdAplicacao(UUID idAplicacao, Pageable pagina);
}
