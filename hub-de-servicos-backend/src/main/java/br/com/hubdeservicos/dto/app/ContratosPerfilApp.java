package br.com.hubdeservicos.dto.app;

import br.com.hubdeservicos.dto.usuario.SituacaoTelefone;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ContratosPerfilApp {
    private ContratosPerfilApp() {
    }

    public record AtualizarCadastro(@Size(max = 150) String nome, @Size(max = 32) String telefone,
                                    @Valid Remuneracao remuneracao) {
    }

    public record Remuneracao(@JsonFormat(shape = JsonFormat.Shape.STRING) @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal valorHora,
                              @JsonFormat(shape = JsonFormat.Shape.STRING) @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal valorDiaria) {
    }

    public record SelecionarPapel(@NotNull Boolean selecionado) {
    }

    public record IdentificarMunicipio(@NotBlank @Size(max = 120) String cidade,
                                       @NotBlank @Size(max = 40) String estado) {
    }

    public record Municipio(int codigoIbge, String nome, String uf, Instant observadoEm) {
    }

    public record IniciarConfirmacao(@NotBlank @Size(max = 32) String telefone) {
    }

    public record ConfirmarTelefone(@NotNull UUID idDesafio, @NotNull @Pattern(regexp = "[0-9]{6}") String codigo) {
        @Override
        public String toString() { return "ConfirmarTelefone"; }
    }

    public enum Pendencia { NOME, TELEFONE, CONFIRMACAO_TELEFONE }

    public record Cadastro(UUID id, String email, String nome, String telefone, SituacaoTelefone situacaoTelefone,
                           List<Pendencia> pendencias, List<PapelUsuario> papeis,
                           Remuneracao remuneracao, Municipio municipio) {
        public static Cadastro de(UsuarioEntity usuario) {
            List<Pendencia> pendencias = new ArrayList<>();
            if (usuario.getNome() == null) pendencias.add(Pendencia.NOME);
            SituacaoTelefone situacao = SituacaoTelefone.de(usuario);
            if (situacao == SituacaoTelefone.NAO_INFORMADO) pendencias.add(Pendencia.TELEFONE);
            if (situacao == SituacaoTelefone.PENDENTE) pendencias.add(Pendencia.CONFIRMACAO_TELEFONE);
            List<PapelUsuario> papeis = new ArrayList<>();
            if (usuario.isPapelEmpresa()) papeis.add(PapelUsuario.EMPRESA);
            if (usuario.isPapelTrabalhador()) papeis.add(PapelUsuario.TRABALHADOR);
            var local = usuario.getMunicipioAtual();
            Municipio municipio = local == null ? null : new Municipio(local.getCodigoIbge(), local.getNome(),
                    local.getUf(), usuario.getMunicipioObservadoEm());
            return new Cadastro(usuario.getId(), usuario.getEmail(), usuario.getNome(), usuario.getTelefone(),
                    situacao, List.copyOf(pendencias), List.copyOf(papeis),
                    new Remuneracao(usuario.getValorHora(), usuario.getValorDiaria()), municipio);
        }
    }

    public record Desafio(UUID idDesafio, String telefoneMascarado, Instant expiraEm, Cadastro cadastro) {
    }
}
