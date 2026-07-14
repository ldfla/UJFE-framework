package ujfe.core;

import java.util.Objects;
import java.util.function.Function;

/**
 * Renders success, empty, error, and loading-like states from a LoadResult.
 */
public final class LoadBoundary<T> implements Node {
    private final LoadResult<T> result;
    private Function<T, Node> successRenderer = value -> UI.text(String.valueOf(value));
    private Node emptyNode = UI.div()
        .data("ujfe-empty-state", "true")
        .child("No data");
    private Node errorNode = UI.div()
        .role("alert")
        .data("ujfe-error-boundary", "true")
        .child("Could not load data");
    private Node timeoutNode = UI.div()
        .role("alert")
        .data("ujfe-timeout-boundary", "true")
        .child("Data load timed out");

    LoadBoundary(LoadResult<T> result) {
        this.result = Objects.requireNonNull(result, "result");
    }

    public LoadBoundary<T> success(Function<T, Node> renderer) {
        this.successRenderer = Objects.requireNonNull(renderer, "renderer");
        return this;
    }

    public LoadBoundary<T> empty(Node emptyNode) {
        this.emptyNode = Objects.requireNonNull(emptyNode, "emptyNode");
        return this;
    }

    public LoadBoundary<T> error(Node errorNode) {
        this.errorNode = Objects.requireNonNull(errorNode, "errorNode");
        return this;
    }

    public LoadBoundary<T> timeout(Node timeoutNode) {
        this.timeoutNode = Objects.requireNonNull(timeoutNode, "timeoutNode");
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        Node rendered;
        switch (result.status()) {
            case SUCCESS:
                rendered = successRenderer.apply(result.value()
                    .orElseThrow());
                break;
            case EMPTY:
                rendered = emptyNode;
                break;
            case TIMEOUT:
                rendered = timeoutNode;
                break;
            case FAILURE:
            default:
                rendered = errorNode;
                break;
        }
        return UI.div()
            .data("ujfe-load-boundary", result.status()
                .name()
                .toLowerCase())
            .child(rendered)
            .render(context);
    }
}
