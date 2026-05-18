package ujfe.spring;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ujfe.core.ClientStatePolicy;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties("ujfe.client-state")
public final class UjfeSpringClientStateProperties {
    private List<String> cookies = new ArrayList<>();
    private List<String> localStorageKeys = new ArrayList<>();
    private List<String> sessionStorageKeys = new ArrayList<>();

    ClientStatePolicy toClientStatePolicy() {
        return ClientStatePolicy.builder()
                .allowCookies(cookies)
                .allowLocalStorageKeys(localStorageKeys)
                .allowSessionStorageKeys(sessionStorageKeys)
                .build();
    }

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies == null ? new ArrayList<>() : new ArrayList<>(cookies);
    }

    public List<String> getLocalStorageKeys() {
        return localStorageKeys;
    }

    public void setLocalStorageKeys(List<String> localStorageKeys) {
        this.localStorageKeys = localStorageKeys == null ? new ArrayList<>() : new ArrayList<>(localStorageKeys);
    }

    public List<String> getSessionStorageKeys() {
        return sessionStorageKeys;
    }

    public void setSessionStorageKeys(List<String> sessionStorageKeys) {
        this.sessionStorageKeys = sessionStorageKeys == null ? new ArrayList<>() : new ArrayList<>(sessionStorageKeys);
    }
}
