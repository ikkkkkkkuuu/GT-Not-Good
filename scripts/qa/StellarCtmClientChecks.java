package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.blocks.stellar.BlockStellarPanel;
import com.xyp.gtnotgood.common.blocks.stellar.StellarBlockVariant;
import com.xyp.gtnotgood.common.blocks.stellar.StellarBlocks;
import com.xyp.gtnotgood.common.blocks.stellar.StellarPanelVariant;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.ITexture;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.render.SBRContextHolder;
import gregtech.common.render.IIconTexture;

@Mod(
    modid = "stellarctmqa",
    name = "Stellar CTM QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StellarCtmClientChecks {

    private static final int[][] RIGHT = { { 1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };
    private static final int[][] UP = { { 0, 0, -1 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 },
        { 0, 1, 0 } };
    private static final int[][] OFFSETS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 },
        { 1, 1 } };
    private static final int[] MASKS = { 0, 1, 2, 4, 8, 15, 255, 239 };
    private final List<ItemStack> variants = new ArrayList<>();
    private final List<Capture> captures = new ArrayList<>();
    private final List<String> report = new ArrayList<>();
    private Method getMask;
    private Method getIcon;
    private BaseMetaTileEntity vaultBase;
    private SingularityDataHub vault;
    private IIcon probeIcon;
    private boolean started;
    private volatile boolean ready;
    private volatile boolean finished;
    private volatile Throwable failure;
    private volatile int requested = -1;
    private volatile int completed = -1;
    private int positioned = -1;
    private int ticks;
    private int serverTicks;
    private int frames;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.stellarCtm.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            if (failure != null) throw new AssertionError("Stellar CTM server checks failed", failure);
            if (++ticks > 6000) throw new AssertionError("Stellar CTM client timed out");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                require(
                    Loader.instance().getModList().stream()
                        .noneMatch(mod -> mod.getModId().toLowerCase().contains("myctm")),
                    "the actual client has no MyCTMLib dependency");
                require(
                    Loader.isModLoaded(ModList.Angelica.getID())
                        != Boolean.getBoolean("gtng.stellarCtm.qa.withoutAngelica"),
                    "Angelica presence matches the selected isolated client profile");
                getMask = Class.forName("com.xyp.gtnotgood.common.blocks.stellar.StellarConnections")
                    .getMethod("getMask", IBlockAccess.class, int.class, int.class, int.class, int.class);
                getIcon = Class.forName("com.xyp.gtnotgood.client.stellar.StellarConnectedIcons").getMethod("getIcon",
                    String.class, int.class);
                checkAssetsAndAtlas();
                mc.launchIntegratedServer("stellar-ctm-qa-" + System.currentTimeMillis(), "Stellar CTM QA",
                    new WorldSettings(94L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (ready && mc.theWorld != null && mc.thePlayer != null) {
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void checkAssetsAndAtlas() throws Exception {
        Set<String> textures = new HashSet<>();
        Set<String> originals = new HashSet<>();
        for (StellarBlockVariant variant : StellarBlockVariant.values()) {
            textures.add(variant.getSideTexture());
            textures.add(variant.getEndTexture());
            originals.add(variant.getSideTexture());
            originals.add(variant.getEndTexture());
            variants.add(variant.getItemContainer().get(1));
        }
        require(textures.size() == 56 && variants.size() == 54,
            "54 ordinary variants retain all 56 side and end texture keys");
        for (StellarPanelVariant variant : StellarPanelVariant.values()) {
            variants.add(variant.getItemContainer().get(1));
            textures
                .add((String) variant.getBase().getClass().getMethod("getConnectedTexture").invoke(variant.getBase()));
            originals.add("iconsets/" + variant.getOverlayTexture());
            originals.add("iconsets/" + variant.getOverlayTexture() + "_GLOW");
        }
        require(variants.size() == 82, "all 82 ordinary and panel appearance variants participate in the gallery");
        require(textures.size() == 61, "all 61 ordinary and native-base texture sets participate in the atlas checks");
        for (String texture : textures) {
            Set<IIcon> tiles = new HashSet<>();
            for (int mask = 0; mask < 256; mask++) {
                if (normalize(mask) != mask) continue;
                IIcon icon = icon(texture, mask);
                require(
                    icon != null && !icon.getIconName().contains("missing")
                        && Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
                    texture + " has a stitched native CTM tile for mask " + mask);
                tiles.add(icon);
            }
            require(tiles.size() == 47, texture + " exposes all 47 distinct blob tiles");
        }
        File source = new File(System.getProperty("gtng.stellarCtm.qa.reference"));
        File destination = new File(System.getProperty("gtng.stellarCtm.qa.assets"));
        for (String original : originals) {
            File input = new File(source, original + ".png");
            require(input.isFile(), "the pinned reference contains " + original);
            for (String suffix : new String[] { ".png", ".png.mcmeta" }) {
                input = new File(source, original + suffix);
                if (!input.isFile()) continue;
                File output = new File(destination, original + suffix);
                require(output.isFile() && Arrays.equals(hash(input), hash(output)),
                    original + suffix + " retains its exact upstream SHA-256");
            }
        }
        checkPanelLayers();
    }

    private void checkPanelLayers() throws Exception {
        for (StellarPanelVariant variant : StellarPanelVariant.values()) {
            BlockStellarPanel block = StellarBlocks.getPanel(variant.getGroup());
            ITexture[][] layers = block.getTextures(variant.getMetadata());
            require(layers[2].length == 3 && layers[4].length == 1,
                variant + " retains front-only overlay and glow over its complete casing");
            IIcon overlay = ((IIconTexture) layers[2][1]).getIcon(2, null);
            IIcon glow = ((IIconTexture) layers[2][2]).getIcon(2, null);
            require(
                overlay.getIconName().endsWith("/" + variant.getOverlayTexture())
                    && glow.getIconName().endsWith("/" + variant.getOverlayTexture() + "_GLOW"),
                variant + " retains the original overlay and glow assets");
            Field glowing = layers[2][2].getClass().getDeclaredField("glow");
            glowing.setAccessible(true);
            require(glowing.getBoolean(layers[2][2]), variant + " retains full-bright glow rendering");
            if (variant.getOverlayTexture().endsWith("_ACTIVE")) {
                require(
                    ((TextureAtlasSprite) overlay).hasAnimationMetadata()
                        && ((TextureAtlasSprite) glow).hasAnimationMetadata(),
                    variant + " retains both upstream animations");
            }
            require(block.getInventoryTextures(variant.getMetadata())[4] == layers[2],
                variant + " retains the native inventory front orientation");
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || failure != null) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (++serverTicks == 1) setup(player);
            if (!ready) {
                if (!vault.mMachine) {
                    if (serverTicks > 220)
                        throw new AssertionError("The ordinary vault failed to form after general CTM integration");
                    return;
                }
                require(vault.mCountCasing == 10 && vault.portHatch != null,
                    "the existing vault still forms with ten Eddington walls and its port");
                ready = true;
                requested = 0;
            }
            if (completed == captures.size() - 1) {
                finish("PASS; " + checks + " Stellar CTM checks; 82 gallery variants");
                return;
            }
            if (requested == completed) requested++;
            if (positioned == requested) return;
            Capture capture = captures.get(requested);
            ItemStack first = variants.get(0);
            Block probe = Block.getBlockFromItem(first.getItem());
            if (capture.name.equals("chunk-removed")) player.worldObj.setBlockToAir(16, 77, 0);
            if (capture.name.equals("chunk-restored"))
                player.worldObj.setBlock(16, 77, 0, probe, first.getItemDamage(), 3);
            player.playerNetServerHandler.setPlayerLocation(capture.x, capture.y, capture.z, capture.yaw,
                capture.pitch);
            positioned = requested;
        } catch (Throwable error) {
            failure = error;
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world.setWorldTime(6000);
        List<String> gallery = new ArrayList<>();
        gallery.add("Index\tName\tX\tY\tZ");
        for (int index = 0; index < variants.size(); index++) {
            int page = index / 18, local = index % 18;
            int x = 15 + page * 24 + local % 6 * 3, y = 70 + (2 - local / 6) * 3;
            ItemStack stack = variants.get(index);
            Block block = Block.getBlockFromItem(stack.getItem());
            for (int dx = 0; dx < 2; dx++) {
                for (int dy = 0; dy < 2; dy++) world.setBlock(x + dx, y + dy, 0, block, stack.getItemDamage(), 3);
            }
            gallery.add((index + 1) + "\t" + stack.getDisplayName() + "\t" + x + "\t" + y + "\t0");
        }
        Files.write(new File(output(), "gallery.tsv").toPath(), gallery, StandardCharsets.UTF_8);
        for (int page = 0; page < 5; page++) {
            captures.add(new Capture("gallery-" + (page + 1), 23.5 + page * 24, 72.4, -15, 0, 0));
            if (page == 0) {
                captures.add(new Capture("chunk-removed", 23.5, 72.4, -15, 0, 0));
                captures.add(new Capture("chunk-restored", 23.5, 72.4, -15, 0, 0));
            }
        }
        Block glass = StellarBlocks.getBlock(StellarBlockVariant.Group.Glass);
        for (int x = 0; x < 6; x++) {
            for (int y = 70; y <= 72; y++) {
                world.setBlock(x, y, 8, glass, x < 3 ? 0 : 1, 3);
                world.setBlock(x, y, 9, Blocks.wool, (x + y) % 2 == 0 ? 4 : 11, 3);
            }
        }
        require(!glass.isOpaqueCube() && glass.getLightOpacity(world, 0, 70, 8) == 0 && glass.getRenderBlockPass() == 1,
            "glass remains translucent and transmits skylight");
        require(!glass.shouldSideBeRendered(world, 1, 70, 8, 5), "same-metadata glass hides its internal face");
        require(glass.shouldSideBeRendered(world, 3, 70, 8, 5),
            "different glass metadata retain the visible material boundary");
        captures.add(new Capture("glass-boundary", 2.5, 70.3, 1, 0, 0));
        Block sample = Block.getBlockFromItem(variants.get(0).getItem());
        for (int x = 0; x <= 2; x++) {
            for (int y = 75; y <= 77; y++) {
                for (int z = 20; z <= 22; z++) world.setBlock(x, y, z, sample, variants.get(0).getItemDamage(), 3);
            }
        }
        world.setBlockToAir(1, 77, 21);
        captures.add(new Capture("six-faces-top-north-east", 8, 81, 14, 42, 28));
        captures.add(new Capture("six-faces-bottom-south-west", -5, 70, 28, -135, -35));
        vaultBase = place(world, 20, 70, 20, GTNGMachineID.SingularityDataHub.id);
        vaultBase.disableWorking();
        vault = (SingularityDataHub) vaultBase.getMetaTileEntity();
        vault.construct(new ItemStack(Items.stick), false);
        place(world, 19, 70, 20, GTNGMachineID.VaultPortHatch.id);
        captures.add(new Capture("ordinary-vault", 16.2, 71.7, 14.5, -35, 22));
        checkNeighborhoods();
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private void checkNeighborhoods() throws Exception {
        for (StellarBlockVariant variant : StellarBlockVariant.values()) {
            Block block = StellarBlocks.getBlock(variant.getGroup());
            for (int side = 0; side < 6; side++) {
                for (int pattern : MASKS) {
                    Grid grid = new Grid();
                    grid.put(0, 0, 0, block, variant.getMetadata());
                    for (int neighbor = 0; neighbor < 8; neighbor++) {
                        if ((pattern & (1 << neighbor)) == 0) continue;
                        int[] offset = offset(side, OFFSETS[neighbor][0], OFFSETS[neighbor][1]);
                        grid.put(offset[0], offset[1], offset[2], block, variant.getMetadata());
                    }
                    require(mask(grid.access, side) == normalize(pattern),
                        variant + " face " + side + " resolves mask " + pattern);
                }
                Grid different = new Grid();
                different.put(0, 0, 0, block, variant.getMetadata());
                int[] left = offset(side, -1, 0);
                int otherMetadata = (variant.getMetadata() + 1) & 15;
                different.put(left[0], left[1], left[2], block, otherMetadata);
                StellarBlockVariant other = Arrays.stream(StellarBlockVariant.values()).filter(
                    candidate -> candidate.getGroup() == variant.getGroup() && candidate.getMetadata() == otherMetadata)
                    .findFirst().orElse(null);
                String texture = side < 2 ? variant.getEndTexture() : variant.getSideTexture();
                boolean sameFace = other != null
                    && texture.equals(side < 2 ? other.getEndTexture() : other.getSideTexture());
                require(mask(different.access, side) == (sameFace ? 1 : 0),
                    variant + " face " + side + " connects only an identical face texture across metadata");
                Grid step = new Grid();
                step.put(0, 0, 0, block, variant.getMetadata());
                step.put(left[0], left[1], left[2], block, variant.getMetadata());
                ForgeDirection normal = ForgeDirection.getOrientation(side);
                step.put(left[0] + normal.offsetX, left[1] + normal.offsetY, left[2] + normal.offsetZ, Blocks.stone, 0);
                require(mask(step.access, side) == 0,
                    variant + " face " + side + " does not connect a neighbor hidden behind a normal step");
                step.put(normal.offsetX, normal.offsetY, normal.offsetZ, Blocks.stone, 0);
                require(mask(step.access, side) == 0,
                    variant + " face " + side + " cannot participate when its own face is covered");
            }
        }
        for (StellarPanelVariant variant : StellarPanelVariant.values()) {
            StellarPanelVariant other = Arrays.stream(StellarPanelVariant.values())
                .filter(candidate -> candidate != variant && candidate.getBase() == variant.getBase()).findFirst()
                .get();
            for (int side = 0; side < 6; side++) {
                Grid grid = new Grid();
                grid.put(0, 0, 0, StellarBlocks.getPanel(variant.getGroup()), variant.getMetadata());
                int[] left = offset(side, -1, 0);
                grid.put(left[0], left[1], left[2], StellarBlocks.getPanel(other.getGroup()), other.getMetadata());
                require(mask(grid.access, side) == 1,
                    variant + " face " + side + " connects another overlay on the same base");
                grid.put(left[0], left[1], left[2], variant.getBase().getBlock(), variant.getBase().getMetadata());
                boolean extraction = variant.getBase() == StellarPanelVariant.Base.Extraction;
                require(mask(grid.access, side) == (extraction ? 1 : 0),
                    variant + " face " + side + " limits native base participation to the imported extraction casing");
            }
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !ready || requested < 0 || requested == completed) return;
        try {
            if (failure != null) throw new AssertionError("Stellar CTM server checks failed", failure);
            Minecraft mc = Minecraft.getMinecraft();
            Capture capture = captures.get(requested);
            if (
                mc.thePlayer == null
                    || !mc.theWorld.getChunkProvider().chunkExists((int) capture.x >> 4, (int) capture.z >> 4)
            ) return;
            if (++frames < 120) return;
            Block sample = Block.getBlockFromItem(variants.get(0).getItem());
            if (capture.name.startsWith("gallery-")) {
                int page = Integer.parseInt(capture.name.substring("gallery-".length())) - 1;
                if (!checkWorldSelection(page)) return;
                if (page == 0) probeIcon = sample.getIcon(mc.theWorld, 15, 77, 0, 2);
            } else if (capture.name.equals("chunk-removed")) {
                if (mc.theWorld.getBlock(16, 77, 0) != Blocks.air) return;
                require(sample.getIcon(mc.theWorld, 15, 77, 0, 2) != probeIcon,
                    "removal refreshes the adjacent CTM tile across the chunk boundary");
            } else if (capture.name.equals("chunk-restored")) {
                if (mc.theWorld.getBlock(16, 77, 0) != sample) return;
                require(sample.getIcon(mc.theWorld, 15, 77, 0, 2) == probeIcon,
                    "replacement restores the adjacent CTM tile across the chunk boundary");
            }
            ScreenShotHelper.saveScreenshot(output(), capture.name + ".png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            completed = requested;
            frames = 0;
        } catch (Throwable error) {
            fail(error);
        }
    }

    private boolean checkWorldSelection(int page) throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        for (int index = page * 18; index < Math.min(variants.size(), (page + 1) * 18); index++) {
            ItemStack stack = variants.get(index);
            Block block = Block.getBlockFromItem(stack.getItem());
            int local = index % 18;
            int x = 15 + page * 24 + local % 6 * 3, y = 70 + (2 - local / 6) * 3;
            if (
                mc.theWorld.getBlock(x, y, 0) != block || mc.theWorld.getBlockMetadata(x, y, 0) != stack.getItemDamage()
            ) return false;
            for (int side = 0; side < 6; side++) {
                int mask = (Integer) getMask.invoke(null, mc.theWorld, x, y, 0, side);
                if (index < 54) {
                    StellarBlockVariant variant = StellarBlockVariant.values()[index];
                    String texture = side < 2 ? variant.getEndTexture() : variant.getSideTexture();
                    require(block.getIcon(mc.theWorld, x, y, 0, side) == icon(texture, mask),
                        variant + " selects the correct world side or end CTM icon on face " + side);
                    require(block.getIcon(side, stack.getItemDamage()).getIconName().endsWith("stellar/" + texture),
                        variant + " retains its original inventory face " + side);
                } else {
                    StellarPanelVariant variant = StellarPanelVariant.values()[index - 54];
                    String texture = (String) variant.getBase().getClass().getMethod("getConnectedTexture")
                        .invoke(variant.getBase());
                    var context = new SBRContextHolder().getSBRWorldContext(x, y, 0, block, block.getRenderType(),
                        new RenderBlocks(mc.theWorld));
                    ITexture base = ((BlockStellarPanel) block).getTextures(stack.getItemDamage())[side][0];
                    require(((IIconTexture) base).getIcon(side, context) == icon(texture, mask),
                        variant + " uses the correct CTM base through the native GT world context on face " + side);
                }
            }
        }
        return true;
    }

    private int mask(IBlockAccess access, int side) throws Exception {
        return (Integer) getMask.invoke(null, access, 0, 0, 0, side);
    }

    private IIcon icon(String texture, int mask) throws Exception {
        return (IIcon) getIcon.invoke(null, texture, mask);
    }

    private static int normalize(int mask) {
        int result = mask & 15;
        int[] gates = { 5, 6, 9, 10 };
        for (int corner = 0; corner < 4; corner++) {
            if ((mask & gates[corner]) == gates[corner]) result |= mask & (16 << corner);
        }
        return result;
    }

    private static int[] offset(int side, int right, int up) {
        return new int[] { RIGHT[side][0] * right + UP[side][0] * up, RIGHT[side][1] * right + UP[side][1] * up,
            RIGHT[side][2] * right + UP[side][2] * up };
    }

    private static byte[] hash(File file) throws Exception {
        return MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file.toPath()));
    }

    private final class Grid {

        private final Map<String, Block> blocks = new HashMap<>();
        private final Map<String, Integer> metadata = new HashMap<>();
        private final IBlockAccess access = (IBlockAccess) Proxy.newProxyInstance(IBlockAccess.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class }, (proxy, method, arguments) -> {
                if (arguments != null && arguments.length >= 3 && arguments[0] instanceof Integer) {
                    int x = (Integer) arguments[0], y = (Integer) arguments[1], z = (Integer) arguments[2];
                    if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1)
                        throw new AssertionError("Unbounded CTM lookup");
                    String key = x + "," + y + "," + z;
                    return switch (method.getName()) {
                        case "getBlock" -> blocks.getOrDefault(key, Blocks.air);
                        case "getBlockMetadata" -> metadata.getOrDefault(key, 0);
                        case "getTileEntity" -> null;
                        case "isAirBlock" -> !blocks.containsKey(key);
                        case "isSideSolid" -> blocks.getOrDefault(key, Blocks.air).isSideSolid((IBlockAccess) proxy, x,
                            y, z, (ForgeDirection) arguments[3]);
                        default -> method.getReturnType() == boolean.class ? false
                            : method.getReturnType() == int.class ? 0 : null;
                    };
                }
                return method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
            });

        private void put(int x, int y, int z, Block block, int meta) {
            String key = x + "," + y + "," + z;
            blocks.put(key, block);
            metadata.put(key, meta);
        }
    }

    private static final class Capture {

        private final String name;
        private final double x, y, z;
        private final float yaw, pitch;

        private Capture(String name, double x, double y, double z, float yaw, float pitch) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.yaw = yaw;
            this.pitch = pitch;
        }
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private static File output() {
        return new File(System.getProperty("gtng.stellarCtm.qa.output", "."));
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        report.add(message);
        System.out.println("STELLAR_CTM_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        finish("FAIL; " + error);
    }

    private void finish(String result) {
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
            Files.write(new File(output(), "checks.txt").toPath(), report, StandardCharsets.UTF_8);
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("STELLAR_CTM_QA: " + result);
        finished = true;
        Minecraft.getMinecraft().shutdown();
    }
}
