package ujfe.live;

import ujfe.core.ClientState;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class LiveHttpCodec {
    private LiveHttpCodec() {
    }

    public static String livePayload(LiveRenderResult result) {
        Objects.requireNonNull(result, "result");
        return "{\"html\":\"" + jsonEscape(result.html()) + "\",\"css\":\"" + jsonEscape(result.css()) + "\"}";
    }

    public static Set<String> parseCssClasses(String classes) {
        Set<String> parsedClasses = new LinkedHashSet<>();
        if (classes == null || classes.trim().isEmpty()) {
            return parsedClasses;
        }

        for (String className : classes.trim().split("\\s+")) {
            if (!className.isBlank()) {
                parsedClasses.add(className);
            }
        }
        return parsedClasses;
    }

    public static String extractEventId(String json) {
        Objects.requireNonNull(json, "json");
        int keyIndex = findKey(json, "eventId").orElse(-1);
        if (keyIndex < 0) {
            throw new IllegalArgumentException("Missing eventId");
        }

        int colonIndex = json.indexOf(':', keyIndex);
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid event payload");
        }

        int valueStart = findStringStart(json, colonIndex + 1);
        StringBuilder value = new StringBuilder();
        for (int index = valueStart + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"') {
                return value.toString();
            }
            if (current == '\\') {
                index++;
                if (index >= json.length()) {
                    throw new IllegalArgumentException("Invalid escaped eventId");
                }
                value.append(unescape(json.charAt(index)));
            } else {
                value.append(current);
            }
        }
        throw new IllegalArgumentException("Unterminated eventId");
    }

    public static ClientState extractClientState(String json) {
        Objects.requireNonNull(json, "json");
        String cookieHeader = extractStringField(json, "cookies").orElse("");
        Map<String, String> localStorage = extractStringMapField(json, "localStorage");
        return ClientState.of(parseCookies(cookieHeader), localStorage);
    }

    public static Map<String, String> parseCookies(String cookieHeader) {
        Map<String, String> cookies = new LinkedHashMap<>();
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            return cookies;
        }

        String[] pairs = cookieHeader.split(";");
        for (String pair : pairs) {
            String trimmed = pair.trim();
            if (trimmed.isEmpty()) {
                continue;
            }

            int separator = trimmed.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            String name = trimmed.substring(0, separator).trim();
            String value = trimmed.substring(separator + 1).trim();
            if (!name.isEmpty()) {
                cookies.put(name, value);
            }
        }
        return cookies;
    }

    private static Optional<String> extractStringField(String json, String fieldName) {
        Optional<Integer> keyIndex = findKey(json, fieldName);
        if (keyIndex.isEmpty()) {
            return Optional.empty();
        }

        int colonIndex = json.indexOf(':', keyIndex.get());
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid JSON field: " + fieldName);
        }

        int valueStart = findStringStart(json, colonIndex + 1);
        JsonString jsonString = readJsonString(json, valueStart);
        return Optional.of(jsonString.value());
    }

    private static Map<String, String> extractStringMapField(String json, String fieldName) {
        Optional<Integer> keyIndex = findKey(json, fieldName);
        if (keyIndex.isEmpty()) {
            return Map.of();
        }

        int colonIndex = json.indexOf(':', keyIndex.get());
        if (colonIndex < 0) {
            throw new IllegalArgumentException("Invalid JSON field: " + fieldName);
        }

        int objectStart = findObjectStart(json, colonIndex + 1);
        Map<String, String> values = new LinkedHashMap<>();
        int index = objectStart + 1;
        while (index < json.length()) {
            index = skipWhitespace(json, index);
            if (index < json.length() && json.charAt(index) == '}') {
                return values;
            }

            if (index >= json.length() || json.charAt(index) != '"') {
                throw new IllegalArgumentException("Expected string key in " + fieldName);
            }
            JsonString key = readJsonString(json, index);
            index = skipWhitespace(json, key.nextIndex());
            if (index >= json.length() || json.charAt(index) != ':') {
                throw new IllegalArgumentException("Expected ':' in " + fieldName);
            }
            index = skipWhitespace(json, index + 1);

            if (startsWith(json, index, "null")) {
                index += 4;
            } else {
                if (index >= json.length() || json.charAt(index) != '"') {
                    throw new IllegalArgumentException("Expected string value in " + fieldName);
                }
                JsonString value = readJsonString(json, index);
                values.put(key.value(), value.value());
                index = value.nextIndex();
            }

            index = skipWhitespace(json, index);
            if (index < json.length() && json.charAt(index) == ',') {
                index++;
                continue;
            }
            if (index < json.length() && json.charAt(index) == '}') {
                return values;
            }
            throw new IllegalArgumentException("Expected ',' or '}' in " + fieldName);
        }
        throw new IllegalArgumentException("Unterminated object field: " + fieldName);
    }

    private static Optional<Integer> findKey(String json, String fieldName) {
        return Optional.of(json.indexOf("\"" + fieldName + "\""))
                .filter(index -> index >= 0);
    }

    private static int findStringStart(String json, int startIndex) {
        for (int index = startIndex; index < json.length(); index++) {
            char current = json.charAt(index);
            if (!Character.isWhitespace(current)) {
                if (current != '"') {
                    throw new IllegalArgumentException("eventId must be a string");
                }
                return index;
            }
        }
        throw new IllegalArgumentException("Missing eventId value");
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
                throw new IllegalArgumentException("Unsupported JSON escape: \\" + escaped);
        }
    }

    private static int findObjectStart(String json, int startIndex) {
        for (int index = startIndex; index < json.length(); index++) {
            char current = json.charAt(index);
            if (!Character.isWhitespace(current)) {
                if (current != '{') {
                    throw new IllegalArgumentException("Expected object");
                }
                return index;
            }
        }
        throw new IllegalArgumentException("Missing object value");
    }

    private static JsonString readJsonString(String json, int quoteIndex) {
        if (quoteIndex >= json.length() || json.charAt(quoteIndex) != '"') {
            throw new IllegalArgumentException("Expected JSON string");
        }

        StringBuilder value = new StringBuilder();
        for (int index = quoteIndex + 1; index < json.length(); index++) {
            char current = json.charAt(index);
            if (current == '"') {
                return new JsonString(value.toString(), index + 1);
            }
            if (current == '\\') {
                index++;
                if (index >= json.length()) {
                    throw new IllegalArgumentException("Invalid JSON escape");
                }
                char escaped = json.charAt(index);
                if (escaped == 'u') {
                    if (index + 4 >= json.length()) {
                        throw new IllegalArgumentException("Invalid unicode escape");
                    }
                    value.append((char) Integer.parseInt(json.substring(index + 1, index + 5), 16));
                    index += 4;
                } else {
                    value.append(unescape(escaped));
                }
            } else {
                value.append(current);
            }
        }
        throw new IllegalArgumentException("Unterminated JSON string");
    }

    private static int skipWhitespace(String json, int startIndex) {
        int index = startIndex;
        while (index < json.length() && Character.isWhitespace(json.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean startsWith(String value, int offset, String prefix) {
        return offset >= 0 && offset + prefix.length() <= value.length()
                && value.startsWith(prefix, offset);
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (current < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) current));
                    } else {
                        escaped.append(current);
                    }
                    break;
            }
        }
        return escaped.toString();
    }

    private static final class JsonString {
        private final String value;
        private final int nextIndex;

        private JsonString(String value, int nextIndex) {
            this.value = value;
            this.nextIndex = nextIndex;
        }

        private String value() {
            return value;
        }

        private int nextIndex() {
            return nextIndex;
        }
    }
}
