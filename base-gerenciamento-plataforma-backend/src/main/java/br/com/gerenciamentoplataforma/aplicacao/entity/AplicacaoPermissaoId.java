package br.com.gerenciamentoplataforma.aplicacao.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class AplicacaoPermissaoId implements Serializable {
    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    @Column(name = "id_permissao", nullable = false)
    private UUID idPermissao;

    protected AplicacaoPermissaoId() {
    }

    public AplicacaoPermissaoId(UUID idAplicacao, UUID idPermissao) {
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
        this.idPermissao = Objects.requireNonNull(idPermissao, "idPermissao");
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public UUID getIdPermissao() {
        return idPermissao;
    }

    @Override
    public boolean equals(Object outro) {
        if (this == outro) {
            return true;
        }
        if (!(outro instanceof AplicacaoPermissaoId chave)) {
            return false;
        }
        return Objects.equals(idAplicacao, chave.idAplicacao)
                && Objects.equals(idPermissao, chave.idPermissao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idAplicacao, idPermissao);
    }
}
