package br.com.hubdeservicos.service.usuario;

import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.repository.usuario.DesafioTelefoneRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TelefoneUsuarioService {
    private final UsuarioRepository usuarios;
    private final DesafioTelefoneRepository desafios;

    public TelefoneUsuarioService(UsuarioRepository usuarios, DesafioTelefoneRepository desafios) {
        this.usuarios = usuarios;
        this.desafios = desafios;
    }

    public void atualizar(UsuarioEntity usuario, String entrada) {
        String telefone = normalizar(entrada);
        if (Objects.equals(usuario.getTelefone(), telefone)) return;
        verificarDisponibilidade(usuario.getId(), telefone);
        usuario.atualizarTelefone(telefone);
        try {
            usuarios.flush();
        } catch (DataIntegrityViolationException erro) {
            if (erro.getCause() instanceof ConstraintViolationException violacao
                    && "uk_usuario_telefone".equals(violacao.getConstraintName())) throw duplicado();
            throw erro;
        }
        desafios.invalidar(usuario.getId());
    }

    public void verificarDisponibilidade(UUID id, String telefone) {
        if (telefone != null && usuarios.existsByTelefoneAndIdNot(telefone, id)) throw duplicado();
    }

    public static String normalizar(String entrada) {
        if (entrada == null || entrada.isBlank()) return null;
        if (!entrada.strip().matches("[+]?[0-9(). -]+")) throw invalido();
        String numero = entrada.replaceAll("[^0-9]", "");
        if (entrada.strip().startsWith("+") || numero.length() >= 12) {
            if (!numero.startsWith("55")) throw invalido();
            numero = numero.substring(2);
        }
        if (!numero.matches("[1-9][0-9]([2-5][0-9]{7}|9[0-9]{8})")) throw invalido();
        return "+55" + numero;
    }

    private static ResponseStatusException invalido() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um telefone brasileiro válido com DDD.");
    }

    private static ResponseStatusException duplicado() {
        return new ResponseStatusException(HttpStatus.CONFLICT, "Este telefone já está em uso por outra conta.");
    }
}
