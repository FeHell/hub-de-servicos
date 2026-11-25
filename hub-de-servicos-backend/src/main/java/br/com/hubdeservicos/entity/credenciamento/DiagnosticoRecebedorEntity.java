package br.com.hubdeservicos.entity.credenciamento;

import br.com.hubdeservicos.service.configuracao.AmbienteServicosService.Ambiente;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credenciamento_recebedor_diagnostico")
public class DiagnosticoRecebedorEntity {
    @Id
    @Column(name = "tentativa_ref")
    private UUID tentativa;

    @Column(name = "id_credenciamento", nullable = false)
    private UUID credenciamento;

    @Enumerated(EnumType.STRING)
    @Column(name = "ambiente", nullable = false)
    private Ambiente ambiente;

    @Column(name = "capturado_em", nullable = false)
    private Instant capturadoEm;

    @Column(name = "resposta_protegida", nullable = false, columnDefinition = "text")
    private String respostaProtegida;

    protected DiagnosticoRecebedorEntity() {
    }

    public DiagnosticoRecebedorEntity(UUID tentativa, UUID credenciamento, Ambiente ambiente, Instant capturadoEm,
            String respostaProtegida) {
        this.tentativa = tentativa;
        this.credenciamento = credenciamento;
        this.ambiente = ambiente;
        this.capturadoEm = capturadoEm;
        this.respostaProtegida = respostaProtegida;
    }

    public UUID credenciamento() {
        return credenciamento;
    }

    public Ambiente ambiente() {
        return ambiente;
    }

    public Instant capturadoEm() {
        return capturadoEm;
    }

    public String respostaProtegida() {
        return respostaProtegida;
    }
}
