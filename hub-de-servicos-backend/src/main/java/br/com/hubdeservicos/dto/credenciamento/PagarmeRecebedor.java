package br.com.hubdeservicos.dto.credenciamento;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record PagarmeRecebedor(String code, Registro registerInformation, Banco defaultBankAccount,
                              Antecipacao automaticAnticipationSettings, Metadata metadata,
                              Transferencia transferSettings) {
    public PagarmeRecebedor(String code, Registro registro, Banco banco, UUID usuario) {
        this(code, registro, banco, new Antecipacao(false),
                new Metadata(java.util.Objects.requireNonNull(usuario).toString()),
                new Transferencia(true, "Daily", 0));
    }

    public record Antecipacao(boolean enabled) {}

    public record Metadata(@JsonProperty("companyToken") String companyToken) {}

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Transferencia(boolean transferEnabled, String transferInterval, int transferDay) {}

    @Override public String toString() { return "PagarmeRecebedor"; }

    public enum UnidadeMonetaria { REAIS, CENTAVOS }

    public sealed interface Registro permits Individual, Corporation {}

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Individual(String type, String name, String email, String document, String siteUrl,
                             String birthdate, BigDecimal monthlyIncome,
                             String professionalOccupation, Endereco address,
                             List<Telefone> phoneNumbers) implements Registro {
        @Override public String toString() { return "Individual"; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Corporation(String type, String companyName, String tradingName, String email,
                              String document, String siteUrl, BigDecimal annualRevenue,
                              String corporationType, String foundingDate, Endereco mainAddress,
                              List<Telefone> phoneNumbers, List<Representante> managingPartners) implements Registro {
        @Override public String toString() { return "Corporation"; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Representante(String type, String name, String email, String document,
                                String birthdate, BigDecimal monthlyIncome, String professionalOccupation,
                                @JsonProperty("self_declared_legal_representative") boolean representanteLegal,
                                Endereco address, List<Telefone> phoneNumbers) {
        @Override public String toString() { return "Representante"; }
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Endereco(String street, String complementary, String streetNumber, String neighborhood,
                           String city, String state, String zipCode, String referencePoint) {
        @Override public String toString() { return "Endereco"; }
    }

    public record Telefone(String ddd, String number, String type) {
        @Override public String toString() { return "Telefone"; }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public record Banco(String holderName, String holderType, String holderDocument, String bank, String branchNumber,
                        String branchCheckDigit, String accountNumber, String accountCheckDigit, String type) {
        @Override public String toString() { return "Banco"; }
    }
}
