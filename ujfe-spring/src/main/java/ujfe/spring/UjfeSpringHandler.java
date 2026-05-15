package ujfe.spring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ujfe.core.ClientState;
import ujfe.live.LiveClientScript;
import ujfe.live.LiveDevToolsScript;
import ujfe.live.LiveHttpCodec;
import ujfe.live.LiveRenderResult;
import ujfe.live.LiveSession;
import org.springframework.web.HttpRequestHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

public final class UjfeSpringHandler implements HttpRequestHandler {
    private final LiveSession liveSession;

    public UjfeSpringHandler(LiveSession liveSession) {
        this.liveSession = Objects.requireNonNull(liveSession, "liveSession");
    }

    @Override
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String method = request.getMethod();
        String path = UjfeSpringPaths.pathWithinApplication(request);

        try {
            if ("GET".equals(method) && "/_ujfe/client.js".equals(path)) {
                write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveClientScript.script());
                return;
            }

            if ("GET".equals(method) && "/_ujfe/dev.js".equals(path)) {
                write(response, HttpServletResponse.SC_OK, "application/javascript; charset=utf-8", LiveDevToolsScript.script());
                return;
            }

            if ("GET".equals(method) && "/_ujfe/css".equals(path)) {
                String classes = request.getParameter("classes");
                write(response, HttpServletResponse.SC_OK, "text/css; charset=utf-8",
                        liveSession.renderCss(LiveHttpCodec.parseCssClasses(classes)));
                return;
            }

            if ("POST".equals(method) && "/_ujfe/event".equals(path)) {
                String body = readBody(request);
                String eventId = LiveHttpCodec.extractEventId(body);
                ClientState clientState = LiveHttpCodec.extractClientState(body);
                LiveRenderResult result = liveSession.handleEvent(eventId, clientState);
                write(response, HttpServletResponse.SC_OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
                return;
            }

            if ("POST".equals(method) && "/_ujfe/state".equals(path)) {
                LiveRenderResult result = liveSession.updateClientState(LiveHttpCodec.extractClientState(readBody(request)));
                write(response, HttpServletResponse.SC_OK, "application/json; charset=utf-8", LiveHttpCodec.livePayload(result));
                return;
            }

            if ("GET".equals(method)) {
                Map<String, String> cookies = LiveHttpCodec.parseCookies(request.getHeader("Cookie"));
                String document = liveSession.renderDocument(path, ClientState.of(cookies, Map.of()));
                write(response, HttpServletResponse.SC_OK, "text/html; charset=utf-8", document);
                return;
            }

            write(response, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "text/plain; charset=utf-8", "Method not allowed");
        } catch (IllegalArgumentException exception) {
            write(response, HttpServletResponse.SC_BAD_REQUEST, "text/plain; charset=utf-8", exception.getMessage());
        } catch (RuntimeException exception) {
            write(response, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "text/plain; charset=utf-8", exception.getMessage());
        }
    }

    private static String readBody(HttpServletRequest request) throws IOException {
        StringBuilder body = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) >= 0) {
                body.append(buffer, 0, read);
            }
        }
        return body.toString();
    }

    private static void write(HttpServletResponse response, int status, String contentType, String content) throws IOException {
        response.setStatus(status);
        response.setContentType(contentType);
        response.setCharacterEncoding("UTF-8");
        applySecurityHeaders(response);
        response.getWriter().write(content);
    }

    private static void applySecurityHeaders(HttpServletResponse response) {
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "no-referrer");
        response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        response.setHeader("Content-Security-Policy",
                "default-src 'self'; "
                        + "script-src 'self'; "
                        + "style-src 'self' 'unsafe-inline'; "
                        + "img-src 'self' data: https:; "
                        + "media-src 'self' data: https:; "
                        + "object-src 'none'; "
                        + "base-uri 'none'; "
                        + "frame-ancestors 'none'; "
                        + "form-action 'self'");
    }
}
