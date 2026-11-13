package br.com.gerenciamentoplataforma.permissao.repository;

import br.com.gerenciamentoplataforma.permissao.entity.PermissaoEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PermissaoRepository extends JpaRepository<PermissaoEntity, UUID> {

    List<PermissaoEntity> findByNomePlataformaIsNotNullOrderByNomePlataformaAscCodigoAsc();

    List<PermissaoEntity> findByNomePlataformaIsNotNullAndCodigoIn(List<String> codigos);

    Optional<PermissaoEntity> findByCodigo(String codigo);
}
