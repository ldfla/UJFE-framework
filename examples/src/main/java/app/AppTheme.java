package app;

import ujfe.core.CssTheme;
import ujfe.signals.Signal;
import ujfe.signals.Signals;

public final class AppTheme {
    private final Signal<String> primaryColor = Signals.signal("#2563eb");
    private final Signal<String> secondaryColor = Signals.signal("#059669");
    private final Signal<Boolean> darkMode = Signals.signal(false);
    private final Signal<Boolean> devPreview = Signals.signal(false);

    public CssTheme cssTheme() {
        return CssTheme.of(primaryColor.get(), secondaryColor.get());
    }

    public String primaryColor() {
        return primaryColor.get();
    }

    public String secondaryColor() {
        return secondaryColor.get();
    }

    public boolean darkMode() {
        return darkMode.get();
    }

    public boolean devPreviewEnabled() {
        return devPreview.get();
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

    public void toggleDarkMode() {
        darkMode.update(enabled -> !enabled);
    }

    public void toggleDevPreview() {
        devPreview.update(enabled -> !enabled);
    }
}
