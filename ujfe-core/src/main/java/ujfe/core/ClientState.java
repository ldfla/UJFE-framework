package ujfe.core;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ClientState {
    private static final ClientState EMPTY = new ClientState(Collections.emptyMap(), Collections.emptyMap());

    private final Map<String, String> cookies;
    private final Map<String, String> localStorage;

    private ClientState(Map<String, String> cookies, Map<String, String> localStorage) {
        this.cookies = copy(cookies);
        this.localStorage = copy(localStorage);
    }

    public static ClientState empty() {
        return EMPTY;
    }

    public static ClientState of(Map<String, String> cookies, Map<String, String> localStorage) {
        return new ClientState(cookies, localStorage);
    }

    public ClientState merge(ClientState next) {
        Objects.requireNonNull(next, "next");
        Map<String, String> mergedCookies = new LinkedHashMap<>(cookies);
        mergedCookies.putAll(next.cookies);

        Map<String, String> mergedLocalStorage = new LinkedHashMap<>(localStorage);
        mergedLocalStorage.putAll(next.localStorage);

        return new ClientState(mergedCookies, mergedLocalStorage);
    }

    public ClientState mergeCookiesAndReplaceLocalStorage(ClientState next) {
        Objects.requireNonNull(next, "next");
        Map<String, String> mergedCookies = new LinkedHashMap<>(cookies);
        mergedCookies.putAll(next.cookies);
        return new ClientState(mergedCookies, next.localStorage);
    }

    public Optional<String> cookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }

    public Optional<String> localStorage(String key) {
        return Optional.ofNullable(localStorage.get(key));
    }

    public Map<String, String> cookies() {
        return cookies;
    }

    public Map<String, String> localStorage() {
        return localStorage;
    }

    private static Map<String, String> copy(Map<String, String> source) {
        Objects.requireNonNull(source, "source");
        Map<String, String> copy = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (key != null && value != null) {
                copy.put(key, value);
            }
        });
        return Collections.unmodifiableMap(copy);
    }
}
