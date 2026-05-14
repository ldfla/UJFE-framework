package ujfe.live;

import ujfe.core.ElementIdGenerator;
import ujfe.core.ClientState;
import ujfe.core.Node;
import ujfe.core.UjfeContext;
import ujfe.html.CssTheme;
import ujfe.html.UtilityCssRenderer;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;

public final class LiveComponentRenderer {
    private final LiveEventRegistry eventRegistry;
    private final ElementIdGenerator elementIdGenerator;
    private final Supplier<CssTheme> themeSupplier;

    public LiveComponentRenderer(LiveEventRegistry eventRegistry) {
        this(eventRegistry, ElementIdGenerator.sequential(), CssTheme::defaultTheme);
    }

    public LiveComponentRenderer(LiveEventRegistry eventRegistry, ElementIdGenerator elementIdGenerator) {
        this(eventRegistry, elementIdGenerator, CssTheme::defaultTheme);
    }

    public LiveComponentRenderer(
            LiveEventRegistry eventRegistry,
            ElementIdGenerator elementIdGenerator,
            Supplier<CssTheme> themeSupplier
    ) {
        this.eventRegistry = Objects.requireNonNull(eventRegistry, "eventRegistry");
        this.elementIdGenerator = Objects.requireNonNull(elementIdGenerator, "elementIdGenerator");
        this.themeSupplier = Objects.requireNonNull(themeSupplier, "themeSupplier");
    }

    public LiveRenderResult render(Supplier<? extends Node> nodeSupplier, ClientState clientState) {
        Objects.requireNonNull(nodeSupplier, "nodeSupplier");
        Objects.requireNonNull(clientState, "clientState");
        eventRegistry.clear();
        UjfeContext context = UjfeContext.builder()
                .elementIdGenerator(elementIdGenerator)
                .eventRegistrar(eventRegistry::register)
                .clientState(clientState)
                .build();

        Node node = UjfeContext.withCurrent(context, nodeSupplier);
        String html = UjfeContext.withCurrent(context, () -> node.render(context));
        String css = UtilityCssRenderer.render(context.cssClasses(), themeSupplier.get());
        return new LiveRenderResult(html, css);
    }

    public String renderCss(Collection<String> classes) {
        Objects.requireNonNull(classes, "classes");
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
