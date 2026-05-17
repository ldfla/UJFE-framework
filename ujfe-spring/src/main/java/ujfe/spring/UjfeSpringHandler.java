package ujfe.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.web.HttpRequestHandler;
import ujfe.core.ClientState;
import ujfe.live.ErrorResponseContext;
import ujfe.live.ErrorResponseRenderer;
import ujfe.live.LiveClientScript;
import ujfe.live.LiveDevToolsScript;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveHttpCodecException;
import ujfe.live.LiveHttpEventPayload;
import ujfe.live.LiveHttpPaths;
import ujfe.live.LiveHttpSecurity;
import ujfe.live.LiveRenderResult;
import ujfe.live.LiveSession;
import ujfe.live.LiveCsrfException;
import ujfe.live.LiveHttpRequestMetadata;
import ujfe.live.LiveRateLimitException;
import ujfe.live.UjfeErrorResponse;
import ujfe.runtime.action.RuntimePhase;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class UjfeSpringHandler implements HttpRequestHandler {
    private final LiveSession liveSession;
    private final int maxJsonPayloadBytes;

    public UjfeSpringHandler(LiveSession liveSession) {
        this(liveSession, LiveHttpCodec.DEFAULT_MAX_JSON_PAYLOAD_BYTES);
    }

    public UjfeSpringHandler(LiveSession liveSession, int maxJsonPayloadBytes) {
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
        LiveHttpCodec.requirePayloadSize(0, maxJsonPayloadBytes);
        this.maxJsonPayloadBytes = maxJsonPayloadBytes;
    }

    @Override
    public void handleRequest(HttpServletRequest request, @NonNull HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String path = UjfeSpringPaths.pathWithinApplication(request);

        try {
            if ("GET".equals(method) && LiveHttpPaths.CLIENT_SCRIPT.equals(path)) {
                write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveClientScript.script());
                return;
            }

            if ("GET".equals(method) && LiveHttpPaths.DEV_SCRIPT.equals(path)) {
                write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveDevToolsScript.script());
                return;
            }

            if ("GET".equals(method) && LiveHttpPaths.CSS.equals(path)) {
                String classes = request.getParameter("classes");
                write(response, HttpServletResponse.SC_OK, "text/css; charset=utf-8",
                        liveSession.renderCss(LiveHttpCodec.parseCssClasses(classes)));
                return;
            }

            if ("POST".equals(method) && LiveHttpPaths.EVENT.equals(path)) {
                LiveHttpRequestMetadata metadata = createMetadata(request);
                liveSession.checkInternalEndpointRateLimit(path, metadata);
                LiveHttpEventPayload payload = LiveHttpCodec.parseEventPayload(
                        readBody(request),
                        maxJsonPayloadBytes
                );
                LiveRenderResult result = liveSession.handleEvent(payload.eventId(), payload.clientState(), metadata);
                write(response, HttpServletResponse.SC_OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
                return;
            }

            if ("POST".equals(method) && LiveHttpPaths.STATE.equals(path)) {
                LiveHttpRequestMetadata metadata = createMetadata(request);
                liveSession.checkInternalEndpointRateLimit(path, metadata);
                LiveRenderResult result = liveSession.updateClientState(
                        LiveHttpCodec.parseStatePayload(readBody(request), maxJsonPayloadBytes),
                        metadata
                );
                write(response, HttpServletResponse.SC_OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> cookies = LiveHttpCodec.parseCookies(request.getHeader("Cookie"));
                String document = liveSession.renderDocument(path, ClientState.of(cookies, Map.of()));
                write(response, HttpServletResponse.SC_OK, "text/html; charset=utf-8", document);
                return;
            }

            writeError(response, errorRenderer().methodNotAllowed(errorContext(request, path)));
        } catch (LiveRateLimitException exception) {
            LiveHttpCodec.logRejectedRateLimit(exception, "spring", correlationId(request));
            writeError(response, errorRenderer().render(exception, errorContext(request, path)));
        } catch (LiveCsrfException exception) {
            LiveHttpCodec.logRejectedCsrf(exception, "spring", correlationId(request));
            writeError(response, errorRenderer().render(exception, errorContext(request, path)));
        } catch (LiveHttpCodecException exception) {
            LiveHttpCodec.logRejectedPayload(exception, "spring", correlationId(request));
            reportHttpError(exception, request, path);
            writeError(response, errorRenderer().render(exception, errorContext(request, path)));
        } catch (RuntimeException exception) {
            writeError(response, errorRenderer().render(exception, errorContext(request, path)));
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        LiveHttpCodec.requirePayloadSize(request.getContentLengthLong(), maxJsonPayloadBytes);
        try (BufferedReader reader = request.getReader()) {
            return LiveHttpCodec.readPayload(reader, maxJsonPayloadBytes);
        }
    }

    private static String correlationId(HttpServletRequest request) {
        String requestId = request.getHeader("X-Request-Id");
        if (requestId != null && !requestId.isBlank()) {
            return requestId;
        }
        return request.getHeader("X-Correlation-Id");
    }

    private static LiveHttpRequestMetadata createMetadata(HttpServletRequest request) {
        return new LiveHttpRequestMetadata(
                request.getHeader("X-UJFE-CSRF"),
                request.getHeader("Origin"),
                request.getHeader("Referer"),
                request.getHeader("Host"),
                request.getScheme(),
                request.getRemoteAddr(),
                request.getHeader("Forwarded"),
                request.getHeader("X-Forwarded-For"),
                request.getHeader("X-Real-IP")
        );
    }

    private ErrorResponseRenderer errorRenderer() {
        return ErrorResponseRenderer.create(liveSession.isDevelopmentErrorDetailsEnabled());
    }

    private ErrorResponseContext errorContext(HttpServletRequest request, String path) {
        return ErrorResponseContext.builder()
                .adapter("spring")
                .method(request.getMethod())
                .path(path)
                .requestId(correlationId(request))
                .phase(phaseFor(request.getMethod(), path))
                .build();
    }

    private void reportHttpError(LiveHttpCodecException exception, HttpServletRequest request, String path) {
        liveSession.reportHttpError(
                exception,
                phaseFor(request.getMethod(), path),
                path,
                null,
                correlationId(request),
                Map.of("adapter", "spring", "method", request.getMethod(), "path", path),
                Map.of("errorCode", "UJFE_BAD_REQUEST", "httpStatus", exception.httpStatus())
        );
    }

    private static RuntimePhase phaseFor(String method, String path) {
        if (LiveHttpPaths.EVENT.equals(path)) {
            return RuntimePhase.EVENT;
        }
        if (LiveHttpPaths.STATE.equals(path)) {
            return RuntimePhase.STATE;
        }
        if ("GET".equals(method)) {
            return RuntimePhase.RENDER;
        }
        return RuntimePhase.ADAPTER;
    }

    private static void write(HttpServletResponse response, int status, String contentType, String content) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        applySecurityHeaders(response);
        response.getWriter().write(content);
    }

    private static void writeError(HttpServletResponse response, UjfeErrorResponse error) throws IOException {
        error.retryAfterSeconds().ifPresent(seconds -> response.setHeader("Retry-After", Long.toString(seconds)));
        write(response, error.httpStatus(), UjfeErrorResponse.CONTENT_TYPE, error.body());
    }

    private static void applySecurityHeaders(HttpServletResponse response) {
        LiveHttpSecurity.securityHeaders().forEach(response::setHeader);
    }
}
