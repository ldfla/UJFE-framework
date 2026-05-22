package ujfe.core;

import java.util.Objects;
import java.util.Optional;

public final class LoadResult<T> {
    public enum Status {
        SUCCESS,
        EMPTY,
        FAILURE,
        TIMEOUT
    }

    private final Status status;
    private final T value;
    private final Throwable error;

    private LoadResult(Status status, T value, Throwable error) {
        this.status = Objects.requireNonNull(status, "status");
        this.value = value;
        this.error = error;
    }

    public static <T> LoadResult<T> success(T value) {
        return new LoadResult<>(value == null ? Status.EMPTY : Status.SUCCESS, value, null);
    }

    public static <T> LoadResult<T> failure(Throwable error) {
        return new LoadResult<>(Status.FAILURE, null, Objects.requireNonNull(error, "error"));
    }

    public static <T> LoadResult<T> timeout(Throwable error) {
        return new LoadResult<>(Status.TIMEOUT, null, Objects.requireNonNull(error, "error"));
    }

    public Status status() {
        return status;
    }

    public Optional<T> value() {
        return Optional.ofNullable(value);
    }

    public Optional<Throwable> error() {
        return Optional.ofNullable(error);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
