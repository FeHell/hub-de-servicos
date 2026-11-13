package br.com.hubdeservicos.sessao.service;

import br.com.hubdeservicos.sessao.entity.SessaoEntity;
import br.com.hubdeservicos.sessao.entity.StatusSessao;
import br.com.hubdeservicos.sessao.repository.SessaoRepository;
import br.com.hubdeservicos.sessao.repository.TokenAtualizacaoRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(propagation = Propagation.MANDATORY)
public class RevogacaoSessaoService {

    private final SessaoRepository sessoes;
    private final TokenAtualizacaoRepository tokens;
    private final Clock relogio;

    public RevogacaoSessaoService(SessaoRepository sessoes, TokenAtualizacaoRepository tokens, Clock relogio) {
        this.sessoes = sessoes;
        this.tokens = tokens;
        this.relogio = relogio;
    }

    public void revogar(SessaoEntity sessao, Instant agora) {
        sessao.revogar(agora);
        tokens.revogarFamilia(sessao.getId(), sessao.getIdFamilia(), agora);
    }

    public void revogarDoUsuario(UUID idUsuario) {
        Instant agora = relogio.instant();
        sessoes.findByIdUsuarioAndStatusOrderById(idUsuario, StatusSessao.ATIVO)
                .forEach(sessao -> revogar(sessao, agora));
    }

    public void revogarDaAplicacao(UUID idAplicacao) {
        Instant agora = relogio.instant();
        sessoes.findByIdAplicacaoAndStatusOrderById(idAplicacao, StatusSessao.ATIVO)
                .forEach(sessao -> revogar(sessao, agora));
    }

    public void revogarDoVinculo(UUID idUsuario, UUID idAplicacao) {
        Instant agora = relogio.instant();
        sessoes.findByIdUsuarioAndIdAplicacaoAndStatusOrderById(idUsuario, idAplicacao, StatusSessao.ATIVO)
                .forEach(sessao -> revogar(sessao, agora));
    }
}
