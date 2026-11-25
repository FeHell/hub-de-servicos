package br.com.hubdeservicos.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import br.com.hubdeservicos.dto.usuario.SituacaoTelefone;
import br.com.hubdeservicos.entity.usuario.MunicipioEntity;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ContratosPerfilApp {
    private ContratosPerfilApp() {
    }

    @Schema(name = "AtualizarCadastro")
    public static final class AtualizarCadastroBean {
        @Size(max = 150)
        private String nome;

        @Size(max = 32)
        private String telefone;

        @Valid
        private RemuneracaoBean remuneracao;

        public AtualizarCadastroBean() {
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public RemuneracaoBean getRemuneracao() {
            return remuneracao;
        }

        public void setRemuneracao(RemuneracaoBean remuneracao) {
            this.remuneracao = remuneracao;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AtualizarCadastroBean outro)) {
                return false;
            }
            return Objects.equals(nome, outro.nome) && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(remuneracao, outro.remuneracao);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(remuneracao);
            return resultado;
        }

        @Override
        public String toString() {
            return "AtualizarCadastro[" + "nome=" + nome + ", telefone=" + telefone + ", remuneracao=" + remuneracao
                    + "]";
        }
    }

    @Schema(name = "Remuneracao")
    public static final class RemuneracaoBean {
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        private BigDecimal valorHora;

        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        private BigDecimal valorDiaria;

        public RemuneracaoBean() {
        }

        public BigDecimal getValorHora() {
            return valorHora;
        }

        public void setValorHora(BigDecimal valorHora) {
            this.valorHora = valorHora;
        }

        public BigDecimal getValorDiaria() {
            return valorDiaria;
        }

        public void setValorDiaria(BigDecimal valorDiaria) {
            this.valorDiaria = valorDiaria;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof RemuneracaoBean outro)) {
                return false;
            }
            return Objects.equals(valorHora, outro.valorHora) && Objects.equals(valorDiaria, outro.valorDiaria);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(valorHora);
            resultado = 31 * resultado + Objects.hashCode(valorDiaria);
            return resultado;
        }

        @Override
        public String toString() {
            return "Remuneracao[" + "valorHora=" + valorHora + ", valorDiaria=" + valorDiaria + "]";
        }
    }

    @Schema(name = "SelecionarPapel")
    public static final class SelecionarPapelBean {
        @NotNull
        private Boolean selecionado;

        public SelecionarPapelBean() {
        }

        public Boolean getSelecionado() {
            return selecionado;
        }

        public void setSelecionado(Boolean selecionado) {
            this.selecionado = selecionado;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof SelecionarPapelBean outro)) {
                return false;
            }
            return Objects.equals(selecionado, outro.selecionado);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(selecionado);
            return resultado;
        }

        @Override
        public String toString() {
            return "SelecionarPapel[" + "selecionado=" + selecionado + "]";
        }
    }

    @Schema(name = "IdentificarMunicipio")
    public static final class IdentificarMunicipioBean {
        @NotBlank
        @Size(max = 120)
        private String cidade;

        @NotBlank
        @Size(max = 40)
        private String estado;

        private boolean confirmada;

        public IdentificarMunicipioBean() {
        }

        public String getCidade() {
            return cidade;
        }

        public void setCidade(String cidade) {
            this.cidade = cidade;
        }

        public String getEstado() {
            return estado;
        }

        public void setEstado(String estado) {
            this.estado = estado;
        }

        public boolean getConfirmada() {
            return confirmada;
        }

        public void setConfirmada(boolean confirmada) {
            this.confirmada = confirmada;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof IdentificarMunicipioBean outro)) {
                return false;
            }
            return Objects.equals(cidade, outro.cidade) && Objects.equals(estado, outro.estado)
                    && Objects.equals(confirmada, outro.confirmada);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(cidade);
            resultado = 31 * resultado + Objects.hashCode(estado);
            resultado = 31 * resultado + Objects.hashCode(confirmada);
            return resultado;
        }

        @Override
        public String toString() {
            return "IdentificarMunicipio[" + "cidade=" + cidade + ", estado=" + estado + ", confirmada=" + confirmada
                    + "]";
        }
    }

    @Schema(name = "SelecionarMunicipio")
    public static final class SelecionarMunicipioBean {
        @NotNull
        @Positive
        private Integer codigoIbge;

        public SelecionarMunicipioBean() {
        }

        public Integer getCodigoIbge() {
            return codigoIbge;
        }

        public void setCodigoIbge(Integer codigoIbge) {
            this.codigoIbge = codigoIbge;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof SelecionarMunicipioBean outro)) {
                return false;
            }
            return Objects.equals(codigoIbge, outro.codigoIbge);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigoIbge);
            return resultado;
        }

        @Override
        public String toString() {
            return "SelecionarMunicipio[" + "codigoIbge=" + codigoIbge + "]";
        }
    }

    @Schema(name = "OpcaoMunicipio")
    public static final class OpcaoMunicipioBean {
        private int codigoIbge;

        private String nome;

        private String uf;

        public OpcaoMunicipioBean() {
        }

        public int getCodigoIbge() {
            return codigoIbge;
        }

        public void setCodigoIbge(int codigoIbge) {
            this.codigoIbge = codigoIbge;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getUf() {
            return uf;
        }

        public void setUf(String uf) {
            this.uf = uf;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof OpcaoMunicipioBean outro)) {
                return false;
            }
            return Objects.equals(codigoIbge, outro.codigoIbge) && Objects.equals(nome, outro.nome)
                    && Objects.equals(uf, outro.uf);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigoIbge);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(uf);
            return resultado;
        }

        @Override
        public String toString() {
            return "OpcaoMunicipio[" + "codigoIbge=" + codigoIbge + ", nome=" + nome + ", uf=" + uf + "]";
        }

        public static OpcaoMunicipioBean de(MunicipioEntity municipio) {
            OpcaoMunicipioBean opcaoMunicipioPreparado = new OpcaoMunicipioBean();
            opcaoMunicipioPreparado.setCodigoIbge(municipio.getCodigoIbge());
            opcaoMunicipioPreparado.setNome(municipio.getNome());
            opcaoMunicipioPreparado.setUf(municipio.getUf());
            return opcaoMunicipioPreparado;
        }
    }

    @Schema(name = "Municipio")
    public static final class MunicipioBean {
        private int codigoIbge;

        private String nome;

        private String uf;

        private Instant observadoEm;

        private boolean manual;

        public MunicipioBean() {
        }

        public int getCodigoIbge() {
            return codigoIbge;
        }

        public void setCodigoIbge(int codigoIbge) {
            this.codigoIbge = codigoIbge;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getUf() {
            return uf;
        }

        public void setUf(String uf) {
            this.uf = uf;
        }

        public Instant getObservadoEm() {
            return observadoEm;
        }

        public void setObservadoEm(Instant observadoEm) {
            this.observadoEm = observadoEm;
        }

        public boolean getManual() {
            return manual;
        }

        public void setManual(boolean manual) {
            this.manual = manual;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof MunicipioBean outro)) {
                return false;
            }
            return Objects.equals(codigoIbge, outro.codigoIbge) && Objects.equals(nome, outro.nome)
                    && Objects.equals(uf, outro.uf) && Objects.equals(observadoEm, outro.observadoEm)
                    && Objects.equals(manual, outro.manual);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(codigoIbge);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(uf);
            resultado = 31 * resultado + Objects.hashCode(observadoEm);
            resultado = 31 * resultado + Objects.hashCode(manual);
            return resultado;
        }

        @Override
        public String toString() {
            return "Municipio[" + "codigoIbge=" + codigoIbge + ", nome=" + nome + ", uf=" + uf + ", observadoEm="
                    + observadoEm + ", manual=" + manual + "]";
        }
    }

    @Schema(name = "IniciarConfirmacao")
    public static final class IniciarConfirmacaoBean {
        @NotBlank
        @Size(max = 32)
        private String telefone;

        public IniciarConfirmacaoBean() {
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof IniciarConfirmacaoBean outro)) {
                return false;
            }
            return Objects.equals(telefone, outro.telefone);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(telefone);
            return resultado;
        }

        @Override
        public String toString() {
            return "IniciarConfirmacao[" + "telefone=" + telefone + "]";
        }
    }

    @Schema(name = "ConfirmarTelefone")
    public static final class ConfirmarTelefoneBean {
        @NotNull
        private UUID idDesafio;

        @NotNull
        @Pattern(regexp = "[0-9]{6}")
        private String codigo;

        public ConfirmarTelefoneBean() {
        }

        public UUID getIdDesafio() {
            return idDesafio;
        }

        public void setIdDesafio(UUID idDesafio) {
            this.idDesafio = idDesafio;
        }

        public String getCodigo() {
            return codigo;
        }

        public void setCodigo(String codigo) {
            this.codigo = codigo;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof ConfirmarTelefoneBean outro)) {
                return false;
            }
            return Objects.equals(idDesafio, outro.idDesafio) && Objects.equals(codigo, outro.codigo);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(idDesafio);
            resultado = 31 * resultado + Objects.hashCode(codigo);
            return resultado;
        }

        @Override
        public String toString() {
            return "ConfirmarTelefone";
        }
    }

    public enum Pendencia {
        NOME, TELEFONE, CONFIRMACAO_TELEFONE
    }

    @Schema(name = "Cadastro")
    public static final class CadastroBean {
        private UUID id;

        private String email;

        private String nome;

        private String telefone;

        private SituacaoTelefone situacaoTelefone;

        private List<Pendencia> pendencias;

        private List<PapelUsuario> papeis;

        private RemuneracaoBean remuneracao;

        private MunicipioBean municipio;

        public CadastroBean() {
        }

        public UUID getId() {
            return id;
        }

        public void setId(UUID id) {
            this.id = id;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }

        public SituacaoTelefone getSituacaoTelefone() {
            return situacaoTelefone;
        }

        public void setSituacaoTelefone(SituacaoTelefone situacaoTelefone) {
            this.situacaoTelefone = situacaoTelefone;
        }

        public List<Pendencia> getPendencias() {
            return pendencias;
        }

        public void setPendencias(List<Pendencia> pendencias) {
            this.pendencias = pendencias;
        }

        public List<PapelUsuario> getPapeis() {
            return papeis;
        }

        public void setPapeis(List<PapelUsuario> papeis) {
            this.papeis = papeis;
        }

        public RemuneracaoBean getRemuneracao() {
            return remuneracao;
        }

        public void setRemuneracao(RemuneracaoBean remuneracao) {
            this.remuneracao = remuneracao;
        }

        public MunicipioBean getMunicipio() {
            return municipio;
        }

        public void setMunicipio(MunicipioBean municipio) {
            this.municipio = municipio;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof CadastroBean outro)) {
                return false;
            }
            return Objects.equals(id, outro.id) && Objects.equals(email, outro.email)
                    && Objects.equals(nome, outro.nome) && Objects.equals(telefone, outro.telefone)
                    && Objects.equals(situacaoTelefone, outro.situacaoTelefone)
                    && Objects.equals(pendencias, outro.pendencias) && Objects.equals(papeis, outro.papeis)
                    && Objects.equals(remuneracao, outro.remuneracao) && Objects.equals(municipio, outro.municipio);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(id);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(nome);
            resultado = 31 * resultado + Objects.hashCode(telefone);
            resultado = 31 * resultado + Objects.hashCode(situacaoTelefone);
            resultado = 31 * resultado + Objects.hashCode(pendencias);
            resultado = 31 * resultado + Objects.hashCode(papeis);
            resultado = 31 * resultado + Objects.hashCode(remuneracao);
            resultado = 31 * resultado + Objects.hashCode(municipio);
            return resultado;
        }

        @Override
        public String toString() {
            return "Cadastro[" + "id=" + id + ", email=" + email + ", nome=" + nome + ", telefone=" + telefone
                    + ", situacaoTelefone=" + situacaoTelefone + ", pendencias=" + pendencias + ", papeis=" + papeis
                    + ", remuneracao=" + remuneracao + ", municipio=" + municipio + "]";
        }

        public static CadastroBean de(UsuarioEntity usuario, List<PapelUsuario> papeis) {
            List<Pendencia> pendencias = new ArrayList<>();
            if (usuario.getNome() == null) {
                pendencias.add(Pendencia.NOME);
            }
            SituacaoTelefone situacao = SituacaoTelefone.de(usuario);
            if (situacao == SituacaoTelefone.NAO_INFORMADO) {
                pendencias.add(Pendencia.TELEFONE);
            }
            if (situacao == SituacaoTelefone.PENDENTE) {
                pendencias.add(Pendencia.CONFIRMACAO_TELEFONE);
            }
            var local = usuario.getMunicipioAtual();
            MunicipioBean municipio;
            if (local == null) {
                municipio = null;
            } else {
                MunicipioBean municipioPreparado = new MunicipioBean();
                municipioPreparado.setCodigoIbge(local.getCodigoIbge());
                municipioPreparado.setNome(local.getNome());
                municipioPreparado.setUf(local.getUf());
                municipioPreparado.setObservadoEm(usuario.getMunicipioObservadoEm());
                municipioPreparado.setManual(usuario.isMunicipioManual());
                municipio = municipioPreparado;
            }
            CadastroBean cadastroPreparado = new CadastroBean();
            cadastroPreparado.setId(usuario.getId());
            cadastroPreparado.setEmail(usuario.getEmail());
            cadastroPreparado.setNome(usuario.getNome());
            cadastroPreparado.setTelefone(usuario.getTelefone());
            cadastroPreparado.setSituacaoTelefone(situacao);
            cadastroPreparado.setPendencias(List.copyOf(pendencias));
            cadastroPreparado.setPapeis(papeis);
            RemuneracaoBean remuneracaoPreparado = new RemuneracaoBean();
            remuneracaoPreparado.setValorHora(usuario.getValorHora());
            remuneracaoPreparado.setValorDiaria(usuario.getValorDiaria());
            cadastroPreparado.setRemuneracao(remuneracaoPreparado);
            cadastroPreparado.setMunicipio(municipio);
            return cadastroPreparado;
        }
    }

    @Schema(name = "Desafio")
    public static final class DesafioBean {
        private UUID idDesafio;

        private String telefoneMascarado;

        private Instant expiraEm;

        private CadastroBean cadastro;

        public DesafioBean() {
        }

        public UUID getIdDesafio() {
            return idDesafio;
        }

        public void setIdDesafio(UUID idDesafio) {
            this.idDesafio = idDesafio;
        }

        public String getTelefoneMascarado() {
            return telefoneMascarado;
        }

        public void setTelefoneMascarado(String telefoneMascarado) {
            this.telefoneMascarado = telefoneMascarado;
        }

        public Instant getExpiraEm() {
            return expiraEm;
        }

        public void setExpiraEm(Instant expiraEm) {
            this.expiraEm = expiraEm;
        }

        public CadastroBean getCadastro() {
            return cadastro;
        }

        public void setCadastro(CadastroBean cadastro) {
            this.cadastro = cadastro;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof DesafioBean outro)) {
                return false;
            }
            return Objects.equals(idDesafio, outro.idDesafio)
                    && Objects.equals(telefoneMascarado, outro.telefoneMascarado)
                    && Objects.equals(expiraEm, outro.expiraEm) && Objects.equals(cadastro, outro.cadastro);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(idDesafio);
            resultado = 31 * resultado + Objects.hashCode(telefoneMascarado);
            resultado = 31 * resultado + Objects.hashCode(expiraEm);
            resultado = 31 * resultado + Objects.hashCode(cadastro);
            return resultado;
        }

        @Override
        public String toString() {
            return "Desafio[" + "idDesafio=" + idDesafio + ", telefoneMascarado=" + telefoneMascarado + ", expiraEm="
                    + expiraEm + ", cadastro=" + cadastro + "]";
        }
    }
}
