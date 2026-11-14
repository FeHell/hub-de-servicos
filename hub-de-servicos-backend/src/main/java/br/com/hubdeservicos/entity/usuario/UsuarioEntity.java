package br.com.hubdeservicos.entity.usuario;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(name = "usuario")
public class UsuarioEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "login", nullable = false, length = 50)
    private String login;

    @JsonIgnore
    @Column(name = "senha_hash", nullable = false, columnDefinition = "text")
    private String senhaHash;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private StatusUsuario status;

    @Generated(event = EventType.INSERT)
    @Column(name = "data_criacao", nullable = false, insertable = false, updatable = false)
    private Instant dataCriacao;

    @Column(name = "nome", length = 150)
    private String nome;

    @Column(name = "telefone", length = 16)
    private String telefone;

    @Column(name = "telefone_verificado_em")
    private Instant telefoneVerificadoEm;

    protected UsuarioEntity() {
    }

    public UsuarioEntity(String login, String senhaHash, String email, StatusUsuario status) {
        this.login = Objects.requireNonNull(login, "login");
        this.senhaHash = Objects.requireNonNull(senhaHash, "senhaHash");
        this.email = Objects.requireNonNull(email, "email");
        this.status = Objects.requireNonNull(status, "status");
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public String getEmail() {
        return email;
    }

    public StatusUsuario getStatus() {
        return status;
    }

    public Instant getDataCriacao() {
        return dataCriacao;
    }

    public String getNome() {
        return nome;
    }

    public String getTelefone() {
        return telefone;
    }

    public Instant getTelefoneVerificadoEm() {
        return telefoneVerificadoEm;
    }

    public void atualizarNome(String nome) {
        String valor = nome == null ? "" : nome.strip();
        this.nome = valor.isEmpty() ? null : valor;
    }

    public void atualizarTelefone(String telefone) {
        if (!Objects.equals(this.telefone, telefone)) {
            this.telefone = telefone;
            this.telefoneVerificadoEm = null;
        }
    }

    public void verificarTelefone(String telefone, Instant agora) {
        if (telefone == null || !telefone.equals(this.telefone)) {
            throw new IllegalArgumentException("O número do desafio difere do telefone atual.");
        }
        this.telefoneVerificadoEm = Objects.requireNonNull(agora);
    }

    public void atualizar(String login, String email, StatusUsuario status) {
        this.login = Objects.requireNonNull(login);
        this.email = Objects.requireNonNull(email);
        this.status = Objects.requireNonNull(status);
    }

    public void alterarSenhaHash(String senhaHash) {
        this.senhaHash = Objects.requireNonNull(senhaHash);
    }
}
