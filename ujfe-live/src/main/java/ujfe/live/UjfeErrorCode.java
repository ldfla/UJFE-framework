package ujfe.live;

/**
 * Stable framework-level error codes used in safe HTTP error responses.
 */
public enum UjfeErrorCode {
    UJFE_INTERNAL_ERROR("An internal error occurred."),
    UJFE_ROUTE_NOT_FOUND("The requested route was not found."),
    UJFE_RENDER_ERROR("An error occurred while rendering the page."),
    UJFE_EVENT_HANDLER_ERROR("An error occurred while handling the live event."),
    UJFE_INVALID_REQUEST("The request is invalid."),
    UJFE_BAD_REQUEST("The request is invalid."),
    UJFE_UNAUTHORIZED("Authentication is required."),
    UJFE_FORBIDDEN("The request is forbidden."),
    UJFE_RATE_LIMITED("Too many requests."),
    UJFE_CSRF_VALIDATION_FAILED("The request could not be verified."),
    UJFE_STATE_ERROR("An error occurred while updating client state."),
    UJFE_CONFIGURATION_ERROR("The application is not configured correctly.");

    private final String safeMessage;

    UjfeErrorCode(String safeMessage) {
        this.safeMessage = safeMessage;
    }

    public String safeMessage() {
        return safeMessage;
    }
}
