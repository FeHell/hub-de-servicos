package br.com.hubdeservicos.auditoria.entity;

import br.com.hubdeservicos.aplicacao.entity.AplicacaoEntity;
import br.com.hubdeservicos.usuario.entity.UsuarioEntity;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.generator.EventType;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "auditoria")
public class AuditoriaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "id_usuario")
    private UUID idUsuario;

    @Column(name = "id_aplicacao")
    private UUID idAplicacao;

    @Column(name = "acao", nullable = false, length = 100)
    private String acao;

    @Column(name = "tabela_afetada", nullable = false, length = 100)
    private String tabelaAfetada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_antigo", columnDefinition = "jsonb")
    private JsonNode valorAntigo;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "valor_novo", columnDefinition = "jsonb")
    private JsonNode valorNovo;

    @Column(name = "ip_origem", length = 45)
    private String ipOrigem;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_registro", nullable = false, insertable = false, updatable = false)
    private Instant dataRegistro;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", referencedColumnName = "id", insertable = false, updatable = false)
    private UsuarioEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_aplicacao", referencedColumnName = "id", insertable = false, updatable = false)
    private AplicacaoEntity aplicacao;

    protected AuditoriaEntity() {
    }

    public AuditoriaEntity(
            UUID idUsuario,
            UUID idAplicacao,
            String acao,
            String tabelaAfetada,
            JsonNode valorAntigo,
            JsonNode valorNovo,
            String ipOrigem) {
        this.idUsuario = idUsuario;
        this.idAplicacao = idAplicacao;
        this.acao = Objects.requireNonNull(acao, "acao");
        this.tabelaAfetada = Objects.requireNonNull(tabelaAfetada, "tabelaAfetada");
        this.valorAntigo = valorAntigo;
        this.valorNovo = valorNovo;
        this.ipOrigem = ipOrigem;
    }

    public Long getId() {
        return id;
    }

    public UUID getIdUsuario() {
        return idUsuario;
    }

    public UUID getIdAplicacao() {
        return idAplicacao;
    }

    public String getAcao() {
        return acao;
    }

    public String getTabelaAfetada() {
        return tabelaAfetada;
    }

    public JsonNode getValorAntigo() {
        return valorAntigo;
    }

    public JsonNode getValorNovo() {
        return valorNovo;
    }

    public String getIpOrigem() {
        return ipOrigem;
    }

    public Instant getDataRegistro() {
        return dataRegistro;
    }

    public UsuarioEntity getUsuario() {
        return usuario;
    }

    public AplicacaoEntity getAplicacao() {
        return aplicacao;
    }
}
