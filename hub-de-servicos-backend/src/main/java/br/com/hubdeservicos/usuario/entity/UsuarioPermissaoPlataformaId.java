package br.com.hubdeservicos.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UsuarioPermissaoPlataformaId implements Serializable {
    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_permissao", nullable = false)
    private UUID idPermissao;

    protected UsuarioPermissaoPlataformaId() {
    }

    public UsuarioPermissaoPlataformaId(UUID idUsuario, UUID idPermissao) {
        this.idUsuario = Objects.requireNonNull(idUsuario, "idUsuario");
        this.idPermissao = Objects.requireNonNull(idPermissao, "idPermissao");
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdPermissao() {
        return idPermissao;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof UsuarioPermissaoPlataformaId chave)) {
            return false;
        }
        return Objects.equals(idUsuario, chave.idUsuario)
                && Objects.equals(idPermissao, chave.idPermissao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idPermissao);
    }
}
