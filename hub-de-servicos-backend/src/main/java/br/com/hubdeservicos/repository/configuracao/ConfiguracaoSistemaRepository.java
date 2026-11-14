package br.com.hubdeservicos.repository.configuracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.configuracao.ConfiguracaoSistemaEntity;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ConfiguracaoSistemaRepository extends JpaRepository<ConfiguracaoSistemaEntity, UUID> {

    @Query("select c from ConfiguracaoSistemaEntity c where c.servico = 'SISTEMA' and c.sensivel = false and c.chave = :chave")
    Optional<ConfiguracaoSistemaEntity> findByChave(String chave);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ConfiguracaoSistemaEntity c where c.servico = 'SISTEMA' and c.sensivel = false and c.chave = :chave")
    Optional<ConfiguracaoSistemaEntity> findForUpdateByChave(String chave);

    @Query("select c.valor from ConfiguracaoSistemaEntity c where c.servico = 'SISTEMA' and c.sensivel = false and c.chave = :chave and c.status = :status")
    Optional<String> buscarValor(String chave, StatusCadastro status);

    List<ConfiguracaoSistemaEntity> findByServicoOrderByChave(String servico);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<ConfiguracaoSistemaEntity> findForUpdateByServicoOrderByChave(String servico);
}
