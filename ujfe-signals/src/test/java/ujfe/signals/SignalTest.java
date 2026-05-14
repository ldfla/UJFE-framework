package ujfe.signals;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class SignalTest {
    @Test
    void updatesAndNotifiesSubscribers() throws Exception {
        Signal<Integer> count = Signals.signal(0);
        List<Integer> updates = new ArrayList<>();

        AutoCloseable subscription = count.subscribe(updates::add);
        count.set(1);
        count.update(value -> value + 1);
        subscription.close();
        count.set(3);

        assertEquals(3, count.get());
        assertEquals(List.of(1, 2), updates);
    }
}
