package br.com.gerenciamentoplataforma.administracao.bootstrap;

import br.com.gerenciamentoplataforma.administracao.service.AcessoAdministrativo;
import br.com.gerenciamentoplataforma.administracao.service.SenhasAdministrativas;
import br.com.gerenciamentoplataforma.aplicacao.repository.AplicacaoRepository;
import br.com.gerenciamentoplataforma.auditoria.entity.AuditoriaEntity;
import br.com.gerenciamentoplataforma.auditoria.repository.AuditoriaRepository;
import br.com.gerenciamentoplataforma.common.StatusCadastro;
import br.com.gerenciamentoplataforma.permissao.entity.PermissaoEntity;
import br.com.gerenciamentoplataforma.permissao.repository.PermissaoRepository;
import br.com.gerenciamentoplataforma.usuario.entity.StatusUsuario;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoId;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioAplicacaoRepository;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaId;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioPermissaoPlataformaRepository;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioRepository;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import jakarta.validation.Validator;

import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.gerenciamentoplataforma.administracao.dto.ContratosAdministrativos.NovoUsuario;

@Service
@Profile("bootstrap-admin")
public class InicializacaoAdministrativa {

    public static final String SLUG = AcessoAdministrativo.SLUG_PLATAFORMA;

    private final JdbcTemplate jdbc;
    private final AplicacaoRepository aplicacoes;
    private final UsuarioRepository usuarios;
    private final PermissaoRepository permissoes;
    private final UsuarioAplicacaoRepository vinculos;
    private final UsuarioPermissaoPlataformaRepository atribuicoes;
    private final AuditoriaRepository auditoria;
    private final SenhasAdministrativas senhas;
    private final Validator validador;

    public InicializacaoAdministrativa(JdbcTemplate jdbc, AplicacaoRepository aplicacoes, UsuarioRepository usuarios,
                                       PermissaoRepository permissoes, UsuarioAplicacaoRepository vinculos,
                                       UsuarioPermissaoPlataformaRepository atribuicoes,
                                       AuditoriaRepository auditoria, SenhasAdministrativas senhas, Validator validador) {
        this.jdbc = jdbc;
        this.aplicacoes = aplicacoes;
        this.usuarios = usuarios;
        this.permissoes = permissoes;
        this.vinculos = vinculos;
        this.atribuicoes = atribuicoes;
        this.auditoria = auditoria;
        this.senhas = senhas;
        this.validador = validador;
    }

    @Transactional
    public void criarPrimeiroAdministrador(UUID idAplicacao, NovoUsuario dados) {
        if (!validador.validate(dados).isEmpty()) {
            throw new IllegalArgumentException("Informe login, e-mail e senha válidos para o primeiro administrador.");
        }
        if (aplicacoes.existsById(idAplicacao) || aplicacoes.findBySlug(SLUG).isPresent()
                || usuarios.buscarPorLogin(dados.login().strip()).isPresent()
                || usuarios.buscarPorEmail(dados.email().strip()).isPresent()) {
            throw new IllegalStateException("A aplicação ou a conta administrativa já existe.");
        }

        String hash = senhas.gerarHash(dados.senha());
        jdbc.update("""
                insert into aplicacao (id, nome, slug, tipo, status)
                values (?, 'Administração da plataforma', ?, 'PUBLICO', 'ATIVO')
                """, idAplicacao, SLUG);
        var usuario = usuarios.saveAndFlush(new UsuarioEntity(dados.login().strip(), hash, dados.email().strip(),
                StatusUsuario.ATIVO));
        var permissao = permissoes.findByCodigo(AcessoAdministrativo.PLATAFORMA_ADMINISTRAR)
                .orElseThrow(() -> new IllegalStateException("Aplique as migrações antes de criar o administrador."));
        if (permissoes.findByCodigo(AcessoAdministrativo.APLICACAO_ADMINISTRAR).isEmpty()) {
            permissoes.save(new PermissaoEntity(AcessoAdministrativo.APLICACAO_ADMINISTRAR,
                    "Administrar perfis, vínculos e sessões da aplicação.", StatusCadastro.ATIVO));
        }
        vinculos.saveAndFlush(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), idAplicacao)));
        atribuicoes.save(new UsuarioPermissaoPlataformaEntity(
                new UsuarioPermissaoPlataformaId(usuario.getId(), permissao.getId())));
        var evento = JsonNodeFactory.instance.objectNode().put("idRecurso", usuario.getId().toString());
        auditoria.save(new AuditoriaEntity(null, idAplicacao, "CRIAR_PRIMEIRO_ADMINISTRADOR",
                "usuario", null, evento, null));
    }
}
