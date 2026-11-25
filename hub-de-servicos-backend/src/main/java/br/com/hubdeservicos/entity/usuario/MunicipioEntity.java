package br.com.hubdeservicos.entity.usuario;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "municipio_ibge")
public class MunicipioEntity {
    @Id
    @Column(name = "codigo_ibge")
    private Integer codigoIbge;

    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    @Column(name = "uf", nullable = false, length = 2)
    private String uf;

    @Column(name = "nome_busca", nullable = false, length = 120)
    private String nomeBusca;

    @Column(name = "uf_busca", nullable = false, length = 40)
    private String ufBusca;

    protected MunicipioEntity() {
    }

    public Integer getCodigoIbge() {
        return codigoIbge;
    }

    public String getNome() {
        return nome;
    }

    public String getUf() {
        return uf;
    }
}
