package ujfe.core;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class EscaperTest {
    @Test
    void escapesHtmlText() {
        assertEquals("&lt;strong&gt;A&amp;B&lt;/strong&gt;", HtmlEscaper.escape("<strong>A&B</strong>"));
    }

    @Test
    void escapesAttributeValues() {
        assertEquals("A&quot;B&#39;&amp;&lt;&gt;", AttributeEscaper.escape("A\"B'&<>"));
    }

    @Test
    void exposesCurrentClientState() {
        ClientState clientState = ClientState.of(
                Map.of("ujfe_demo", "ativo"),
                Map.of("ujfe.theme", "dark"),
                Map.of("ujfe.tab", "docs")
        );
        UjfeContext context = UjfeContext.builder()
                .clientState(clientState)
                .build();

        String value = UjfeContext.withCurrent(context, () ->
                Ujfe.cookie("ujfe_demo").orElse("missing")
                        + "/" + Ujfe.localStorage("ujfe.theme").orElse("missing")
                        + "/" + Ujfe.sessionStorage("ujfe.tab").orElse("missing")
        );

        assertEquals("ativo/dark/docs", value);
    }
}
