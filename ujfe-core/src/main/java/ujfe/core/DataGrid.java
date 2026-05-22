package ujfe.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Server-driven administrative data grid.
 */
public final class DataGrid<T> implements Node {
    private final List<T> rows;
    private final List<Column<T>> columns = new ArrayList<>();
    private final List<RowAction<T>> rowActions = new ArrayList<>();
    private final List<Filter> filters = new ArrayList<>();
    private String caption;
    private String emptyTitle = "No results";
    private String emptyDescription;
    private String rowKeyName = "id";
    private Function<T, String> rowKey;
    private boolean selectable;
    private String sortedBy;
    private boolean descending;
    private int page = 1;
    private int pageSize;
    private int totalRows = -1;
    private String cssClasses;

    DataGrid(Collection<T> rows) {
        Objects.requireNonNull(rows, "rows");
        this.rows = List.copyOf(rows);
    }

    public DataGrid<T> caption(String caption) {
        this.caption = requireText(caption, "caption");
        return this;
    }

    public DataGrid<T> emptyState(String title) {
        this.emptyTitle = requireText(title, "title");
        this.emptyDescription = null;
        return this;
    }

    public DataGrid<T> emptyState(String title, String description) {
        this.emptyTitle = requireText(title, "title");
        this.emptyDescription = requireText(description, "description");
        return this;
    }

    public DataGrid<T> rowKey(String name, Function<T, String> rowKey) {
        this.rowKeyName = requireText(name, "name");
        this.rowKey = Objects.requireNonNull(rowKey, "rowKey");
        return this;
    }

    public DataGrid<T> selectable(boolean selectable) {
        this.selectable = selectable;
        return this;
    }

    public DataGrid<T> sortedBy(String columnKey, boolean descending) {
        this.sortedBy = requireText(columnKey, "columnKey");
        this.descending = descending;
        return this;
    }

    public DataGrid<T> pagination(int page, int pageSize, int totalRows) {
        if (page < 1) {
            throw new IllegalArgumentException("page must be greater than zero");
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be greater than zero");
        }
        if (totalRows < 0) {
            throw new IllegalArgumentException("totalRows cannot be negative");
        }
        this.page = page;
        this.pageSize = pageSize;
        this.totalRows = totalRows;
        return this;
    }

    public DataGrid<T> css(String classes) {
        this.cssClasses = classes;
        return this;
    }

    public DataGrid<T> filter(String name, String label, Object value) {
        filters.add(new Filter(name, label, value == null ? null : value.toString()));
        return this;
    }

    public DataGrid<T> column(String header, Function<T, Node> renderer) {
        columns.add(new Column<>(header, renderer));
        return this;
    }

    public DataGrid<T> textColumn(String header, Function<T, ?> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        return column(header, row -> UI.text(String.valueOf(renderer.apply(row))));
    }

    public DataGrid<T> sortableTextColumn(String key, String header, Function<T, ?> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        columns.add(new Column<T>(header, row -> UI.text(String.valueOf(renderer.apply(row))))
            .sortKey(key));
        return this;
    }

    public DataGrid<T> responsiveTextColumn(String key, String header, int priority, Function<T, ?> renderer) {
        Objects.requireNonNull(renderer, "renderer");
        columns.add(new Column<T>(header, row -> UI.text(String.valueOf(renderer.apply(row))))
            .sortKey(key)
            .priority(priority));
        return this;
    }

    public DataGrid<T> rowAction(String label, Function<T, String> href) {
        rowActions.add(new RowAction<>(label, href));
        return this;
    }

    @Override
    public String render(UjfeContext context) {
        Objects.requireNonNull(context, "context");
        if (columns.isEmpty()) {
            throw new IllegalStateException("DataGrid requires at least one column");
        }

        Element root = UI.section()
            .data("ujfe-data-grid", "true")
            .data("ujfe-grid-selected-key", rowKeyName);
        if (cssClasses != null) {
            root.css(cssClasses);
        }
        if (sortedBy != null) {
            root.data("ujfe-sort", sortedBy)
                .data("ujfe-sort-direction", descending ? "desc" : "asc");
        }

        if (!filters.isEmpty()) {
            root.child(filterBar());
        }

        if (rows.isEmpty()) {
            return root.child(emptyState())
                .render(context);
        }

        root.child(tableNode());
        if (pageSize > 0) {
            root.child(paginationNode());
        }
        return root.render(context);
    }

    private Element tableNode() {
        Element table = UI.table();
        if (caption != null) {
            table.child(UI.caption(caption));
        }
        table.child(headerNode());
        Element body = UI.tbody();
        for (T row : rows) {
            body.child(rowNode(row));
        }
        table.child(body);
        return table;
    }

    private Element headerNode() {
        Element header = UI.tr();
        if (selectable) {
            header.child(UI.th()
                .attr("scope", "col")
                .child(UI.checkbox()
                    .ariaLabel("Select all rows")
                    .data("ujfe-grid-select-all", "true")));
        }
        for (Column<T> column : columns) {
            Element cell = UI.th(column.header)
                .attr("scope", "col");
            if (column.sortKey != null) {
                cell.data("ujfe-sort-key", column.sortKey);
                if (column.sortKey.equals(sortedBy)) {
                    cell.aria("sort", descending ? "descending" : "ascending");
                }
            }
            if (column.priority > 0) {
                cell.data("ujfe-column-priority", Integer.toString(column.priority));
            }
            header.child(cell);
        }
        if (!rowActions.isEmpty()) {
            header.child(UI.th("Actions")
                .attr("scope", "col"));
        }
        return UI.thead()
            .child(header);
    }

    private Element rowNode(T row) {
        Element tr = UI.tr();
        String key = rowKey == null ? null : rowKey.apply(row);
        if (key != null) {
            tr.data("ujfe-row-key", key);
        }
        if (selectable) {
            Element checkbox = UI.checkbox()
                .name(rowKeyName)
                .ariaLabel("Select row");
            if (key != null) {
                checkbox.value(key);
            }
            tr.child(UI.td()
                .child(checkbox));
        }
        for (Column<T> column : columns) {
            Element cell = UI.td()
                .child(column.renderer.apply(row));
            if (column.priority > 0) {
                cell.data("ujfe-column-priority", Integer.toString(column.priority));
            }
            tr.child(cell);
        }
        if (!rowActions.isEmpty()) {
            Element actions = UI.div()
                .data("ujfe-row-actions", "true");
            for (RowAction<T> action : rowActions) {
                actions.child(UI.a(action.label)
                    .href(action.href.apply(row)));
            }
            tr.child(UI.td()
                .child(actions));
        }
        return tr;
    }

    private Element filterBar() {
        Element form = UI.form()
            .method("get")
            .data("ujfe-grid-filters", "true")
            .role("search");
        for (Filter filter : filters) {
            form.child(UI.label(filter.label)
                .forId("ujfe-grid-filter-" + filter.name));
            Element input = UI.inputSearch()
                .id("ujfe-grid-filter-" + filter.name)
                .name(filter.name)
                .data("ujfe-grid-filter", filter.name);
            if (filter.value != null) {
                input.value(filter.value);
            }
            form.child(input);
        }
        form.child(UI.button("Apply")
            .type("submit"));
        return form;
    }

    private Element emptyState() {
        Element empty = UI.div()
            .data("ujfe-empty-state", "true")
            .role("status")
            .child(UI.strong(emptyTitle));
        if (emptyDescription != null) {
            empty.child(UI.p(emptyDescription));
        }
        return empty;
    }

    private Element paginationNode() {
        int from = Math.min(((page - 1) * pageSize) + 1, totalRows);
        int to = Math.min(page * pageSize, totalRows);
        return UI.nav()
            .ariaLabel("Grid pagination")
            .data("ujfe-pagination", "true")
            .data("ujfe-page", Integer.toString(page))
            .data("ujfe-page-size", Integer.toString(pageSize))
            .data("ujfe-total-rows", Integer.toString(totalRows))
            .child(UI.span(from + "-" + to + " of " + totalRows));
    }

    public static final class Column<T> {
        private final String header;
        private final Function<T, Node> renderer;
        private String sortKey;
        private int priority;

        private Column(String header, Function<T, Node> renderer) {
            this.header = requireText(header, "header");
            this.renderer = Objects.requireNonNull(renderer, "renderer");
        }

        private Column<T> sortKey(String sortKey) {
            this.sortKey = requireText(sortKey, "sortKey");
            return this;
        }

        private Column<T> priority(int priority) {
            if (priority < 1) {
                throw new IllegalArgumentException("column priority must be greater than zero");
            }
            this.priority = priority;
            return this;
        }
    }

    private static final class Filter {
        private final String name;
        private final String label;
        private final String value;

        private Filter(String name, String label, String value) {
            this.name = safeName(name);
            this.label = requireText(label, "label");
            this.value = value;
        }
    }

    private static final class RowAction<T> {
        private final String label;
        private final Function<T, String> href;

        private RowAction(String label, Function<T, String> href) {
            this.label = requireText(label, "label");
            this.href = Objects.requireNonNull(href, "href");
        }
    }

    private static String requireText(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
        return value.trim();
    }

    private static String safeName(String value) {
        String name = requireText(value, "name");
        if (!name.matches("[A-Za-z][A-Za-z0-9_-]*")) {
            throw new IllegalArgumentException("Invalid grid filter name: " + value);
        }
        return name;
    }
}
