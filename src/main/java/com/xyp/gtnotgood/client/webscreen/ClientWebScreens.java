package com.xyp.gtnotgood.client.webscreen;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.WorldEvent;

import com.xyp.gtnotgood.common.blocks.webscreen.TileWebScreen;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenAddress;
import com.xyp.gtnotgood.common.blocks.webscreen.WebScreenShape;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class ClientWebScreens {

    private static final ClientWebScreens INSTANCE = new ClientWebScreens();
    private static final Map<UUID, Display> displays = new LinkedHashMap<>();
    private static WebScreenProfiles profiles;
    private static File profilesDirectory;
    private int topologyTicks;

    private ClientWebScreens() {}

    public static void register() {
        ClientRegistry.bindTileEntitySpecialRenderer(TileWebScreen.class, new WebScreenRenderer());
        FMLCommonHandler.instance().bus().register(INSTANCE);
        MinecraftForge.EVENT_BUS.register(INSTANCE);
    }

    public static void open(TileWebScreen tile) {
        Minecraft mc = Minecraft.getMinecraft();
        if (tile.getScreenId() == null) return;
        WebScreenShape shape = discover(tile);
        Display display = null;
        for (Display candidate : displays.values()) {
            if (
                candidate.world == tile.getWorldObj()
                    && shape.contains(candidate.tile.xCoord, candidate.tile.yCoord, candidate.tile.zCoord)
                    && candidate.tile.getFacing() == shape.facing
            ) {
                display = candidate;
                break;
            }
        }
        if (display != null) display.reshape(shape);
        else if (Config.enableWebScreens) {
            while (displays.size() >= Config.webScreenMaxSessions) removeOldest();
            File scope = profileDirectory(mc);
            UUID profileId = selectProfile(tile, shape, scope);
            File profile = new File(scope, profileId.toString());
            ChromiumSession session = new ChromiumSession(profile, Config.webScreenBrowserExecutable,
                Boolean.getBoolean("gtng.webscreen.qa"), WebScreenSettings.load(profile));
            display = new Display(tile, shape, profileId, session, freeTextureSlot());
            displays.put(profileId, display);
            session.start(WebScreenAddress.DEFAULT_HOME);
        }
        mc.displayGuiScreen(new GuiWebScreen(tile));
    }

    private static UUID selectProfile(TileWebScreen tile, WebScreenShape shape, File scope) {
        if (!scope.equals(profilesDirectory)) {
            if (profiles != null) profiles.close();
            profilesDirectory = scope;
            profiles = new WebScreenProfiles(scope);
        }
        List<UUID> members = memberIds(tile.getWorldObj(), shape);
        TileWebScreen anchor = screenAt(tile.getWorldObj(), shape.anchorX, shape.anchorY, shape.anchorZ, shape.facing);
        UUID profileId = anchor == null || anchor.getScreenId() == null ? tile.getScreenId() : anchor.getScreenId();
        for (UUID member : members) {
            UUID saved = profiles.find(member);
            if (saved != null) {
                profileId = saved;
                break;
            }
        }
        if (displays.containsKey(profileId)) profileId = tile.getScreenId();
        if (displays.containsKey(profileId)) profileId = UUID.randomUUID();
        profiles.bind(members, profileId);
        return profileId;
    }

    public static WebScreenSettings getSettings(TileWebScreen tile) {
        Display display = find(tile);
        if (display != null) return display.session.getSettings().copy();
        if (tile.getScreenId() == null) return new WebScreenSettings();
        File scope = profileDirectory(Minecraft.getMinecraft());
        UUID profileId = selectProfile(tile, discover(tile), scope);
        return WebScreenSettings.load(new File(scope, profileId.toString()));
    }

    public static void applySettings(TileWebScreen tile, WebScreenSettings draft) {
        if (tile.getScreenId() == null || draft == null) return;
        WebScreenSettings settings = draft.copy();
        Minecraft mc = Minecraft.getMinecraft();
        File scope = profileDirectory(mc);
        Display display = find(tile);
        if (display == null && Config.enableWebScreens) {
            open(tile);
            display = find(tile);
        }
        if (display == null) {
            UUID profileId = selectProfile(tile, discover(tile), scope);
            ChromiumSession persistence = new ChromiumSession(new File(scope, profileId.toString()),
                Config.webScreenBrowserExecutable, Boolean.getBoolean("gtng.webscreen.qa"), settings);
            persistence.persistSettings();
            return;
        }
        WebScreenSettings current = display.session.getSettings();
        settings.setZoomPercent(display.session.getZoomPercent());
        if (
            current.getResolution() == settings.getResolution() && current.getFrameRate() == settings.getFrameRate()
                && current.getJpegQuality() == settings.getJpegQuality()
                && current.isMuted() == settings.isMuted()
        ) return;
        String url = display.session.getUrl();
        display.close();
        ChromiumSession session = new ChromiumSession(new File(scope, display.profileId.toString()),
            Config.webScreenBrowserExecutable, Boolean.getBoolean("gtng.webscreen.qa"), settings);
        Display replacement = new Display(tile, discover(tile), display.profileId, session, display.slot);
        replacement.pendingBindings = true;
        displays.put(display.profileId, replacement);
        session.start(url);
    }

    private static File profileDirectory(Minecraft mc) {
        String world = mc.isSingleplayer() && mc.getIntegratedServer() != null
            ? "local:" + mc.getIntegratedServer().getFolderName()
            : "server:" + (mc.func_147104_D() == null ? "unknown" : mc.func_147104_D().serverIP);
        UUID scope = UUID.nameUUIDFromBytes(world.getBytes(StandardCharsets.UTF_8));
        return new File(mc.mcDataDir, ModList.GTNotGood.getID() + "/webscreens/" + scope);
    }

    public static void stop(TileWebScreen tile) {
        Display display = find(tile);
        if (display == null) return;
        displays.remove(display.profileId);
        display.close();
    }

    public static ChromiumSession getSession(TileWebScreen tile) {
        Display display = find(tile);
        return display == null ? null : display.session;
    }

    public static ResourceLocation getTexture(TileWebScreen tile) {
        Display display = find(tile);
        return display == null || !display.hasFrame ? null : display.location;
    }

    public static WebScreenShape getShape(TileWebScreen tile) {
        Display display = find(tile);
        return display == null ? discover(tile) : display.shape;
    }

    public static boolean isRenderAnchor(TileWebScreen tile) {
        Display display = find(tile);
        return display != null && display.shape.anchorX == tile.xCoord
            && display.shape.anchorY == tile.yCoord
            && display.shape.anchorZ == tile.zCoord;
    }

    public static AxisAlignedBB getRenderBounds(TileWebScreen tile) {
        Display display = find(tile);
        return display == null
            ? AxisAlignedBB.getBoundingBox(tile.xCoord, tile.yCoord, tile.zCoord, tile.xCoord + 1, tile.yCoord + 1,
                tile.zCoord + 1)
            : bounds(display.shape);
    }

    private static Display find(TileWebScreen tile) {
        for (Display display : displays.values()) {
            if (
                display.world == tile.getWorldObj() && display.shape.facing == tile.getFacing()
                    && display.shape.contains(tile.xCoord, tile.yCoord, tile.zCoord)
            ) return display;
        }
        return null;
    }

    private static WebScreenShape discover(TileWebScreen tile) {
        World world = tile.getWorldObj();
        int facing = tile.getFacing();
        return WebScreenShape.discover(tile.xCoord, tile.yCoord, tile.zCoord, facing,
            (x, y, z) -> screenAt(world, x, y, z, facing) != null);
    }

    private static TileWebScreen screenAt(World world, int x, int y, int z, int facing) {
        if (world == null || y < 0 || y > 255 || !world.blockExists(x, y, z)) return null;
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileWebScreen screen && !screen.isInvalid() && screen.getFacing() == facing ? screen
            : null;
    }

    private static List<UUID> memberIds(World world, WebScreenShape shape) {
        List<UUID> ids = new ArrayList<>();
        for (int horizontal = 0; horizontal < shape.width; horizontal++) {
            for (int vertical = 0; vertical < shape.height; vertical++) {
                TileWebScreen member = screenAt(world, shape.anchorX + (shape.facing <= 3 ? horizontal : 0),
                    shape.anchorY + vertical, shape.anchorZ + (shape.facing >= 4 ? horizontal : 0), shape.facing);
                if (member != null && member.getScreenId() != null) ids.add(member.getScreenId());
            }
        }
        return ids;
    }

    private static AxisAlignedBB bounds(WebScreenShape shape) {
        return AxisAlignedBB.getBoundingBox(shape.anchorX, shape.anchorY, shape.anchorZ,
            shape.anchorX + (shape.facing <= 3 ? shape.width : 1), shape.anchorY + shape.height,
            shape.anchorZ + (shape.facing >= 4 ? shape.width : 1));
    }

    private static int freeTextureSlot() {
        for (int slot = 0; slot < 4; slot++) {
            int candidate = slot;
            if (displays.values().stream().noneMatch(display -> display.slot == candidate)) return slot;
        }
        throw new IllegalStateException("No free web screen texture slot");
    }

    private static void removeOldest() {
        Iterator<Display> iterator = displays.values().iterator();
        Display display = iterator.next();
        iterator.remove();
        display.close();
    }

    @SubscribeEvent
    public void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || displays.isEmpty()) return;
        Minecraft mc = Minecraft.getMinecraft();
        while (displays.size() > Config.webScreenMaxSessions) removeOldest();
        boolean refresh = ++topologyTicks % 10 == 0;
        List<Display> kept = new ArrayList<>();
        Iterator<Display> iterator = displays.values().iterator();
        while (iterator.hasNext()) {
            Display display = iterator.next();
            if (
                !Config.enableWebScreens || mc.theWorld != display.world
                    || mc.thePlayer == null
                    || (refresh && !display.refresh())
                    || display.distanceSquared(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
                        > Config.webScreenRange * Config.webScreenRange
                    || kept.stream()
                        .anyMatch(other -> other.shape.facing == display.shape.facing
                            && other.shape.contains(display.tile.xCoord, display.tile.yCoord, display.tile.zCoord))
            ) {
                iterator.remove();
                display.close();
                continue;
            }
            kept.add(display);
            if (display.pendingBindings && profiles != null) {
                profiles.bind(memberIds(display.world, display.shape), display.profileId);
                display.pendingBindings = false;
            }
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START || displays.isEmpty()) return;
        for (Display display : displays.values()) {
            BufferedImage image = display.session.pollFrame();
            if (image != null && display.texture.uploadFrame(image)) display.hasFrame = true;
        }
    }

    @SubscribeEvent
    public void unload(WorldEvent.Unload event) {
        if (!event.world.isRemote) return;
        Iterator<Display> iterator = displays.values().iterator();
        while (iterator.hasNext()) {
            Display display = iterator.next();
            if (display.world != event.world) continue;
            iterator.remove();
            display.close();
        }
    }

    private static final class Display {

        private TileWebScreen tile;
        private WebScreenShape shape;
        private final World world;
        private final UUID profileId;
        private final ChromiumSession session;
        private final int slot;
        private final WebScreenTexture texture;
        private final ResourceLocation location;
        private boolean hasFrame;
        private boolean pendingBindings;

        private Display(TileWebScreen tile, WebScreenShape shape, UUID profileId, ChromiumSession session, int slot) {
            this.tile = tile;
            this.shape = shape;
            this.world = tile.getWorldObj();
            this.profileId = profileId;
            this.session = session;
            this.slot = slot;
            texture = new WebScreenTexture(session.getWidth(), session.getHeight());
            // TextureManager retains deleted entries in 1.7.10; reuse a bounded set of names.
            location = ModList.GTNotGood.getResourceLocation("dynamic/web_screen/" + slot);
            Minecraft.getMinecraft().getTextureManager().loadTexture(location, texture);
        }

        private void reshape(WebScreenShape replacement) {
            boolean changed = replacement.anchorX != shape.anchorX || replacement.anchorY != shape.anchorY
                || replacement.anchorZ != shape.anchorZ
                || replacement.width != shape.width
                || replacement.height != shape.height
                || replacement.facing != shape.facing;
            shape = replacement;
            pendingBindings |= changed;
        }

        private boolean refresh() {
            TileWebScreen current = screenAt(world, tile.xCoord, tile.yCoord, tile.zCoord, shape.facing);
            if (current == null) {
                for (int horizontal = 0; horizontal < shape.width && current == null; horizontal++) {
                    for (int vertical = 0; vertical < shape.height && current == null; vertical++) {
                        current = screenAt(world, shape.anchorX + (shape.facing <= 3 ? horizontal : 0),
                            shape.anchorY + vertical, shape.anchorZ + (shape.facing >= 4 ? horizontal : 0),
                            shape.facing);
                    }
                }
            }
            if (current == null) return false;
            tile = current;
            reshape(discover(current));
            return true;
        }

        private double distanceSquared(double x, double y, double z) {
            AxisAlignedBB box = bounds(shape);
            double dx = Math.max(box.minX - x, Math.max(0, x - box.maxX));
            double dy = Math.max(box.minY - y, Math.max(0, y - box.maxY));
            double dz = Math.max(box.minZ - z, Math.max(0, z - box.maxZ));
            return dx * dx + dy * dy + dz * dz;
        }

        private void close() {
            session.close();
            texture.deleteGlTexture();
        }
    }
}
