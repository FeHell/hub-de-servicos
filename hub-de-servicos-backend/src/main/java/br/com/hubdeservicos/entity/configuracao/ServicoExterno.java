package br.com.hubdeservicos.entity.configuracao;

import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

public enum ServicoExterno {
    INFOBIP, STONE;

    private static final Pattern CONTA_INFOBIP = Pattern
            .compile("[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.api\\.infobip\\.com");

    public static Optional<URI> origemInfobip(String valor) {
        try {
            URI uri = URI.create(valor.strip());
            if ("https".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null
                    && CONTA_INFOBIP.matcher(uri.getHost().toLowerCase(Locale.ROOT)).matches()
                    && (uri.getPort() == -1 || uri.getPort() == 443) && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null
                    && (uri.getRawPath().isEmpty() || "/".equals(uri.getRawPath()))) {
                return Optional.of(URI.create("https://" + uri.getHost().toLowerCase(Locale.ROOT)));
            }
        } catch (IllegalArgumentException ignorado) {
            // A entrada rejeitada pode conter dados sensíveis.
        }
        return Optional.empty();
    }
}
