package br.com.hubdeservicos.entity.permissao;

import br.com.hubdeservicos.common.StatusCadastro;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "permissao")
public class PermissaoEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "codigo", nullable = false, length = 100)
    private String codigo;

    @Column(name = "descricao", columnDefinition = "text")
    private String descricao;

    @Column(name = "nome_plataforma", length = 100)
    private String nomePlataforma;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusCadastro status;

    protected PermissaoEntity() {
    }

    public PermissaoEntity(String codigo, String descricao, StatusCadastro status) {
        this.codigo = Objects.requireNonNull(codigo, "codigo");
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status, "status");
    }

    public static PermissaoEntity daPlataforma(String codigo, String nome, String descricao) {
        var permissao = new PermissaoEntity(codigo, descricao, StatusCadastro.ATIVO);
        permissao.nomePlataforma = Objects.requireNonNull(nome, "nome");
        return permissao;
    }

    public String getNomePlataforma() {
        return nomePlataforma;
    }

    public UUID getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }

    public StatusCadastro getStatus() {
        return status;
    }

    public void atualizar(String descricao, StatusCadastro status) {
        this.descricao = descricao;
        this.status = Objects.requireNonNull(status);
    }
}
