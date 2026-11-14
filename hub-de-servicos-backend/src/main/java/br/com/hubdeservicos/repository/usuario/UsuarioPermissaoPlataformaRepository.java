package br.com.hubdeservicos.repository.usuario;

import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaId;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioPermissaoPlataformaRepository
        extends JpaRepository<UsuarioPermissaoPlataformaEntity, UsuarioPermissaoPlataformaId> {

    String PERMISSOES_ATRIBUIDAS = """
            from UsuarioPermissaoPlataformaEntity concessao
            join PermissaoEntity permissao on permissao.id = concessao.id.idPermissao
            where permissao.nomePlataforma is not null
            """;

    List<UsuarioPermissaoPlataformaEntity> findByIdIdUsuario(UUID idUsuario);

    @Query("select concessao.id.idUsuario as idUsuario, permissao.codigo as codigo " + PERMISSOES_ATRIBUIDAS + """
            and concessao.id.idUsuario in :usuarios
            order by permissao.codigo
            """)
    List<Atribuicao> listarAtribuicoes(List<UUID> usuarios);

    @Query("select permissao.codigo " + PERMISSOES_ATRIBUIDAS + """
            and concessao.id.idUsuario = :idUsuario
            and permissao.status = br.com.hubdeservicos.common.StatusCadastro.ATIVO
            order by permissao.codigo
            """)
    List<String> listarCodigosAtivos(UUID idUsuario);

    interface Atribuicao {
        UUID getIdUsuario();
        String getCodigo();
    }
}
