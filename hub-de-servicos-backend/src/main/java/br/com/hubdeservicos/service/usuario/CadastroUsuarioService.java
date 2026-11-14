package br.com.hubdeservicos.service.usuario;

import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CadastroUsuarioService {
    private final UsuarioRepository usuarios;

    public CadastroUsuarioService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public UsuarioEntity salvar(String login, String senhaHash, String email, StatusUsuario status) {
        return usuarios.saveAndFlush(new UsuarioEntity(login.strip(), senhaHash, email.strip(), status));
    }
}
