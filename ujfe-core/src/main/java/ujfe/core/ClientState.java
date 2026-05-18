package ujfe.core;

import java.util.*;

public final class ClientState {
    private static final ClientState EMPTY = new ClientState(
        Collections.emptyMap(),
        Collections.emptyMap(),
        Collections.emptyMap()
    );

    private final Map<String, String> cookies;
    private final Map<String, String> localStorage;
    private final Map<String, String> sessionStorage;

    private ClientState(
        Map<String, String> cookies,
        Map<String, String> localStorage,
        Map<String, String> sessionStorage
    ) {
        this.cookies = copy(cookies);
        this.localStorage = copy(localStorage);
        this.sessionStorage = copy(sessionStorage);
    }

    public static ClientState empty() {
        return EMPTY;
    }

    public static ClientState of(Map<String, String> cookies, Map<String, String> localStorage) {
        return of(cookies, localStorage, Map.of());
    }

    public static ClientState of(
        Map<String, String> cookies,
        Map<String, String> localStorage,
        Map<String, String> sessionStorage
    ) {
        return new ClientState(cookies, localStorage, sessionStorage);
    }

    public ClientState merge(ClientState next) {
        Objects.requireNonNull(next, "next");
        Map<String, String> mergedCookies = new LinkedHashMap<>(cookies);
        mergedCookies.putAll(next.cookies);

        Map<String, String> mergedLocalStorage = new LinkedHashMap<>(localStorage);
        mergedLocalStorage.putAll(next.localStorage);

        Map<String, String> mergedSessionStorage = new LinkedHashMap<>(sessionStorage);
        mergedSessionStorage.putAll(next.sessionStorage);

        return new ClientState(mergedCookies, mergedLocalStorage, mergedSessionStorage);
    }

    public ClientState mergeCookiesAndReplaceLocalStorage(ClientState next) {
        Objects.requireNonNull(next, "next");
        Map<String, String> mergedCookies = new LinkedHashMap<>(cookies);
        mergedCookies.putAll(next.cookies);
        return new ClientState(mergedCookies, next.localStorage, next.sessionStorage);
    }

    public Optional<String> cookie(String name) {
        return Optional.ofNullable(cookies.get(name));
    }

    public Optional<String> localStorage(String key) {
        return Optional.ofNullable(localStorage.get(key));
    }

    public Optional<String> sessionStorage(String key) {
        return Optional.ofNullable(sessionStorage.get(key));
    }

    public Map<String, String> cookies() {
        return cookies;
    }

    public Map<String, String> localStorage() {
        return localStorage;
    }

    public Map<String, String> sessionStorage() {
        return sessionStorage;
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
