package br.com.hubdeservicos.entity.credenciamento;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credenciamento_empresarial")
public class CredenciamentoEntity {
    @Id
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "id_solicitante", updatable = false, nullable = false)
    private UUID solicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "provedor", updatable = false, nullable = false, length = 50)
    private ProvedorCredenciamento provedor;

    @Column(name = "dados_protegidos", nullable = false, columnDefinition = "text")
    private String dadosProtegidos;

    @Column(name = "data_criacao", updatable = false, nullable = false)
    private Instant criadoEm;

    @Column(name = "data_atualizacao", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "versao", nullable = false)
    private long versao;

    @Column(name = "excluido_em")
    private Instant excluidoEm;

    protected CredenciamentoEntity() {
    }

    public CredenciamentoEntity(UUID id, UUID solicitante, ProvedorCredenciamento provedor, String dados,
            Instant agora) {
        this.id = id;
        this.solicitante = solicitante;
        this.provedor = provedor;
        this.criadoEm = agora;
        this.dadosProtegidos = dados;
        this.atualizadoEm = agora;
    }

    public void atualizar(String dados, Instant agora) {
        dadosProtegidos = dados;
        atualizadoEm = agora;
        versao++;
    }

    public void excluir(String dadosVaziosProtegidos, Instant agora) {
        atualizar(dadosVaziosProtegidos, agora);
        excluidoEm = agora;
    }

    public boolean excluido() {
        return excluidoEm != null;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSolicitante() {
        return solicitante;
    }

    public ProvedorCredenciamento getProvedor() {
        return provedor;
    }

    public String getDadosProtegidos() {
        return dadosProtegidos;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public long versao() {
        return versao;
    }
}
