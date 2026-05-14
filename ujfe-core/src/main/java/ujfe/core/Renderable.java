package ujfe.core;

public interface Renderable {
    String render(UjfeContext context);

    default String render() {
        return render(UjfeContext.create());
    }
}
