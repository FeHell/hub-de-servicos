package br.com.hubdeservicos.entity.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "credenciamento_recebedor_externo")
public class RecebedorExternoEntity {
    @EmbeddedId
    private Chave id;

    @Embeddable
    public static class Chave implements Serializable {
        @Column(name = "id_credenciamento")
        private UUID credenciamento;

        @Enumerated(EnumType.STRING)
        @Column(name = "provedor")
        private ProvedorCredenciamento provedor;

        @Enumerated(EnumType.STRING)
        @Column(name = "ambiente")
        private Ambiente ambiente;

        protected Chave() {}

        public Chave(UUID credenciamento, ProvedorCredenciamento provedor, Ambiente ambiente) {
            this.credenciamento = credenciamento;
            this.provedor = provedor;
            this.ambiente = ambiente;
        }

        public Ambiente ambiente() { return ambiente; }
        public UUID credenciamento() { return credenciamento; }
        public String codigo() { return credenciamento.toString(); }

        @Override
        public boolean equals(Object outro) {
            return outro instanceof Chave chave && credenciamento.equals(chave.credenciamento)
                    && provedor == chave.provedor && ambiente == chave.ambiente;
        }

        @Override
        public int hashCode() { return Objects.hash(credenciamento, provedor, ambiente); }
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade")
    private Modalidade modalidade;

    @Column(name = "documento_digest")
    private String documentoDigest;

    @Column(name = "conta_digest")
    private String contaDigest;

    @Enumerated(EnumType.STRING)
    @Column(name = "situacao")
    private SituacaoRecebedor situacao;

    @Column(name = "id_recebedor")
    private String idRecebedor;

    @Column(name = "tentativa_ref")
    private UUID tentativa;

    @Column(name = "versao_submetida")
    private Long versaoSubmetida;

    @Column(name = "tentativa_em")
    private Instant tentativaEm;

    @Column(name = "confirmado_em")
    private Instant confirmadoEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_provedor")
    private StatusPagarme statusProvedor;

    @Enumerated(EnumType.STRING)
    @Column(name = "erro_sanitizado")
    private ErroRecebedor erro;

    @Column(name = "campos_recusados")
    private String camposRecusados;

    protected RecebedorExternoEntity() {}

    public RecebedorExternoEntity(Chave id, String contaDigest) {
        this.id = id;
        this.contaDigest = contaDigest;
    }

    public void iniciar(Modalidade modalidade, String documentoDigest, UUID tentativa, long versao, Instant agora) {
        this.modalidade = modalidade;
        this.documentoDigest = documentoDigest;
        this.tentativa = tentativa;
        versaoSubmetida = versao;
        tentativaEm = agora;
        situacao = SituacaoRecebedor.EM_ANALISE;
        erro = null;
        camposRecusados = null;
        statusProvedor = null;
    }

    public void confirmar(String idRecebedor, StatusPagarme status, Instant agora) {
        if (this.idRecebedor == null) {
            this.idRecebedor = idRecebedor;
            confirmadoEm = agora;
        }
        situacao = SituacaoRecebedor.CRIADO;
        statusProvedor = status;
        erro = null;
        camposRecusados = null;
    }

    public void registrarFalha(SituacaoRecebedor situacao, ErroRecebedor erro, String campos) {
        this.situacao = situacao;
        this.erro = erro;
        camposRecusados = campos;
    }

    public Chave chave() { return id; }
    public boolean daConta(String digest) { return Objects.equals(contaDigest, digest); }
    public UUID tentativa() { return tentativa; }
    public Instant tentativaEm() { return tentativaEm; }
    public boolean representa(long versao) { return Objects.equals(versaoSubmetida, versao); }
    public String idRecebedor() { return idRecebedor; }
    public SituacaoRecebedor situacao() { return situacao; }
    public StatusPagarme statusProvedor() { return statusProvedor; }
    public ErroRecebedor erro() { return erro; }
    public String camposRecusados() { return camposRecusados; }
}
