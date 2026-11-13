package br.com.hubdeservicos.perfil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class PerfilPermissaoId implements Serializable {
    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    @Column(name = "id_perfil", nullable = false)
    private UUID idPerfil;

    @Column(name = "id_permissao", nullable = false)
    private UUID idPermissao;

    protected PerfilPermissaoId() {
    }

    public PerfilPermissaoId(UUID idAplicacao, UUID idPerfil, UUID idPermissao) {
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
        this.idPerfil = Objects.requireNonNull(idPerfil, "idPerfil");
        this.idPermissao = Objects.requireNonNull(idPermissao, "idPermissao");
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public UUID getIdPerfil() {
        return idPerfil;
    }

    public UUID getIdPermissao() {
        return idPermissao;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof PerfilPermissaoId chave)) {
            return false;
        }
        return Objects.equals(idAplicacao, chave.idAplicacao)
                && Objects.equals(idPerfil, chave.idPerfil)
                && Objects.equals(idPermissao, chave.idPermissao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAplicacao, idPerfil, idPermissao);
    }
}
