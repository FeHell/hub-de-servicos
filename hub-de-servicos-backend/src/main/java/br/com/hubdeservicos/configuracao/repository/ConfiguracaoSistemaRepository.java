package br.com.hubdeservicos.configuracao.repository;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.configuracao.entity.ConfiguracaoSistemaEntity;

import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistemaEntity, UUID> {

    Optional<ConfiguracaoSistemaEntity> findByChave(String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ConfiguracaoSistemaEntity> findForUpdateByChave(String chave);

    @Query("select c.valor from ConfiguracaoSistemaEntity c where c.chave = :chave and c.status = :status")
    Optional<String> buscarValor(String chave, StatusCadastro status);
}
