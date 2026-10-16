package com.xyp.gtnotgood.utils.journeymap;

import java.io.IOException;

import net.minecraft.launchwrapper.Launch;
import net.minecraft.launchwrapper.LaunchClassLoader;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import com.xyp.gtnotgood.utils.enums.ModList;

public final class JourneyMapCompat {

    private static final Logger LOGGER = LogManager.getLogger(ModList.GTNotGood.getID());
    private static final String FULLSCREEN_CLASS = "journeymap.client.ui.fullscreen.Fullscreen";
    private static Boolean compatible;
    private static boolean warned;

    private JourneyMapCompat() {}

    /** Inspects optional classes without defining them before late mixins are registered. */
    public static synchronized boolean isCompatible() {
        if (compatible != null) return compatible;
        compatible = false;
        try {
            LaunchClassLoader loader = Launch.classLoader;
            if (loader == null) return false;
            if (loader.getClassBytes("local.jmquicktp.MapMenu") != null) {
                warnOnce("another JourneyMap quick-teleport menu is installed");
                return false;
            }
            ClassNode fullscreen = readClass(loader, FULLSCREEN_CLASS);
            requireMethod(fullscreen, "(III)V", false, "mouseClicked", "func_73864_a");
            requireMethod(fullscreen, "(CI)V", false, "keyTyped", "func_73869_a");
            requireMethod(fullscreen, "(IIF)V", false, "drawScreen", "func_73863_a");
            requireMethod(fullscreen, "()V", false, "onGuiClosed", "func_146281_b");
            requireMethod(fullscreen, "()Ljourneymap/client/model/MapState;", true, "state");
            requireField(fullscreen, "state", "Ljourneymap/client/model/MapState;", true);
            requireField(fullscreen, "gridRenderer", "Ljourneymap/client/render/map/GridRenderer;", true);
            requireField(fullscreen, "chat", "Ljourneymap/client/ui/fullscreen/MapChat;", false);
            ClassNode ui = readClass(loader, "journeymap.client.ui.component.JmUI");
            requireMethod(ui, "(II)Z", false, "isMouseOverButton");
            ClassNode chat = readClass(loader, "journeymap.client.ui.fullscreen.MapChat");
            requireMethod(chat, "()Z", false, "isHidden");
            ClassNode grid = readClass(loader, "journeymap.client.render.map.GridRenderer");
            requireMethod(grid, "(DDII)Ljourneymap/client/model/BlockCoordIntPair;", false, "getBlockUnderMouse");
            requireMethod(grid, "()Ljourneymap/client/model/MapType;", false, "getMapType");
            ClassNode coordinate = readClass(loader, "journeymap.client.model.BlockCoordIntPair");
            requireField(coordinate, "x", "I", false);
            requireField(coordinate, "z", "I", false);
            verifyMapApi(loader);
            verifyWaypointApi(loader);
            compatible = true;
        } catch (IOException | RuntimeException | LinkageError exception) {
            warnOnce(exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage());
        }
        return compatible;
    }

    private static void verifyMapApi(LaunchClassLoader loader) throws IOException {
        ClassNode mapType = readClass(loader, "journeymap.client.model.MapType");
        requireField(mapType, "dimension", "I", false);
        requireMethod(mapType, "()Z", false, "isUnderground");
        ClassNode state = readClass(loader, "journeymap.client.model.MapState");
        requireMethod(state, "()Ljava/util/List;", false, "getDrawWaypointSteps");
        ClassNode drawStep = readClass(loader, "journeymap.client.render.draw.DrawWayPointStep");
        requireField(drawStep, "waypoint", "Ljourneymap/client/model/Waypoint;", false);
        requireMethod(drawStep, "()Ljava/awt/geom/Point2D$Double;", false, "getLastWindowPosition");
        requireMethod(drawStep, "()I", false, "getTextureSize");
    }

    private static void verifyWaypointApi(LaunchClassLoader loader) throws IOException {
        ClassNode waypoint = readClass(loader, "journeymap.client.model.Waypoint");
        requireMethod(waypoint, "()I", false, "getX");
        requireMethod(waypoint, "()I", false, "getY");
        requireMethod(waypoint, "()I", false, "getZ");
        requireMethod(waypoint, "()Ljava/lang/String;", false, "getName");
        requireMethod(waypoint, "()Ljava/util/Collection;", false, "getDimensions");
        requireMethod(waypoint, "()Z", false, "isEnable");
        requireMethod(waypoint, "(IIILjourneymap/client/model/Waypoint$Type;I)Ljourneymap/client/model/Waypoint;", true,
            "at");
        ClassNode waypointType = readClass(loader, "journeymap.client.model.Waypoint$Type");
        requireField(waypointType, "Normal", "Ljourneymap/client/model/Waypoint$Type;", true);
        ClassNode manager = readClass(loader, "journeymap.client.ui.UIManager");
        requireMethod(manager, "()Ljourneymap/client/ui/UIManager;", true, "getInstance");
        requireMethod(manager, "(Ljourneymap/client/model/Waypoint;ZLjourneymap/client/ui/component/JmUI;)V", false,
            "openWaypointEditor");
        requireMethod(manager, "()V", false, "closeAll");
        ClassNode teleport = readClass(loader, "journeymap.client.command.CmdTeleportWaypoint");
        requireMethod(teleport, "(Lnet/minecraft/client/Minecraft;)Z", true, "isPermitted");
    }

    private static ClassNode readClass(LaunchClassLoader loader, String name) throws IOException {
        byte[] bytes = loader.getClassBytes(name);
        if (bytes == null) throw new IllegalStateException("missing class " + name);
        ClassNode type = new ClassNode();
        new ClassReader(bytes).accept(type, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        return type;
    }

    private static void requireMethod(ClassNode type, String descriptor, boolean staticMember, String... names) {
        for (MethodNode method : type.methods) {
            if (!descriptor.equals(method.desc) || ((method.access & Opcodes.ACC_STATIC) != 0) != staticMember)
                continue;
            for (String name : names) {
                if (name.equals(method.name)) return;
            }
        }
        throw new IllegalStateException("missing method " + type.name + "." + names[0] + descriptor);
    }

    private static void requireField(ClassNode type, String name, String descriptor, boolean staticMember) {
        for (FieldNode field : type.fields) {
            if (
                name.equals(field.name) && descriptor.equals(field.desc)
                    && ((field.access & Opcodes.ACC_STATIC) != 0) == staticMember
            ) return;
        }
        throw new IllegalStateException("missing field " + type.name + "." + name);
    }

    private static void warnOnce(String reason) {
        if (warned) return;
        warned = true;
        LOGGER.warn("Skipping {} right-click menu integration: {}.", ModList.JourneyMap.getDisplayName(), reason);
    }
}
