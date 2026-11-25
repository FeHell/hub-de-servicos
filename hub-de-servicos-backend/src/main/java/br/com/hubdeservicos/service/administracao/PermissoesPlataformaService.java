package br.com.hubdeservicos.service.administracao;

import br.com.hubdeservicos.common.StatusCadastro;
import br.com.hubdeservicos.entity.permissao.PermissaoEntity;
import br.com.hubdeservicos.repository.permissao.PermissaoRepository;
import br.com.hubdeservicos.entity.usuario.PapelUsuario;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioAplicacaoId;
import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaEntity;
import br.com.hubdeservicos.entity.usuario.UsuarioPermissaoPlataformaId;
import br.com.hubdeservicos.repository.usuario.UsuarioAplicacaoRepository;
import br.com.hubdeservicos.repository.usuario.UsuarioPermissaoPlataformaRepository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PermissoesPlataformaService {

    private final PermissaoRepository catalogo;
    private final UsuarioPermissaoPlataformaRepository atribuicoes;
    private final UsuarioAplicacaoRepository vinculos;
    private final RegistroAdministrativo registro;

    PermissoesPlataformaService(PermissaoRepository catalogo, UsuarioPermissaoPlataformaRepository atribuicoes,
            UsuarioAplicacaoRepository vinculos, RegistroAdministrativo registro) {
        this.catalogo = catalogo;
        this.atribuicoes = atribuicoes;
        this.vinculos = vinculos;
        this.registro = registro;
    }

    List<String> consultar(UUID idUsuario) {
        return atribuicoes.listarAtribuicoes(List.of(idUsuario)).stream()
                .map(UsuarioPermissaoPlataformaRepository.Atribuicao::getCodigo).toList();
    }

    public List<PapelUsuario> consultarPapeis(UUID idUsuario) {
        var codigos = consultar(idUsuario);
        return Arrays.stream(PapelUsuario.values()).filter(papel -> codigos.contains(papel.codigoPermissao())).toList();
    }

    // O chamador bloqueia a conta. O app só pode alterar o código mapeado pelo
    // enum.
    public void selecionarPapel(UUID idUsuario, UUID idAplicacao, PapelUsuario papel, boolean selecionado) {
        var permissao = catalogo.findByCodigo(papel.codigoPermissao()).filter(item -> item.getNomePlataforma() != null)
                .orElseThrow(() -> {
                    var responseStatusExceptionPreparado = new ResponseStatusException(HttpStatus.CONFLICT,
                            "Perfil indisponível.");
                    return responseStatusExceptionPreparado;
                });
        var chave = new UsuarioPermissaoPlataformaId(idUsuario, permissao.getId());
        var atual = atribuicoes.findById(chave);
        if (selecionado && atual.isEmpty()) {
            if (permissao.getStatus() != StatusCadastro.ATIVO) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Perfil indisponível.");
            }
            atribuir(chave, idAplicacao);
        } else {
            if (!selecionado && atual.isPresent()) {
                remover(atual.get(), idAplicacao);
            }
        }
    }

    // Preserva alterações externas em permissões que não foram editadas no
    // formulário.
    List<String> conciliar(List<String> atuais, List<String> desejadas, List<String> originais) {
        if (desejadas == null || originais == null) {
            return desejadas;
        }
        Set<String> resultado = new HashSet<>(atuais);
        resultado.removeIf(codigo -> originais.contains(codigo) && !desejadas.contains(codigo));
        desejadas.stream().filter(codigo -> !originais.contains(codigo)).forEach(resultado::add);
        return resultado.stream().sorted().toList();
    }

    List<String> substituir(UUID idUsuario, UUID idAplicacao, List<String> codigos) {
        var atuais = atribuicoes.findByIdIdUsuario(idUsuario);
        Set<UUID> idsAtuais = atuais.stream().map(a -> a.getId().getIdPermissao()).collect(Collectors.toSet());
        List<String> solicitados = codigos.stream().distinct().sorted().toList();
        List<PermissaoEntity> desejadas = solicitados.isEmpty() ? List.of()
                : catalogo.findByNomePlataformaIsNotNullAndCodigoIn(solicitados);
        if (desejadas.size() != solicitados.size() || desejadas.stream()
                .anyMatch(p -> p.getStatus() != StatusCadastro.ATIVO && !idsAtuais.contains(p.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Há permissões da plataforma inválidas ou inativas.");
        }

        if (desejadas.stream().anyMatch(p -> p.getStatus() == StatusCadastro.ATIVO)) {
            var chave = new UsuarioAplicacaoId(idUsuario, idAplicacao);
            var vinculo = vinculos.findById(chave).orElseGet(() -> {
                var usuarioAplicacaoEntityPreparado = new UsuarioAplicacaoEntity(chave);
                return vinculos.save(usuarioAplicacaoEntityPreparado);
            });
            vinculo.definirAtivo(true);
        }

        Set<UUID> idsDesejados = new HashSet<>();
        for (PermissaoEntity permissao : desejadas) {
            UUID idPermissao = permissao.getId();
            idsDesejados.add(idPermissao);
            if (!idsAtuais.contains(idPermissao)) {
                var usuarioPermissaoPlataformaIdPreparado = new UsuarioPermissaoPlataformaId(idUsuario, idPermissao);
                atribuir(usuarioPermissaoPlataformaIdPreparado, idAplicacao);
            }
        }
        for (var atribuicao : atuais) {
            UUID idPermissao = atribuicao.getId().getIdPermissao();
            if (!idsDesejados.contains(idPermissao)) {
                remover(atribuicao, idAplicacao);
            }
        }
        return solicitados;
    }

    private void atribuir(UsuarioPermissaoPlataformaId chave, UUID idAplicacao) {
        var usuarioPermissaoPlataformaEntityPreparado = new UsuarioPermissaoPlataformaEntity(chave);
        atribuicoes.save(usuarioPermissaoPlataformaEntityPreparado);
        registro.registrar("ATRIBUIR_PERMISSAO_PLATAFORMA", "usuario_permissao_plataforma", idAplicacao,
                chave.getIdUsuario(), chave.getIdPermissao());
    }

    private void remover(UsuarioPermissaoPlataformaEntity atribuicao, UUID idAplicacao) {
        var chave = atribuicao.getId();
        atribuicoes.delete(atribuicao);
        registro.registrar("REMOVER_PERMISSAO_PLATAFORMA", "usuario_permissao_plataforma", idAplicacao,
                chave.getIdUsuario(), chave.getIdPermissao());
    }
}
