package br.com.hubdeservicos.repository.auditoria;

import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<AuditoriaEntity, Long> {

    Page<AuditoriaEntity> findByIdAplicacao(UUID idAplicacao, Pageable pagina);
}
