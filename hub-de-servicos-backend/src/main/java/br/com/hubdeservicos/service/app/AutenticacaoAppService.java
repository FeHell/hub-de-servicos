package br.com.hubdeservicos.service.app;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.dto.app.ContratosApp.AcessoApp;
import br.com.hubdeservicos.dto.app.ContratosApp.CadastroEntrada;
import br.com.hubdeservicos.dto.app.ContratosApp.ContaApp;
import br.com.hubdeservicos.dto.app.ContratosApp.LoginAppEntrada;
import br.com.hubdeservicos.entity.aplicacao.TipoAplicacao;
import br.com.hubdeservicos.entity.auditoria.AuditoriaEntity;
import br.com.hubdeservicos.entity.usuario.StatusUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.repository.aplicacao.AplicacaoRepository;
import br.com.hubdeservicos.repository.auditoria.AuditoriaRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioAplicacaoRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioRepository;
import br.com.hubdeservicos.service.administracao.SenhasAdministrativas;
import br.com.hubdeservicos.service.autenticacao.AutenticacaoService;
import br.com.hubdeservicos.service.usuario.CadastroUsuarioService;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutenticacaoAppService {
    public static final String SLUG = "hub-de-servicos-app";

    private final UsuarioRepository usuarios;
    private final AplicacaoRepository aplicacoes;
    private final UsuarioAplicacaoRepository vinculos;
    private final AuditoriaRepository auditoria;
    private final SenhasAdministrativas senhas;
    private final CadastroUsuarioService cadastro;
    private final AutenticacaoService autenticacao;

    public AutenticacaoAppService(UsuarioRepository usuarios, AplicacaoRepository aplicacoes,
                                  UsuarioAplicacaoRepository vinculos, AuditoriaRepository auditoria,
                                  SenhasAdministrativas senhas, CadastroUsuarioService cadastro,
                                  AutenticacaoService autenticacao) {
        this.usuarios = usuarios;
        this.aplicacoes = aplicacoes;
        this.vinculos = vinculos;
        this.auditoria = auditoria;
        this.senhas = senhas;
        this.cadastro = cadastro;
        this.autenticacao = autenticacao;
    }

    @Transactional
    public ContaApp cadastrar(CadastroEntrada entrada, String ip) {
        String email = entrada.email().strip();
        String hash = senhas.gerarHash(entrada.senha());
        if (usuarios.buscarPorEmail(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não foi possível concluir o cadastro.");
        }
        var aplicacao = aplicacoes.buscarSlugParaLogin(SLUG)
                .filter(app -> app.getTipo() == TipoAplicacao.PUBLICO && app.getStatus() == StatusCadastro.ATIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "Cadastro temporariamente indisponível."));
        var usuario = cadastro.salvar("app-" + UUID.randomUUID(), hash, email, StatusUsuario.ATIVO);
        vinculos.save(new UsuarioAplicacaoEntity(new UsuarioAplicacaoId(usuario.getId(), aplicacao.getId())));
        auditoria.save(new AuditoriaEntity(usuario.getId(), aplicacao.getId(), "CADASTRAR_USUARIO_APP",
                "usuario", null, null, ip != null && ip.length() <= 45 ? ip : null));
        return new ContaApp(usuario.getId(), usuario.getEmail());
    }

    public AcessoApp login(LoginAppEntrada entrada, String ip) {
        var sessao = autenticacao.loginPorEmail(SLUG, entrada.email().strip(), entrada.senha(), ip);
        return AcessoApp.de(sessao.tokens(), sessao.usuario());
    }
}
