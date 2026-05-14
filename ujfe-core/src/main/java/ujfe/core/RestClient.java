package ujfe.core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class RestClient {
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final Duration timeout;

    RestClient(HttpClient httpClient, Duration timeout) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient");
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    public static RestClient create() {
        return create(DEFAULT_TIMEOUT);
    }

    public static RestClient create(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        return new RestClient(
                HttpClient.newBuilder()
                        .connectTimeout(timeout)
                        .build(),
                timeout
        );
    }

    public RestResponse get(String uri) {
        return get(URI.create(uri));
    }

    public RestResponse get(URI uri) {
        Objects.requireNonNull(uri, "uri");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(timeout)
                .GET()
                .header("Accept", "application/json")
                .build();
        return send(request);
    }

    public RestResponse send(HttpRequest request) {
        Objects.requireNonNull(request, "request");
        try {
            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            return new RestResponse(response.statusCode(), response.body(), response.headers().map());
        } catch (IOException exception) {
            throw new IllegalStateException("REST request failed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("REST request interrupted", exception);
        }
    }
}
