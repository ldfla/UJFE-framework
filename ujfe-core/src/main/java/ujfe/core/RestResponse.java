package ujfe.core;

import java.util.*;

public final class RestResponse {
    private final int statusCode;
    private final String body;
    private final Map<String, List<String>> headers;

    public RestResponse(int statusCode, String body, Map<String, List<String>> headers) {
        this.statusCode = statusCode;
        this.body = Objects.requireNonNull(body, "body");
        this.headers = copyHeaders(headers);
    }

    public int statusCode() {
        return statusCode;
    }

    public String body() {
        return body;
    }

    public Map<String, List<String>> headers() {
        return headers;
    }

    public boolean successful() {
        return statusCode >= 200 && statusCode < 300;
    }

    public Optional<String> firstHeader(String name) {
        Objects.requireNonNull(name, "name");
        return headers.entrySet()
            .stream()
            .filter(entry -> entry.getKey()
                .equalsIgnoreCase(name))
            .flatMap(entry -> entry.getValue()
                .stream())
            .findFirst();
    }

    public RestResponse requireSuccessful() {
        if (!successful()) {
            throw new IllegalStateException("REST request failed with HTTP " + statusCode);
        }
        return this;
    }

    private static Map<String, List<String>> copyHeaders(Map<String, List<String>> headers) {
        Objects.requireNonNull(headers, "headers");
        Map<String, List<String>> copy = new LinkedHashMap<>();
        headers.forEach((name, values) -> copy.put(
            name,
            Collections.unmodifiableList(new ArrayList<>(values))
        ));
        return Collections.unmodifiableMap(copy);
    }
}
