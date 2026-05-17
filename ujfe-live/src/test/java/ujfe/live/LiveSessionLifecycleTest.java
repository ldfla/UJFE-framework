package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Component;
import ujfe.core.Lifecycle;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.runtime.action.RuntimeActionRegistry;
import ujfe.runtime.action.RuntimePhase;
import ujfe.runtime.lifecycle.LifecycleException;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.html.UI.*;

final class LiveSessionLifecycleTest {
    @Test
    void pageMountsOnceAndStableEventRerenderDoesNotRemount() {
        StatefulLifecyclePage page = new StatefulLifecyclePage();
        try (LiveSession session = new LiveSession(new Router().register(page))) {
            String document = session.renderDocument("/", ClientState.empty());
            String eventId = extractEventId(document);
            session.handleEvent(eventId, ClientState.empty(), new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http"));
        }

        assertEquals(1, page.mounts());
        assertEquals(1, page.unmounts());
        assertEquals(1, page.count());
    }

    @Test
    void sessionCloseUnmountsMountedPage() {
        StatefulLifecyclePage page = new StatefulLifecyclePage();
        try (LiveSession session = new LiveSession(new Router().register(page))) {
            session.renderDocument("/", ClientState.empty());
        }

        assertEquals(1, page.mounts());
        assertEquals(1, page.unmounts());
    }

    @Test
    void routeSwitchUnmountsOldPageAndMountsNewPage() {
        FirstRoutePage first = new FirstRoutePage();
        SecondRoutePage second = new SecondRoutePage();
        try (LiveSession session = new LiveSession(new Router().register(first).register(second))) {
            session.renderDocument("/first", ClientState.empty());
            session.renderDocument("/second", ClientState.empty());
        }

        assertEquals(1, first.mounts());
        assertEquals(1, first.unmounts());
        assertEquals(1, second.mounts());
        assertEquals(1, second.unmounts());
    }

    @Test
    void nestedLifecycleComponentsAreTracked() {
        ParentPage page = new ParentPage();
        try (LiveSession session = new LiveSession(new Router().register(page))) {
            session.renderDocument("/", ClientState.empty());
            session.renderPath("/");
        }

        assertEquals(1, page.mounts());
        assertEquals(1, page.unmounts());
        assertEquals(1, page.child.mounts());
        assertEquals(1, page.child.unmounts());
    }

    @Test
    void lifecycleMountFailuresRouteToRuntimeActions() {
        AtomicInteger errors = new AtomicInteger();
        AtomicReference<RuntimePhase> phase = new AtomicReference<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .onError(context -> {
                    errors.incrementAndGet();
                    phase.set(context.phase());
                })
                .build();

        try (LiveSession session = new LiveSession(
                new Router().register(new FailingMountPage()),
                LiveSessionConfig.builder().runtimeActions(actions).build()
        )) {
            assertThrows(LifecycleException.class, () -> session.renderDocument("/", ClientState.empty()));
        }
        assertEquals(1, errors.get());
        assertEquals(RuntimePhase.LIFECYCLE, phase.get());
    }

    @Test
    void lifecycleUnmountFailuresRouteToRuntimeActionsAndCleanupContinues() {
        FailingUnmountPage failing = new FailingUnmountPage();
        SecondRoutePage stable = new SecondRoutePage();
        AtomicInteger errors = new AtomicInteger();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .onError(context -> errors.incrementAndGet())
                .build();

        try (LiveSession session = new LiveSession(
                new Router().register(failing).register(stable),
                LiveSessionConfig.builder().runtimeActions(actions).build()
        )) {
            session.renderDocument("/failing", ClientState.empty());
            assertDoesNotThrow(() -> session.renderDocument("/second", ClientState.empty()));
            assertDoesNotThrow(session::close);
        }

        assertEquals(1, failing.unmountAttempts());
        assertEquals(1, stable.unmounts());
        assertEquals(1, errors.get());
    }

    @Test
    void lifecycleFailuresDuringEventRerenderIncludeEventId() {
        EventUnmountFailurePage page = new EventUnmountFailurePage();
        AtomicReference<String> routedEventId = new AtomicReference<>();
        RuntimeActionRegistry actions = RuntimeActionRegistry.builder()
                .onError(context -> routedEventId.set(context.eventId()))
                .build();

        String eventId;
        try (LiveSession session = new LiveSession(
                new Router().register(page),
                LiveSessionConfig.builder().runtimeActions(actions).build()
        )) {
            String document = session.renderDocument("/", ClientState.empty());
            eventId = extractEventId(document);

            session.handleEvent(eventId, ClientState.empty(), new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http"));
        }

        assertEquals(eventId, routedEventId.get());
    }

    private static String extractEventId(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find());
        return matcher.group(1);
    }

    private static class CountingComponent implements Component, Lifecycle {
        private final AtomicInteger mounts = new AtomicInteger();
        private final AtomicInteger unmounts = new AtomicInteger();

        @Override
        public void onMount() {
            mounts.incrementAndGet();
        }

        @Override
        public void onUnmount() {
            unmounts.incrementAndGet();
        }

        @Override
        public Node render() {
            return div();
        }

        int mounts() {
            return mounts.get();
        }

        int unmounts() {
            return unmounts.get();
        }
    }

    @Page("/")
    public static final class StatefulLifecyclePage extends CountingComponent {
        private final AtomicInteger count = new AtomicInteger();

        @Override
        public Node render() {
            return div()
                    .child(p(() -> "Count: " + count.get()))
                    .child(button("Increment").onClick(count::incrementAndGet));
        }

        int count() {
            return count.get();
        }
    }

    @Page("/first")
    public static final class FirstRoutePage extends CountingComponent {
        @Override
        public Node render() {
            return div().child("First");
        }
    }

    @Page("/second")
    public static final class SecondRoutePage extends CountingComponent {
        @Override
        public Node render() {
            return div().child("Second");
        }
    }

    @Page("/")
    public static final class ParentPage extends CountingComponent {
        private final CountingComponent child = new CountingComponent();

        @Override
        public Node render() {
            return div().child(component(child));
        }
    }

    @Page("/")
    public static final class FailingMountPage extends CountingComponent {
        @Override
        public void onMount() {
            super.onMount();
            throw new IllegalStateException("mount failed");
        }
    }

    @Page("/failing")
    public static final class FailingUnmountPage extends CountingComponent {
        private final AtomicInteger unmountAttempts = new AtomicInteger();

        @Override
        public void onUnmount() {
            unmountAttempts.incrementAndGet();
            throw new IllegalStateException("unmount failed");
        }

        int unmountAttempts() {
            return unmountAttempts.get();
        }

        @Override
        public Node render() {
            return div().child("Failing");
        }
    }

    @Page("/")
    public static final class EventUnmountFailurePage implements Component {
        private final AtomicReference<Boolean> showChild = new AtomicReference<>(true);
        private final FailingUnmountPage child = new FailingUnmountPage();

        @Override
        public Node render() {
            var root = div()
                    .child(button("Hide child").onClick(() -> showChild.set(false)));

            if (showChild.get()) {
                root.child(component(child));
            }

            return root;
        }
    }
}
