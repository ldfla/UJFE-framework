package ujfe.router;

public final class RouteDiscoveryException extends RuntimeException {
    public RouteDiscoveryException(String message) {
        super(message);
    }

    public RouteDiscoveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
