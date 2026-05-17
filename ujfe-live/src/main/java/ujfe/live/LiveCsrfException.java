package ujfe.live;

public final class LiveCsrfException extends RuntimeException {
    private final LiveHttpFailureCategory category;
    private final String safeMessage;
    private final boolean csrfHeaderPresent;

    public LiveCsrfException(
            LiveHttpFailureCategory category,
            String safeMessage,
            boolean csrfHeaderPresent
    ) {
        super(safeMessage);
        this.category = category;
        this.safeMessage = safeMessage;
        this.csrfHeaderPresent = csrfHeaderPresent;
    }

    public LiveHttpFailureCategory category() {
        return category;
    }

    public String safeMessage() {
        return safeMessage;
    }

    public boolean isCsrfHeaderPresent() {
        return csrfHeaderPresent;
    }
}
