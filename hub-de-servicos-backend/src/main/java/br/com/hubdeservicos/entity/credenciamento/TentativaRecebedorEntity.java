package br.com.hubdeservicos.entity.credenciamento;

import br.com.hubdeservicos.dto.credenciamento.ContratosCredenciamento.*;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "credenciamento_recebedor_tentativa")
public class TentativaRecebedorEntity {
    public enum Operacao { CRIACAO, ATUALIZACAO_CADASTRAL, ATUALIZACAO_BANCARIA }

    @Id
    @Column(name = "tentativa_ref", updatable = false)
    private UUID id;
    @Embedded
    private RecebedorExternoEntity.Chave recebedor;
    @Column(name = "versao_submetida", updatable = false)
    private Long versao;
    @Enumerated(EnumType.STRING)
    @Column(name = "operacao", updatable = false)
    private Operacao operacao;
    @Column(name = "tentativa_em", updatable = false)
    private Instant tentativaEm;
    @Column(name = "dados_protegidos", updatable = false, columnDefinition = "text")
    private String dadosProtegidos;
    @Enumerated(EnumType.STRING)
    @Column(name = "situacao")
    private SituacaoRecebedor situacao;
    @Column(name = "id_recebedor")
    private String idRecebedor;
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
    @Column(name = "passou_por_analise")
    private boolean passouPorAnalise;

    protected TentativaRecebedorEntity() {}

    public TentativaRecebedorEntity(RecebedorExternoEntity registro, CredenciamentoEntity cadastro, Operacao operacao) {
        id = registro.tentativa();
        recebedor = registro.chave();
        versao = cadastro.versao();
        tentativaEm = registro.tentativaEm();
        dadosProtegidos = cadastro.getDadosProtegidos();
        this.operacao = operacao;
        situacao = SituacaoRecebedor.EM_ANALISE;
        idRecebedor = registro.idRecebedor();
    }

    public void registrar(RecebedorExternoEntity registro, Instant agora) {
        situacao = registro.situacao();
        idRecebedor = registro.idRecebedor();
        statusProvedor = registro.statusProvedor();
        erro = registro.erro();
        camposRecusados = registro.camposRecusados();
        if (situacao == SituacaoRecebedor.CRIADO) {
            if (confirmadoEm == null) confirmadoEm = agora;
            if (statusProvedor == StatusPagarme.REGISTRATION || statusProvedor == StatusPagarme.AFFILIATION) passouPorAnalise = true;
        }
    }

    public Operacao operacao() { return operacao; }
    public String dadosProtegidos() { return dadosProtegidos; }
    public boolean aprovacaoConfirmada() { return operacao == Operacao.CRIACAO || passouPorAnalise; }
}
