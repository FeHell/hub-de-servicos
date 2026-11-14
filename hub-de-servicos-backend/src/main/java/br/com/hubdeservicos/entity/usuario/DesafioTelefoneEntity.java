package br.com.hubdeservicos.entity.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "usuario_desafio_telefone")
public class DesafioTelefoneEntity {
    @Id
    private UUID id;

    @Column(name = "id_usuario", nullable = false)
    private UUID idUsuario;

    @Column(nullable = false, length = 16)
    private String telefone;

    @Column(name = "codigo_hash", length = 100)
    private String codigoHash;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(nullable = false)
    private boolean enviado;

    @Column(nullable = false)
    private int tentativas;

    protected DesafioTelefoneEntity() {
    }

    public DesafioTelefoneEntity(UUID id, UUID idUsuario, String telefone, String codigoHash, Instant criadoEm, Instant expiraEm) {
        this.id = id;
        this.idUsuario = idUsuario;
        this.telefone = telefone;
        this.codigoHash = codigoHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public UUID getId() { return id; }
    public UUID getIdUsuario() { return idUsuario; }
    public String getTelefone() { return telefone; }
    public String getCodigoHash() { return codigoHash; }
    public Instant getExpiraEm() { return expiraEm; }
    public boolean isEnviado() { return enviado; }
    public int getTentativas() { return tentativas; }

    public void registrarEnvio() { enviado = true; }
    public void consumir() { codigoHash = null; }
    public void registrarErro() { tentativas++; }
}
