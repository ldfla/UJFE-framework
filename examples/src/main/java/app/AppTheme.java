package app;

import ujfe.html.CssTheme;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

public final class AppTheme {
    private final Signal<String> primaryColor = Signals.signal("#2563eb");
    private final Signal<String> secondaryColor = Signals.signal("#059669");

    public CssTheme cssTheme() {
        return CssTheme.of(primaryColor.get(), secondaryColor.get());
    }

    public String primaryColor() {
        return primaryColor.get();
    }

    public String secondaryColor() {
        return secondaryColor.get();
    }

    public void useBlueEmerald() {
        primaryColor.set("#2563eb");
        secondaryColor.set("#059669");
    }

    public void useRoseAmber() {
        primaryColor.set("#be123c");
        secondaryColor.set("#d97706");
    }

    public void useIndigoCyan() {
        primaryColor.set("#4338ca");
        secondaryColor.set("#0891b2");
    }
}
