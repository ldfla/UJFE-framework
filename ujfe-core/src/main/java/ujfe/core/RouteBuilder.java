package ujfe.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Safe route and link builder for application paths with required parameters.
 */
public final class RouteBuilder {
    private static final Pattern PARAMETER = Pattern.compile("\\{([A-Za-z][A-Za-z0-9_-]*)}");

    private final String pattern;
    private final Map<String, String> parameters = new LinkedHashMap<>();
    private final Map<String, List<String>> query = new LinkedHashMap<>();
    private String fragment;

    RouteBuilder(String pattern) {
        this.pattern = validatePattern(pattern);
    }

    public RouteBuilder with(String name, Object value) {
        parameters.put(validateName(name), Objects.requireNonNull(value, "value")
            .toString());
        return this;
    }

    public RouteBuilder query(String name, Object value) {
        if (value == null) {
            return this;
        }
        query.computeIfAbsent(validateName(name), key -> new ArrayList<>())
            .add(value.toString());
        return this;
    }

    public RouteBuilder query(String name, Collection<?> values) {
        if (values == null) {
            return this;
        }
        for (Object value : values) {
            query(name, value);
        }
        return this;
    }

    public RouteBuilder continueUrl(String continueUrl) {
        if (continueUrl == null || continueUrl.isBlank()) {
            return this;
        }
        query("continue", validateContinueUrl(continueUrl));
        return this;
    }

    public RouteBuilder fragment(String fragment) {
        if (fragment == null || fragment.isBlank()) {
            this.fragment = null;
            return this;
        }
        String normalized = fragment.startsWith("#") ? fragment.substring(1) : fragment;
        validateNoControlCharacters(normalized, "fragment");
        this.fragment = normalized;
        return this;
    }

    public String build() {
        StringBuilder path = new StringBuilder();
        Matcher matcher = PARAMETER.matcher(pattern);
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = parameters.get(name);
            if (value == null) {
                throw new IllegalStateException("Missing route parameter: " + name);
            }
            matcher.appendReplacement(path, Matcher.quoteReplacement(encodePathSegment(value)));
        }
        matcher.appendTail(path);

        String built = path.toString();
        if (built.indexOf('{') >= 0 || built.indexOf('}') >= 0) {
            throw new IllegalStateException("Route pattern contains invalid parameter syntax: " + pattern);
        }
        if (!query.isEmpty()) {
            built += "?" + queryString();
        }
        if (fragment != null) {
            built += "#" + encodeFragment(fragment);
        }
        return built;
    }

    public Element link(String label) {
        return UI.a(label)
            .href(build());
    }

    public Element link(Node child) {
        return UI.a()
            .href(build())
            .child(child);
    }

    @Override
    public String toString() {
        return build();
    }

    static String validateContinueUrl(String value) {
        String trimmed = value.trim();
        if (!trimmed.startsWith("/") || trimmed.startsWith("//")) {
            throw new IllegalArgumentException("Continue URL must be an application-relative path");
        }
        if (trimmed.indexOf('\\') >= 0 || trimmed.contains("://")) {
            throw new IllegalArgumentException("Continue URL must not be absolute or protocol-relative");
        }
        validateNoControlCharacters(trimmed, "continueUrl");
        return trimmed;
    }

    private String queryString() {
        List<String> pairs = new ArrayList<>();
        query.forEach((name, values) -> values.forEach(value ->
            pairs.add(encodeQuery(name) + "=" + encodeQuery(value))
        ));
        return String.join("&", pairs);
    }

    private static String validatePattern(String value) {
        Objects.requireNonNull(value, "pattern");
        if (value.isBlank()) {
            throw new IllegalArgumentException("Route pattern cannot be blank");
        }
        String pattern = value.startsWith("/") ? value : "/" + value;
        if (pattern.indexOf('?') >= 0 || pattern.indexOf('#') >= 0) {
            throw new IllegalArgumentException("Route pattern cannot include query or fragment");
        }
        if (pattern.indexOf('\\') >= 0 || pattern.contains("//")) {
            throw new IllegalArgumentException("Route pattern must be an application path");
        }
        validateNoControlCharacters(pattern, "pattern");
        return pattern;
    }

    private static String validateName(String name) {
        Objects.requireNonNull(name, "name");
        if (!name.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid route parameter or query name: " + name);
        }
        return name;
    }

    private static void validateNoControlCharacters(String value, String fieldName) {
        for (int index = 0; index < value.length(); index++) {
            if (Character.isISOControl(value.charAt(index))) {
                throw new IllegalArgumentException(fieldName + " cannot contain control characters");
            }
        }
    }

    private static String encodePathSegment(String value) {
        return encode(value).replace("+", "%20");
    }

    private static String encodeQuery(String value) {
        return encode(value);
    }

    private static String encodeFragment(String value) {
        return encode(value).replace("+", "%20");
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
