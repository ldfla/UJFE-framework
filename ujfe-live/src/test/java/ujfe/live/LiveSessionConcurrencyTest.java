package ujfe.live;

import org.junit.jupiter.api.Test;
import ujfe.core.ClientState;
import ujfe.core.Component;
import ujfe.core.Node;
import ujfe.router.Page;
import ujfe.router.Router;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static ujfe.core.UI.*;

final class LiveSessionConcurrencyTest {
    private static final Duration SHORT_WAIT = Duration.ofMillis(150);
    private static final Duration LONG_WAIT = Duration.ofSeconds(2);

    @Test
    void concurrentEventsInSameSessionAreSerializedAndRemainConsistent() throws Exception {
        BlockingTwoButtonPage page = new BlockingTwoButtonPage();
        try (LiveSession session = new LiveSession(new Router().register(page))) {
            List<String> eventIds = extractEventIds(session.renderDocument("/", ClientState.empty()));
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<LiveRenderResult> first = executor.submit(
                    () -> session.handleEvent(eventIds.get(0), ClientState.empty(), metadata(session)));
                assertTrue(page.first.awaitEntered(LONG_WAIT));

                Future<LiveRenderResult> second = executor.submit(
                    () -> session.handleEvent(eventIds.get(1), ClientState.empty(), metadata(session)));
                assertFalse(page.second.awaitEntered(SHORT_WAIT));

                page.first.release();
                assertTrue(first.get(2, TimeUnit.SECONDS)
                    .html()
                    .contains("Counter: 1"));
                assertTrue(page.second.awaitEntered(LONG_WAIT));

                page.second.release();
                assertTrue(second.get(2, TimeUnit.SECONDS)
                    .html()
                    .contains("Counter: 2"));
                assertEquals(2, page.count());
                assertTrue(session.renderPath("/")
                    .html()
                    .contains("Counter: 2"));
            } finally {
                page.releaseAll();
                shutdown(executor);
            }
        }
    }

    @Test
    void independentSessionsCanProcessEventsAtTheSameTime() throws Exception {
        BlockingSingleButtonPage firstPage = new BlockingSingleButtonPage();
        BlockingSingleButtonPage secondPage = new BlockingSingleButtonPage();
        try (LiveSession firstSession = new LiveSession(new Router().register(firstPage));
             LiveSession secondSession = new LiveSession(new Router().register(secondPage))) {
            String firstEventId = extractEventIds(firstSession.renderDocument("/", ClientState.empty())).get(0);
            String secondEventId = extractEventIds(secondSession.renderDocument("/", ClientState.empty())).get(0);
            CountDownLatch start = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Future<LiveRenderResult> first = executor.submit(() -> {
                    start.await();
                    return firstSession.handleEvent(firstEventId, ClientState.empty(), metadata(firstSession));
                });
                Future<LiveRenderResult> second = executor.submit(() -> {
                    start.await();
                    return secondSession.handleEvent(secondEventId, ClientState.empty(), metadata(secondSession));
                });

                start.countDown();
                assertTrue(firstPage.handler.awaitEntered(LONG_WAIT));
                assertTrue(secondPage.handler.awaitEntered(LONG_WAIT));

                firstPage.handler.release();
                secondPage.handler.release();
                assertTrue(first.get(2, TimeUnit.SECONDS)
                    .html()
                    .contains("Counter: 1"));
                assertTrue(second.get(2, TimeUnit.SECONDS)
                    .html()
                    .contains("Counter: 1"));
                assertEquals(1, firstPage.count());
                assertEquals(1, secondPage.count());
            } finally {
                firstPage.handler.release();
                secondPage.handler.release();
                shutdown(executor);
            }
        }
    }

    @Test
    void noSignalUpdatesAreLostUnderSameSessionContention() throws Exception {
        SignalCounterPage page = new SignalCounterPage();
        try (LiveSession session = new LiveSession(new Router().register(page))) {
            String eventId = extractEventIds(session.renderDocument("/", ClientState.empty())).get(0);
            int eventCount = 32;
            ExecutorService executor = Executors.newFixedThreadPool(8);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<LiveRenderResult>> results = new ArrayList<>();
            try {
                for (int index = 0; index < eventCount; index++) {
                    results.add(executor.submit(() -> {
                        start.await();
                        return session.handleEvent(eventId, ClientState.empty(), metadata(session));
                    }));
                }

                start.countDown();
                for (Future<LiveRenderResult> result : results) {
                    assertTrue(result.get(5, TimeUnit.SECONDS)
                        .html()
                        .contains("Counter:"));
                }

                assertEquals(eventCount, page.count());
                String html = session.renderPath("/")
                    .html();
                assertTrue(html.contains("Counter: " + eventCount), html);
                assertTrue(html.contains("<button"));
                assertTrue(html.contains("</button>"));
            } finally {
                shutdown(executor);
            }
        }
    }

    private static LiveHttpRequestMetadata metadata(LiveSession session) {
        return new LiveHttpRequestMetadata(session.csrfToken(), "http://localhost", null, "localhost", "http");
    }

    private static List<String> extractEventIds(String html) {
        Matcher matcher = Pattern.compile("data-ujfe-event=\"([^\"]+)\"")
            .matcher(html);
        List<String> eventIds = new ArrayList<>();
        while (matcher.find()) {
            eventIds.add(matcher.group(1));
        }
        assertFalse(eventIds.isEmpty(), "Expected rendered HTML to include at least one live event id");
        return eventIds;
    }

    private static void shutdown(ExecutorService executor) throws InterruptedException {
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
    }

    @Page("/")
    public static final class SignalCounterPage implements Component {
        private final Signal<Integer> count = Signals.signal(0);

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Counter: " + count.get()))
                .child(button("Increment").onClick(() -> count.update(value -> value + 1)));
        }

        int count() {
            return count.get();
        }
    }

    @Page("/")
    public static final class BlockingSingleButtonPage implements Component {
        private final Signal<Integer> count = Signals.signal(0);
        private final BlockingIncrement handler = new BlockingIncrement(count);

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Counter: " + count.get()))
                .child(button("Increment").onClick(handler::run));
        }

        int count() {
            return count.get();
        }
    }

    @Page("/")
    public static final class BlockingTwoButtonPage implements Component {
        private final Signal<Integer> count = Signals.signal(0);
        private final BlockingIncrement first = new BlockingIncrement(count);
        private final BlockingIncrement second = new BlockingIncrement(count);

        @Override
        public Node render() {
            return div()
                .child(p(() -> "Counter: " + count.get()))
                .child(button("First").onClick(first::run))
                .child(button("Second").onClick(second::run));
        }

        int count() {
            return count.get();
        }

        void releaseAll() {
            first.release();
            second.release();
        }
    }

    private static final class BlockingIncrement {
        private final Signal<Integer> count;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release = new CountDownLatch(1);

        private BlockingIncrement(Signal<Integer> count) {
            this.count = count;
        }

        void run() {
            entered.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new AssertionError("Timed out waiting to release live event handler");
                }
            } catch (InterruptedException exception) {
                Thread.currentThread()
                    .interrupt();
                throw new AssertionError("Interrupted while waiting to release live event handler", exception);
            }
            count.update(value -> value + 1);
        }

        boolean awaitEntered(Duration duration) throws InterruptedException {
            return entered.await(duration.toMillis(), TimeUnit.MILLISECONDS);
        }

        void release() {
            release.countDown();
        }
    }
}
