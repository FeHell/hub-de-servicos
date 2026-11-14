package br.com.hubdeservicos.repository.usuario;

import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioAplicacaoRepository extends JpaRepository<UsuarioAplicacaoEntity, UsuarioAplicacaoId> {

    boolean existsByIdAndAtivoTrue(UsuarioAplicacaoId id);

    Page<UsuarioAplicacaoEntity> findByIdIdAplicacao(UUID idAplicacao, Pageable pagina);
}
