package ujfe.live;

import ujfe.runtime.action.RuntimePhase;

import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central renderer for production-safe HTTP error responses.
 */
public final class ErrorResponseRenderer {
    private static final Logger LOGGER = Logger.getLogger(ErrorResponseRenderer.class.getName());
    private final boolean developmentDetailsEnabled;

    private ErrorResponseRenderer(boolean developmentDetailsEnabled) {
        this.developmentDetailsEnabled = developmentDetailsEnabled;
    }

    public static ErrorResponseRenderer create(boolean developmentDetailsEnabled) {
        return new ErrorResponseRenderer(developmentDetailsEnabled);
    }

    public UjfeErrorResponse render(
        UjfeErrorCode code,
        int httpStatus,
        Throwable exception,
        ErrorResponseContext context
    ) {
        Objects.requireNonNull(code, "code");
        ErrorResponseContext safeContext = context == null ? ErrorResponseContext.builder()
                                                             .build() : context;
        UjfeErrorResponse response = new UjfeErrorResponse(
            httpStatus,
            code,
            code.safeMessage(),
            safeContext.requestId(),
            details(exception),
            retryAfterSeconds(exception)
        );
        log(response, exception, safeContext);
        return response;
    }

    public UjfeErrorResponse render(Throwable exception, ErrorResponseContext context) {
        Objects.requireNonNull(exception, "exception");
        ErrorResponseContext safeContext = context == null ? ErrorResponseContext.builder()
                                                             .build() : context;
        Mapping mapping = map(exception, safeContext);
        return render(mapping.code, mapping.status, exception, safeContext);
    }

    public UjfeErrorResponse routeNotFound(ErrorResponseContext context) {
        return render(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND, 404, null, context);
    }

    public UjfeErrorResponse methodNotAllowed(ErrorResponseContext context) {
        return render(UjfeErrorCode.UJFE_INVALID_REQUEST, 405, null, context);
    }

    private static Mapping map(Throwable exception, ErrorResponseContext context) {
        if (exception instanceof LiveRateLimitException) {
            return new Mapping(UjfeErrorCode.UJFE_RATE_LIMITED, 429);
        }
        if (exception instanceof LiveCsrfException) {
            return new Mapping(UjfeErrorCode.UJFE_CSRF_VALIDATION_FAILED, 403);
        }
        if (exception instanceof LiveHttpCodecException) {
            LiveHttpCodecException codecException = (LiveHttpCodecException) exception;
            return new Mapping(UjfeErrorCode.UJFE_BAD_REQUEST, codecException.httpStatus());
        }
        if (exception instanceof IllegalArgumentException && isMissingRoute(exception)) {
            return new Mapping(UjfeErrorCode.UJFE_ROUTE_NOT_FOUND, 404);
        }
        if (context.phase() == RuntimePhase.EVENT) {
            return new Mapping(UjfeErrorCode.UJFE_EVENT_HANDLER_ERROR, 500);
        }
        if (context.phase() == RuntimePhase.STATE) {
            return new Mapping(UjfeErrorCode.UJFE_STATE_ERROR, 500);
        }
        if (context.phase() == RuntimePhase.RENDER || "GET".equalsIgnoreCase(context.method())) {
            return new Mapping(UjfeErrorCode.UJFE_RENDER_ERROR, 500);
        }
        return new Mapping(UjfeErrorCode.UJFE_INTERNAL_ERROR, 500);
    }

    private static boolean isMissingRoute(Throwable exception) {
        String message = exception.getMessage();
        return message != null && message.startsWith("No UJFE route registered");
    }

    private String details(Throwable exception) {
        if (!developmentDetailsEnabled || exception == null) {
            return null;
        }
        String message = exception.getMessage();
        if (message == null || message.isBlank()) {
            message = "No exception message available.";
        }
        return "Development diagnostics only: " + sanitizeDevelopmentDetail(message);
    }

    private static long retryAfterSeconds(Throwable exception) {
        if (exception instanceof LiveRateLimitException) {
            LiveRateLimitException rateLimitException = (LiveRateLimitException) exception;
            return rateLimitException.retryAfterSeconds()
                .orElse(-1L);
        }
        return -1L;
    }

    private static String sanitizeDevelopmentDetail(String message) {
        String sanitized = message
            .replace('\r', ' ')
            .replace('\n', ' ')
            .replaceAll("(?i)(authorization|cookie|csrf|token|password|secret|session)\\s*[:=]\\s*[^\\s,;]+", "$1=<redacted>");
        if (sanitized.length() > 300) {
            return sanitized.substring(0, 300) + "...";
        }
        return sanitized;
    }

    private static void log(UjfeErrorResponse response, Throwable exception, ErrorResponseContext context) {
        if (exception == null) {
            return;
        }
        String message = "event=ujfe.http_error"
            + " code=" + response.code()
            .name()
            + " httpStatus=" + response.httpStatus()
            + " adapter=" + safeLogValue(context.adapter(), "unknown")
            + " method=" + safeLogValue(context.method(), "unknown")
            + " path=" + safeLogValue(context.path(), "unknown")
            + " phase=" + context.phase()
            .name()
            .toLowerCase()
            + " requestId=" + safeLogValue(context.requestId(), "unavailable")
            + " exceptionType=" + exception.getClass()
            .getName();
        LOGGER.log(Level.SEVERE, message, exception);
    }

    private static String safeLogValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.replaceAll("[^A-Za-z0-9._:/@-]", "_");
    }

    private static final class Mapping {
        private final UjfeErrorCode code;
        private final int status;

        private Mapping(UjfeErrorCode code, int status) {
            this.code = code;
            this.status = status;
        }
    }
}
