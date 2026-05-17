package ujfe.live;

public enum RateLimitKeyType {
    SESSION("session"),
    IP("ip");

    private final String logValue;

    RateLimitKeyType(String logValue) {
        this.logValue = logValue;
    }

    public String logValue() {
        return logValue;
    }
}
