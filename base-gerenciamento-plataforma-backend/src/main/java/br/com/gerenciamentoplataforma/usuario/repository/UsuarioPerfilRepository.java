package br.com.gerenciamentoplataforma.usuario.repository;

import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPerfilEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPerfilId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioPerfilRepository extends JpaRepository<UsuarioPerfilEntity, UsuarioPerfilId> {

    String VINCULOS_ATIVOS = """
            from UsuarioPerfilEntity vinculoPerfil
            join vinculoPerfil.usuarioAplicacao vinculoAplicacao
            join vinculoAplicacao.usuario usuario
            join vinculoAplicacao.aplicacao aplicacao
            join vinculoPerfil.perfil perfil
            join PerfilPermissaoEntity atribuicao
              on atribuicao.id.idAplicacao = vinculoPerfil.id.idAplicacao
             and atribuicao.id.idPerfil = vinculoPerfil.id.idPerfil
            join atribuicao.aplicacaoPermissao habilitacao
            join habilitacao.permissao permissao
            where vinculoAplicacao.ativo = true
              and usuario.status = br.com.gerenciamentoplataforma.usuario.entity.StatusUsuario.ATIVO
              and aplicacao.status = br.com.gerenciamentoplataforma.common.StatusCadastro.ATIVO
              and perfil.status = br.com.gerenciamentoplataforma.common.StatusCadastro.ATIVO
              and permissao.status = br.com.gerenciamentoplataforma.common.StatusCadastro.ATIVO
            """;

    List<UsuarioPerfilEntity> findByIdIdUsuarioAndIdIdAplicacaoOrderByIdIdPerfil(UUID idUsuario, UUID idAplicacao);

    @Modifying
    @Query("delete from UsuarioPerfilEntity vp where vp.id.idUsuario = :idUsuario and vp.id.idAplicacao = :idAplicacao")
    void removerDaAplicacao(@Param("idUsuario") UUID idUsuario, @Param("idAplicacao") UUID idAplicacao);

    @Query("select distinct permissao.codigo " + VINCULOS_ATIVOS + """
               and vinculoPerfil.id.idUsuario = :idUsuario
               and vinculoPerfil.id.idAplicacao = :idAplicacao
             order by permissao.codigo
            """)
    List<String> buscarPermissoesEfetivas(@Param("idUsuario") UUID idUsuario,
                                         @Param("idAplicacao") UUID idAplicacao);
}
