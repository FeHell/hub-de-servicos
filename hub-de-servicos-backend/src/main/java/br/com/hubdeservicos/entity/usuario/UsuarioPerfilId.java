package br.com.hubdeservicos.entity.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UsuarioPerfilId implements Serializable {
    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    @Column(name = "id_perfil", nullable = false)
    private UUID idPerfil;

    protected UsuarioPerfilId() {
    }

    public UsuarioPerfilId(UUID idUsuario, UUID idAplicacao, UUID idPerfil) {
        this.idUsuario = Objects.requireNonNull(idUsuario, "idUsuario");
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
        this.idPerfil = Objects.requireNonNull(idPerfil, "idPerfil");
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public UUID getIdPerfil() {
        return idPerfil;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof UsuarioPerfilId chave)) {
            return false;
        }
        return Objects.equals(idUsuario, chave.idUsuario)
                && Objects.equals(idAplicacao, chave.idAplicacao)
                && Objects.equals(idPerfil, chave.idPerfil);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idAplicacao, idPerfil);
    }
}
