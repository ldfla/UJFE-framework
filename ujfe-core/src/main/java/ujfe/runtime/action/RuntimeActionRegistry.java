package ujfe.runtime.action;

import ujfe.core.Node;
import ujfe.observability.EventTrace;
import ujfe.observability.RenderTrace;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, thread-safe registry of server-side runtime actions.
 *
 * <p>The registry is built through {@link #builder()} and contains ordered
 * chains of actions for each extension point. All runtimes (pure UJFE,
 * Netty, Servlet, Spring) share the same registry through
 * {@code LiveSession}.</p>
 *
 * <p>An empty registry ({@link #empty()}) is the default when no actions
 * are configured. The runtime remains fully functional.</p>
 */
public final class RuntimeActionRegistry {
    private static final RuntimeActionRegistry EMPTY = new RuntimeActionRegistry(new Builder());

    private final ActionChain<BeforeRenderAction> beforeRender;
    private final ActionChain<AfterRenderAction> afterRender;
    private final ActionChain<BeforeEventAction> beforeEvent;
    private final ActionChain<AfterEventAction> afterEvent;
    private final ActionChain<ErrorAction> onError;
    private final ActionChain<HeadContributionAction> contributeHead;
    private final ActionChain<RenderTraceAction> renderTrace;
    private final ActionChain<EventTraceAction> eventTrace;

    private RuntimeActionRegistry(Builder builder) {
        this.beforeRender = ActionChain.of(builder.beforeRender);
        this.afterRender = ActionChain.of(builder.afterRender);
        this.beforeEvent = ActionChain.of(builder.beforeEvent);
        this.afterEvent = ActionChain.of(builder.afterEvent);
        this.onError = ActionChain.of(builder.onError);
        this.contributeHead = ActionChain.of(builder.contributeHead);
        this.renderTrace = ActionChain.of(builder.renderTrace);
        this.eventTrace = ActionChain.of(builder.eventTrace);
    }

    /**
     * Returns an empty registry with no actions. This is the default
     * when no extension points are configured.
     */
    public static RuntimeActionRegistry empty() {
        return EMPTY;
    }

    /**
     * Creates a new builder for configuring runtime actions.
     */
    public static RuntimeActionRegistryBuilder builder() {
        return new RuntimeActionRegistryBuilder();
    }

    /**
     * Executes all {@link BeforeRenderAction} actions in order.
     * Exceptions are routed to {@link #executeOnError}.
     */
    public void executeBeforeRender(RenderContext context) {
        Objects.requireNonNull(context, "context");
        for (BeforeRenderAction action : beforeRender.actions()) {
            try {
                action.execute(context);
            } catch (Exception exception) {
                executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.RENDER,
                    context.path(), null, context.traceId(), null));
                throw propagate(exception);
            }
        }
    }

    /**
     * Executes all {@link AfterRenderAction} actions in order.
     * Exceptions are routed to {@link #executeOnError}.
     */
    public void executeAfterRender(RenderResult result) {
        Objects.requireNonNull(result, "result");
        for (AfterRenderAction action : afterRender.actions()) {
            try {
                action.execute(result);
            } catch (Exception exception) {
                executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.RENDER,
                    result.path(), null, result.traceId(), null));
                throw propagate(exception);
            }
        }
    }

    /**
     * Executes all {@link BeforeEventAction} actions in order.
     * Exceptions are routed to {@link #executeOnError}.
     */
    public void executeBeforeEvent(LiveEventContext context) {
        Objects.requireNonNull(context, "context");
        for (BeforeEventAction action : beforeEvent.actions()) {
            try {
                action.execute(context);
            } catch (Exception exception) {
                executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.EVENT,
                    null, context.eventId(), context.traceId(), null));
                throw propagate(exception);
            }
        }
    }

    /**
     * Executes all {@link AfterEventAction} actions in order.
     * Exceptions are routed to {@link #executeOnError}.
     */
    public void executeAfterEvent(LiveEventResult result) {
        Objects.requireNonNull(result, "result");
        for (AfterEventAction action : afterEvent.actions()) {
            try {
                action.execute(result);
            } catch (Exception exception) {
                executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.EVENT,
                    null, result.eventId(), result.traceId(), null));
                throw propagate(exception);
            }
        }
    }

    /**
     * Executes all {@link ErrorAction} actions in order.
     *
     * <p>If an error action itself throws, the exception is printed to
     * stderr and execution continues. This prevents infinite recursion
     * and cascading failures.</p>
     */
    public void executeOnError(RuntimeErrorContext context) {
        Objects.requireNonNull(context, "context");
        for (ErrorAction action : onError.actions()) {
            try {
                action.execute(context);
            } catch (Exception nested) {
                System.err.println("UJFE: error action failed: " + nested.getMessage());
                nested.printStackTrace(System.err);
            }
        }
    }

    /**
     * Executes render trace actions. Trace action failures are isolated from
     * user rendering so observability cannot break normal requests.
     */
    public void executeRenderTrace(RenderTrace trace) {
        Objects.requireNonNull(trace, "trace");
        for (RenderTraceAction action : renderTrace.actions()) {
            try {
                action.execute(trace);
            } catch (Exception exception) {
                System.err.println("UJFE: render trace action failed: " + exception.getMessage());
            }
        }
    }

    /**
     * Executes event trace actions. Trace action failures are isolated from
     * user event handling so observability cannot break normal requests.
     */
    public void executeEventTrace(EventTrace trace) {
        Objects.requireNonNull(trace, "trace");
        for (EventTraceAction action : eventTrace.actions()) {
            try {
                action.execute(trace);
            } catch (Exception exception) {
                System.err.println("UJFE: event trace action failed: " + exception.getMessage());
            }
        }
    }

    /**
     * Executes all {@link HeadContributionAction} actions and returns
     * the contributed nodes in insertion order.
     *
     * <p>Exceptions are routed to {@link #executeOnError}.</p>
     */
    public List<Node> executeHeadContributions() {
        if (contributeHead.isEmpty()) {
            return List.of();
        }
        HeadContributionContext context = new HeadContributionContext();
        for (HeadContributionAction action : contributeHead.actions()) {
            try {
                action.execute(context);
            } catch (Exception exception) {
                executeOnError(new RuntimeErrorContext(
                    exception, RuntimePhase.HEAD_CONTRIBUTION,
                    null, null, "", null));
                throw propagate(exception);
            }
        }
        return context.nodes();
    }

    /**
     * Returns true if this registry has no actions of any type.
     */
    public boolean isEmpty() {
        return beforeRender.isEmpty()
            && afterRender.isEmpty()
            && beforeEvent.isEmpty()
            && afterEvent.isEmpty()
            && onError.isEmpty()
            && contributeHead.isEmpty()
            && renderTrace.isEmpty()
            && eventTrace.isEmpty();
    }

    private static RuntimeException propagate(Exception exception) {
        if (exception instanceof RuntimeException) {
            return (RuntimeException) exception;
        }
        return new IllegalStateException("Runtime action failed", exception);
    }

    public static class Builder {
        private int nextIndex = 0;
        private final List<ActionChain.OrderedAction<BeforeRenderAction>> beforeRender = new ArrayList<>();
        private final List<ActionChain.OrderedAction<AfterRenderAction>> afterRender = new ArrayList<>();
        private final List<ActionChain.OrderedAction<BeforeEventAction>> beforeEvent = new ArrayList<>();
        private final List<ActionChain.OrderedAction<AfterEventAction>> afterEvent = new ArrayList<>();
        private final List<ActionChain.OrderedAction<ErrorAction>> onError = new ArrayList<>();
        private final List<ActionChain.OrderedAction<HeadContributionAction>> contributeHead = new ArrayList<>();
        private final List<ActionChain.OrderedAction<RenderTraceAction>> renderTrace = new ArrayList<>();
        private final List<ActionChain.OrderedAction<EventTraceAction>> eventTrace = new ArrayList<>();

        protected Builder() {
        }

        public Builder beforeRender(BeforeRenderAction action) {
            return beforeRender(ActionOrder.NORMAL, action);
        }

        public Builder beforeRender(ActionOrder order, BeforeRenderAction action) {
            beforeRender.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder afterRender(AfterRenderAction action) {
            return afterRender(ActionOrder.NORMAL, action);
        }

        public Builder afterRender(ActionOrder order, AfterRenderAction action) {
            afterRender.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder beforeEvent(BeforeEventAction action) {
            return beforeEvent(ActionOrder.NORMAL, action);
        }

        public Builder beforeEvent(ActionOrder order, BeforeEventAction action) {
            beforeEvent.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder afterEvent(AfterEventAction action) {
            return afterEvent(ActionOrder.NORMAL, action);
        }

        public Builder afterEvent(ActionOrder order, AfterEventAction action) {
            afterEvent.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder onError(ErrorAction action) {
            return onError(ActionOrder.NORMAL, action);
        }

        public Builder onError(ActionOrder order, ErrorAction action) {
            onError.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder contributeHead(HeadContributionAction action) {
            return contributeHead(ActionOrder.NORMAL, action);
        }

        public Builder contributeHead(ActionOrder order, HeadContributionAction action) {
            contributeHead.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder renderTrace(RenderTraceAction action) {
            return renderTrace(ActionOrder.NORMAL, action);
        }

        public Builder renderTrace(ActionOrder order, RenderTraceAction action) {
            renderTrace.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public Builder eventTrace(EventTraceAction action) {
            return eventTrace(ActionOrder.NORMAL, action);
        }

        public Builder eventTrace(ActionOrder order, EventTraceAction action) {
            eventTrace.add(new ActionChain.OrderedAction<>(order, nextIndex++, action));
            return this;
        }

        public RuntimeActionRegistry build() {
            return new RuntimeActionRegistry(this);
        }
    }
}
