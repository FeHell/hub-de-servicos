package br.com.hubdeservicos.repository.credenciamento;

import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity;
import br.com.hubdeservicos.entity.credenciamento.ProvedorCredenciamento;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RecebedorExternoRepository extends JpaRepository<RecebedorExternoEntity, Chave> {
    @Query(value = "select 1 from pg_advisory_xact_lock(:documento)", nativeQuery = true)
    int bloquearDocumento(long documento);

    @Query("""
            select count(r) > 0 from RecebedorExternoEntity r
            where r.id.provedor = :provedor and r.id.ambiente = :ambiente and r.documentoDigest = :digest
                and r.id.credenciamento <> :credenciamento
            """)
    boolean documentoVinculado(ProvedorCredenciamento provedor, Ambiente ambiente, String digest, UUID credenciamento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<RecebedorExternoEntity> findByIdCredenciamentoOrderByIdAmbiente(UUID credenciamento);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RecebedorExternoEntity r where r.id = :chave")
    Optional<RecebedorExternoEntity> bloquear(Chave chave);
}
