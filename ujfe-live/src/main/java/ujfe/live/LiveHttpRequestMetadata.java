package ujfe.live;

import java.util.Optional;

public final class LiveHttpRequestMetadata {
    private final String csrfToken;
    private final String origin;
    private final String referer;
    private final String host;
    private final String scheme;
    private final String remoteAddress;
    private final String forwarded;
    private final String xForwardedFor;
    private final String xRealIp;
    private final String adapterName;
    private final String method;
    private final String requestId;

    public LiveHttpRequestMetadata(String csrfToken, String origin, String referer, String host) {
        this(csrfToken, origin, referer, host, null);
    }

    public LiveHttpRequestMetadata(String csrfToken, String origin, String referer, String host, String scheme) {
        this(csrfToken, origin, referer, host, scheme, null, null, null, null);
    }

    public LiveHttpRequestMetadata(
        String csrfToken,
        String origin,
        String referer,
        String host,
        String scheme,
        String remoteAddress,
        String forwarded,
        String xForwardedFor,
        String xRealIp
    ) {
        this(csrfToken, origin, referer, host, scheme, remoteAddress, forwarded, xForwardedFor, xRealIp, null, null, null);
    }

    public LiveHttpRequestMetadata(
        String csrfToken,
        String origin,
        String referer,
        String host,
        String scheme,
        String remoteAddress,
        String forwarded,
        String xForwardedFor,
        String xRealIp,
        String adapterName,
        String method,
        String requestId
    ) {
        this.csrfToken = csrfToken; // nullable
        this.origin = origin; // nullable
        this.referer = referer; // nullable
        this.host = host; // nullable
        this.scheme = scheme; // nullable
        this.remoteAddress = remoteAddress; // nullable
        this.forwarded = forwarded; // nullable
        this.xForwardedFor = xForwardedFor; // nullable
        this.xRealIp = xRealIp; // nullable
        this.adapterName = adapterName; // nullable
        this.method = method; // nullable
        this.requestId = requestId; // nullable
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

    public Optional<String> remoteAddress() {
        return Optional.ofNullable(remoteAddress);
    }

    public Optional<String> forwarded() {
        return Optional.ofNullable(forwarded);
    }

    public Optional<String> xForwardedFor() {
        return Optional.ofNullable(xForwardedFor);
    }

    public Optional<String> xRealIp() {
        return Optional.ofNullable(xRealIp);
    }

    public Optional<String> adapterName() {
        return Optional.ofNullable(adapterName);
    }

    public Optional<String> method() {
        return Optional.ofNullable(method);
    }

    public Optional<String> requestId() {
        return Optional.ofNullable(requestId);
    }

    public boolean hasOrigin() {
        return origin != null && !origin.isBlank();
    }

    public boolean hasReferer() {
        return referer != null && !referer.isBlank();
    }
}
