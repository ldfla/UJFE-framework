package ujfe.servlet;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import ujfe.live.CssMode;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveSessionConfig;
import ujfe.live.SecurityHeadersConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;

final class UjfeServletSettings {
    static final String ROUTE_PACKAGES = "ujfe.routes.packages";
    static final String TITLE = "ujfe.live.title";
    static final String LANG = "ujfe.live.lang";
    static final String DEV_TOOLS_ENABLED = "ujfe.live.dev-tools-enabled";
    static final String CSS_MODE = "ujfe.live.css-mode";
    static final String MAX_JSON_PAYLOAD_BYTES = "ujfe.live.max-json-payload-bytes";
    static final String DISABLE_CSRF_PROTECTION_FOR_DEVELOPMENT_UNSAFE = "ujfe.live.disable-csrf-protection-for-development-unsafe";
    static final String DEVELOPMENT_ERROR_DETAILS_ENABLED = "ujfe.errors.development-details.enabled";
    static final String RATE_LIMIT_ENABLED = "ujfe.live.rate-limit.enabled";
    static final String RATE_LIMIT_CAPACITY = "ujfe.live.rate-limit.capacity";
    static final String RATE_LIMIT_REFILL_TOKENS = "ujfe.live.rate-limit.refill-tokens";
    static final String RATE_LIMIT_REFILL_PERIOD_MS = "ujfe.live.rate-limit.refill-period-ms";
    static final String TRUSTED_PROXIES = "ujfe.live.trusted-proxies";
    static final String CLIENT_STATE_COOKIES = "ujfe.client-state.cookies";
    static final String CLIENT_STATE_LOCAL_STORAGE_KEYS = "ujfe.client-state.local-storage-keys";
    static final String CLIENT_STATE_SESSION_STORAGE_KEYS = "ujfe.client-state.session-storage-keys";
    static final String SECURITY_HEADERS_ENABLED = "ujfe.security.headers.enabled";
    static final String SECURITY_HEADER_CONTENT_TYPE_OPTIONS = "ujfe.security.headers.x-content-type-options";
    static final String SECURITY_HEADER_FRAME_OPTIONS = "ujfe.security.headers.x-frame-options";
    static final String SECURITY_HEADER_REFERRER_POLICY = "ujfe.security.headers.referrer-policy";
    static final String SECURITY_HEADER_CONTENT_SECURITY_POLICY = "ujfe.security.headers.content-security-policy";
    static final String SECURITY_HEADER_PERMISSIONS_POLICY = "ujfe.security.headers.permissions-policy";

    private final Properties values;

    private UjfeServletSettings(Properties values) {
        this.values = copy(values);
    }

    static UjfeServletSettings from(ServletConfig config) {
        Objects.requireNonNull(config, "config");
        Properties values = loadClasspathSettings();
        ServletContext context = config.getServletContext();
        if (context != null) {
            copyInitParameters(context.getInitParameterNames(), context::getInitParameter, values);
        }
        copyInitParameters(config.getInitParameterNames(), config::getInitParameter, values);
        return new UjfeServletSettings(values);
    }

    static UjfeServletSettings fromProperties(Properties values) {
        return new UjfeServletSettings(values);
    }

    static UjfeServletSettings fromYaml(String yaml) {
        Properties values = new Properties();
        String[] lines = Objects.requireNonNull(yaml, "yaml")
            .split("\\R");
        List<String> path = new ArrayList<>();
        for (String line : lines) {
            String withoutComment = stripComment(line);
            if (withoutComment.trim()
                .isEmpty()) {
                continue;
            }

            int indent = leadingSpaces(withoutComment);
            int level = indent / 2;
            String trimmed = withoutComment.trim();
            int separator = trimmed.indexOf(':');
            if (separator <= 0) {
                continue;
            }

            while (path.size() > level) {
                path.remove(path.size() - 1);
            }

            String key = trimmed.substring(0, separator)
                .trim();
            String value = trimmed.substring(separator + 1)
                .trim();
            if (value.isEmpty()) {
                path.add(key);
            } else {
                List<String> fullPath = new ArrayList<>(path);
                fullPath.add(key);
                values.setProperty(String.join(".", fullPath), unquote(value));
            }
        }
        return new UjfeServletSettings(values);
    }

    List<String> routePackages() {
        String configured = values.getProperty(ROUTE_PACKAGES, "");
        if (configured.isBlank()) {
            return List.of();
        }

        List<String> packages = new ArrayList<>();
        for (String packageName : configured.split(",")) {
            String trimmed = packageName.trim();
            if (!trimmed.isEmpty()) {
                packages.add(trimmed);
            }
        }
        return List.copyOf(packages);
    }

    LiveSessionConfig toLiveSessionConfig() {
        LiveSessionConfig.Builder builder = LiveSessionConfig.builder();
        value(TITLE).ifPresent(builder::title);
        value(LANG).ifPresent(builder::lang);
        value(DEV_TOOLS_ENABLED).ifPresent(value -> builder.devToolsEnabled(Boolean.parseBoolean(value)));
        value(CSS_MODE).ifPresent(value -> builder.cssMode(CssMode.valueOf(value.trim()
            .toUpperCase())));
        value(DISABLE_CSRF_PROTECTION_FOR_DEVELOPMENT_UNSAFE).ifPresent(value -> {
            if (Boolean.parseBoolean(value)) {
                builder.disableCsrfProtectionForDevelopmentUnsafe();
            }
        });
        value(DEVELOPMENT_ERROR_DETAILS_ENABLED).ifPresent(value -> {
            if (Boolean.parseBoolean(value)) {
                builder.enableDevelopmentErrorDetailsUnsafe();
            }
        });
        builder.securityHeaders(securityHeadersConfig());
        value(RATE_LIMIT_ENABLED).ifPresent(value -> builder.internalEndpointRateLimitingEnabled(Boolean.parseBoolean(value)));
        if (value(RATE_LIMIT_CAPACITY).isPresent()
            || value(RATE_LIMIT_REFILL_TOKENS).isPresent()
            || value(RATE_LIMIT_REFILL_PERIOD_MS).isPresent()) {
            builder.internalEndpointRateLimit(
                value(RATE_LIMIT_CAPACITY)
                    .map(Integer::parseInt)
                    .orElse(LiveSessionConfig.DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_CAPACITY),
                value(RATE_LIMIT_REFILL_TOKENS)
                    .map(Integer::parseInt)
                    .orElse(LiveSessionConfig.DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_TOKENS),
                value(RATE_LIMIT_REFILL_PERIOD_MS)
                    .map(Long::parseLong)
                    .map(Duration::ofMillis)
                    .orElse(LiveSessionConfig.DEFAULT_INTERNAL_ENDPOINT_RATE_LIMIT_REFILL_PERIOD)
            );
        }
        value(TRUSTED_PROXIES).ifPresent(value -> builder.trustedProxies(splitCsv(value)));
        value(CLIENT_STATE_COOKIES).ifPresent(value -> builder.allowClientCookies(splitCsv(value)));
        value(CLIENT_STATE_LOCAL_STORAGE_KEYS).ifPresent(value -> builder.allowLocalStorageKeys(splitCsv(value)));
        value(CLIENT_STATE_SESSION_STORAGE_KEYS).ifPresent(value -> builder.allowSessionStorageKeys(splitCsv(value)));
        return builder.build();
    }

    private SecurityHeadersConfig securityHeadersConfig() {
        SecurityHeadersConfig.Builder builder = SecurityHeadersConfig.builder();
        value(SECURITY_HEADERS_ENABLED).ifPresent(value -> builder.enabled(Boolean.parseBoolean(value)));
        value(SECURITY_HEADER_CONTENT_TYPE_OPTIONS).ifPresent(value ->
            builder.header(SecurityHeadersConfig.X_CONTENT_TYPE_OPTIONS, value));
        value(SECURITY_HEADER_FRAME_OPTIONS).ifPresent(value ->
            builder.header(SecurityHeadersConfig.X_FRAME_OPTIONS, value));
        value(SECURITY_HEADER_REFERRER_POLICY).ifPresent(value ->
            builder.header(SecurityHeadersConfig.REFERRER_POLICY, value));
        value(SECURITY_HEADER_CONTENT_SECURITY_POLICY).ifPresent(value ->
            builder.header(SecurityHeadersConfig.CONTENT_SECURITY_POLICY, value));
        value(SECURITY_HEADER_PERMISSIONS_POLICY).ifPresent(value ->
            builder.header(SecurityHeadersConfig.PERMISSIONS_POLICY, value));
        return builder.build();
    }

    int maxJsonPayloadBytes() {
        return value(MAX_JSON_PAYLOAD_BYTES)
            .map(Integer::parseInt)
            .map(value -> {
                LiveHttpCodec.requirePayloadSize(0, value);
                return value;
            })
            .orElse(LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    private java.util.Optional<String> value(String key) {
        return java.util.Optional.ofNullable(values.getProperty(key))
            .map(String::trim)
            .filter(value -> !value.isEmpty());
    }

    private static List<String> splitCsv(String value) {
        List<String> items = new ArrayList<>();
        for (String item : value.split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }
        return items;
    }

    private static Properties loadClasspathSettings() {
        Properties values = new Properties();
        ClassLoader loader = Thread.currentThread()
            .getContextClassLoader();
        if (loader == null) {
            loader = UjfeServletSettings.class.getClassLoader();
        }
        loadPropertiesResource(loader, "application.properties", values);
        loadYamlResource(loader, "application.yml", values);
        loadYamlResource(loader, "application.yaml", values);
        return values;
    }

    private static void loadPropertiesResource(ClassLoader loader, String resourceName, Properties values) {
        try (InputStream input = loader.getResourceAsStream(resourceName)) {
            if (input != null) {
                values.load(input);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load " + resourceName, exception);
        }
    }

    private static void loadYamlResource(ClassLoader loader, String resourceName, Properties values) {
        try (InputStream input = loader.getResourceAsStream(resourceName)) {
            if (input != null) {
                String yaml = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                values.putAll(fromYaml(yaml).values);
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Could not load " + resourceName, exception);
        }
    }

    private static void copyInitParameters(
        Enumeration<String> names,
        ParameterLookup lookup,
        Properties target
    ) {
        if (names == null) {
            return;
        }
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            String value = lookup.get(name);
            if (value != null) {
                target.setProperty(name, value);
            }
        }
    }

    private static Properties copy(Properties source) {
        Properties target = new Properties();
        for (String name : source.stringPropertyNames()) {
            target.setProperty(name, source.getProperty(name));
        }
        return target;
    }

    private static String stripComment(String line) {
        int index = line.indexOf('#');
        return index < 0 ? line : line.substring(0, index);
    }

    private static int leadingSpaces(String line) {
        int count = 0;
        while (count < line.length() && line.charAt(count) == ' ') {
            count++;
        }
        return count;
    }

    private static String unquote(String value) {
        if (value.length() >= 2
            && ((value.startsWith("\"") && value.endsWith("\""))
            || (value.startsWith("'") && value.endsWith("'")))) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private interface ParameterLookup {
        String get(String name);
    }
}
