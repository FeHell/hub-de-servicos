package br.com.hubdeservicos.dto.app;

import br.com.hubdeservicos.dto.usuario.SituacaoTelefone;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ContratosPerfilApp {
    private ContratosPerfilApp() {
    }

    public record AtualizarCadastro(@Size(max = 150) String nome, @Size(max = 32) String telefone) {
    }

    public record IniciarConfirmacao(@NotBlank @Size(max = 32) String telefone) {
    }

    public record ConfirmarTelefone(@NotNull UUID idDesafio, @NotNull @Pattern(regexp = "[0-9]{6}") String codigo) {
        @Override
        public String toString() { return "ConfirmarTelefone"; }
    }

    public enum Pendencia { NOME, TELEFONE, CONFIRMACAO_TELEFONE }

    public record Cadastro(UUID id, String email, String nome, String telefone, SituacaoTelefone situacaoTelefone,
                           List<Pendencia> pendencias) {
        public static Cadastro de(UsuarioEntity usuario) {
            List<Pendencia> pendencias = new ArrayList<>();
            if (usuario.getNome() == null) pendencias.add(Pendencia.NOME);
            SituacaoTelefone situacao = SituacaoTelefone.de(usuario);
            if (situacao == SituacaoTelefone.NAO_INFORMADO) pendencias.add(Pendencia.TELEFONE);
            if (situacao == SituacaoTelefone.PENDENTE) pendencias.add(Pendencia.CONFIRMACAO_TELEFONE);
            return new Cadastro(usuario.getId(), usuario.getEmail(), usuario.getNome(), usuario.getTelefone(),
                    situacao, List.copyOf(pendencias));
        }
    }

    public record Desafio(UUID idDesafio, String telefoneMascarado, Instant expiraEm, Cadastro cadastro) {
    }
}
