package ujfe.live;

import ujfe.core.ClientState;
import ujfe.core.ElementIdGenerator;
import ujfe.core.Node;
import ujfe.core.UjfeContext;
import ujfe.html.CssTheme;
import ujfe.html.UtilityCssRenderer;
import ujfe.runtime.lifecycle.LifecycleTracker;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public final class LiveComponentRenderer {
    private final LiveEventRegistry eventRegistry;
    private final ElementIdGenerator elementIdGenerator;
    private final Supplier<CssTheme> themeSupplier;
    private final CssMode cssMode;

    public LiveComponentRenderer(LiveEventRegistry eventRegistry) {
        this(eventRegistry, ElementIdGenerator.sequential(), CssTheme::defaultTheme, CssMode.INTERNAL);
    }

    public LiveComponentRenderer(LiveEventRegistry eventRegistry, ElementIdGenerator elementIdGenerator) {
        this(eventRegistry, elementIdGenerator, CssTheme::defaultTheme, CssMode.INTERNAL);
    }

    public LiveComponentRenderer(
        LiveEventRegistry eventRegistry,
        ElementIdGenerator elementIdGenerator,
        Supplier<CssTheme> themeSupplier
    ) {
        this(eventRegistry, elementIdGenerator, themeSupplier, CssMode.INTERNAL);
    }

    public LiveComponentRenderer(
        LiveEventRegistry eventRegistry,
        ElementIdGenerator elementIdGenerator,
        Supplier<CssTheme> themeSupplier,
        CssMode cssMode
    ) {
        this.eventRegistry = Objects.requireNonNull(eventRegistry, "eventRegistry");
        this.elementIdGenerator = Objects.requireNonNull(elementIdGenerator, "elementIdGenerator");
        this.themeSupplier = Objects.requireNonNull(themeSupplier, "themeSupplier");
        this.cssMode = Objects.requireNonNull(cssMode, "cssMode");
    }

    public LiveRenderResult render(Supplier<? extends Node> nodeSupplier, ClientState clientState) {
        return render(nodeSupplier, clientState, null, null);
    }

    public LiveRenderResult render(
        Supplier<? extends Node> nodeSupplier,
        ClientState clientState,
        LifecycleTracker lifecycleTracker
    ) {
        return render(nodeSupplier, clientState, lifecycleTracker, null);
    }

    LiveRenderResult render(
        Supplier<? extends Node> nodeSupplier,
        ClientState clientState,
        LifecycleTracker lifecycleTracker,
        String eventScope
    ) {
        Objects.requireNonNull(nodeSupplier, "nodeSupplier");
        Objects.requireNonNull(clientState, "clientState");
        eventRegistry.beginRender(eventScope);
        UjfeContext.Builder contextBuilder = UjfeContext.builder()
            .elementIdGenerator(elementIdGenerator)
            .eventRegistrar(handler -> eventRegistry.register(handler))
            .clientState(clientState);
        if (lifecycleTracker != null) {
            contextBuilder.lifecycleTracker(lifecycleTracker);
        }
        UjfeContext context = contextBuilder.build();

        try {
            Node node = UjfeContext.withCurrent(context, nodeSupplier);
            String html = UjfeContext.withCurrent(context, () -> node.render(context));
            String css = renderCss(context.cssClasses());
            eventRegistry.completeRender();
            return new LiveRenderResult(html, css);
        } catch (RuntimeException | Error exception) {
            eventRegistry.abortRender();
            throw exception;
        }
    }

    public String renderCss(Collection<String> classes) {
        Objects.requireNonNull(classes, "classes");
        if (cssMode == CssMode.EXTERNAL) {
            return "";
        }
        return UtilityCssRenderer.render(classes, themeSupplier.get());
    }

    public void handleWithClientState(ClientState clientState, Runnable runnable) {
        Objects.requireNonNull(clientState, "clientState");
        Objects.requireNonNull(runnable, "runnable");
        UjfeContext context = UjfeContext.builder()
            .clientState(clientState)
            .build();
        UjfeContext.withCurrent(context, () -> {
            runnable.run();
            return null;
        });
    }
}
