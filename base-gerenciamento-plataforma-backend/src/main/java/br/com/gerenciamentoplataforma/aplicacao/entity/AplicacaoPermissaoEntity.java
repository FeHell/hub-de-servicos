package br.com.gerenciamentoplataforma.aplicacao.entity;

import br.com.gerenciamentoplataforma.permissao.entity.PermissaoEntity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "aplicacao_permissao")
public class AplicacaoPermissaoEntity {
    @EmbeddedId
    private AplicacaoPermissaoId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aplicacao", referencedColumnName = "id", insertable = false, updatable = false)
    private AplicacaoEntity aplicacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_permissao", referencedColumnName = "id", insertable = false, updatable = false)
    private PermissaoEntity permissao;

    protected AplicacaoPermissaoEntity() {
    }

    public AplicacaoPermissaoEntity(AplicacaoPermissaoId id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public AplicacaoPermissaoId getId() {
        return id;
    }

    public AplicacaoEntity getAplicacao() {
        return aplicacao;
    }

    public PermissaoEntity getPermissao() {
        return permissao;
    }
}
