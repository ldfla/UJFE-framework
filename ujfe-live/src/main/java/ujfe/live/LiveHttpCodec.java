package ujfe.live;

import ujfe.core.ClientState;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;

public final class LiveHttpCodec {
    public static final int DEFAULT_MAX_JSON_PAYLOAD_BYTES = 1_048_576;
    private static final Logger LOGGER = Logger.getLogger(LiveHttpCodec.class.getName());

    private LiveHttpCodec() {
    }

    public static LiveHttpEventPayload parseEventPayload(String json) {
        return parseEventPayload(json, DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public static LiveHttpEventPayload parseEventPayload(String json, int maxPayloadBytes) {
        try {
            String payload = validateJsonObjectPayload(json, maxPayloadBytes);
            String eventId = extractRequiredEventId(payload, maxPayloadBytes);
            String value = extractOptionalEventValue(payload);
            ClientState clientState = extractClientStateFromPayload(payload);
            return new LiveHttpEventPayload(eventId, clientState, value);
        } catch (LiveHttpCodecException exception) {
            throw normalizeInvalidPayloadLimit(exception, maxPayloadBytes);
        }
    }

    public static ClientState parseStatePayload(String json) {
        return parseStatePayload(json, DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public static ClientState parseStatePayload(String json, int maxPayloadBytes) {
        try {
            String payload = validateJsonObjectPayload(json, maxPayloadBytes);
            JsonField stateField = requireObjectField(
                    payload,
                    "clientState",
                    "Live state payload is missing clientState.",
                    maxPayloadBytes
            );
            return extractClientStateFromObject(payload.substring(stateField.valueStart(), stateField.valueEnd()));
        } catch (LiveHttpCodecException exception) {
            throw normalizeInvalidPayloadLimit(exception, maxPayloadBytes);
        }
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
        return parseEventPayload(json).eventId();
    }

    public static ClientState extractClientState(String json) {
        try {
            String payload = validateJsonObjectPayload(json, DEFAULT_MAX_JSON_PAYLOAD_BYTES);
            return extractClientStateFromPayload(payload);
        } catch (LiveHttpCodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidPayload("Invalid live JSON payload.");
        }
    }

    public static void requirePayloadSize(long payloadSizeBytes, int maxPayloadBytes) {
        validateMaxPayloadBytes(maxPayloadBytes);
        if (payloadSizeBytes >= 0 && payloadSizeBytes > maxPayloadBytes) {
            throw new LiveHttpCodecException(
                    LiveHttpFailureCategory.PAYLOAD_TOO_LARGE,
                    "Live JSON payload exceeds maximum size.",
                    413,
                    maxPayloadBytes,
                    payloadSizeBytes
            );
        }
    }

    public static String readPayload(Reader reader, int maxPayloadBytes) throws IOException {
        Objects.requireNonNull(reader, "reader");
        validateMaxPayloadBytes(maxPayloadBytes);
        StringBuilder body = new StringBuilder();
        char[] buffer = new char[1024];
        int bytes = 0;
        int read;
        while ((read = reader.read(buffer)) >= 0) {
            String chunk = new String(buffer, 0, read);
            bytes += chunk.getBytes(StandardCharsets.UTF_8).length;
            requirePayloadSize(bytes, maxPayloadBytes);
            body.append(chunk);
        }
        return body.toString();
    }

    public static void logRejectedPayload(
            LiveHttpCodecException exception,
            String runtimeAdapter,
            String traceId
    ) {
        Objects.requireNonNull(exception, "exception");
        String adapter = safeLogValue(runtimeAdapter, "unknown");
        String trace = safeLogValue(traceId, "unavailable");
        LOGGER.warning(() -> "event=ujfe.live_http_payload_rejected"
                + " category=" + exception.category().logValue()
                + " adapter=" + adapter
                + " httpStatus=" + exception.httpStatus()
                + " payloadLimitBytes=" + exception.payloadLimitBytes()
                + " payloadSizeBytes=" + exception.payloadSizeBytes()
                + " traceId=" + trace
                + " message=\"" + exception.safeMessage() + "\"");
    }

    public static void logRejectedCsrf(
            LiveCsrfException exception,
            String runtimeAdapter,
            String traceId
    ) {
        Objects.requireNonNull(exception, "exception");
        String adapter = safeLogValue(runtimeAdapter, "unknown");
        String trace = safeLogValue(traceId, "unavailable");
        LOGGER.warning(() -> "event=ujfe.live_csrf_rejected"
                + " category=" + exception.category().logValue()
                + " adapter=" + adapter
                + " hasHeader=" + exception.isCsrfHeaderPresent()
                + " traceId=" + trace
                + " message=\"" + exception.safeMessage() + "\"");
    }

    public static void logRejectedRateLimit(
            LiveRateLimitException exception,
            String runtimeAdapter,
            String traceId
    ) {
        Objects.requireNonNull(exception, "exception");
        String adapter = safeLogValue(runtimeAdapter, "unknown");
        String trace = safeLogValue(traceId, "unavailable");
        LOGGER.warning(() -> "event=ujfe.live_rate_limit_rejected"
                + " reason=rate_limit_exceeded"
                + " endpoint=" + safeLogValue(exception.endpointPath(), "unknown")
                + " keyType=" + exception.keyType().logValue()
                + " adapter=" + adapter
                + " retryAfterSeconds=" + exception.retryAfterSeconds().orElse(-1)
                + " traceId=" + trace
                + " message=\"" + exception.safeMessage() + "\"");
    }

    static String validateJsonObjectPayload(String json, int maxPayloadBytes) {
        Objects.requireNonNull(json, "json");
        validateMaxPayloadBytes(maxPayloadBytes);
        requirePayloadSize(json.getBytes(StandardCharsets.UTF_8).length, maxPayloadBytes);
        String trimmed = json.trim();
        if (trimmed.isEmpty()) {
            throw new LiveHttpCodecException(
                    LiveHttpFailureCategory.EMPTY_PAYLOAD,
                    "Live JSON payload is empty.",
                    400,
                    maxPayloadBytes,
                    0
            );
        }
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) {
            throw invalidPayload("Invalid live JSON payload.");
        }
        if (isEmptyObject(trimmed)) {
            throw new LiveHttpCodecException(
                    LiveHttpFailureCategory.EMPTY_JSON,
                    "Live JSON payload must not be empty.",
                    400,
                    maxPayloadBytes,
                    trimmed.getBytes(StandardCharsets.UTF_8).length
            );
        }
        validateJsonStructure(trimmed);
        return trimmed;
    }

    private static String extractRequiredEventId(String json, int maxPayloadBytes) {
        Optional<JsonField> field = findObjectField(json, "eventId");
        if (field.isEmpty()) {
            throw new LiveHttpCodecException(
                    LiveHttpFailureCategory.MISSING_EVENT_ID,
                    "Live event payload is missing eventId.",
                    400,
                    maxPayloadBytes,
                    -1
            );
        }

        JsonField eventIdField = field.get();
        if (json.charAt(eventIdField.valueStart()) != '"') {
            throw invalidPayload("Invalid live JSON payload.");
        }

        try {
            String eventId = readJsonString(json, eventIdField.valueStart()).value();
            if (eventId.isBlank()) {
                throw new LiveHttpCodecException(
                        LiveHttpFailureCategory.MISSING_EVENT_ID,
                        "Live event payload is missing eventId.",
                        400,
                        maxPayloadBytes,
                        -1
                );
            }
            return eventId;
        } catch (LiveHttpCodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidPayload("Invalid live JSON payload.");
        }
    }

    private static String extractOptionalEventValue(String json) {
        Optional<JsonField> field = findObjectField(json, "value");
        if (field.isEmpty()) {
            return "";
        }

        JsonField valueField = field.get();
        if (startsWith(json, valueField.valueStart(), "null")) {
            return "";
        }
        if (json.charAt(valueField.valueStart()) != '"') {
            throw invalidPayload("Invalid live JSON payload.");
        }
        try {
            return readJsonString(json, valueField.valueStart()).value();
        } catch (LiveHttpCodecException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalidPayload("Invalid live JSON payload.");
        }
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
        Optional<JsonField> field = findObjectField(json, fieldName);
        if (field.isEmpty()) {
            return Optional.empty();
        }

        int valueStart = field.get().valueStart();
        if (json.charAt(valueStart) != '"') {
            throw invalidPayload("Invalid live JSON payload.");
        }
        JsonString jsonString = readJsonString(json, valueStart);
        return Optional.of(jsonString.value());
    }

    private static Map<String, String> extractStringMapField(String json, String fieldName) {
        Optional<JsonField> field = findObjectField(json, fieldName);
        if (field.isEmpty()) {
            return Map.of();
        }

        int objectStart = field.get().valueStart();
        if (json.charAt(objectStart) != '{') {
            throw invalidPayload("Invalid live JSON payload.");
        }
        Map<String, String> values = new LinkedHashMap<>();
        int index = objectStart + 1;
        while (index < json.length()) {
            index = skipWhitespace(json, index);
            if (index < json.length() && json.charAt(index) == '}') {
                return values;
            }

            if (index >= json.length() || json.charAt(index) != '"') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            JsonString key = readJsonString(json, index);
            index = skipWhitespace(json, key.nextIndex());
            if (index >= json.length() || json.charAt(index) != ':') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            index = skipWhitespace(json, index + 1);

            if (startsWith(json, index, "null")) {
                index += 4;
            } else {
                if (index >= json.length() || json.charAt(index) != '"') {
                    throw invalidPayload("Invalid live JSON payload.");
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
            throw invalidPayload("Invalid live JSON payload.");
        }
        throw invalidPayload("Invalid live JSON payload.");
    }

    private static ClientState extractClientStateFromPayload(String json) {
        Optional<JsonField> stateField = findObjectField(json, "clientState");
        if (stateField.isEmpty()) {
            return extractClientStateFromObject(json);
        }
        JsonField field = stateField.get();
        if (json.charAt(field.valueStart()) != '{') {
            throw invalidPayload("Invalid live JSON payload.");
        }
        return extractClientStateFromObject(json.substring(field.valueStart(), field.valueEnd()));
    }

    private static ClientState extractClientStateFromObject(String json) {
        String cookieHeader = extractStringField(json, "cookies").orElse("");
        Map<String, String> localStorage = extractStringMapField(json, "localStorage");
        return ClientState.of(parseCookies(cookieHeader), localStorage);
    }

    private static Optional<JsonField> findObjectField(String json, String fieldName) {
        int objectStart = skipWhitespace(json, 0);
        if (objectStart >= json.length() || json.charAt(objectStart) != '{') {
            throw invalidPayload("Invalid live JSON payload.");
        }

        int index = skipWhitespace(json, objectStart + 1);
        if (index < json.length() && json.charAt(index) == '}') {
            return Optional.empty();
        }

        while (index < json.length()) {
            if (json.charAt(index) != '"') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            JsonString key = readJsonString(json, index);
            index = skipWhitespace(json, key.nextIndex());
            if (index >= json.length() || json.charAt(index) != ':') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            int valueStart = skipWhitespace(json, index + 1);
            int valueEnd = readJsonValue(json, valueStart);
            if (fieldName.equals(key.value())) {
                return Optional.of(new JsonField(valueStart, valueEnd));
            }

            index = skipWhitespace(json, valueEnd);
            if (index < json.length() && json.charAt(index) == ',') {
                index = skipWhitespace(json, index + 1);
                continue;
            }
            if (index < json.length() && json.charAt(index) == '}') {
                return Optional.empty();
            }
            throw invalidPayload("Invalid live JSON payload.");
        }
        throw invalidPayload("Invalid live JSON payload.");
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
                throw invalidPayload("Invalid live JSON payload.");
        }
    }

    private static JsonString readJsonString(String json, int quoteIndex) {
        if (quoteIndex >= json.length() || json.charAt(quoteIndex) != '"') {
            throw invalidPayload("Invalid live JSON payload.");
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
                    throw invalidPayload("Invalid live JSON payload.");
                }
                char escaped = json.charAt(index);
                if (escaped == 'u') {
                    if (index + 4 >= json.length()) {
                        throw invalidPayload("Invalid live JSON payload.");
                    }
                    value.append(unicodeCharacter(json, index));
                    index += 4;
                } else {
                    value.append(unescape(escaped));
                }
            } else {
                if (current < 0x20) {
                    throw invalidPayload("Invalid live JSON payload.");
                }
                value.append(current);
            }
        }
        throw invalidPayload("Invalid live JSON payload.");
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

    private static boolean isEmptyObject(String json) {
        int index = skipWhitespace(json, 1);
        return index == json.length() - 1 && json.charAt(index) == '}';
    }

    private static void validateJsonStructure(String json) {
        int nextIndex = readJsonValue(json, 0);
        if (skipWhitespace(json, nextIndex) != json.length()) {
            throw invalidPayload("Invalid live JSON payload.");
        }
    }

    private static int readJsonValue(String json, int startIndex) {
        int index = skipWhitespace(json, startIndex);
        if (index >= json.length()) {
            throw invalidPayload("Invalid live JSON payload.");
        }

        char current = json.charAt(index);
        if (current == '"') {
            return readJsonString(json, index).nextIndex();
        }
        if (current == '{') {
            return readJsonObject(json, index);
        }
        if (current == '[') {
            return readJsonArray(json, index);
        }
        if (startsWith(json, index, "true")) {
            return index + 4;
        }
        if (startsWith(json, index, "false")) {
            return index + 5;
        }
        if (startsWith(json, index, "null")) {
            return index + 4;
        }
        if (current == '-' || Character.isDigit(current)) {
            return readJsonNumber(json, index);
        }
        throw invalidPayload("Invalid live JSON payload.");
    }

    private static int readJsonObject(String json, int objectStart) {
        int index = skipWhitespace(json, objectStart + 1);
        if (index < json.length() && json.charAt(index) == '}') {
            return index + 1;
        }

        while (index < json.length()) {
            if (json.charAt(index) != '"') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            JsonString key = readJsonString(json, index);
            index = skipWhitespace(json, key.nextIndex());
            if (index >= json.length() || json.charAt(index) != ':') {
                throw invalidPayload("Invalid live JSON payload.");
            }
            index = skipWhitespace(json, readJsonValue(json, index + 1));
            if (index < json.length() && json.charAt(index) == ',') {
                index = skipWhitespace(json, index + 1);
                continue;
            }
            if (index < json.length() && json.charAt(index) == '}') {
                return index + 1;
            }
            throw invalidPayload("Invalid live JSON payload.");
        }
        throw invalidPayload("Invalid live JSON payload.");
    }

    private static int readJsonArray(String json, int arrayStart) {
        int index = skipWhitespace(json, arrayStart + 1);
        if (index < json.length() && json.charAt(index) == ']') {
            return index + 1;
        }

        while (index < json.length()) {
            index = skipWhitespace(json, readJsonValue(json, index));
            if (index < json.length() && json.charAt(index) == ',') {
                index = skipWhitespace(json, index + 1);
                continue;
            }
            if (index < json.length() && json.charAt(index) == ']') {
                return index + 1;
            }
            throw invalidPayload("Invalid live JSON payload.");
        }
        throw invalidPayload("Invalid live JSON payload.");
    }

    private static int readJsonNumber(String json, int startIndex) {
        int index = startIndex;
        if (json.charAt(index) == '-') {
            index++;
        }

        if (index >= json.length()) {
            throw invalidPayload("Invalid live JSON payload.");
        }

        if (json.charAt(index) == '0') {
            index++;
        } else if (json.charAt(index) >= '1' && json.charAt(index) <= '9') {
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
        } else {
            throw invalidPayload("Invalid live JSON payload.");
        }

        if (index < json.length() && json.charAt(index) == '.') {
            index++;
            int fractionStart = index;
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (fractionStart == index) {
                throw invalidPayload("Invalid live JSON payload.");
            }
        }

        if (index < json.length() && (json.charAt(index) == 'e' || json.charAt(index) == 'E')) {
            index++;
            if (index < json.length() && (json.charAt(index) == '+' || json.charAt(index) == '-')) {
                index++;
            }
            int exponentStart = index;
            while (index < json.length() && Character.isDigit(json.charAt(index))) {
                index++;
            }
            if (exponentStart == index) {
                throw invalidPayload("Invalid live JSON payload.");
            }
        }

        return index;
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

    private static JsonField requireObjectField(String json, String fieldName, String safeMessage, int maxPayloadBytes) {
        Optional<JsonField> field = findObjectField(json, fieldName);
        if (field.isEmpty()) {
            throw new LiveHttpCodecException(
                    LiveHttpFailureCategory.MISSING_CLIENT_STATE,
                    safeMessage,
                    400,
                    maxPayloadBytes,
                    -1
            );
        }
        JsonField objectField = field.get();
        if (json.charAt(objectField.valueStart()) != '{') {
            throw invalidPayload("Invalid live JSON payload.");
        }
        return objectField;
    }

    private static char unicodeCharacter(String json, int unicodeMarkerIndex) {
        try {
            return (char) Integer.parseInt(json.substring(unicodeMarkerIndex + 1, unicodeMarkerIndex + 5), 16);
        } catch (NumberFormatException exception) {
            throw invalidPayload("Invalid live JSON payload.");
        }
    }

    private static void validateMaxPayloadBytes(int maxPayloadBytes) {
        if (maxPayloadBytes < 1) {
            throw new IllegalArgumentException("maxPayloadBytes must be greater than zero");
        }
    }

    private static LiveHttpCodecException invalidPayload(String safeMessage) {
        return new LiveHttpCodecException(
                LiveHttpFailureCategory.INVALID_JSON,
                safeMessage,
                400,
                DEFAULT_MAX_JSON_PAYLOAD_BYTES,
                -1
        );
    }

    private static LiveHttpCodecException normalizeInvalidPayloadLimit(
            LiveHttpCodecException exception,
            int maxPayloadBytes
    ) {
        if (exception.category() != LiveHttpFailureCategory.INVALID_JSON
                || exception.payloadLimitBytes() == maxPayloadBytes) {
            return exception;
        }
        return new LiveHttpCodecException(
                exception.category(),
                exception.safeMessage(),
                exception.httpStatus(),
                maxPayloadBytes,
                exception.payloadSizeBytes()
        );
    }

    private static String safeLogValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9_.:/-]", "_");
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

    private static final class JsonField {
        private final int valueStart;
        private final int valueEnd;

        private JsonField(int valueStart, int valueEnd) {
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
        }

        private int valueStart() {
            return valueStart;
        }

        private int valueEnd() {
            return valueEnd;
        }
    }
}
