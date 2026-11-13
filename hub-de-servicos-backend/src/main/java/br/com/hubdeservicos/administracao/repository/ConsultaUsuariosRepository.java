package br.com.hubdeservicos.administracao.repository;

import br.com.hubdeservicos.usuario.entity.StatusUsuario;
import br.com.hubdeservicos.usuario.entity.UsuarioEntity;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import static br.com.hubdeservicos.usuario.repository.UsuarioPerfilRepository.VINCULOS_ATIVOS;

public interface ConsultaUsuariosRepository extends Repository<UsuarioEntity, UUID> {

    String VINCULOS_PLATAFORMA_ATIVOS = """
            from UsuarioPermissaoPlataformaEntity concessao
            join PermissaoEntity permissao on permissao.id = concessao.id.idPermissao
            join UsuarioEntity usuario on usuario.id = concessao.id.idUsuario
            join UsuarioAplicacaoEntity vinculo on vinculo.id.idUsuario = usuario.id and vinculo.id.idAplicacao = :administracao
            join vinculo.aplicacao aplicacao
            where permissao.nomePlataforma is not null
              and vinculo.ativo = true
              and usuario.status = br.com.hubdeservicos.usuario.entity.StatusUsuario.ATIVO
              and aplicacao.status = br.com.hubdeservicos.common.StatusCadastro.ATIVO
              and permissao.status = br.com.hubdeservicos.common.StatusCadastro.ATIVO
            """;

    String TIPO_ACESSO = "case when exists (select 1 " + VINCULOS_PLATAFORMA_ATIVOS + """
            and usuario.id = u.id and permissao.codigo = :permissaoPlataforma)
            then 'PLATAFORMA'
            when exists (select 1
            """ + VINCULOS_ATIVOS + """
            and usuario.id = u.id and aplicacao.id <> :administracao
            and permissao.codigo = :permissaoAplicacao)
            then 'APLICACAO'
            else 'USUARIO' end
            """;

    @Query("select count(usuario) " + VINCULOS_PLATAFORMA_ATIVOS + """
            and permissao.codigo = :codigo and usuario.id <> :idUsuario
            """)
    long contarOutrosAdministradores(UUID administracao, String codigo, UUID idUsuario);

    @Query("""
            select usuario.id as idUsuario, aplicacao.id as idAplicacao,
              aplicacao.slug as aplicacao, permissao.codigo as codigo
            """ + VINCULOS_PLATAFORMA_ATIVOS + """
              and usuario.id in :usuarios
            order by permissao.codigo
            """)
    List<PermissaoDoUsuario> permissoesDaPlataforma(UUID administracao, List<UUID> usuarios);

    @Query("""
            select u from UsuarioEntity u
            where locate(lower(:login), lower(u.login)) > 0
              and locate(lower(:email), lower(u.email)) > 0
              and (:status is null or u.status = :status)
              and (:tipo is null or :tipo =
            """ + TIPO_ACESSO + ")")
    Page<UsuarioEntity> pesquisar(@Param("login") String login, @Param("email") String email,
            @Param("status") StatusUsuario status, @Param("tipo") String tipo,
            @Param("administracao") UUID administracao, @Param("permissaoPlataforma") String permissaoPlataforma,
            @Param("permissaoAplicacao") String permissaoAplicacao, Pageable pagina);

    @Query("""
            select distinct usuario.id as idUsuario, aplicacao.id as idAplicacao,
              aplicacao.slug as aplicacao, permissao.codigo as codigo
            """ + VINCULOS_ATIVOS + """
              and usuario.id in :usuarios and aplicacao.id <> :administracao
            order by aplicacao.slug, permissao.codigo
            """)
    List<PermissaoDoUsuario> permissoes(UUID administracao, List<UUID> usuarios);

    @Query("""
            select u.status as status, count(u) as total from UsuarioEntity u
            where u.dataCriacao >= :inicio and u.dataCriacao <= :fim
            group by u.status
            """)
    List<ContagemStatus> contarPorStatus(Instant inicio, Instant fim);

    @Query("select " + TIPO_ACESSO + """
            as tipo, count(u) as total from UsuarioEntity u
            where u.dataCriacao >= :inicio and u.dataCriacao <= :fim
            group by 1
            """)
    List<ContagemTipo> contarPorTipo(Instant inicio, Instant fim, UUID administracao,
                                     String permissaoPlataforma, String permissaoAplicacao);

    @Query("""
            select aplicacao.slug as aplicacao, permissao.codigo as codigo, count(distinct usuario.id) as total
            """ + VINCULOS_ATIVOS + """
            and usuario.dataCriacao >= :inicio and usuario.dataCriacao <= :fim and aplicacao.id <> :administracao
            group by aplicacao.slug, permissao.codigo
            """)
    List<ContagemPermissao> contarPorPermissao(UUID administracao, Instant inicio, Instant fim);

    @Query("""
            select aplicacao.slug as aplicacao, permissao.codigo as codigo, count(usuario) as total
            """ + VINCULOS_PLATAFORMA_ATIVOS + """
            and usuario.dataCriacao >= :inicio and usuario.dataCriacao <= :fim
            group by aplicacao.slug, permissao.codigo
            """)
    List<ContagemPermissao> contarPorPermissaoDaPlataforma(UUID administracao, Instant inicio, Instant fim);

    long countByDataCriacaoBetween(Instant inicio, Instant fim);

    @Query(value = """
            select to_char(data_criacao at time zone 'UTC', 'YYYY-MM') as mes, count(*) as total
            from usuario
            where data_criacao >= :inicio and data_criacao <= :fim
            group by 1
            """, nativeQuery = true)
    List<ContagemMes> contarPorMes(@Param("inicio") Instant inicio, @Param("fim") Instant fim);

    interface PermissaoDoUsuario {
        UUID getIdUsuario();
        UUID getIdAplicacao();
        String getAplicacao();
        String getCodigo();
    }

    interface ContagemStatus {
        StatusUsuario getStatus();
        long getTotal();
    }

    interface ContagemMes {
        String getMes();
        long getTotal();
    }
    interface ContagemTipo {
        String getTipo();
        long getTotal();
    }

    interface ContagemPermissao {
        String getAplicacao();
        String getCodigo();
        long getTotal();
    }
}
