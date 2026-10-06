package com.xyp.gtnotgood.common.mestock;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** GUI-local cached search/group index. Only the visible lines are synchronized; no world or per-tick list scan. */
final class StockRequesterView {

    private final StockGuiFactory.Data data;
    private final List<Line> lines = new ArrayList<>();
    private long revision = Long.MIN_VALUE;
    private String indexedQuery = "";
    String query = "";
    int scroll;
    int visibleRows = 5;

    StockRequesterView(StockGuiFactory.Data data) {
        this.data = data;
    }

    private void refresh() {
        StockGridCache cache = data.cache();
        long current = cache == null ? -1 : cache.directoryRevision();
        if (current == revision && indexedQuery.equals(query)) return;
        revision = current;
        indexedQuery = query;
        lines.clear();
        if (cache == null) return;
        String search = query.toLowerCase(Locale.ROOT);
        Map<String, List<TileMERequester>> groups = new TreeMap<>();
        for (TileMERequester tile : cache.listing()) {
            String name = tile.name();
            boolean matches = (name.isEmpty() ? StockText.Requester.text() : name).toLowerCase(Locale.ROOT)
                .contains(search);
            if (!matches && !search.isEmpty()) for (int row = 0; row < tile.stockConfig()
                .size(); row++) {
                    var sample = StockResources.display(
                        tile.stockConfig()
                            .key(row));
                    if (sample != null && sample.getDisplayName()
                        .toLowerCase(Locale.ROOT)
                        .contains(search)) matches = true;
                }
            if (matches) groups.computeIfAbsent(name, ignored -> new ArrayList<>())
                .add(tile);
        }
        for (var group : groups.entrySet()) {
            lines.add(
                new Line(
                    null,
                    -1,
                    group.getKey(),
                    group.getValue()
                        .size()));
            for (TileMERequester tile : group.getValue()) for (int row = 0; row < tile.stockConfig()
                .size(); row++) lines.add(new Line(tile, row, ""));
        }
        scroll = Math.max(0, Math.min(scroll, maximumScroll()));
    }

    int maximumScroll() {
        refreshIfNeeded();
        return Math.max(0, lines.size() - visibleRows);
    }

    private void refreshIfNeeded() {
        StockGridCache cache = data.cache();
        if ((cache == null ? -1 : cache.directoryRevision()) != revision || !indexedQuery.equals(query)) refresh();
    }

    Line line(int viewportRow) {
        if (viewportRow >= visibleRows) return null;
        refreshIfNeeded();
        int index = scroll + viewportRow;
        return index < 0 || index >= lines.size() ? null : lines.get(index);
    }

    void setScroll(int value) {
        scroll = Math.max(0, Math.min(value, maximumScroll()));
    }

    static final class Line {

        final TileMERequester tile;
        final int row;
        final String header;
        final int groupCount;

        Line(TileMERequester tile, int row, String header) {
            this(tile, row, header, 0);
        }

        Line(TileMERequester tile, int row, String header, int groupCount) {
            this.tile = tile;
            this.row = row;
            this.header = header;
            this.groupCount = groupCount;
        }

        String token() {
            return tile == null ? ""
                : tile.getWorldObj().provider.dimensionId + ":"
                    + tile.xCoord
                    + ":"
                    + tile.yCoord
                    + ":"
                    + tile.zCoord
                    + ":"
                    + row
                    + ":"
                    + System.identityHashCode(tile)
                    + ":"
                    + System.identityHashCode(
                        tile.stockConfig()
                            .key(row));
        }
    }
}
