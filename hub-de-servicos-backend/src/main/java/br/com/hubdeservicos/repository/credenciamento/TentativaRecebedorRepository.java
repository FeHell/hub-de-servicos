package br.com.hubdeservicos.repository.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.SituacaoRecebedor;
import br.com.hubdeservicos.entity.credenciamento.RecebedorExternoEntity.Chave;
import br.com.hubdeservicos.entity.credenciamento.TentativaRecebedorEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TentativaRecebedorRepository extends JpaRepository<TentativaRecebedorEntity, UUID> {
    Optional<TentativaRecebedorEntity> findFirstByRecebedorAndSituacaoOrderByTentativaEmDesc(Chave recebedor,
                                                                                         SituacaoRecebedor situacao);
}
