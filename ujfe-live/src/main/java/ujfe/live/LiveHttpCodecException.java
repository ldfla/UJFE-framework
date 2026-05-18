package ujfe.live;

public final class LiveHttpCodecException extends IllegalArgumentException {
    private final LiveHttpFailureCategory category;
    private final String safeMessage;
    private final int httpStatus;
    private final int payloadLimitBytes;
    private final long payloadSizeBytes;

    public LiveHttpCodecException(
        LiveHttpFailureCategory category,
        String safeMessage,
        int httpStatus,
        int payloadLimitBytes,
        long payloadSizeBytes
    ) {
        super(safeMessage);
        this.category = category;
        this.safeMessage = safeMessage;
        this.httpStatus = httpStatus;
        this.payloadLimitBytes = payloadLimitBytes;
        this.payloadSizeBytes = payloadSizeBytes;
    }

    public LiveHttpFailureCategory category() {
        return category;
    }

    public String safeMessage() {
        return safeMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public int payloadLimitBytes() {
        return payloadLimitBytes;
    }

    public long payloadSizeBytes() {
        return payloadSizeBytes;
    }
}
