package br.com.hubdeservicos.dto.administracao;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.usuario.SituacaoTelefone;

import br.com.hubdeservicos.dto.usuario.EmailUsuario;
import br.com.hubdeservicos.dto.usuario.SenhaNova;
import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;
import br.com.hubdeservicos.entity.aplicacao.TipoAplicacao;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.perfil.PerfilEntity;
import br.com.hubdeservicos.entity.permissao.PermissaoEntity;
import br.com.hubdeservicos.entity.sessao.SessaoEntity;
import br.com.hubdeservicos.entity.sessao.StatusSessao;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
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

    @Schema(name = "NovaAplicacao")
    public static final class NovaAplicacaoBean {
        @NotBlank
        @Size(max = 255)
        private String nome;

        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[a-z0-9]+(?:-[a-z0-9]+)*")
        private String slug;

        @NotNull
        private TipoAplicacao tipo;

        @Size(max = 2000)
        private String descricao;

        public NovaAplicacaoBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public TipoAplicacao getTipo() {
            return tipo;
        }

        public void setTipo(TipoAplicacao tipo) {
            this.tipo = tipo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovaAplicacaoBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(slug, outro.slug)
                    && Objects.equals(tipo, outro.tipo) && Objects.equals(descricao, outro.descricao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(slug);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovaAplicacao[" + "nome=" + nome + ", slug=" + slug + ", tipo=" + tipo + ", descricao=" + descricao
                    + "]";
        }
    }

    @Schema(name = "AtualizarAplicacao")
    public static final class AtualizarAplicacaoBean {
        @NotBlank
        @Size(max = 255)
        private String nome;

        @Size(max = 2000)
        private String descricao;

        @NotNull
        private StatusCadastro status;

        public AtualizarAplicacaoBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarAplicacaoBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(descricao, outro.descricao)
                    && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarAplicacao[" + "nome=" + nome + ", descricao=" + descricao + ", status=" + status + "]";
        }
    }

    @Schema(name = "NovoUsuario")
    public static final class NovoUsuarioBean {
        @NotBlank
        @Size(max = 50)
        private String login;

        @EmailUsuario
        private String email;

        @SenhaNova
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String senha;

        private StatusUsuario status;

        private List<String> permissoesPlataforma;

        @Size(max = 32)
        private String telefone;

        @Size(max = 150)
        private String nome;

        public NovoUsuarioBean() {
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public List<@NotBlank @Size(max = 100) String> getPermissoesPlataforma() {
            return permissoesPlataforma;
        }

        public void setPermissoesPlataforma(List<String> permissoesPlataforma) {
            this.permissoesPlataforma = permissoesPlataforma;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovoUsuarioBean outro)) {
                return false;
            }
            return Objects.equals(login, outro.login) && Objects.equals(email, outro.email)
                    && Objects.equals(senha, outro.senha) && Objects.equals(status, outro.status)
                    && Objects.equals(permissoesPlataforma, outro.permissoesPlataforma)
                    && Objects.equals(telefone, outro.telefone) && Objects.equals(nome, outro.nome);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(login);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(senha);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(permissoesPlataforma);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(nome);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovoUsuario";
        }
    }

    @Schema(name = "AtualizarUsuario")
    public static final class AtualizarUsuarioBean {
        @NotBlank
        @Size(max = 50)
        private String login;

        @EmailUsuario
        private String email;

        @NotNull
        private StatusUsuario status;

        private List<String> permissoesPlataforma;

        @Size(max = 32)
        private String telefone;

        @Size(max = 150)
        private String nome;

        @Size(max = 200)
        private List<String> permissoesOriginais;

        public AtualizarUsuarioBean() {
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public List<@NotBlank @Size(max = 100) String> getPermissoesPlataforma() {
            return permissoesPlataforma;
        }

        public void setPermissoesPlataforma(List<String> permissoesPlataforma) {
            this.permissoesPlataforma = permissoesPlataforma;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public List<@NotBlank @Size(max = 100) String> getPermissoesOriginais() {
            return permissoesOriginais;
        }

        public void setPermissoesOriginais(List<String> permissoesOriginais) {
            this.permissoesOriginais = permissoesOriginais;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarUsuarioBean outro)) {
                return false;
            }
            return Objects.equals(login, outro.login) && Objects.equals(email, outro.email)
                    && Objects.equals(status, outro.status)
                    && Objects.equals(permissoesPlataforma, outro.permissoesPlataforma)
                    && Objects.equals(telefone, outro.telefone) && Objects.equals(nome, outro.nome)
                    && Objects.equals(permissoesOriginais, outro.permissoesOriginais);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(login);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(permissoesPlataforma);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(permissoesOriginais);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarUsuario[" + "login=" + login + ", email=" + email + ", status=" + status
                    + ", permissoesPlataforma=" + permissoesPlataforma + ", telefone=" + telefone + ", nome=" + nome
                    + ", permissoesOriginais=" + permissoesOriginais + "]";
        }
    }

    @Schema(name = "NovaSenha")
    public static final class NovaSenhaBean {
        @SenhaNova
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        private String senha;

        public NovaSenhaBean() {
        }

        public String getSenha() {
            return senha;
        }

        public void setSenha(String senha) {
            this.senha = senha;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovaSenhaBean outro)) {
                return false;
            }
            return Objects.equals(senha, outro.senha);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(senha);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovaSenha";
        }
    }

    @Schema(name = "NovaPermissao")
    public static final class NovaPermissaoBean {
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[A-Z][A-Z0-9_]*")
        private String codigo;

        @Size(max = 2000)
        private String descricao;

        public NovaPermissaoBean() {
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovaPermissaoBean outro)) {
                return false;
            }
            return Objects.equals(codigo, outro.codigo) && Objects.equals(descricao, outro.descricao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovaPermissao[" + "codigo=" + codigo + ", descricao=" + descricao + "]";
        }
    }

    @Schema(name = "NovaPermissaoPlataforma")
    public static final class NovaPermissaoPlataformaBean {
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "PLATAFORMA_[A-Z0-9][A-Z0-9_]*")
        private String codigo;

        @NotBlank
        @Size(max = 100)
        private String nome;

        @Size(max = 2000)
        private String descricao;

        public NovaPermissaoPlataformaBean() {
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovaPermissaoPlataformaBean outro)) {
                return false;
            }
            return Objects.equals(codigo, outro.codigo) && Objects.equals(nome, outro.nome)
                    && Objects.equals(descricao, outro.descricao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovaPermissaoPlataforma[" + "codigo=" + codigo + ", nome=" + nome + ", descricao=" + descricao
                    + "]";
        }
    }

    @Schema(name = "PermissaoPlataformaInfo")
    public static final class PermissaoPlataformaInfoBean {
        private UUID id;

        private String codigo;

        private String nome;

        private String descricao;

        private StatusCadastro status;

        public PermissaoPlataformaInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PermissaoPlataformaInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(codigo, outro.codigo)
                    && Objects.equals(nome, outro.nome) && Objects.equals(descricao, outro.descricao)
                    && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "PermissaoPlataformaInfo[" + "id=" + id + ", codigo=" + codigo + ", nome=" + nome + ", descricao="
                    + descricao + ", status=" + status + "]";
        }

        public static PermissaoPlataformaInfoBean de(PermissaoEntity permissao) {
            PermissaoPlataformaInfoBean permissaoPlataformaInfoPreparado = new PermissaoPlataformaInfoBean();
            permissaoPlataformaInfoPreparado.setId(permissao.getId());
            permissaoPlataformaInfoPreparado.setCodigo(permissao.getCodigo());
            permissaoPlataformaInfoPreparado.setNome(permissao.getNomePlataforma());
            permissaoPlataformaInfoPreparado.setDescricao(permissao.getDescricao());
            permissaoPlataformaInfoPreparado.setStatus(permissao.getStatus());
            return permissaoPlataformaInfoPreparado;
        }
    }

    @Schema(name = "AtualizarPermissao")
    public static final class AtualizarPermissaoBean {
        @Size(max = 2000)
        private String descricao;

        @NotNull
        private StatusCadastro status;

        public AtualizarPermissaoBean() {
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarPermissaoBean outro)) {
                return false;
            }
            return Objects.equals(descricao, outro.descricao) && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarPermissao[" + "descricao=" + descricao + ", status=" + status + "]";
        }
    }

    @Schema(name = "NovoPerfil")
    public static final class NovoPerfilBean {
        @NotBlank
        @Size(max = 100)
        private String nome;

        @Size(max = 2000)
        private String descricao;

        public NovoPerfilBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof NovoPerfilBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(descricao, outro.descricao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            return resultado;
        }

        @Override
        public String toString() {
            return "NovoPerfil[" + "nome=" + nome + ", descricao=" + descricao + "]";
        }
    }

    @Schema(name = "AtualizarPerfil")
    public static final class AtualizarPerfilBean {
        @NotBlank
        @Size(max = 100)
        private String nome;

        @Size(max = 2000)
        private String descricao;

        @NotNull
        private StatusCadastro status;

        public AtualizarPerfilBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarPerfilBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(descricao, outro.descricao)
                    && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarPerfil[" + "nome=" + nome + ", descricao=" + descricao + ", status=" + status + "]";
        }
    }

    @Schema(name = "AplicacaoInfo")
    public static final class AplicacaoInfoBean {
        private UUID id;

        private String slug;

        private String nome;

        private TipoAplicacao tipo;

        private String descricao;

        private StatusCadastro status;

        private Instant dataCriacao;

        public AplicacaoInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getSlug() {
            return slug;
        }

        public void setSlug(String slug) {
            this.slug = slug;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public TipoAplicacao getTipo() {
            return tipo;
        }

        public void setTipo(TipoAplicacao tipo) {
            this.tipo = tipo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        public Instant getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(Instant dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AplicacaoInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(slug, outro.slug) && Objects.equals(nome, outro.nome)
                    && Objects.equals(tipo, outro.tipo) && Objects.equals(descricao, outro.descricao)
                    && Objects.equals(status, outro.status) && Objects.equals(dataCriacao, outro.dataCriacao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(slug);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(dataCriacao);
            return resultado;
        }

        @Override
        public String toString() {
            return "AplicacaoInfo[" + "id=" + id + ", slug=" + slug + ", nome=" + nome + ", tipo=" + tipo
                    + ", descricao=" + descricao + ", status=" + status + ", dataCriacao=" + dataCriacao + "]";
        }

        public static AplicacaoInfoBean de(AplicacaoEntity a) {
            AplicacaoInfoBean aplicacaoInfoPreparado = new AplicacaoInfoBean();
            aplicacaoInfoPreparado.setId(a.getId());
            aplicacaoInfoPreparado.setSlug(a.getSlug());
            aplicacaoInfoPreparado.setNome(a.getNome());
            aplicacaoInfoPreparado.setTipo(a.getTipo());
            aplicacaoInfoPreparado.setDescricao(a.getDescricao());
            aplicacaoInfoPreparado.setStatus(a.getStatus());
            aplicacaoInfoPreparado.setDataCriacao(a.getDataCriacao());
            return aplicacaoInfoPreparado;
        }
    }

    @Schema(name = "UsuarioInfo")
    public static final class UsuarioInfoBean {
        private UUID id;

        private String login;

        private String email;

        private StatusUsuario status;

        private Instant dataCriacao;

        private List<String> permissoesPlataforma;

        private String telefone;

        private SituacaoTelefone situacaoTelefone;

        private String nome;

        public UsuarioInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public Instant getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(Instant dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        public List<@NotBlank @Size(max = 100) String> getPermissoesPlataforma() {
            return permissoesPlataforma;
        }

        public void setPermissoesPlataforma(List<String> permissoesPlataforma) {
            this.permissoesPlataforma = permissoesPlataforma;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public SituacaoTelefone getSituacaoTelefone() {
            return situacaoTelefone;
        }

        public void setSituacaoTelefone(SituacaoTelefone situacaoTelefone) {
            this.situacaoTelefone = situacaoTelefone;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof UsuarioInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(login, outro.login)
                    && Objects.equals(email, outro.email) && Objects.equals(status, outro.status)
                    && Objects.equals(dataCriacao, outro.dataCriacao)
                    && Objects.equals(permissoesPlataforma, outro.permissoesPlataforma)
                    && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(situacaoTelefone, outro.situacaoTelefone) && Objects.equals(nome, outro.nome);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(login);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(dataCriacao);
            resultado = 31 * resultado + Objects.hashCode(permissoesPlataforma);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(situacaoTelefone);
            resultado = 31 * resultado + Objects.hashCode(nome);
            return resultado;
        }

        @Override
        public String toString() {
            return "UsuarioInfo[" + "id=" + id + ", login=" + login + ", email=" + email + ", status=" + status
                    + ", dataCriacao=" + dataCriacao + ", permissoesPlataforma=" + permissoesPlataforma + ", telefone="
                    + telefone + ", situacaoTelefone=" + situacaoTelefone + ", nome=" + nome + "]";
        }

        public static UsuarioInfoBean de(UsuarioEntity u, List<String> permissoesPlataforma) {
            UsuarioInfoBean usuarioInfoPreparado = new UsuarioInfoBean();
            usuarioInfoPreparado.setId(u.getId());
            usuarioInfoPreparado.setLogin(u.getLogin());
            usuarioInfoPreparado.setEmail(u.getEmail());
            usuarioInfoPreparado.setStatus(u.getStatus());
            usuarioInfoPreparado.setDataCriacao(u.getDataCriacao());
            usuarioInfoPreparado.setPermissoesPlataforma(permissoesPlataforma);
            usuarioInfoPreparado.setTelefone(u.getTelefone());
            usuarioInfoPreparado.setSituacaoTelefone(SituacaoTelefone.de(u));
            usuarioInfoPreparado.setNome(u.getNome());
            return usuarioInfoPreparado;
        }
    }

    public enum TipoAcessoUsuario {
        PLATAFORMA, APLICACAO, USUARIO
    }

    @Schema(name = "PermissaoUsuario")
    public static final class PermissaoUsuarioBean {
        private String aplicacao;

        private String codigo;

        public PermissaoUsuarioBean() {
        }

        public String getAplicacao() {
            return aplicacao;
        }

        public void setAplicacao(String aplicacao) {
            this.aplicacao = aplicacao;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PermissaoUsuarioBean outro)) {
                return false;
            }
            return Objects.equals(aplicacao, outro.aplicacao) && Objects.equals(codigo, outro.codigo);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(aplicacao);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            return resultado;
        }

        @Override
        public String toString() {
            return "PermissaoUsuario[" + "aplicacao=" + aplicacao + ", codigo=" + codigo + "]";
        }
    }

    @Schema(name = "UsuarioListagem")
    public static final class UsuarioListagemBean {
        private UUID id;

        private String login;

        private String email;

        private StatusUsuario status;

        private Instant dataCriacao;

        private TipoAcessoUsuario tipo;

        private List<PermissaoUsuarioBean> permissoes;

        private List<String> permissoesPlataforma;

        private String telefone;

        private SituacaoTelefone situacaoTelefone;

        private String nome;

        public UsuarioListagemBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getLogin() {
            return login;
        }

        public void setLogin(String login) {
            this.login = login;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public Instant getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(Instant dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        public TipoAcessoUsuario getTipo() {
            return tipo;
        }

        public void setTipo(TipoAcessoUsuario tipo) {
            this.tipo = tipo;
        }

        public List<PermissaoUsuarioBean> getPermissoes() {
            return permissoes;
        }

        public void setPermissoes(List<PermissaoUsuarioBean> permissoes) {
            this.permissoes = permissoes;
        }

        public List<@NotBlank @Size(max = 100) String> getPermissoesPlataforma() {
            return permissoesPlataforma;
        }

        public void setPermissoesPlataforma(List<String> permissoesPlataforma) {
            this.permissoesPlataforma = permissoesPlataforma;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public SituacaoTelefone getSituacaoTelefone() {
            return situacaoTelefone;
        }

        public void setSituacaoTelefone(SituacaoTelefone situacaoTelefone) {
            this.situacaoTelefone = situacaoTelefone;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof UsuarioListagemBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(login, outro.login)
                    && Objects.equals(email, outro.email) && Objects.equals(status, outro.status)
                    && Objects.equals(dataCriacao, outro.dataCriacao) && Objects.equals(tipo, outro.tipo)
                    && Objects.equals(permissoes, outro.permissoes)
                    && Objects.equals(permissoesPlataforma, outro.permissoesPlataforma)
                    && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(situacaoTelefone, outro.situacaoTelefone) && Objects.equals(nome, outro.nome);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(login);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(dataCriacao);
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(permissoes);
            resultado = 31 * resultado + Objects.hashCode(permissoesPlataforma);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(situacaoTelefone);
            resultado = 31 * resultado + Objects.hashCode(nome);
            return resultado;
        }

        @Override
        public String toString() {
            return "UsuarioListagem[" + "id=" + id + ", login=" + login + ", email=" + email + ", status=" + status
                    + ", dataCriacao=" + dataCriacao + ", tipo=" + tipo + ", permissoes=" + permissoes
                    + ", permissoesPlataforma=" + permissoesPlataforma + ", telefone=" + telefone
                    + ", situacaoTelefone=" + situacaoTelefone + ", nome=" + nome + "]";
        }

        public static UsuarioListagemBean de(UsuarioEntity u, TipoAcessoUsuario tipo,
                List<PermissaoUsuarioBean> permissoes, List<String> permissoesPlataforma) {
            UsuarioListagemBean usuarioListagemPreparado = new UsuarioListagemBean();
            usuarioListagemPreparado.setId(u.getId());
            usuarioListagemPreparado.setLogin(u.getLogin());
            usuarioListagemPreparado.setEmail(u.getEmail());
            usuarioListagemPreparado.setStatus(u.getStatus());
            usuarioListagemPreparado.setDataCriacao(u.getDataCriacao());
            usuarioListagemPreparado.setTipo(tipo);
            usuarioListagemPreparado.setPermissoes(permissoes);
            usuarioListagemPreparado.setPermissoesPlataforma(permissoesPlataforma);
            usuarioListagemPreparado.setTelefone(u.getTelefone());
            usuarioListagemPreparado.setSituacaoTelefone(SituacaoTelefone.de(u));
            usuarioListagemPreparado.setNome(u.getNome());
            return usuarioListagemPreparado;
        }
    }

    @Schema(name = "TotalUsuariosStatus")
    public static final class TotalUsuariosStatusBean {
        private StatusUsuario status;

        private long total;

        public TotalUsuariosStatusBean() {
        }

        public StatusUsuario getStatus() {
            return status;
        }

        public void setStatus(StatusUsuario status) {
            this.status = status;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TotalUsuariosStatusBean outro)) {
                return false;
            }
            return Objects.equals(status, outro.status) && Objects.equals(total, outro.total);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(total);
            return resultado;
        }

        @Override
        public String toString() {
            return "TotalUsuariosStatus[" + "status=" + status + ", total=" + total + "]";
        }
    }

    @Schema(name = "TotalUsuariosMes")
    public static final class TotalUsuariosMesBean {
        private String mes;

        private long total;

        public TotalUsuariosMesBean() {
        }

        public String getMes() {
            return mes;
        }

        public void setMes(String mes) {
            this.mes = mes;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TotalUsuariosMesBean outro)) {
                return false;
            }
            return Objects.equals(mes, outro.mes) && Objects.equals(total, outro.total);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(mes);
            resultado = 31 * resultado + Objects.hashCode(total);
            return resultado;
        }

        @Override
        public String toString() {
            return "TotalUsuariosMes[" + "mes=" + mes + ", total=" + total + "]";
        }
    }

    @Schema(name = "TotalUsuariosTipo")
    public static final class TotalUsuariosTipoBean {
        private TipoAcessoUsuario tipo;

        private long total;

        public TotalUsuariosTipoBean() {
        }

        public TipoAcessoUsuario getTipo() {
            return tipo;
        }

        public void setTipo(TipoAcessoUsuario tipo) {
            this.tipo = tipo;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TotalUsuariosTipoBean outro)) {
                return false;
            }
            return Objects.equals(tipo, outro.tipo) && Objects.equals(total, outro.total);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(tipo);
            resultado = 31 * resultado + Objects.hashCode(total);
            return resultado;
        }

        @Override
        public String toString() {
            return "TotalUsuariosTipo[" + "tipo=" + tipo + ", total=" + total + "]";
        }
    }

    @Schema(name = "TotalUsuariosPermissao")
    public static final class TotalUsuariosPermissaoBean {
        private String aplicacao;

        private String codigo;

        private long total;

        public TotalUsuariosPermissaoBean() {
        }

        public String getAplicacao() {
            return aplicacao;
        }

        public void setAplicacao(String aplicacao) {
            this.aplicacao = aplicacao;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TotalUsuariosPermissaoBean outro)) {
                return false;
            }
            return Objects.equals(aplicacao, outro.aplicacao) && Objects.equals(codigo, outro.codigo)
                    && Objects.equals(total, outro.total);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(aplicacao);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(total);
            return resultado;
        }

        @Override
        public String toString() {
            return "TotalUsuariosPermissao[" + "aplicacao=" + aplicacao + ", codigo=" + codigo + ", total=" + total
                    + "]";
        }
    }

    @Schema(name = "DashboardUsuarios")
    public static final class DashboardUsuariosBean {
        private long total;

        private long recentes;

        private List<TotalUsuariosStatusBean> porStatus;

        private List<TotalUsuariosTipoBean> porTipo;

        private List<TotalUsuariosPermissaoBean> porPermissao;

        private List<TotalUsuariosMesBean> porMes;

        public DashboardUsuariosBean() {
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        public long getRecentes() {
            return recentes;
        }

        public void setRecentes(long recentes) {
            this.recentes = recentes;
        }

        public List<TotalUsuariosStatusBean> getPorStatus() {
            return porStatus;
        }

        public void setPorStatus(List<TotalUsuariosStatusBean> porStatus) {
            this.porStatus = porStatus;
        }

        public List<TotalUsuariosTipoBean> getPorTipo() {
            return porTipo;
        }

        public void setPorTipo(List<TotalUsuariosTipoBean> porTipo) {
            this.porTipo = porTipo;
        }

        public List<TotalUsuariosPermissaoBean> getPorPermissao() {
            return porPermissao;
        }

        public void setPorPermissao(List<TotalUsuariosPermissaoBean> porPermissao) {
            this.porPermissao = porPermissao;
        }

        public List<TotalUsuariosMesBean> getPorMes() {
            return porMes;
        }

        public void setPorMes(List<TotalUsuariosMesBean> porMes) {
            this.porMes = porMes;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof DashboardUsuariosBean outro)) {
                return false;
            }
            return Objects.equals(total, outro.total) && Objects.equals(recentes, outro.recentes)
                    && Objects.equals(porStatus, outro.porStatus) && Objects.equals(porTipo, outro.porTipo)
                    && Objects.equals(porPermissao, outro.porPermissao) && Objects.equals(porMes, outro.porMes);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(total);
            resultado = 31 * resultado + Objects.hashCode(recentes);
            resultado = 31 * resultado + Objects.hashCode(porStatus);
            resultado = 31 * resultado + Objects.hashCode(porTipo);
            resultado = 31 * resultado + Objects.hashCode(porPermissao);
            resultado = 31 * resultado + Objects.hashCode(porMes);
            return resultado;
        }

        @Override
        public String toString() {
            return "DashboardUsuarios[" + "total=" + total + ", recentes=" + recentes + ", porStatus=" + porStatus
                    + ", porTipo=" + porTipo + ", porPermissao=" + porPermissao + ", porMes=" + porMes + "]";
        }
    }

    @Schema(name = "PermissaoInfo")
    public static final class PermissaoInfoBean {
        private UUID id;

        private String codigo;

        private String descricao;

        private StatusCadastro status;

        public PermissaoInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PermissaoInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(codigo, outro.codigo)
                    && Objects.equals(descricao, outro.descricao) && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "PermissaoInfo[" + "id=" + id + ", codigo=" + codigo + ", descricao=" + descricao + ", status="
                    + status + "]";
        }

        public static PermissaoInfoBean de(PermissaoEntity p) {
            PermissaoInfoBean permissaoInfoPreparado = new PermissaoInfoBean();
            permissaoInfoPreparado.setId(p.getId());
            permissaoInfoPreparado.setCodigo(p.getCodigo());
            permissaoInfoPreparado.setDescricao(p.getDescricao());
            permissaoInfoPreparado.setStatus(p.getStatus());
            return permissaoInfoPreparado;
        }
    }

    @Schema(name = "PerfilInfo")
    public static final class PerfilInfoBean {
        private UUID id;

        private UUID idAplicacao;

        private String nome;

        private String descricao;

        private StatusCadastro status;

        public PerfilInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getIdAplicacao() {
            return idAplicacao;
        }

        public void setIdAplicacao(UUID idAplicacao) {
            this.idAplicacao = idAplicacao;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }

        public StatusCadastro getStatus() {
            return status;
        }

        public void setStatus(StatusCadastro status) {
            this.status = status;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PerfilInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(idAplicacao, outro.idAplicacao)
                    && Objects.equals(nome, outro.nome) && Objects.equals(descricao, outro.descricao)
                    && Objects.equals(status, outro.status);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(idAplicacao);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(descricao);
            resultado = 31 * resultado + Objects.hashCode(status);
            return resultado;
        }

        @Override
        public String toString() {
            return "PerfilInfo[" + "id=" + id + ", idAplicacao=" + idAplicacao + ", nome=" + nome + ", descricao="
                    + descricao + ", status=" + status + "]";
        }

        public static PerfilInfoBean de(PerfilEntity p) {
            PerfilInfoBean perfilInfoPreparado = new PerfilInfoBean();
            perfilInfoPreparado.setId(p.getId());
            perfilInfoPreparado.setIdAplicacao(p.getIdAplicacao());
            perfilInfoPreparado.setNome(p.getNome());
            perfilInfoPreparado.setDescricao(p.getDescricao());
            perfilInfoPreparado.setStatus(p.getStatus());
            return perfilInfoPreparado;
        }
    }

    @Schema(name = "VinculoInfo")
    public static final class VinculoInfoBean {
        private UUID idUsuario;

        private boolean ativo;

        public VinculoInfoBean() {
        }

        public UUID getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(UUID idUsuario) {
            this.idUsuario = idUsuario;
        }

        public boolean getAtivo() {
            return ativo;
        }

        public void setAtivo(boolean ativo) {
            this.ativo = ativo;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof VinculoInfoBean outro)) {
                return false;
            }
            return Objects.equals(idUsuario, outro.idUsuario) && Objects.equals(ativo, outro.ativo);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(idUsuario);
            resultado = 31 * resultado + Objects.hashCode(ativo);
            return resultado;
        }

        @Override
        public String toString() {
            return "VinculoInfo[" + "idUsuario=" + idUsuario + ", ativo=" + ativo + "]";
        }

        public static VinculoInfoBean de(UsuarioAplicacaoEntity v) {
            VinculoInfoBean vinculoInfoPreparado = new VinculoInfoBean();
            vinculoInfoPreparado.setIdUsuario(v.getId().getIdUsuario());
            vinculoInfoPreparado.setAtivo(v.isAtivo());
            return vinculoInfoPreparado;
        }
    }

    @Schema(name = "SessaoInfo")
    public static final class SessaoInfoBean {
        private UUID id;

        private UUID idUsuario;

        private UUID idAplicacao;

        private StatusSessao status;

        private Instant dataCriacao;

        private Instant expiracaoAbsoluta;

        private Instant expiracaoInatividade;

        private Instant dataRevogacao;

        public SessaoInfoBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public UUID getIdUsuario() {
            return idUsuario;
        }

        public void setIdUsuario(UUID idUsuario) {
            this.idUsuario = idUsuario;
        }

        public UUID getIdAplicacao() {
            return idAplicacao;
        }

        public void setIdAplicacao(UUID idAplicacao) {
            this.idAplicacao = idAplicacao;
        }

        public StatusSessao getStatus() {
            return status;
        }

        public void setStatus(StatusSessao status) {
            this.status = status;
        }

        public Instant getDataCriacao() {
            return dataCriacao;
        }

        public void setDataCriacao(Instant dataCriacao) {
            this.dataCriacao = dataCriacao;
        }

        public Instant getExpiracaoAbsoluta() {
            return expiracaoAbsoluta;
        }

        public void setExpiracaoAbsoluta(Instant expiracaoAbsoluta) {
            this.expiracaoAbsoluta = expiracaoAbsoluta;
        }

        public Instant getExpiracaoInatividade() {
            return expiracaoInatividade;
        }

        public void setExpiracaoInatividade(Instant expiracaoInatividade) {
            this.expiracaoInatividade = expiracaoInatividade;
        }

        public Instant getDataRevogacao() {
            return dataRevogacao;
        }

        public void setDataRevogacao(Instant dataRevogacao) {
            this.dataRevogacao = dataRevogacao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof SessaoInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(idUsuario, outro.idUsuario)
                    && Objects.equals(idAplicacao, outro.idAplicacao) && Objects.equals(status, outro.status)
                    && Objects.equals(dataCriacao, outro.dataCriacao)
                    && Objects.equals(expiracaoAbsoluta, outro.expiracaoAbsoluta)
                    && Objects.equals(expiracaoInatividade, outro.expiracaoInatividade)
                    && Objects.equals(dataRevogacao, outro.dataRevogacao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(idUsuario);
            resultado = 31 * resultado + Objects.hashCode(idAplicacao);
            resultado = 31 * resultado + Objects.hashCode(status);
            resultado = 31 * resultado + Objects.hashCode(dataCriacao);
            resultado = 31 * resultado + Objects.hashCode(expiracaoAbsoluta);
            resultado = 31 * resultado + Objects.hashCode(expiracaoInatividade);
            resultado = 31 * resultado + Objects.hashCode(dataRevogacao);
            return resultado;
        }

        @Override
        public String toString() {
            return "SessaoInfo[" + "id=" + id + ", idUsuario=" + idUsuario + ", idAplicacao=" + idAplicacao
                    + ", status=" + status + ", dataCriacao=" + dataCriacao + ", expiracaoAbsoluta=" + expiracaoAbsoluta
                    + ", expiracaoInatividade=" + expiracaoInatividade + ", dataRevogacao=" + dataRevogacao + "]";
        }

        public static SessaoInfoBean de(SessaoEntity s, Instant agora) {
            StatusSessao status = s.getStatus() == StatusSessao.ATIVO && !s.estaAtivaEm(agora) ? StatusSessao.EXPIRADO
                    : s.getStatus();
            SessaoInfoBean sessaoInfoPreparado = new SessaoInfoBean();
            sessaoInfoPreparado.setId(s.getId());
            sessaoInfoPreparado.setIdUsuario(s.getIdUsuario());
            sessaoInfoPreparado.setIdAplicacao(s.getIdAplicacao());
            sessaoInfoPreparado.setStatus(status);
            sessaoInfoPreparado.setDataCriacao(s.getDataCriacao());
            sessaoInfoPreparado.setExpiracaoAbsoluta(s.getExpiracaoAbsoluta());
            sessaoInfoPreparado.setExpiracaoInatividade(s.getExpiracaoInatividade());
            sessaoInfoPreparado.setDataRevogacao(s.getDataRevogacao());
            return sessaoInfoPreparado;
        }
    }

    @Schema(name = "AuditoriaInfo")
    public static final class AuditoriaInfoBean {
        private Long id;

        private UUID idOperador;

        private UUID idAplicacao;

        private String acao;

        private String tabela;

        private JsonNode dados;

        private Instant dataRegistro;

        public AuditoriaInfoBean() {
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public UUID getIdOperador() {
            return idOperador;
        }

        public void setIdOperador(UUID idOperador) {
            this.idOperador = idOperador;
        }

        public UUID getIdAplicacao() {
            return idAplicacao;
        }

        public void setIdAplicacao(UUID idAplicacao) {
            this.idAplicacao = idAplicacao;
        }

        public String getAcao() {
            return acao;
        }

        public void setAcao(String acao) {
            this.acao = acao;
        }

        public String getTabela() {
            return tabela;
        }

        public void setTabela(String tabela) {
            this.tabela = tabela;
        }

        public JsonNode getDados() {
            return dados;
        }

        public void setDados(JsonNode dados) {
            this.dados = dados;
        }

        public Instant getDataRegistro() {
            return dataRegistro;
        }

        public void setDataRegistro(Instant dataRegistro) {
            this.dataRegistro = dataRegistro;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AuditoriaInfoBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(idOperador, outro.idOperador)
                    && Objects.equals(idAplicacao, outro.idAplicacao) && Objects.equals(acao, outro.acao)
                    && Objects.equals(tabela, outro.tabela) && Objects.equals(dados, outro.dados)
                    && Objects.equals(dataRegistro, outro.dataRegistro);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(idOperador);
            resultado = 31 * resultado + Objects.hashCode(idAplicacao);
            resultado = 31 * resultado + Objects.hashCode(acao);
            resultado = 31 * resultado + Objects.hashCode(tabela);
            resultado = 31 * resultado + Objects.hashCode(dados);
            resultado = 31 * resultado + Objects.hashCode(dataRegistro);
            return resultado;
        }

        @Override
        public String toString() {
            return "AuditoriaInfo[" + "id=" + id + ", idOperador=" + idOperador + ", idAplicacao=" + idAplicacao
                    + ", acao=" + acao + ", tabela=" + tabela + ", dados=" + dados + ", dataRegistro=" + dataRegistro
                    + "]";
        }

        public static AuditoriaInfoBean de(AuditoriaEntity a) {
            AuditoriaInfoBean auditoriaInfoPreparado = new AuditoriaInfoBean();
            auditoriaInfoPreparado.setId(a.getId());
            auditoriaInfoPreparado.setIdOperador(a.getIdUsuario());
            auditoriaInfoPreparado.setIdAplicacao(a.getIdAplicacao());
            auditoriaInfoPreparado.setAcao(a.getAcao());
            auditoriaInfoPreparado.setTabela(a.getTabelaAfetada());
            auditoriaInfoPreparado.setDados(a.getValorNovo());
            auditoriaInfoPreparado.setDataRegistro(a.getDataRegistro());
            return auditoriaInfoPreparado;
        }
    }

    @Schema(name = "Pagina")
    public static final class PaginaBean<T> {
        private List<T> itens;

        private int pagina;

        private int tamanho;

        private long total;

        public PaginaBean() {
        }

        public List<T> getItens() {
            return itens;
        }

        public void setItens(List<T> itens) {
            this.itens = itens;
        }

        public int getPagina() {
            return pagina;
        }

        public void setPagina(int pagina) {
            this.pagina = pagina;
        }

        public int getTamanho() {
            return tamanho;
        }

        public void setTamanho(int tamanho) {
            this.tamanho = tamanho;
        }

        public long getTotal() {
            return total;
        }

        public void setTotal(long total) {
            this.total = total;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof PaginaBean<?> outro)) {
                return false;
            }
            return Objects.equals(itens, outro.itens) && Objects.equals(pagina, outro.pagina)
                    && Objects.equals(tamanho, outro.tamanho) && Objects.equals(total, outro.total);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(itens);
            resultado = 31 * resultado + Objects.hashCode(pagina);
            resultado = 31 * resultado + Objects.hashCode(tamanho);
            resultado = 31 * resultado + Objects.hashCode(total);
            return resultado;
        }

        @Override
        public String toString() {
            return "Pagina[" + "itens=" + itens + ", pagina=" + pagina + ", tamanho=" + tamanho + ", total=" + total
                    + "]";
        }

        public static <T> PaginaBean<T> de(Page<T> pagina) {
            PaginaBean<T> paginaPreparado = new PaginaBean<T>();
            paginaPreparado.setItens(pagina.getContent());
            paginaPreparado.setPagina(pagina.getNumber());
            paginaPreparado.setTamanho(pagina.getSize());
            paginaPreparado.setTotal(pagina.getTotalElements());
            return paginaPreparado;
        }

        public static PageRequest requisicao(int pagina, int tamanho, Sort ordenacao) {
            if (pagina < 0 || tamanho < 1 || tamanho > 100 || (long) pagina * tamanho > Integer.MAX_VALUE) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Paginação inválida. Use tamanho de 1 a 100.");
            }
            return PageRequest.of(pagina, tamanho, ordenacao);
        }
    }
}
