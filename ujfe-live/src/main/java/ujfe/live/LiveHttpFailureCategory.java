package ujfe.live;

public enum LiveHttpFailureCategory {
    EMPTY_PAYLOAD("empty_payload"),
    EMPTY_JSON("empty_json"),
    INVALID_JSON("invalid_json"),
    MISSING_EVENT_ID("missing_event_id"),
    MISSING_CLIENT_STATE("missing_client_state"),
    PAYLOAD_TOO_LARGE("payload_too_large"),
    MISSING_CSRF_TOKEN("missing_csrf_token"),
    INVALID_CSRF_TOKEN("invalid_csrf_token"),
    CROSS_ORIGIN_REQUEST("cross_origin_request");

    private final String logValue;

    LiveHttpFailureCategory(String logValue) {
        this.logValue = logValue;
    }

    public String logValue() {
        return logValue;
    }
}
