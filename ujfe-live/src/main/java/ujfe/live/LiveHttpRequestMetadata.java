package ujfe.live;

import java.util.Optional;

public final class LiveHttpRequestMetadata {
    private final String csrfToken;
    private final String origin;
    private final String referer;
    private final String host;
    private final String scheme;

    public LiveHttpRequestMetadata(String csrfToken, String origin, String referer, String host) {
        this(csrfToken, origin, referer, host, null);
    }

    public LiveHttpRequestMetadata(String csrfToken, String origin, String referer, String host, String scheme) {
        this.csrfToken = csrfToken; // nullable
        this.origin = origin; // nullable
        this.referer = referer; // nullable
        this.host = host; // nullable
        this.scheme = scheme; // nullable
    }

    public Optional<String> csrfToken() {
        return Optional.ofNullable(csrfToken);
    }

    public Optional<String> origin() {
        return Optional.ofNullable(origin);
    }

    public Optional<String> referer() {
        return Optional.ofNullable(referer);
    }

    public Optional<String> host() {
        return Optional.ofNullable(host);
    }

    public Optional<String> scheme() {
        return Optional.ofNullable(scheme);
    }

    public boolean hasOrigin() {
        return origin != null && !origin.isBlank();
    }

    public boolean hasReferer() {
        return referer != null && !referer.isBlank();
    }
}
