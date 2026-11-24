package br.com.hubdeservicos.repository.credenciamento;

import br.com.hubdeservicos.entity.credenciamento.DiagnosticoRecebedorEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiagnosticoRecebedorRepository extends JpaRepository<DiagnosticoRecebedorEntity, UUID> {}
