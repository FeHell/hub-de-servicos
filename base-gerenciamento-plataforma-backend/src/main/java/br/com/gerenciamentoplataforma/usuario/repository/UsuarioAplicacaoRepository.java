package br.com.gerenciamentoplataforma.usuario.repository;

import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoId;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAplicacaoRepository extends JpaRepository<UsuarioAplicacaoEntity, UsuarioAplicacaoId> {

    boolean existsByIdAndAtivoTrue(UsuarioAplicacaoId id);

    Page<UsuarioAplicacaoEntity> findByIdIdAplicacao(UUID idAplicacao, Pageable pagina);
}
