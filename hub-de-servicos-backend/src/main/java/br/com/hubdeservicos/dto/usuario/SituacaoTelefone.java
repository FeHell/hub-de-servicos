package br.com.hubdeservicos.dto.usuario;

import br.com.hubdeservicos.entity.usuario.UsuarioEntity;

public enum SituacaoTelefone {
    NAO_INFORMADO, PENDENTE, VERIFICADO;

    public static SituacaoTelefone de(UsuarioEntity usuario) {
        if (usuario.getTelefone() == null) return NAO_INFORMADO;
        return usuario.getTelefoneVerificadoEm() == null ? PENDENTE : VERIFICADO;
    }
}
