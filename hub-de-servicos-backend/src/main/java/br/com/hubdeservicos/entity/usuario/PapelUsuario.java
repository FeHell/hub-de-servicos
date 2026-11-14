package br.com.hubdeservicos.entity.usuario;

public enum PapelUsuario {
    EMPRESA("PLATAFORMA_EMPRESA"),
    TRABALHADOR("PLATAFORMA_TRABALHADOR");

    private final String codigoPermissao;

    PapelUsuario(String codigoPermissao) {
        this.codigoPermissao = codigoPermissao;
    }

    public String codigoPermissao() {
        return codigoPermissao;
    }
}
