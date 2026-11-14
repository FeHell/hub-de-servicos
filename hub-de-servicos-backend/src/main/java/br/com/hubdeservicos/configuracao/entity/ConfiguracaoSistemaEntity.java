package br.com.hubdeservicos.configuracao.entity;

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
import java.util.UUID;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

@Entity
@Table(name = "configuracao_sistema")
public class ConfiguracaoSistemaEntity {

    public static final String SMS_PROVEDOR_ATIVO = "SMS_PROVEDOR_ATIVO";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "chave", nullable = false, updatable = false, length = 100)
    private String chave;

    @Column(name = "valor", nullable = false, columnDefinition = "text")
    private String valor;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Column(name = "tipo", nullable = false, length = 30)
    private String tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusCadastro status;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private Instant dataCriacao;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_atualizacao", nullable = false, insertable = false)
    private Instant dataAtualizacao;

    protected ConfiguracaoSistemaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public String getChave() {
        return chave;
    }

    public String getValor() {
        return valor;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getTipo() {
        return tipo;
    }

    public StatusCadastro getStatus() {
        return status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public Instant getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void atualizarValor(String valor, Instant agora) {
        this.valor = valor;
        dataAtualizacao = agora;
    }
}
