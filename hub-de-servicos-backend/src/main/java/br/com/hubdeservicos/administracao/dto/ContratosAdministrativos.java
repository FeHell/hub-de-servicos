package br.com.hubdeservicos.administracao.dto;

import br.com.hubdeservicos.aplicacao.entity.AplicacaoEntity;
import br.com.hubdeservicos.aplicacao.entity.TipoAplicacao;
import br.com.hubdeservicos.auditoria.entity.AuditoriaEntity;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.perfil.entity.PerfilEntity;
import br.com.hubdeservicos.permissao.entity.PermissaoEntity;
import br.com.hubdeservicos.sessao.entity.SessaoEntity;
import br.com.hubdeservicos.sessao.entity.StatusSessao;
import br.com.hubdeservicos.usuario.entity.StatusUsuario;
import br.com.hubdeservicos.usuario.entity.UsuarioEntity;
import br.com.hubdeservicos.usuario.entity.UsuarioAplicacaoEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class ContratosAdministrativos {

    private ContratosAdministrativos() {
    }

    public record NovaAplicacao(@NotBlank @Size(max = 255) String nome,
                                @NotBlank @Size(max = 100) @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*") String slug,
                                @NotNull TipoAplicacao tipo, @Size(max = 2000) String descricao) {
    }

    public record AtualizarAplicacao(@NotBlank @Size(max = 255) String nome, @Size(max = 2000) String descricao,
                                     @NotNull StatusCadastro status) {
    }

    public record NovoUsuario(@NotBlank @Size(max = 50) String login,
                              @NotBlank @Email @Size(max = 255) String email,
                              @NotBlank @Size(min = 12, max = 72)
                              @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha, StatusUsuario status,
                              List<@NotBlank @Size(max = 100) String> permissoesPlataforma) {
        public NovoUsuario(String login, String email, String senha) {
            this(login, email, senha, StatusUsuario.ATIVO, null);
        }

        @Override
        public String toString() {
            return "NovoUsuario";
        }
    }

    public record AtualizarUsuario(@NotBlank @Size(max = 50) String login,
                                   @NotBlank @Email @Size(max = 255) String email,
                                   @NotNull StatusUsuario status,
                                   List<@NotBlank @Size(max = 100) String> permissoesPlataforma) {
    }

    public record NovaSenha(@NotBlank @Size(min = 12, max = 72)
                            @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String senha) {
        @Override
        public String toString() {
            return "NovaSenha";
        }
    }

    public record NovaPermissao(@NotBlank @Size(max = 100)
                                @Pattern(regexp = "[A-Z][A-Z0-9_]*") String codigo,
                                @Size(max = 2000) String descricao) {
    }

    public record NovaPermissaoPlataforma(
            @NotBlank @Size(max = 100) @Pattern(regexp = "PLATAFORMA_[A-Z0-9][A-Z0-9_]*") String codigo,
            @NotBlank @Size(max = 100) String nome, @Size(max = 2000) String descricao) {
    }

    public record PermissaoPlataformaInfo(UUID id, String codigo, String nome, String descricao, StatusCadastro status) {
        public static PermissaoPlataformaInfo de(PermissaoEntity permissao) {
            return new PermissaoPlataformaInfo(permissao.getId(), permissao.getCodigo(), permissao.getNomePlataforma(),
                    permissao.getDescricao(), permissao.getStatus());
        }
    }

    public record AtualizarPermissao(@Size(max = 2000) String descricao, @NotNull StatusCadastro status) {
    }

    public record NovoPerfil(@NotBlank @Size(max = 100) String nome, @Size(max = 2000) String descricao) {
    }

    public record AtualizarPerfil(@NotBlank @Size(max = 100) String nome, @Size(max = 2000) String descricao,
                                  @NotNull StatusCadastro status) {
    }

    public record AplicacaoInfo(UUID id, String slug, String nome, TipoAplicacao tipo, String descricao,
                                StatusCadastro status, Instant dataCriacao) {
        public static AplicacaoInfo de(AplicacaoEntity a) {
            return new AplicacaoInfo(a.getId(), a.getSlug(), a.getNome(), a.getTipo(), a.getDescricao(),
                    a.getStatus(), a.getDataCriacao());
        }
    }

    public record UsuarioInfo(UUID id, String login, String email, StatusUsuario status, Instant dataCriacao,
                              List<String> permissoesPlataforma) {
        public static UsuarioInfo de(UsuarioEntity u, List<String> permissoesPlataforma) {
            return new UsuarioInfo(u.getId(), u.getLogin(), u.getEmail(), u.getStatus(), u.getDataCriacao(),
                    permissoesPlataforma);
        }
    }

    public enum TipoAcessoUsuario { PLATAFORMA, APLICACAO, USUARIO }

    public record PermissaoUsuario(String aplicacao, String codigo) {
    }

    public record UsuarioListagem(UUID id, String login, String email, StatusUsuario status, Instant dataCriacao,
                                  TipoAcessoUsuario tipo, List<PermissaoUsuario> permissoes, List<String> permissoesPlataforma) {
        public static UsuarioListagem de(UsuarioEntity u, TipoAcessoUsuario tipo, List<PermissaoUsuario> permissoes,
                                         List<String> permissoesPlataforma) {
            return new UsuarioListagem(u.getId(), u.getLogin(), u.getEmail(), u.getStatus(), u.getDataCriacao(),
                    tipo, permissoes, permissoesPlataforma);
        }
    }

    public record TotalUsuariosStatus(StatusUsuario status, long total) {
    }

    public record TotalUsuariosMes(String mes, long total) {
    }

    public record TotalUsuariosTipo(TipoAcessoUsuario tipo, long total) {
    }

    public record TotalUsuariosPermissao(String aplicacao, String codigo, long total) {
    }

    public record DashboardUsuarios(long total, long recentes, List<TotalUsuariosStatus> porStatus,
                                    List<TotalUsuariosTipo> porTipo, List<TotalUsuariosPermissao> porPermissao,
                                    List<TotalUsuariosMes> porMes) {
    }

    public record PermissaoInfo(UUID id, String codigo, String descricao, StatusCadastro status) {
        public static PermissaoInfo de(PermissaoEntity p) {
            return new PermissaoInfo(p.getId(), p.getCodigo(), p.getDescricao(), p.getStatus());
        }
    }

    public record PerfilInfo(UUID id, UUID idAplicacao, String nome, String descricao, StatusCadastro status) {
        public static PerfilInfo de(PerfilEntity p) {
            return new PerfilInfo(p.getId(), p.getIdAplicacao(), p.getNome(), p.getDescricao(), p.getStatus());
        }
    }

    public record VinculoInfo(UUID idUsuario, boolean ativo) {
        public static VinculoInfo de(UsuarioAplicacaoEntity v) {
            return new VinculoInfo(v.getId().getIdUsuario(), v.isAtivo());
        }
    }

    public record SessaoInfo(UUID id, UUID idUsuario, UUID idAplicacao, StatusSessao status, Instant dataCriacao,
                             Instant expiracaoAbsoluta, Instant expiracaoInatividade, Instant dataRevogacao) {
        public static SessaoInfo de(SessaoEntity s, Instant agora) {
            StatusSessao status = s.getStatus() == StatusSessao.ATIVO && !s.estaAtivaEm(agora)
                    ? StatusSessao.EXPIRADO : s.getStatus();
            return new SessaoInfo(s.getId(), s.getIdUsuario(), s.getIdAplicacao(), status, s.getDataCriacao(),
                    s.getExpiracaoAbsoluta(), s.getExpiracaoInatividade(), s.getDataRevogacao());
        }
    }

    public record AuditoriaInfo(Long id, UUID idOperador, UUID idAplicacao, String acao, String tabela,
                                JsonNode dados, Instant dataRegistro) {
        public static AuditoriaInfo de(AuditoriaEntity a) {
            return new AuditoriaInfo(a.getId(), a.getIdUsuario(), a.getIdAplicacao(), a.getAcao(),
                    a.getTabelaAfetada(), a.getValorNovo(), a.getDataRegistro());
        }
    }

    public record Pagina<T>(List<T> itens, int pagina, int tamanho, long total) {
        public static <T> Pagina<T> de(Page<T> pagina) {
            return new Pagina<>(pagina.getContent(), pagina.getNumber(), pagina.getSize(), pagina.getTotalElements());
        }

        public static PageRequest requisicao(int pagina, int tamanho, Sort ordenacao) {
            if (pagina < 0 || tamanho < 1 || tamanho > 100 || (long) pagina * tamanho > Integer.MAX_VALUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida. Use tamanho de 1 a 100.");
            }
            return PageRequest.of(pagina, tamanho, ordenacao);
        }
    }
}
