package com.xyp.gtnotgood.client.journeymap;

import java.awt.geom.Point2D;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.MathHelper;

import com.xyp.gtnotgood.GTNotGood;

/** Resolves optional JourneyMap members once, after its fullscreen class has loaded. */
final class JourneyMapBridge {

    private static JourneyMapBridge instance;
    private static boolean unavailable;

    private final Field grid;
    private final Field chat;
    private final Field dimension;
    private final Field coordX;
    private final Field coordZ;
    private final Field drawnWaypoint;
    private final Method state;
    private final Method mapType;
    private final Method blockUnderMouse;
    private final Method underground;
    private final Method chatHidden;
    private final Method overButton;
    private final Method drawnWaypoints;
    private final Method windowPosition;
    private final Method textureSize;
    private final Method waypointX;
    private final Method waypointY;
    private final Method waypointZ;
    private final Method waypointName;
    private final Method waypointDimensions;
    private final Method waypointEnabled;
    private final Method waypointAt;
    private final Object normalWaypoint;
    private final Method permitted;
    private final Method managerInstance;
    private final Method openEditor;
    private final Method closeAll;

    private JourneyMapBridge() throws ReflectiveOperationException {
        Class<?> fullscreen = type("journeymap.client.ui.fullscreen.Fullscreen");
        Class<?> jmUI = type("journeymap.client.ui.component.JmUI");
        Class<?> waypoint = type("journeymap.client.model.Waypoint");
        Class<?> waypointType = type("journeymap.client.model.Waypoint$Type");
        Class<?> manager = type("journeymap.client.ui.UIManager");
        Class<?> step = type("journeymap.client.render.draw.DrawWayPointStep");
        grid = field(fullscreen, "gridRenderer");
        chat = field(fullscreen, "chat");
        state = fullscreen.getMethod("state");
        mapType = grid.getType().getMethod("getMapType");
        blockUnderMouse = grid.getType().getMethod("getBlockUnderMouse", double.class, double.class, int.class,
            int.class);
        dimension = mapType.getReturnType().getField("dimension");
        underground = mapType.getReturnType().getMethod("isUnderground");
        coordX = blockUnderMouse.getReturnType().getField("x");
        coordZ = blockUnderMouse.getReturnType().getField("z");
        chatHidden = chat.getType().getMethod("isHidden");
        overButton = jmUI.getDeclaredMethod("isMouseOverButton", int.class, int.class);
        overButton.setAccessible(true);
        drawnWaypoints = state.getReturnType().getMethod("getDrawWaypointSteps");
        drawnWaypoint = step.getField("waypoint");
        windowPosition = step.getMethod("getLastWindowPosition");
        textureSize = step.getMethod("getTextureSize");
        waypointX = waypoint.getMethod("getX");
        waypointY = waypoint.getMethod("getY");
        waypointZ = waypoint.getMethod("getZ");
        waypointName = waypoint.getMethod("getName");
        waypointDimensions = waypoint.getMethod("getDimensions");
        waypointEnabled = waypoint.getMethod("isEnable");
        waypointAt = waypoint.getMethod("at", int.class, int.class, int.class, waypointType, int.class);
        normalWaypoint = waypointType.getField("Normal").get(null);
        permitted = type("journeymap.client.command.CmdTeleportWaypoint").getMethod("isPermitted", Minecraft.class);
        managerInstance = manager.getMethod("getInstance");
        openEditor = manager.getMethod("openWaypointEditor", waypoint, boolean.class, jmUI);
        closeAll = manager.getMethod("closeAll");
    }

    static JourneyMapBridge get() {
        if (unavailable) return null;
        if (instance == null) {
            try {
                instance = new JourneyMapBridge();
            } catch (ReflectiveOperationException | LinkageError | RuntimeException error) {
                disable(error);
            }
        }
        return instance;
    }

    static void disable(Throwable error) {
        if (!unavailable) GTNotGood.LOG.warn("JourneyMap context menu disabled: incompatible client API", error);
        unavailable = true;
        instance = null;
    }

    boolean accepts(GuiScreen screen, int x, int y) throws ReflectiveOperationException {
        Object mapChat = chat.get(screen);
        return (mapChat == null || (Boolean) chatHidden.invoke(mapChat)) && !(Boolean) overButton.invoke(screen, x, y);
    }

    JourneyMapTarget target(GuiScreen screen, int rawX, int rawY) throws ReflectiveOperationException {
        Minecraft mc = Minecraft.getMinecraft();
        Object renderer = grid.get(null);
        Object currentMap = mapType.invoke(renderer);
        if (currentMap == null) return null;
        int dim = dimension.getInt(currentMap);
        Object coordinate = blockUnderMouse.invoke(renderer, (double) rawX, (double) rawY, mc.displayWidth,
            mc.displayHeight);
        int x = coordX.getInt(coordinate);
        int z = coordZ.getInt(coordinate);
        int y = MathHelper.floor_double(mc.thePlayer.boundingBox.minY);
        Object hovered = dim == mc.thePlayer.dimension ? hoveredWaypoint(rawX, mc.displayHeight - rawY, dim) : null;
        String name = "";
        if (hovered != null) {
            x = (Integer) waypointX.invoke(hovered);
            z = (Integer) waypointZ.invoke(hovered);
            name = (String) waypointName.invoke(hovered);
            int waypointHeight = (Integer) waypointY.invoke(hovered);
            if (waypointHeight >= 0) y = waypointHeight;
            else hovered = null;
        }
        if (x < -29999984 || x >= 29999984 || z < -29999984 || z >= 29999984) return null;
        return new JourneyMapTarget(x, y, z, dim, (Boolean) underground.invoke(currentMap), hovered, name);
    }

    private Object hoveredWaypoint(int mouseX, int mouseY, int dim) throws ReflectiveOperationException {
        List<?> steps = (List<?>) drawnWaypoints.invoke(state.invoke(null));
        if (steps == null) return null;
        Object closest = null;
        double distance = Double.POSITIVE_INFINITY;
        for (Object step : steps) {
            Point2D point = (Point2D) windowPosition.invoke(step);
            if (point == null) continue;
            double radius = Math.max(4, ((Integer) textureSize.invoke(step)) / 2.0);
            if (Math.abs(mouseX - point.getX()) > radius || Math.abs(mouseY - point.getY()) > radius) continue;
            Object waypoint = drawnWaypoint.get(step);
            if (
                !(Boolean) waypointEnabled.invoke(waypoint)
                    || !((Collection<?>) waypointDimensions.invoke(waypoint)).contains(dim)
            ) continue;
            double candidateDistance = point.distanceSq(mouseX, mouseY);
            if (candidateDistance <= distance) {
                closest = waypoint;
                distance = candidateDistance;
            }
        }
        return closest;
    }

    boolean isPermitted() throws ReflectiveOperationException {
        return (Boolean) permitted.invoke(null, Minecraft.getMinecraft());
    }

    void newWaypoint(JourneyMapTarget target, int y, GuiScreen parent) throws ReflectiveOperationException {
        Object waypoint = waypointAt.invoke(null, target.x, y, target.z, normalWaypoint, target.dimension);
        openEditor.invoke(managerInstance.invoke(null), waypoint, true, parent);
    }

    void closeMap() throws ReflectiveOperationException {
        closeAll.invoke(managerInstance.invoke(null));
    }

    private static Class<?> type(String name) throws ClassNotFoundException {
        return Class.forName(name, false, JourneyMapBridge.class.getClassLoader());
    }

    private static Field field(Class<?> type, String name) throws NoSuchFieldException {
        Field result = type.getDeclaredField(name);
        result.setAccessible(true);
        return result;
    }
}
