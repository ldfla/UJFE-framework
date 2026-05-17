package ujfe.router;

import org.junit.jupiter.api.Test;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.fixtures.ScannedAlphaPage;
import ujfe.router.fixtures.ScannedBetaPage;
import ujfe.router.source.ManualRouteSource;
import ujfe.router.source.ReflectionPageScanner;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.*;

final class RouterTest {
    @Test
    void registersAnnotatedPageAndRendersIt() {
        Router router = new Router().register(new AboutPage());

        assertTrue(router.resolve("/about").isPresent());
        Node node = new PageRenderer().render(router.resolve("/about").orElseThrow());

        assertEquals("<div><h1>About</h1></div>", node.render());
    }

    @Test
    void scannerFindsAnnotatedClassesAndIgnoresUnannotatedCandidates() {
        ReflectionPageScanner scanner = new ReflectionPageScanner(UnannotatedPage.class, AboutPage.class);
        Router router = new Router().register(scanner);

        assertTrue(router.resolve("/about").isPresent());
        assertFalse(router.resolve("/ignored").isPresent());
    }

    @Test
    void scannerCanDiscoverPagesFromPackageResources() {
        ReflectionPageScanner scanner = ReflectionPageScanner.forPackages("ujfe.router.fixtures");
        List<String> paths = scanner.routes().stream()
                .map(RouteDefinition::path)
                .collect(Collectors.toList());

        assertEquals(List.of("/alpha", "/beta"), paths);
    }

    @Test
    void scannerDetectsDuplicateRoutesWithConflictingClasses() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new ReflectionPageScanner(DuplicateA.class, DuplicateB.class).routes());

        assertTrue(failure.getMessage().contains("Duplicate route path '/same'"));
        assertTrue(failure.getMessage().contains(DuplicateA.class.getName()));
        assertTrue(failure.getMessage().contains(DuplicateB.class.getName()));
    }

    @Test
    void scannerRejectsInvalidRenderMethods() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new ReflectionPageScanner(InvalidRenderPage.class).routes());

        assertTrue(failure.getMessage().contains("render() must return ujfe.core.Node"));
        assertTrue(failure.getMessage().contains(InvalidRenderPage.class.getName()));
    }

    @Test
    void scannerRejectsAnnotatedClassesWithoutRenderMethod() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new ReflectionPageScanner(NoRenderPage.class).routes());

        assertTrue(failure.getMessage().contains("public render()"));
        assertTrue(failure.getMessage().contains(NoRenderPage.class.getName()));
    }

    @Test
    void scannerRejectsAbstractPageClasses() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new ReflectionPageScanner(AbstractPage.class).routes());

        assertTrue(failure.getMessage().contains("abstract"));
        assertTrue(failure.getMessage().contains(AbstractPage.class.getName()));
    }

    @Test
    void scannerIgnoresInterfacesSafely() {
        ReflectionPageScanner scanner = new ReflectionPageScanner(AnnotatedInterfacePage.class, AboutPage.class);
        List<String> paths = scanner.routes().stream()
                .map(RouteDefinition::path)
                .collect(Collectors.toList());

        assertEquals(List.of("/about"), paths);
    }

    @Test
    void scannerRejectsAnnotatedClassesWithoutNoArgumentConstructor() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new ReflectionPageScanner(ConstructorOnlyPage.class).routes());

        assertTrue(failure.getMessage().contains("no-argument constructor"));
        assertTrue(failure.getMessage().contains(ConstructorOnlyPage.class.getName()));
    }

    @Test
    void manualRouteSourceRegistersRoutesWithoutAnnotatedPageClasses() {
        ManualRouteSource source = new ManualRouteSource()
                .register("/", PlainPage::new)
                .register("/dashboard", DashboardPage::new);
        Router router = new Router().register(source);

        assertEquals(List.of("/", "/dashboard"), routePaths(router));
        assertEquals("<div><p>Plain</p></div>", new PageRenderer().render(router.resolve("/").orElseThrow()).render());
    }

    @Test
    void manualRouteSourceAcceptsRouteDefinitionMetadata() {
        RouteDefinition route = new RouteDefinition("/metadata", PlainPage::new, "generated metadata");
        ManualRouteSource source = new ManualRouteSource()
                .register(route);
        Router router = new Router().register(source);

        RouteDefinition resolved = router.resolve("/metadata").orElseThrow();
        assertEquals("generated metadata", resolved.sourceDescription());
        assertEquals("<div><p>Plain</p></div>", new PageRenderer().render(resolved).render());
    }

    @Test
    void registrationOrderIsPreservedAcrossManualSources() {
        ManualRouteSource first = new ManualRouteSource()
                .register("/first", PlainPage::new)
                .register("/second", DashboardPage::new);
        ManualRouteSource second = new ManualRouteSource()
                .register("/third", ComponentPage::new);

        Router router = new Router().register(first, second);

        assertEquals(List.of("/first", "/second", "/third"), routePaths(router));
    }

    @Test
    void reflectionScanningOrderIsDeterministic() {
        ReflectionPageScanner scanner = new ReflectionPageScanner(ScannedBetaPage.class, ScannedAlphaPage.class);
        List<String> paths = scanner.routes().stream()
                .map(RouteDefinition::path)
                .collect(Collectors.toList());

        assertEquals(List.of("/alpha", "/beta"), paths);
    }

    @Test
    void duplicateRoutesFailDuringManualSourceRegistration() {
        ManualRouteSource source = new ManualRouteSource()
                .register("/same", PlainPage::new);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> source.register("/same", DashboardPage::new));

        assertTrue(failure.getMessage().contains("Duplicate route path '/same'"));
    }

    @Test
    void duplicateRoutesFailWhenMergingSourcesIntoRouter() {
        ManualRouteSource first = new ManualRouteSource()
                .register("/same", PlainPage::new);
        ManualRouteSource second = new ManualRouteSource()
                .register("/same", DashboardPage::new);

        IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                () -> new Router().register(first, second));

        assertTrue(failure.getMessage().contains("Duplicate route path '/same'"));
    }

    @Test
    void invalidRoutePathsFailPredictably() {
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition("", PlainPage::new));
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition(" ", PlainPage::new));
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition("/bad path", PlainPage::new));
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition("/bad?query", PlainPage::new));
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition("/bad#fragment", PlainPage::new));
        assertThrows(IllegalArgumentException.class, () -> new RouteDefinition("/<bad>", PlainPage::new));
    }

    @Test
    void routerWorksWithEmptyRouteSource() {
        Router router = new Router().register(new ManualRouteSource());

        assertTrue(router.routes().isEmpty());
        assertFalse(router.resolve("/missing").isPresent());
    }

    @Test
    void routeDefinitionExposesAotFriendlyMetadata() {
        RouteDefinition route = RouteDefinition.pageClass("/component", ComponentPage.class);

        assertEquals("/component", route.path());
        assertEquals(ComponentPage.class, route.pageType().orElseThrow());
        assertEquals(ComponentPage.class.getName(), route.sourceDescription());
        assertEquals("<p>Component</p>", new PageRenderer().render(route).render());
    }

    @Test
    void routerRejectsInvalidAnnotatedPageInstances() {
        RouteDiscoveryException failure = assertThrows(RouteDiscoveryException.class,
                () -> new Router().register(new InvalidRenderPage()));

        assertTrue(failure.getMessage().contains("render() must return ujfe.core.Node"));
    }

    private static List<String> routePaths(Router router) {
        return router.routes().stream()
                .map(RouteDefinition::path)
                .collect(Collectors.toList());
    }

    @Page("/about")
    public static final class AboutPage {
        public Node render() {
            return div().child(h1("About"));
        }
    }

    public static final class UnannotatedPage {
        public Node render() {
            return div().child("Ignored");
        }
    }

    @Page("/same")
    public static final class DuplicateA {
        public Node render() {
            return div();
        }
    }

    @Page("/same")
    public static final class DuplicateB {
        public Node render() {
            return div();
        }
    }

    @Page("/invalid")
    public static final class InvalidRenderPage {
        public String render() {
            return "invalid";
        }
    }

    @Page("/missing-render")
    public static final class NoRenderPage {
    }

    @Page("/abstract")
    public abstract static class AbstractPage {
        public Node render() {
            return div();
        }
    }

    @Page("/interface")
    public interface AnnotatedInterfacePage {
    }

    @Page("/constructor")
    public static final class ConstructorOnlyPage {
        ConstructorOnlyPage(String name) {
        }

        public Node render() {
            return div();
        }
    }

    public static final class PlainPage {
        public Node render() {
            return div().child(p("Plain"));
        }
    }

    public static final class DashboardPage {
        public Node render() {
            return div().child(p("Dashboard"));
        }
    }

    public static final class ComponentPage implements Component {
        @Override
        public Node render() {
            return p("Component");
        }
    }
}
