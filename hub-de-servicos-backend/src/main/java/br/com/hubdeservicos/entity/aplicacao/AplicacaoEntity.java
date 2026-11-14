package br.com.hubdeservicos.entity.aplicacao;

import br.com.hubdeservicos.common.StatusCadastro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "aplicacao")
public class AplicacaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome", nullable = false, length = 255)
    private String nome;

    @Column(name = "slug", nullable = false, length = 100)
    private String slug;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 50)
    private TipoAplicacao tipo;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusCadastro status;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private Instant dataCriacao;

    protected AplicacaoEntity() {
    }

    public AplicacaoEntity(String nome, String slug, TipoAplicacao tipo, String descricao, StatusCadastro status) {
        this.nome = Objects.requireNonNull(nome, "nome");
        this.slug = Objects.requireNonNull(slug, "slug");
        this.tipo = Objects.requireNonNull(tipo, "tipo");
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status, "status");
    }

    public UUID getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getSlug() {
        return slug;
    }

    public TipoAplicacao getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusCadastro getStatus() {
        return status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public void atualizar(String nome, String descricao, StatusCadastro status) {
        this.nome = Objects.requireNonNull(nome);
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status);
    }
}
