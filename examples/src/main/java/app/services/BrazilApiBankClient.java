package app.services;

import app.model.Bank;
import ujfe.core.RestClient;
import ujfe.core.RestResponse;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BrazilApiBankClient {
    private static final String BANKS_ENDPOINT = "https://brasilapi.com.br/api/banks/v1";
    private static final int MAX_BANKS = 30;

    private final RestClient restClient;

    public BrazilApiBankClient() {
        this(RestClient.create(Duration.ofSeconds(4)));
    }

    public BrazilApiBankClient(RestClient restClient) {
        this.restClient = Objects.requireNonNull(restClient, "restClient");
    }

    public List<Bank> fetchBanks() {
        RestResponse response = restClient.get(BANKS_ENDPOINT)
            .requireSuccessful();
        List<Bank> banks = parseBanks(response.body(), MAX_BANKS);
        if (banks.isEmpty()) {
            throw new IllegalStateException("BrasilAPI returned no banks");
        }
        return banks;
    }

    public static List<Bank> fallbackBanks() {
        List<Bank> banks = new ArrayList<>();
        banks.add(new Bank("00000000", "001", "BCO DO BRASIL S.A.", "Banco do Brasil S.A."));
        banks.add(new Bank("00360305", "104", "CAIXA ECONOMICA FEDERAL", "Caixa Economica Federal"));
        banks.add(new Bank("60701190", "341", "ITAU UNIBANCO S.A.", "Itau Unibanco S.A."));
        banks.add(new Bank("90400888", "260", "NU PAGAMENTOS S.A.", "Nu Pagamentos S.A."));
        return banks;
    }

    static List<Bank> parseBanks(String json, int maxItems) {
        Objects.requireNonNull(json, "json");
        List<Bank> banks = new ArrayList<>();
        for (String object : topLevelObjects(json)) {
            String ispb = stringField(object, "ispb").orElse("");
            String code = stringField(object, "code")
                .or(() -> numberField(object, "code"))
                .orElse("");
            String name = stringField(object, "name").orElse("Banco sem nome");
            String fullName = stringField(object, "fullName")
                .or(() -> stringField(object, "full_name"))
                .orElse(name);
            banks.add(new Bank(ispb, code, name, fullName));
            if (banks.size() >= maxItems) {
                return banks;
            }
        }
        return banks;
    }

    private static List<String> topLevelObjects(String json) {
        List<String> objects = new ArrayList<>();
        boolean inString = false;
        boolean escaped = false;
        int depth = 0;
        int objectStart = -1;

        for (int index = 0; index < json.length(); index++) {
            char current = json.charAt(index);
            if (escaped) {
                escaped = false;
                continue;
            }
            if (current == '\\' && inString) {
                escaped = true;
                continue;
            }
            if (current == '"') {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }

            if (current == '{') {
                if (depth == 0) {
                    objectStart = index;
                }
                depth++;
            } else if (current == '}') {
                depth--;
                if (depth == 0 && objectStart >= 0) {
                    objects.add(json.substring(objectStart, index + 1));
                    objectStart = -1;
                }
            }
        }
        return objects;
    }

    private static Optional<String> stringField(String object, String fieldName) {
        int keyIndex = object.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            return Optional.empty();
        }

        int colonIndex = object.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            return Optional.empty();
        }

        int quoteIndex = colonIndex + 1;
        while (quoteIndex < object.length() && Character.isWhitespace(object.charAt(quoteIndex))) {
            quoteIndex++;
        }
        if (quoteIndex >= object.length() || object.charAt(quoteIndex) != '"') {
            return Optional.empty();
        }

        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int index = quoteIndex + 1; index < object.length(); index++) {
            char current = object.charAt(index);
            if (escaped) {
                value.append(unescape(current));
                escaped = false;
                continue;
            }
            if (current == '\\') {
                escaped = true;
                continue;
            }
            if (current == '"') {
                return Optional.of(value.toString());
            }
            value.append(current);
        }
        return Optional.empty();
    }

    private static Optional<String> numberField(String object, String fieldName) {
        int keyIndex = object.indexOf("\"" + fieldName + "\"");
        if (keyIndex < 0) {
            return Optional.empty();
        }

        int colonIndex = object.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            return Optional.empty();
        }

        int start = colonIndex + 1;
        while (start < object.length() && Character.isWhitespace(object.charAt(start))) {
            start++;
        }

        int end = start;
        while (end < object.length() && (Character.isDigit(object.charAt(end)) || object.charAt(end) == '-')) {
            end++;
        }

        if (end == start) {
            return Optional.empty();
        }
        return Optional.of(object.substring(start, end));
    }

    private static char unescape(char escaped) {
        switch (escaped) {
            case '"':
            case '\\':
            case '/':
                return escaped;
            case 'b':
                return '\b';
            case 'f':
                return '\f';
            case 'n':
                return '\n';
            case 'r':
                return '\r';
            case 't':
                return '\t';
            default:
                return escaped;
        }
    }
}
