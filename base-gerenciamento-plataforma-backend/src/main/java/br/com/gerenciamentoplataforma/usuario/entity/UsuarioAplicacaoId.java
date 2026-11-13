package br.com.gerenciamentoplataforma.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class UsuarioAplicacaoId implements Serializable {
    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    protected UsuarioAplicacaoId() {
    }

    public UsuarioAplicacaoId(UUID idUsuario, UUID idAplicacao) {
        this.idUsuario = Objects.requireNonNull(idUsuario, "idUsuario");
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof UsuarioAplicacaoId chave)) {
            return false;
        }
        return Objects.equals(idUsuario, chave.idUsuario)
                && Objects.equals(idAplicacao, chave.idAplicacao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idUsuario, idAplicacao);
    }
}
