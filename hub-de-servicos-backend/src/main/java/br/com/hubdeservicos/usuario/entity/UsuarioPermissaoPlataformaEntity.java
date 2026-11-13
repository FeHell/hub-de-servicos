package br.com.hubdeservicos.usuario.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuario_permissao_plataforma")
public class UsuarioPermissaoPlataformaEntity {
    @EmbeddedId
    private UsuarioPermissaoPlataformaId id;

    protected UsuarioPermissaoPlataformaEntity() {
    }

    public UsuarioPermissaoPlataformaEntity(UsuarioPermissaoPlataformaId id) {
        this.id = id;
    }

    public UsuarioPermissaoPlataformaId getId() {
        return id;
    }
}
