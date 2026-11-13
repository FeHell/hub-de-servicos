package br.com.hubdeservicos.permissao.repository;

import br.com.hubdeservicos.permissao.entity.PermissaoEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<PermissaoEntity, UUID> {

    List<PermissaoEntity> findByNomePlataformaIsNotNullOrderByNomePlataformaAscCodigoAsc();

    List<PermissaoEntity> findByNomePlataformaIsNotNullAndCodigoIn(List<String> codigos);

    Optional<PermissaoEntity> findByCodigo(String codigo);
}
