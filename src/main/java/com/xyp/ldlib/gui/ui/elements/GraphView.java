package com.xyp.ldlib.gui.ui.elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.Tessellator;

import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import com.xyp.ldlib.gui.render.ScissorScope;
import com.xyp.ldlib.gui.texture.IGuiTexture;
import com.xyp.ldlib.gui.texture.TextTexture;
import com.xyp.ldlib.gui.ui.UIElement;
import com.xyp.ldlib.gui.ui.event.UIEvents;

/**
 * LDLib2 GraphView camera contract adapted to a 1.7.10 node canvas: cursor-anchored zoom,
 * pan, selection, draggable nodes and application-supplied connections. This is a display/editor
 * surface, not a recipe solver or a server-side graph execution engine. Nodes use world coordinates;
 * their texture can draw arbitrary two-dimensional content without transformed UIElement hit testing.
 */
public final class GraphView extends UIElement {

    /** Canvas node with a stable identity, finite world bounds and a caller-provided renderer. */
    public static final class Node {

        private double x, y;
        public final int width, height;
        public final IGuiTexture texture;

        public Node(double x, double y, int width, int height, IGuiTexture texture) {
            if (width <= 0 || height <= 0) throw new IllegalArgumentException("Invalid node size");
            this.width = width;
            this.height = height;
            this.texture = Objects.requireNonNull(texture);
            setPosition(x, y);
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public void setPosition(double x, double y) {
            if (!Double.isFinite(x) || !Double.isFinite(y) || Math.abs(x) > 1e7 || Math.abs(y) > 1e7)
                throw new IllegalArgumentException("Invalid node position");
            this.x = x;
            this.y = y;
        }
    }

    /** Directed visual connection; removing either endpoint also removes the connection. */
    public static final class Connection {

        public final Node from, to;
        public final int color;

        private Connection(Node from, Node to, int color) {
            this.from = from;
            this.to = to;
            this.color = color;
        }
    }

    private final List<Node> nodes = new ArrayList<>();
    private final List<Connection> connections = new ArrayList<>();
    private double offsetX, offsetY, scale = 1;
    private int lastX, lastY;
    private Node selected, dragging;
    private Consumer<Node> onSelect = ignored -> {};
    private Consumer<Node> onMove = ignored -> {};

    public GraphView(int x, int y, int width, int height) {
        super(x, y, width, height);
        setFocusable(true);
        addEventListener(UIEvents.MOUSE_DOWN, e -> {
            lastX = e.x;
            lastY = e.y;
            dragging = null;
            if (e.button == 0) {
                Node hit = nodeAt(e.x, e.y);
                if (selected != hit) {
                    selected = hit;
                    onSelect.accept(hit);
                }
                dragging = hit;
            }
            e.preventDefault();
            e.stopPropagation();
        });
        addEventListener(UIEvents.MOUSE_MOVE, e -> {
            if (e.button < 0) return;
            double dx = (e.x - lastX) / scale, dy = (e.y - lastY) / scale;
            lastX = e.x;
            lastY = e.y;
            if (dragging != null && nodes.contains(dragging) && e.button == 0) {
                dragging.setPosition(clampCoordinate(dragging.x + dx), clampCoordinate(dragging.y + dy));
                if (dx != 0 || dy != 0) onMove.accept(dragging);
            } else {
                offsetX = clampCoordinate(offsetX - dx);
                offsetY = clampCoordinate(offsetY - dy);
            }
            e.preventDefault();
            e.stopPropagation();
        });
        addEventListener(UIEvents.MOUSE_UP, e -> dragging = null);
        addEventListener(UIEvents.MOUSE_WHEEL, e -> {
            zoomAt(e.x, e.y, scale * Math.pow(1.15, Integer.signum(e.deltaY)));
            e.preventDefault();
            e.stopPropagation();
        });
        addEventListener(UIEvents.KEY_DOWN, e -> {
            if (e.keyCode == Keyboard.KEY_HOME) fitToContent();
            else if (e.keyCode == Keyboard.KEY_LEFT) offsetX = clampCoordinate(offsetX - 20 / scale);
            else if (e.keyCode == Keyboard.KEY_RIGHT) offsetX = clampCoordinate(offsetX + 20 / scale);
            else if (e.keyCode == Keyboard.KEY_UP) offsetY = clampCoordinate(offsetY - 20 / scale);
            else if (e.keyCode == Keyboard.KEY_DOWN) offsetY = clampCoordinate(offsetY + 20 / scale);
            else return;
            e.preventDefault();
            e.stopPropagation();
        });
    }

    private static double clampCoordinate(double value) {
        return Math.max(-1e7, Math.min(1e7, value));
    }

    public List<Node> getNodes() {
        return Collections.unmodifiableList(nodes);
    }

    public List<Connection> getConnections() {
        return Collections.unmodifiableList(connections);
    }

    public Node getSelected() {
        return selected;
    }

    public double getScale() {
        return scale;
    }

    public double worldX(double screenX) {
        return offsetX + (screenX - getScreenX()) / scale;
    }

    public double worldY(double screenY) {
        return offsetY + (screenY - getScreenY()) / scale;
    }

    public GraphView addNode(Node node) {
        Objects.requireNonNull(node);
        if (nodes.contains(node)) throw new IllegalArgumentException("Duplicate node");
        nodes.add(node);
        return this;
    }

    public void removeNode(Node node) {
        nodes.remove(node);
        connections.removeIf(edge -> edge.from == node || edge.to == node);
        if (selected == node) selected = null;
        if (dragging == node) dragging = null;
    }

    public GraphView connect(Node from, Node to, int color) {
        if (!nodes.contains(from) || !nodes.contains(to)) throw new IllegalArgumentException("Unknown endpoint");
        connections.add(new Connection(from, to, color));
        return this;
    }

    public GraphView setOnSelect(Consumer<Node> callback) {
        onSelect = Objects.requireNonNull(callback);
        return this;
    }

    public GraphView setOnMove(Consumer<Node> callback) {
        onMove = Objects.requireNonNull(callback);
        return this;
    }

    /** Keeps the world point below the cursor fixed while applying the bounded camera scale. */
    public void zoomAt(double screenX, double screenY, double nextScale) {
        if (!Double.isFinite(nextScale)) throw new IllegalArgumentException("Non-finite scale");
        double wx = worldX(screenX), wy = worldY(screenY);
        scale = Math.max(.25, Math.min(4, nextScale));
        offsetX = clampCoordinate(wx - (screenX - getScreenX()) / scale);
        offsetY = clampCoordinate(wy - (screenY - getScreenY()) / scale);
    }

    public Node nodeAt(double screenX, double screenY) {
        double wx = worldX(screenX), wy = worldY(screenY);
        for (int i = nodes.size() - 1; i >= 0; i--) {
            Node node = nodes.get(i);
            if (wx >= node.x && wy >= node.y && wx < node.x + node.width && wy < node.y + node.height) return node;
        }
        return null;
    }

    public void fitToContent() {
        if (nodes.isEmpty()) {
            offsetX = 0;
            offsetY = 0;
            scale = 1;
            return;
        }
        double left = Double.POSITIVE_INFINITY, top = left, right = -left, bottom = -left;
        for (Node node : nodes) {
            left = Math.min(left, node.x);
            top = Math.min(top, node.y);
            right = Math.max(right, node.x + node.width);
            bottom = Math.max(bottom, node.y + node.height);
        }
        scale = Math.max(
            .25,
            Math.min(4, Math.min(Math.max(1, width - 20) / (right - left), Math.max(1, height - 20) / (bottom - top))));
        offsetX = (left + right - width / scale) / 2;
        offsetY = (top + bottom - height / scale) / 2;
    }

    @Override
    protected void drawForeground(int mx, int my, int px, int py) {
        int left = px + x, top = py + y;
        try (ScissorScope ignored = new ScissorScope(left, top, width, height)) {
            Gui.drawRect(left, top, left + width, top + height, 0xFF202830);
            double grid = 32 * scale;
            for (double gx = ((-offsetX * scale) % grid + grid) % grid; gx < width; gx += grid)
                Gui.drawRect(left + (int) gx, top, left + (int) gx + 1, top + height, 0xFF35424B);
            for (double gy = ((-offsetY * scale) % grid + grid) % grid; gy < height; gy += grid)
                Gui.drawRect(left, top + (int) gy, left + width, top + (int) gy + 1, 0xFF35424B);
            GL11.glPushMatrix();
            try {
                GL11.glTranslated(left - offsetX * scale, top - offsetY * scale, 0);
                GL11.glScaled(scale, scale, 1);
                drawConnections();
                for (Node node : nodes) {
                    if (node.x + node.width < offsetX || node.y + node.height < offsetY
                        || node.x > offsetX + width / scale
                        || node.y > offsetY + height / scale) continue;
                    int nx = (int) Math.round(node.x), ny = (int) Math.round(node.y);
                    if (node == selected)
                        Gui.drawRect(nx - 2, ny - 2, nx + node.width + 2, ny + node.height + 2, 0xFFA5D8FF);
                    node.texture.draw((int) worldX(mx), (int) worldY(my), nx, ny, node.width, node.height);
                }
            } finally {
                GL11.glPopMatrix();
            }
            new TextTexture(() -> Math.round(scale * 100) + "%", 0xFFFFFFFF)
                .draw(mx, my, left, top + height - 14, 42, 14);
        }
    }

    private void drawConnections() {
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT | GL11.GL_CURRENT_BIT | GL11.GL_LINE_BIT);
        try {
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glLineWidth(1);
            Tessellator tess = Tessellator.instance;
            tess.startDrawing(GL11.GL_LINES);
            for (Connection edge : connections) {
                tess.setColorOpaque_I(edge.color);
                double sx = edge.from.x + edge.from.width, sy = edge.from.y + edge.from.height / 2d;
                double tx = edge.to.x, ty = edge.to.y + edge.to.height / 2d;
                tess.addVertex(sx, sy, 0);
                tess.addVertex(tx, ty, 0);
                double angle = Math.atan2(ty - sy, tx - sx);
                tess.addVertex(tx, ty, 0);
                tess.addVertex(tx - Math.cos(angle - .5) * 6, ty - Math.sin(angle - .5) * 6, 0);
                tess.addVertex(tx, ty, 0);
                tess.addVertex(tx - Math.cos(angle + .5) * 6, ty - Math.sin(angle + .5) * 6, 0);
            }
            tess.draw();
        } finally {
            GL11.glPopAttrib();
        }
    }
}
