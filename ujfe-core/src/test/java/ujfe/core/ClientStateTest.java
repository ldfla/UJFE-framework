package ujfe.core;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class ClientStateTest {
    @Test
    void policyAllowsOnlyConfiguredCookies() {
        ClientState state = ClientState.of(
                Map.of("ujfe_demo", "active", "session", "secret"),
                Map.of(),
                Map.of()
        );
        ClientStatePolicy policy = ClientStatePolicy.builder()
                .allowCookie("ujfe_demo")
                .build();

        ClientState filtered = policy.filter(state);

        assertEquals("active", filtered.cookie("ujfe_demo").orElseThrow());
        assertTrue(filtered.cookie("session").isEmpty());
        assertEquals(Set.of("ujfe_demo"), policy.allowedCookies());
        assertTrue(policy.allowsCookie("ujfe_demo"));
    }

    @Test
    void policyAllowsOnlyConfiguredLocalAndSessionStorageKeys() {
        ClientState state = ClientState.of(
                Map.of(),
                Map.of("ujfe.theme", "dark", "token", "secret"),
                Map.of("ujfe.tab", "docs", "draft", "sensitive")
        );
        ClientStatePolicy policy = ClientStatePolicy.builder()
                .allowLocalStorageKey("ujfe.theme")
                .allowSessionStorageKey("ujfe.tab")
                .build();

        ClientState filtered = policy.filter(state);

        assertEquals("dark", filtered.localStorage("ujfe.theme").orElseThrow());
        assertEquals("docs", filtered.sessionStorage("ujfe.tab").orElseThrow());
        assertTrue(filtered.localStorage("token").isEmpty());
        assertTrue(filtered.sessionStorage("draft").isEmpty());
        assertTrue(policy.allowsLocalStorageKey("ujfe.theme"));
        assertTrue(policy.allowsSessionStorageKey("ujfe.tab"));
    }

    @Test
    void denyAllPolicyBlocksAllBrowserState() {
        ClientState state = ClientState.of(
                Map.of("ujfe_demo", "active"),
                Map.of("ujfe.theme", "dark"),
                Map.of("ujfe.tab", "docs")
        );

        ClientState filtered = ClientStatePolicy.denyAll().filter(state);

        assertTrue(filtered.cookies().isEmpty());
        assertTrue(filtered.localStorage().isEmpty());
        assertTrue(filtered.sessionStorage().isEmpty());
    }

    @Test
    void mergePreservesAndOverlaysCookiesLocalStorageAndSessionStorage() {
        ClientState current = ClientState.of(
                Map.of("a", "1"),
                Map.of("theme", "dark"),
                Map.of("tab", "docs")
        );
        ClientState next = ClientState.of(
                Map.of("b", "2"),
                Map.of("mode", "compact"),
                Map.of("panel", "open")
        );

        ClientState merged = current.merge(next);

        assertEquals(Map.of("a", "1", "b", "2"), merged.cookies());
        assertEquals(Map.of("theme", "dark", "mode", "compact"), merged.localStorage());
        assertEquals(Map.of("tab", "docs", "panel", "open"), merged.sessionStorage());
    }

    @Test
    void mergeCookiesAndReplaceLocalStorageKeepsCookieHistoryButUsesLatestBrowserStorageSnapshot() {
        ClientState current = ClientState.of(
                Map.of("a", "1"),
                Map.of("theme", "dark"),
                Map.of("tab", "docs")
        );
        ClientState next = ClientState.of(
                Map.of("b", "2"),
                Map.of("mode", "compact"),
                Map.of("panel", "open")
        );

        ClientState merged = current.mergeCookiesAndReplaceLocalStorage(next);

        assertEquals(Map.of("a", "1", "b", "2"), merged.cookies());
        assertEquals(Map.of("mode", "compact"), merged.localStorage());
        assertEquals(Map.of("panel", "open"), merged.sessionStorage());
    }

    @Test
    void rejectsBlankPolicyKeys() {
        assertThrows(IllegalArgumentException.class, () -> ClientStatePolicy.builder().allowCookie(" "));
        assertThrows(IllegalArgumentException.class, () -> ClientStatePolicy.builder().allowLocalStorageKey(""));
        assertThrows(IllegalArgumentException.class, () -> ClientStatePolicy.builder().allowSessionStorageKey("\t"));
    }
}
