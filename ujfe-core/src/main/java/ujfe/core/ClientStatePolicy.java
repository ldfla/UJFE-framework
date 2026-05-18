package ujfe.core;

import java.util.*;

public final class ClientStatePolicy {
    private static final ClientStatePolicy DENY_ALL = builder().build();

    private final Set<String> allowedCookies;
    private final Set<String> allowedLocalStorageKeys;
    private final Set<String> allowedSessionStorageKeys;

    private ClientStatePolicy(Builder builder) {
        this.allowedCookies = copySet(builder.allowedCookies);
        this.allowedLocalStorageKeys = copySet(builder.allowedLocalStorageKeys);
        this.allowedSessionStorageKeys = copySet(builder.allowedSessionStorageKeys);
    }

    public static ClientStatePolicy denyAll() {
        return DENY_ALL;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Set<String> allowedCookies() {
        return allowedCookies;
    }

    public Set<String> allowedLocalStorageKeys() {
        return allowedLocalStorageKeys;
    }

    public Set<String> allowedSessionStorageKeys() {
        return allowedSessionStorageKeys;
    }

    public boolean allowsCookie(String name) {
        return allowedCookies.contains(name);
    }

    public boolean allowsLocalStorageKey(String key) {
        return allowedLocalStorageKeys.contains(key);
    }

    public boolean allowsSessionStorageKey(String key) {
        return allowedSessionStorageKeys.contains(key);
    }

    public ClientState filter(ClientState state) {
        Objects.requireNonNull(state, "state");
        return ClientState.of(
                filterMap(state.cookies(), allowedCookies),
                filterMap(state.localStorage(), allowedLocalStorageKeys),
                filterMap(state.sessionStorage(), allowedSessionStorageKeys)
        );
    }

    private static Map<String, String> filterMap(Map<String, String> source, Set<String> allowedKeys) {
        Map<String, String> filtered = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (allowedKeys.contains(key)) {
                filtered.put(key, value);
            }
        });
        return filtered;
    }

    private static Set<String> copySet(Set<String> source) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(source));
    }

    private static String requireKey(String key, String name) {
        Objects.requireNonNull(key, name);
        if (key.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return key;
    }

    public static final class Builder {
        private final Set<String> allowedCookies = new LinkedHashSet<>();
        private final Set<String> allowedLocalStorageKeys = new LinkedHashSet<>();
        private final Set<String> allowedSessionStorageKeys = new LinkedHashSet<>();

        private Builder() {
        }

        public Builder allowCookie(String name) {
            allowedCookies.add(requireKey(name, "name"));
            return this;
        }

        public Builder allowCookies(Collection<String> names) {
            Objects.requireNonNull(names, "names");
            names.forEach(this::allowCookie);
            return this;
        }

        public Builder allowLocalStorageKey(String key) {
            allowedLocalStorageKeys.add(requireKey(key, "key"));
            return this;
        }

        public Builder allowLocalStorageKeys(Collection<String> keys) {
            Objects.requireNonNull(keys, "keys");
            keys.forEach(this::allowLocalStorageKey);
            return this;
        }

        public Builder allowSessionStorageKey(String key) {
            allowedSessionStorageKeys.add(requireKey(key, "key"));
            return this;
        }

        public Builder allowSessionStorageKeys(Collection<String> keys) {
            Objects.requireNonNull(keys, "keys");
            keys.forEach(this::allowSessionStorageKey);
            return this;
        }

        public ClientStatePolicy build() {
            return new ClientStatePolicy(this);
        }
    }
}
