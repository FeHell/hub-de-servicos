package br.com.hubdeservicos.entity.usuario;

import br.com.hubdeservicos.entity.perfil.PerfilEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "usuario_perfil")
public class UsuarioPerfilEntity {
    @EmbeddedId
    private UsuarioPerfilId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", insertable = false, updatable = false),
        @JoinColumn(name = "id_aplicacao", referencedColumnName = "id_aplicacao", insertable = false, updatable = false)
    })
    private UsuarioAplicacaoEntity usuarioAplicacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_aplicacao", referencedColumnName = "id_aplicacao", insertable = false, updatable = false),
        @JoinColumn(name = "id_perfil", referencedColumnName = "id_perfil", insertable = false, updatable = false)
    })
    private PerfilEntity perfil;

    protected UsuarioPerfilEntity() {
    }

    public UsuarioPerfilEntity(UsuarioPerfilId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public UsuarioPerfilId getId() {
        return id;
    }

    public UsuarioAplicacaoEntity getUsuarioAplicacao() {
        return usuarioAplicacao;
    }

    public PerfilEntity getPerfil() {
        return perfil;
    }
}
