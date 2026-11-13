package br.com.gerenciamentoplataforma.administracao.service;

import br.com.gerenciamentoplataforma.common.StatusCadastro;
import br.com.gerenciamentoplataforma.permissao.entity.PermissaoEntity;
import br.com.gerenciamentoplataforma.permissao.repository.PermissaoRepository;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioAplicacaoId;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaEntity;
import br.com.gerenciamentoplataforma.usuario.entity.UsuarioPermissaoPlataformaId;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioAplicacaoRepository;
import br.com.gerenciamentoplataforma.usuario.repository.UsuarioPermissaoPlataformaRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class PermissoesPlataformaService {

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

    List<String> substituir(UUID idUsuario, UUID idAplicacao, List<String> codigos) {
        var atuais = atribuicoes.findByIdIdUsuario(idUsuario);
        Set<UUID> idsAtuais = atuais.stream().map(a -> a.getId().getIdPermissao()).collect(Collectors.toSet());
        List<String> solicitados = codigos.stream().distinct().sorted().toList();
        List<PermissaoEntity> desejadas = solicitados.isEmpty() ? List.of()
                : catalogo.findByNomePlataformaIsNotNullAndCodigoIn(solicitados);
        if (desejadas.size() != solicitados.size() || desejadas.stream().anyMatch(p ->
                p.getStatus() != StatusCadastro.ATIVO && !idsAtuais.contains(p.getId()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Há permissões da plataforma inválidas ou inativas.");
        }

        if (desejadas.stream().anyMatch(p -> p.getStatus() == StatusCadastro.ATIVO)) {
            var chave = new UsuarioAplicacaoId(idUsuario, idAplicacao);
            var vinculo = vinculos.findById(chave).orElseGet(() -> vinculos.save(new UsuarioAplicacaoEntity(chave)));
            vinculo.definirAtivo(true);
        }

        Set<UUID> idsDesejados = new HashSet<>();
        for (PermissaoEntity permissao : desejadas) {
            UUID idPermissao = permissao.getId();
            idsDesejados.add(idPermissao);
            if (!idsAtuais.contains(idPermissao)) {
                atribuicoes.save(new UsuarioPermissaoPlataformaEntity(new UsuarioPermissaoPlataformaId(idUsuario, idPermissao)));
                registro.registrar("ATRIBUIR_PERMISSAO_PLATAFORMA", "usuario_permissao_plataforma",
                        idAplicacao, idUsuario, idPermissao);
            }
        }
        for (var atribuicao : atuais) {
            UUID idPermissao = atribuicao.getId().getIdPermissao();
            if (!idsDesejados.contains(idPermissao)) {
                atribuicoes.delete(atribuicao);
                registro.registrar("REMOVER_PERMISSAO_PLATAFORMA", "usuario_permissao_plataforma",
                        idAplicacao, idUsuario, idPermissao);
            }
        }
        return solicitados;
    }
}
