package br.com.hubdeservicos.perfil.entity;

import br.com.hubdeservicos.aplicacao.entity.AplicacaoEntity;
import br.com.hubdeservicos.common.StatusCadastro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "perfil")
public class PerfilEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_perfil", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    @Column(name = "nome", nullable = false, length = 100)
    private String nome;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusCadastro status;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_aplicacao", referencedColumnName = "id", insertable = false, updatable = false)
    private AplicacaoEntity aplicacao;

    protected PerfilEntity() {
    }

    public PerfilEntity(UUID idAplicacao, String nome, String descricao, StatusCadastro status) {
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
        this.nome = Objects.requireNonNull(nome, "nome");
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status, "status");
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public String getNome() {
        return nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusCadastro getStatus() {
        return status;
    }

    public AplicacaoEntity getAplicacao() {
        return aplicacao;
    }

    public void atualizar(String nome, String descricao, StatusCadastro status) {
        this.nome = Objects.requireNonNull(nome);
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status);
    }
}
