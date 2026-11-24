package br.com.hubdeservicos.repository.credenciamento;

import br.com.hubdeservicos.entity.credenciamento.CredenciamentoEntity;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CredenciamentoRepository extends JpaRepository<CredenciamentoEntity, UUID> {
    @Query(value = """
            select exists(select 1 from credenciamento_recebedor_externo where id_credenciamento = :id)
                or exists(select 1 from credenciamento_recebedor_diagnostico where id_credenciamento = :id)
            """, nativeQuery = true)
    boolean possuiHistorico(UUID id);

    @Query("select c from CredenciamentoEntity c where c.solicitante = :solicitante and c.provedor = :provedor and c.excluidoEm is null")
    Optional<CredenciamentoEntity> findBySolicitanteAndProvedor(UUID solicitante, ProvedorCredenciamento provedor);
}
