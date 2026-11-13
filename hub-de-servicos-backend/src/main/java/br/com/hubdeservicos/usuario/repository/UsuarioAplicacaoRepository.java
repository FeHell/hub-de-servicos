package br.com.hubdeservicos.usuario.repository;

import br.com.hubdeservicos.usuario.entity.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.usuario.entity.UsuarioAplicacaoId;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAplicacaoRepository extends JpaRepository<UsuarioAplicacaoEntity, UsuarioAplicacaoId> {

    boolean existsByIdAndAtivoTrue(UsuarioAplicacaoId id);

    Page<UsuarioAplicacaoEntity> findByIdIdAplicacao(UUID idAplicacao, Pageable pagina);
}
