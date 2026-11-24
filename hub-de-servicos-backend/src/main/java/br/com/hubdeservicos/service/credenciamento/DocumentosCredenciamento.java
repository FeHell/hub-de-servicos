package br.com.hubdeservicos.service.credenciamento;

import java.util.Locale;

final class DocumentosCredenciamento {
    private DocumentosCredenciamento() {}

    static boolean cpfValido(String valor) {
        if (!valor.matches("[0-9]{11}|[0-9]{3}\\.[0-9]{3}\\.[0-9]{3}-[0-9]{2}")) return false;
        String cpf = valor.replaceAll("[.-]", "");
        return cpf.chars().distinct().count() > 1
                && digito(cpf, 9, 10, 1) == cpf.charAt(9) - '0'
                && digito(cpf, 10, 11, 1) == cpf.charAt(10) - '0';
    }

    static boolean cnpjValido(String valor) {
        String entrada = valor.toUpperCase(Locale.ROOT);
        if (!entrada.matches("[0-9A-Z]{12}[0-9]{2}|[0-9A-Z]{2}\\.[0-9A-Z]{3}\\.[0-9A-Z]{3}/[0-9A-Z]{4}-[0-9]{2}")) {
            return false;
        }
        String cnpj = entrada.replaceAll("[./-]", "");
        return cnpj.chars().distinct().count() > 1
                && digito(cnpj, 12, 5, 9) == cnpj.charAt(12) - '0'
                && digito(cnpj, 13, 6, 9) == cnpj.charAt(13) - '0';
    }

    private static int digito(String valor, int tamanho, int peso, int reinicio) {
        int soma = 0;
        for (int indice = 0; indice < tamanho; indice++) {
            soma += (valor.charAt(indice) - '0') * peso--;
            if (peso == 1) peso = reinicio;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
