package br.com.hubdeservicos.dto.credenciamento;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.math.BigDecimal;
import java.util.List;

@Schema(name = "PagarmeRecebedor")
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public final class PagarmeRecebedorBean {
    private String code;

    private RegistroBean registerInformation;

    private BancoBean defaultBankAccount;

    private AntecipacaoBean automaticAnticipationSettings;

    private MetadataBean metadata;

    private TransferenciaBean transferSettings;

    public PagarmeRecebedorBean() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public RegistroBean getRegisterInformation() {
        return registerInformation;
    }

    public void setRegisterInformation(RegistroBean registerInformation) {
        this.registerInformation = registerInformation;
    }

    public BancoBean getDefaultBankAccount() {
        return defaultBankAccount;
    }

    public void setDefaultBankAccount(BancoBean defaultBankAccount) {
        this.defaultBankAccount = defaultBankAccount;
    }

    public AntecipacaoBean getAutomaticAnticipationSettings() {
        return automaticAnticipationSettings;
    }

    public void setAutomaticAnticipationSettings(AntecipacaoBean automaticAnticipationSettings) {
        this.automaticAnticipationSettings = automaticAnticipationSettings;
    }

    public MetadataBean getMetadata() {
        return metadata;
    }

    public void setMetadata(MetadataBean metadata) {
        this.metadata = metadata;
    }

    public TransferenciaBean getTransferSettings() {
        return transferSettings;
    }

    public void setTransferSettings(TransferenciaBean transferSettings) {
        this.transferSettings = transferSettings;
    }

    @Override
    public boolean equals(Object objeto) {
        if (this == objeto) {
            return true;
        }
        if (!(objeto instanceof PagarmeRecebedorBean outro)) {
            return false;
        }
        return Objects.equals(code, outro.code) && Objects.equals(registerInformation, outro.registerInformation)
                && Objects.equals(defaultBankAccount, outro.defaultBankAccount)
                && Objects.equals(automaticAnticipationSettings, outro.automaticAnticipationSettings)
                && Objects.equals(metadata, outro.metadata) && Objects.equals(transferSettings, outro.transferSettings);
    }

    @Override
    public int hashCode() {
        int resultado = 0;
        resultado = 31 * resultado + Objects.hashCode(code);
        resultado = 31 * resultado + Objects.hashCode(registerInformation);
        resultado = 31 * resultado + Objects.hashCode(defaultBankAccount);
        resultado = 31 * resultado + Objects.hashCode(automaticAnticipationSettings);
        resultado = 31 * resultado + Objects.hashCode(metadata);
        resultado = 31 * resultado + Objects.hashCode(transferSettings);
        return resultado;
    }

    @Schema(name = "Antecipacao")
    public static final class AntecipacaoBean {
        private boolean enabled;

        public AntecipacaoBean() {
        }

        public boolean getEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof AntecipacaoBean outro)) {
                return false;
            }
            return Objects.equals(enabled, outro.enabled);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(enabled);
            return resultado;
        }

        @Override
        public String toString() {
            return "Antecipacao[" + "enabled=" + enabled + "]";
        }
    }

    @Schema(name = "Metadata")
    public static final class MetadataBean {
        @JsonProperty("companyToken")
        private String companyToken;

        public MetadataBean() {
        }

        public String getCompanyToken() {
            return companyToken;
        }

        public void setCompanyToken(String companyToken) {
            this.companyToken = companyToken;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof MetadataBean outro)) {
                return false;
            }
            return Objects.equals(companyToken, outro.companyToken);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(companyToken);
            return resultado;
        }

        @Override
        public String toString() {
            return "Metadata[" + "companyToken=" + companyToken + "]";
        }
    }

    @Schema(name = "Transferencia")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class TransferenciaBean {
        private boolean transferEnabled;

        private String transferInterval;

        private int transferDay;

        public TransferenciaBean() {
        }

        public boolean getTransferEnabled() {
            return transferEnabled;
        }

        public void setTransferEnabled(boolean transferEnabled) {
            this.transferEnabled = transferEnabled;
        }

        public String getTransferInterval() {
            return transferInterval;
        }

        public void setTransferInterval(String transferInterval) {
            this.transferInterval = transferInterval;
        }

        public int getTransferDay() {
            return transferDay;
        }

        public void setTransferDay(int transferDay) {
            this.transferDay = transferDay;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TransferenciaBean outro)) {
                return false;
            }
            return Objects.equals(transferEnabled, outro.transferEnabled)
                    && Objects.equals(transferInterval, outro.transferInterval)
                    && Objects.equals(transferDay, outro.transferDay);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(transferEnabled);
            resultado = 31 * resultado + Objects.hashCode(transferInterval);
            resultado = 31 * resultado + Objects.hashCode(transferDay);
            return resultado;
        }

        @Override
        public String toString() {
            return "Transferencia[" + "transferEnabled=" + transferEnabled + ", transferInterval=" + transferInterval
                    + ", transferDay=" + transferDay + "]";
        }
    }

    @Override
    public String toString() {
        return "PagarmeRecebedor";
    }

    public enum UnidadeMonetaria {
        REAIS, CENTAVOS
    }

    @Schema(name = "Registro")
    public sealed interface RegistroBean permits IndividualBean, CorporationBean {
    }

    @Schema(name = "Individual")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class IndividualBean implements RegistroBean {
        private String type;

        private String name;

        private String email;

        private String document;

        private String siteUrl;

        private String birthdate;

        private BigDecimal monthlyIncome;

        private String professionalOccupation;

        private EnderecoBean address;

        private List<TelefoneBean> phoneNumbers;

        public IndividualBean() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDocument() {
            return document;
        }

        public void setDocument(String document) {
            this.document = document;
        }

        public String getSiteUrl() {
            return siteUrl;
        }

        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        public String getBirthdate() {
            return birthdate;
        }

        public void setBirthdate(String birthdate) {
            this.birthdate = birthdate;
        }

        public BigDecimal getMonthlyIncome() {
            return monthlyIncome;
        }

        public void setMonthlyIncome(BigDecimal monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
        }

        public String getProfessionalOccupation() {
            return professionalOccupation;
        }

        public void setProfessionalOccupation(String professionalOccupation) {
            this.professionalOccupation = professionalOccupation;
        }

        public EnderecoBean getAddress() {
            return address;
        }

        public void setAddress(EnderecoBean address) {
            this.address = address;
        }

        public List<TelefoneBean> getPhoneNumbers() {
            return phoneNumbers;
        }

        public void setPhoneNumbers(List<TelefoneBean> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof IndividualBean outro)) {
                return false;
            }
            return Objects.equals(type, outro.type) && Objects.equals(name, outro.name)
                    && Objects.equals(email, outro.email) && Objects.equals(document, outro.document)
                    && Objects.equals(siteUrl, outro.siteUrl) && Objects.equals(birthdate, outro.birthdate)
                    && Objects.equals(monthlyIncome, outro.monthlyIncome)
                    && Objects.equals(professionalOccupation, outro.professionalOccupation)
                    && Objects.equals(address, outro.address) && Objects.equals(phoneNumbers, outro.phoneNumbers);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(type);
            resultado = 31 * resultado + Objects.hashCode(name);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(document);
            resultado = 31 * resultado + Objects.hashCode(siteUrl);
            resultado = 31 * resultado + Objects.hashCode(birthdate);
            resultado = 31 * resultado + Objects.hashCode(monthlyIncome);
            resultado = 31 * resultado + Objects.hashCode(professionalOccupation);
            resultado = 31 * resultado + Objects.hashCode(address);
            resultado = 31 * resultado + Objects.hashCode(phoneNumbers);
            return resultado;
        }

        @Override
        public String toString() {
            return "Individual";
        }
    }

    @Schema(name = "Corporation")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class CorporationBean implements RegistroBean {
        private String type;

        private String companyName;

        private String tradingName;

        private String email;

        private String document;

        private String siteUrl;

        private BigDecimal annualRevenue;

        private String corporationType;

        private String foundingDate;

        private EnderecoBean mainAddress;

        private List<TelefoneBean> phoneNumbers;

        private List<RepresentanteBean> managingPartners;

        public CorporationBean() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getTradingName() {
            return tradingName;
        }

        public void setTradingName(String tradingName) {
            this.tradingName = tradingName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDocument() {
            return document;
        }

        public void setDocument(String document) {
            this.document = document;
        }

        public String getSiteUrl() {
            return siteUrl;
        }

        public void setSiteUrl(String siteUrl) {
            this.siteUrl = siteUrl;
        }

        public BigDecimal getAnnualRevenue() {
            return annualRevenue;
        }

        public void setAnnualRevenue(BigDecimal annualRevenue) {
            this.annualRevenue = annualRevenue;
        }

        public String getCorporationType() {
            return corporationType;
        }

        public void setCorporationType(String corporationType) {
            this.corporationType = corporationType;
        }

        public String getFoundingDate() {
            return foundingDate;
        }

        public void setFoundingDate(String foundingDate) {
            this.foundingDate = foundingDate;
        }

        public EnderecoBean getMainAddress() {
            return mainAddress;
        }

        public void setMainAddress(EnderecoBean mainAddress) {
            this.mainAddress = mainAddress;
        }

        public List<TelefoneBean> getPhoneNumbers() {
            return phoneNumbers;
        }

        public void setPhoneNumbers(List<TelefoneBean> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }

        public List<RepresentanteBean> getManagingPartners() {
            return managingPartners;
        }

        public void setManagingPartners(List<RepresentanteBean> managingPartners) {
            this.managingPartners = managingPartners;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof CorporationBean outro)) {
                return false;
            }
            return Objects.equals(type, outro.type) && Objects.equals(companyName, outro.companyName)
                    && Objects.equals(tradingName, outro.tradingName) && Objects.equals(email, outro.email)
                    && Objects.equals(document, outro.document) && Objects.equals(siteUrl, outro.siteUrl)
                    && Objects.equals(annualRevenue, outro.annualRevenue)
                    && Objects.equals(corporationType, outro.corporationType)
                    && Objects.equals(foundingDate, outro.foundingDate)
                    && Objects.equals(mainAddress, outro.mainAddress)
                    && Objects.equals(phoneNumbers, outro.phoneNumbers)
                    && Objects.equals(managingPartners, outro.managingPartners);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(type);
            resultado = 31 * resultado + Objects.hashCode(companyName);
            resultado = 31 * resultado + Objects.hashCode(tradingName);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(document);
            resultado = 31 * resultado + Objects.hashCode(siteUrl);
            resultado = 31 * resultado + Objects.hashCode(annualRevenue);
            resultado = 31 * resultado + Objects.hashCode(corporationType);
            resultado = 31 * resultado + Objects.hashCode(foundingDate);
            resultado = 31 * resultado + Objects.hashCode(mainAddress);
            resultado = 31 * resultado + Objects.hashCode(phoneNumbers);
            resultado = 31 * resultado + Objects.hashCode(managingPartners);
            return resultado;
        }

        @Override
        public String toString() {
            return "Corporation";
        }
    }

    @Schema(name = "Representante")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class RepresentanteBean {
        private String type;

        private String name;

        private String email;

        private String document;

        private String birthdate;

        private BigDecimal monthlyIncome;

        private String professionalOccupation;

        @JsonProperty("self_declared_legal_representative")
        private boolean representanteLegal;

        private EnderecoBean address;

        private List<TelefoneBean> phoneNumbers;

        public RepresentanteBean() {
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDocument() {
            return document;
        }

        public void setDocument(String document) {
            this.document = document;
        }

        public String getBirthdate() {
            return birthdate;
        }

        public void setBirthdate(String birthdate) {
            this.birthdate = birthdate;
        }

        public BigDecimal getMonthlyIncome() {
            return monthlyIncome;
        }

        public void setMonthlyIncome(BigDecimal monthlyIncome) {
            this.monthlyIncome = monthlyIncome;
        }

        public String getProfessionalOccupation() {
            return professionalOccupation;
        }

        public void setProfessionalOccupation(String professionalOccupation) {
            this.professionalOccupation = professionalOccupation;
        }

        public boolean getRepresentanteLegal() {
            return representanteLegal;
        }

        public void setRepresentanteLegal(boolean representanteLegal) {
            this.representanteLegal = representanteLegal;
        }

        public EnderecoBean getAddress() {
            return address;
        }

        public void setAddress(EnderecoBean address) {
            this.address = address;
        }

        public List<TelefoneBean> getPhoneNumbers() {
            return phoneNumbers;
        }

        public void setPhoneNumbers(List<TelefoneBean> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof RepresentanteBean outro)) {
                return false;
            }
            return Objects.equals(type, outro.type) && Objects.equals(name, outro.name)
                    && Objects.equals(email, outro.email) && Objects.equals(document, outro.document)
                    && Objects.equals(birthdate, outro.birthdate) && Objects.equals(monthlyIncome, outro.monthlyIncome)
                    && Objects.equals(professionalOccupation, outro.professionalOccupation)
                    && Objects.equals(representanteLegal, outro.representanteLegal)
                    && Objects.equals(address, outro.address) && Objects.equals(phoneNumbers, outro.phoneNumbers);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(type);
            resultado = 31 * resultado + Objects.hashCode(name);
            resultado = 31 * resultado + Objects.hashCode(email);
            resultado = 31 * resultado + Objects.hashCode(document);
            resultado = 31 * resultado + Objects.hashCode(birthdate);
            resultado = 31 * resultado + Objects.hashCode(monthlyIncome);
            resultado = 31 * resultado + Objects.hashCode(professionalOccupation);
            resultado = 31 * resultado + Objects.hashCode(representanteLegal);
            resultado = 31 * resultado + Objects.hashCode(address);
            resultado = 31 * resultado + Objects.hashCode(phoneNumbers);
            return resultado;
        }

        @Override
        public String toString() {
            return "Representante";
        }
    }

    @Schema(name = "Endereco")
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class EnderecoBean {
        private String street;

        private String complementary;

        private String streetNumber;

        private String neighborhood;

        private String city;

        private String state;

        private String zipCode;

        private String referencePoint;

        public EnderecoBean() {
        }

        public String getStreet() {
            return street;
        }

        public void setStreet(String street) {
            this.street = street;
        }

        public String getComplementary() {
            return complementary;
        }

        public void setComplementary(String complementary) {
            this.complementary = complementary;
        }

        public String getStreetNumber() {
            return streetNumber;
        }

        public void setStreetNumber(String streetNumber) {
            this.streetNumber = streetNumber;
        }

        public String getNeighborhood() {
            return neighborhood;
        }

        public void setNeighborhood(String neighborhood) {
            this.neighborhood = neighborhood;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getZipCode() {
            return zipCode;
        }

        public void setZipCode(String zipCode) {
            this.zipCode = zipCode;
        }

        public String getReferencePoint() {
            return referencePoint;
        }

        public void setReferencePoint(String referencePoint) {
            this.referencePoint = referencePoint;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof EnderecoBean outro)) {
                return false;
            }
            return Objects.equals(street, outro.street) && Objects.equals(complementary, outro.complementary)
                    && Objects.equals(streetNumber, outro.streetNumber)
                    && Objects.equals(neighborhood, outro.neighborhood) && Objects.equals(city, outro.city)
                    && Objects.equals(state, outro.state) && Objects.equals(zipCode, outro.zipCode)
                    && Objects.equals(referencePoint, outro.referencePoint);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(street);
            resultado = 31 * resultado + Objects.hashCode(complementary);
            resultado = 31 * resultado + Objects.hashCode(streetNumber);
            resultado = 31 * resultado + Objects.hashCode(neighborhood);
            resultado = 31 * resultado + Objects.hashCode(city);
            resultado = 31 * resultado + Objects.hashCode(state);
            resultado = 31 * resultado + Objects.hashCode(zipCode);
            resultado = 31 * resultado + Objects.hashCode(referencePoint);
            return resultado;
        }

        @Override
        public String toString() {
            return "Endereco";
        }
    }

    @Schema(name = "Telefone")
    public static final class TelefoneBean {
        private String ddd;

        private String number;

        private String type;

        public TelefoneBean() {
        }

        public String getDdd() {
            return ddd;
        }

        public void setDdd(String ddd) {
            this.ddd = ddd;
        }

        public String getNumber() {
            return number;
        }

        public void setNumber(String number) {
            this.number = number;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof TelefoneBean outro)) {
                return false;
            }
            return Objects.equals(ddd, outro.ddd) && Objects.equals(number, outro.number)
                    && Objects.equals(type, outro.type);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(ddd);
            resultado = 31 * resultado + Objects.hashCode(number);
            resultado = 31 * resultado + Objects.hashCode(type);
            return resultado;
        }

        @Override
        public String toString() {
            return "Telefone";
        }
    }

    @Schema(name = "Banco")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    public static final class BancoBean {
        private String holderName;

        private String holderType;

        private String holderDocument;

        private String bank;

        private String branchNumber;

        private String branchCheckDigit;

        private String accountNumber;

        private String accountCheckDigit;

        private String type;

        public BancoBean() {
        }

        public String getHolderName() {
            return holderName;
        }

        public void setHolderName(String holderName) {
            this.holderName = holderName;
        }

        public String getHolderType() {
            return holderType;
        }

        public void setHolderType(String holderType) {
            this.holderType = holderType;
        }

        public String getHolderDocument() {
            return holderDocument;
        }

        public void setHolderDocument(String holderDocument) {
            this.holderDocument = holderDocument;
        }

        public String getBank() {
            return bank;
        }

        public void setBank(String bank) {
            this.bank = bank;
        }

        public String getBranchNumber() {
            return branchNumber;
        }

        public void setBranchNumber(String branchNumber) {
            this.branchNumber = branchNumber;
        }

        public String getBranchCheckDigit() {
            return branchCheckDigit;
        }

        public void setBranchCheckDigit(String branchCheckDigit) {
            this.branchCheckDigit = branchCheckDigit;
        }

        public String getAccountNumber() {
            return accountNumber;
        }

        public void setAccountNumber(String accountNumber) {
            this.accountNumber = accountNumber;
        }

        public String getAccountCheckDigit() {
            return accountCheckDigit;
        }

        public void setAccountCheckDigit(String accountCheckDigit) {
            this.accountCheckDigit = accountCheckDigit;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object objeto) {
            if (this == objeto) {
                return true;
            }
            if (!(objeto instanceof BancoBean outro)) {
                return false;
            }
            return Objects.equals(holderName, outro.holderName) && Objects.equals(holderType, outro.holderType)
                    && Objects.equals(holderDocument, outro.holderDocument) && Objects.equals(bank, outro.bank)
                    && Objects.equals(branchNumber, outro.branchNumber)
                    && Objects.equals(branchCheckDigit, outro.branchCheckDigit)
                    && Objects.equals(accountNumber, outro.accountNumber)
                    && Objects.equals(accountCheckDigit, outro.accountCheckDigit) && Objects.equals(type, outro.type);
        }

        @Override
        public int hashCode() {
            int resultado = 0;
            resultado = 31 * resultado + Objects.hashCode(holderName);
            resultado = 31 * resultado + Objects.hashCode(holderType);
            resultado = 31 * resultado + Objects.hashCode(holderDocument);
            resultado = 31 * resultado + Objects.hashCode(bank);
            resultado = 31 * resultado + Objects.hashCode(branchNumber);
            resultado = 31 * resultado + Objects.hashCode(branchCheckDigit);
            resultado = 31 * resultado + Objects.hashCode(accountNumber);
            resultado = 31 * resultado + Objects.hashCode(accountCheckDigit);
            resultado = 31 * resultado + Objects.hashCode(type);
            return resultado;
        }

        @Override
        public String toString() {
            return "Banco";
        }
    }
}
