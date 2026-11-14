package br.com.hubdeservicos.entity.usuario;

import br.com.hubdeservicos.entity.aplicacao.AplicacaoEntity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "usuario_aplicacao")
public class UsuarioAplicacaoEntity {
    @EmbeddedId
    private UsuarioAplicacaoId id;

    @Column(name = "ativo", nullable = false)
    private boolean ativo = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_usuario", referencedColumnName = "id", insertable = false, updatable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aplicacao", referencedColumnName = "id", insertable = false, updatable = false)
    private AplicacaoEntity aplicacao;

    protected UsuarioAplicacaoEntity() {
    }

    public UsuarioAplicacaoEntity(UsuarioAplicacaoId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public UsuarioAplicacaoId getId() {
        return id;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public AplicacaoEntity getAplicacao() {
        return aplicacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void definirAtivo(boolean ativo) {
        this.ativo = ativo;
    }
}
