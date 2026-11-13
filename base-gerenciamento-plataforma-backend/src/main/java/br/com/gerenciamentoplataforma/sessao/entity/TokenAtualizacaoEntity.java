package br.com.gerenciamentoplataforma.sessao.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinColumns;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "tokens_atualizacao")
public class TokenAtualizacaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_sessao", nullable = false)
    private UUID idSessao;

    @Column(name = "id_familia", nullable = false)
    private UUID idFamilia;

    @JsonIgnore
    @Column(name = "hash_token", nullable = false, length = 255)
    private String hashToken;

    @Column(name = "id_anterior")
    private UUID idAnterior;

    @Column(name = "data_utilizacao")
    private Instant dataUtilizacao;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private Instant dataCriacao;

    @Column(name = "expiracao_absoluta", nullable = false)
    private Instant expiracaoAbsoluta;

    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    @Column(name = "is_revogado", nullable = false)
    private boolean revogado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_sessao", referencedColumnName = "id", insertable = false, updatable = false),
        @JoinColumn(name = "id_familia", referencedColumnName = "id_familia", insertable = false, updatable = false)
    })
    private SessaoEntity sessao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
        @JoinColumn(name = "id_anterior", referencedColumnName = "id", insertable = false, updatable = false),
        @JoinColumn(name = "id_familia", referencedColumnName = "id_familia", insertable = false, updatable = false)
    })
    private TokenAtualizacaoEntity anterior;

    protected TokenAtualizacaoEntity() {
    }

    public TokenAtualizacaoEntity(
            UUID idSessao,
            UUID idFamilia,
            String hashToken,
            UUID idAnterior,
            Instant expiracaoAbsoluta) {
        this.idSessao = Objects.requireNonNull(idSessao, "idSessao");
        this.idFamilia = Objects.requireNonNull(idFamilia, "idFamilia");
        this.hashToken = Objects.requireNonNull(hashToken, "hashToken");
        this.idAnterior = idAnterior;
        this.expiracaoAbsoluta = Objects.requireNonNull(expiracaoAbsoluta, "expiracaoAbsoluta");
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdSessao() {
        return idSessao;
    }

    public UUID getIdFamilia() {
        return idFamilia;
    }

    public String getHashToken() {
        return hashToken;
    }

    public UUID getIdAnterior() {
        return idAnterior;
    }

    public Instant getDataUtilizacao() {
        return dataUtilizacao;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getExpiracaoAbsoluta() {
        return expiracaoAbsoluta;
    }

    public Instant getDataRevogacao() {
        return dataRevogacao;
    }

    public boolean isRevogado() {
        return revogado;
    }

    public SessaoEntity getSessao() {
        return sessao;
    }

    public TokenAtualizacaoEntity getAnterior() {
        return anterior;
    }

    public void registrarUtilizacao(Instant agora) {
        dataUtilizacao = agora;
    }
}
