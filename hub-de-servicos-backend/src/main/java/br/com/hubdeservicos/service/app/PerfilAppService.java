package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.dto.app.ContratosPerfilApp.AtualizarCadastro;
import br.com.hubdeservicos.dto.app.ContratosPerfilApp.Cadastro;
import br.com.hubdeservicos.entity.usuario.UsuarioEntity;
import br.com.hubdeservicos.exception.autenticacao.AutenticacaoNegadaException;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.usuario.TelefoneUsuarioService;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PerfilAppService {
    private final UsuarioRepository usuarios;
    private final TelefoneUsuarioService telefones;
    private final EntityManager persistencia;

    public PerfilAppService(UsuarioRepository usuarios, TelefoneUsuarioService telefones, EntityManager persistencia) {
        this.usuarios = usuarios;
        this.telefones = telefones;
        this.persistencia = persistencia;
    }

    @Transactional(readOnly = true)
    public Cadastro consultar(UUID id) {
        return Cadastro.de(usuarios.findById(id).orElseThrow(AutenticacaoNegadaException::new));
    }

    @Transactional
    public Cadastro atualizar(UUID id, AtualizarCadastro dados) {
        var usuario = bloquear(id);
        if (dados.nome() != null) usuario.atualizarNome(dados.nome());
        if (dados.telefone() != null) telefones.atualizar(usuario, dados.telefone());
        return Cadastro.de(usuario);
    }

    UsuarioEntity bloquear(UUID id) {
        var usuario = usuarios.bloquearPorId(id).orElseThrow(AutenticacaoNegadaException::new);
        // A validação da sessão pode ter carregado a conta antes da aquisição do bloqueio.
        persistencia.refresh(usuario);
        return usuario;
    }
}
