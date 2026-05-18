package ujfe.cli;

final class ConversionStats {
    private final int convertedElements;
    private final int preservedAttributes;
    private final int unsafeFallbacks;
    private final int componentMethods;
    private final int cssBlocks;

    ConversionStats(int convertedElements, int preservedAttributes, int unsafeFallbacks, int componentMethods, int cssBlocks) {
        this.convertedElements = convertedElements;
        this.preservedAttributes = preservedAttributes;
        this.unsafeFallbacks = unsafeFallbacks;
        this.componentMethods = componentMethods;
        this.cssBlocks = cssBlocks;
    }

    int convertedElements() {
        return convertedElements;
    }

    int preservedAttributes() {
        return preservedAttributes;
    }

    int unsafeFallbacks() {
        return unsafeFallbacks;
    }

    int componentMethods() {
        return componentMethods;
    }

    int cssBlocks() {
        return cssBlocks;
    }
}
