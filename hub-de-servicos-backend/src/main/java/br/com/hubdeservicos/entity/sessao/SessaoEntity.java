package br.com.hubdeservicos.entity.sessao;

import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "sessoes")
public class SessaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(name = "id_aplicacao", nullable = false)
    private UUID idAplicacao;

    @Column(name = "id_familia", nullable = false)
    private UUID idFamilia;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusSessao status;

    @Column(name = "expiracao_absoluta", nullable = false)
    private Instant expiracaoAbsoluta;

    @Column(name = "expiracao_inatividade", nullable = false)
    private Instant expiracaoInatividade;

    @Column(name = "data_ultima_utilizacao", nullable = false)
    private Instant dataUltimaUtilizacao;

    @Column(name = "data_revogacao")
    private Instant dataRevogacao;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private Instant dataCriacao;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
        @JoinColumn(name = "id_usuario", referencedColumnName = "id_usuario", insertable = false, updatable = false),
        @JoinColumn(name = "id_aplicacao", referencedColumnName = "id_aplicacao", insertable = false, updatable = false)
    })
    private UsuarioAplicacaoEntity usuarioAplicacao;

    protected SessaoEntity() {
    }

    public SessaoEntity(
            UUID idUsuario,
            UUID idAplicacao,
            UUID idFamilia,
            StatusSessao status,
            Instant expiracaoAbsoluta,
            Instant expiracaoInatividade,
            Instant dataUltimaUtilizacao) {
        this.idUsuario = Objects.requireNonNull(idUsuario, "idUsuario");
        this.idAplicacao = Objects.requireNonNull(idAplicacao, "idAplicacao");
        this.idFamilia = Objects.requireNonNull(idFamilia, "idFamilia");
        this.status = Objects.requireNonNull(status, "status");
        this.expiracaoAbsoluta = Objects.requireNonNull(expiracaoAbsoluta, "expiracaoAbsoluta");
        this.expiracaoInatividade = Objects.requireNonNull(expiracaoInatividade, "expiracaoInatividade");
        this.dataUltimaUtilizacao = Objects.requireNonNull(dataUltimaUtilizacao, "dataUltimaUtilizacao");
    }

    public UUID getId() {
        return id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public UUID getIdFamilia() {
        return idFamilia;
    }

    public StatusSessao getStatus() {
        return status;
    }

    public Instant getExpiracaoAbsoluta() {
        return expiracaoAbsoluta;
    }

    public Instant getExpiracaoInatividade() {
        return expiracaoInatividade;
    }

    public Instant getDataUltimaUtilizacao() {
        return dataUltimaUtilizacao;
    }

    public Instant getDataRevogacao() {
        return dataRevogacao;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public UsuarioAplicacaoEntity getUsuarioAplicacao() {
        return usuarioAplicacao;
    }

    public boolean estaAtivaEm(Instant agora) {
        return status == StatusSessao.ATIVO
                && expiracaoAbsoluta.isAfter(agora)
                && expiracaoInatividade.isAfter(agora);
    }

    public void registrarRenovacao(Instant agora, Instant novaExpiracaoInatividade) {
        dataUltimaUtilizacao = agora;
        expiracaoInatividade = novaExpiracaoInatividade;
    }

    public void expirar() {
        status = StatusSessao.EXPIRADO;
    }

    public void revogar(Instant agora) {
        status = StatusSessao.REVOGADO;
        dataRevogacao = agora;
    }
}
