package ujfe.live;

import java.util.OptionalLong;

/**
 * Rendered safe HTTP error response.
 */
public final class UjfeErrorResponse {
    public static final String CONTENT_TYPE = "application/json; charset=utf-8";

    private final int httpStatus;
    private final UjfeErrorCode code;
    private final String message;
    private final String requestId;
    private final String details;
    private final long retryAfterSeconds;

    UjfeErrorResponse(
            int httpStatus,
            UjfeErrorCode code,
            String message,
            String requestId,
            String details,
            long retryAfterSeconds
    ) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
        this.requestId = requestId;
        this.details = details;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public UjfeErrorCode code() {
        return code;
    }

    public String message() {
        return message;
    }

    public String requestId() {
        return requestId;
    }

    public String details() {
        return details;
    }

    public OptionalLong retryAfterSeconds() {
        return retryAfterSeconds > 0 ? OptionalLong.of(retryAfterSeconds) : OptionalLong.empty();
    }

    public String body() {
        StringBuilder json = new StringBuilder();
        json.append("{\"error\":{\"code\":\"")
                .append(code.name())
                .append("\",\"message\":\"")
                .append(jsonEscape(message))
                .append("\"");
        if (requestId != null && !requestId.isBlank()) {
            json.append(",\"requestId\":\"")
                    .append(jsonEscape(requestId))
                    .append("\"");
        }
        if (details != null && !details.isBlank()) {
            json.append(",\"details\":\"")
                    .append(jsonEscape(details))
                    .append("\"");
        }
        json.append("}}");
        return json.toString();
    }

    private static String jsonEscape(String value) {
        StringBuilder escaped = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
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
                    if (character < 0x20) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
                    break;
            }
        }
        return escaped.toString();
    }
}
