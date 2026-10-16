package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
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
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.client.stellar.StellarConnectedIcons;
import com.xyp.gtnotgood.common.blocks.stellar.StellarCasingTextures;
import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
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
    modid = "eddingtonctmqa",
    name = "Eddington CTM QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class EddingtonCtmClientChecks {

    private static final int[][] RIGHT = { { 1, 0, 0 }, { 1, 0, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };
    private static final int[][] UP = { { 0, 0, -1 }, { 0, 0, -1 }, { 0, 1, 0 }, { 0, 1, 0 }, { 0, 1, 0 },
        { 0, 1, 0 } };
    private static final int[][] OFFSETS = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { -1, -1 }, { 1, -1 }, { -1, 1 },
        { 1, 1 } };
    private Block casing;
    private int metadata;
    private BaseMetaTileEntity controllerBase;
    private BaseMetaTileEntity portBase;
    private BaseMetaTileEntity vaultBase;
    private BaseMetaTileEntity ordinaryBase;
    private SingularityDataHub vault;
    private Method getMask;
    private Method normalizeMask;
    private IIcon originalProbe;
    private boolean started;
    private volatile boolean ready;
    private volatile boolean finished;
    private volatile Throwable serverFailure;
    private volatile int request;
    private volatile int completed;
    private int stage;
    private int ticks;
    private int serverTicks;
    private int stageTicks;
    private int frames;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.eddingtonCtm.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (serverFailure != null) throw new AssertionError("CTM server checks failed", serverFailure);
            if (++ticks > 6000) throw new AssertionError("CTM QA client timeout at stage " + stage);
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                require(
                    Loader.instance().getModList().stream()
                        .noneMatch(mod -> mod.getModId().toLowerCase().contains("myctm")),
                    "MyCTMLib is absent from the actual runtime");
                require(
                    Loader.isModLoaded(ModList.Angelica.getID())
                        != Boolean.getBoolean("gtng.eddingtonCtm.qa.withoutAngelica"),
                    "Angelica runtime state matches this isolated test run");
                Class<?> connections = Class.forName("com.xyp.gtnotgood.common.blocks.stellar.StellarConnections");
                getMask = connections.getMethod("getMask", IBlockAccess.class, int.class, int.class, int.class,
                    int.class);
                normalizeMask = connections.getMethod("normalizeMask", int.class);
                mc.launchIntegratedServer("eddington-ctm-qa-" + System.currentTimeMillis(), "Eddington CTM QA",
                    new WorldSettings(93L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (ready && mc.theWorld != null && mc.thePlayer != null) {
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                if (originalProbe == null && mc.theWorld.getBlock(15, 72, 0) == casing) {
                    checkWorldTextures();
                    originalProbe = casing.getIcon(mc.theWorld, 15, 72, 0, 2);
                }
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || serverFailure != null) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            serverTicks++;
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.worldObj;
            switch (stage) {
                case 0 -> setup(player);
                case 1 -> {
                    if (!vault.mMachine) {
                        if (serverTicks - stageTicks > 200) throw new AssertionError("Normal CTM vault did not form");
                        return;
                    }
                    require(vault.mCountCasing == 10 && vault.portHatch != null,
                        "ordinary vault remains formed with ten Eddington walls and one port");
                    ready = true;
                    player.playerNetServerHandler.setPlayerLocation(15.5, 70.8, -8, 0, 0);
                    request = 1;
                    advance(2);
                }
                case 2 -> {
                    if (completed < 1) return;
                    world.setBlockToAir(16, 72, 0);
                    request = 2;
                    advance(3);
                }
                case 3 -> {
                    if (completed < 2) return;
                    world.setBlock(16, 72, 0, casing, metadata, 3);
                    request = 3;
                    advance(4);
                }
                case 4 -> {
                    if (completed < 3) return;
                    player.playerNetServerHandler.setPlayerLocation(9, 81, 2, 39, 25);
                    request = 4;
                    advance(5);
                }
                case 5 -> {
                    if (completed < 4) return;
                    player.playerNetServerHandler.setPlayerLocation(-6, 69, 17, -135, -35);
                    request = 5;
                    advance(6);
                }
                case 6 -> {
                    if (completed < 5) return;
                    player.playerNetServerHandler.setPlayerLocation(29.2, 71.7, -5.5, -35, 22);
                    vaultBase.enableWorking();
                    request = 6;
                    advance(7);
                }
                case 7 -> {
                    if (completed < 6) return;
                    require(vault.mMachine, "CTM world queries and redraws preserve the ordinary vault structure");
                    finish("PASS; " + checks + " Eddington CTM checks");
                }
                default -> throw new AssertionError("Unknown CTM stage " + stage);
            }
        } catch (Throwable error) {
            serverFailure = error;
        }
    }

    private void setup(EntityPlayerMP player) throws Exception {
        World world = player.worldObj;
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world.setWorldTime(6000);
        ItemStack stack = GTNGItemList.EddingtonWallCasing.get(1);
        casing = Block.getBlockFromItem(stack.getItem());
        metadata = stack.getItemDamage();
        for (int x = -3; x <= 38; x++) {
            for (int z = -3; z <= 14; z++) world.setBlock(x, 68, z, Blocks.stone, 0, 2);
        }
        for (int x = 12; x <= 18; x++) {
            for (int y = 70; y <= 74; y++) world.setBlock(x, y, 0, casing, metadata, 3);
        }
        world.setBlockToAir(13, 72, 0);
        world.setBlock(17, 72, -1, casing, metadata, 3);
        controllerBase = place(world, 15, 71, 0, GTNGMachineID.SingularityDataHub.id);
        controllerBase.disableWorking();
        portBase = place(world, 14, 71, 0, GTNGMachineID.VaultPortHatch.id);
        ordinaryBase = place(world, 21, 70, 12, GTNGMachineID.SeedConverter.id);
        for (int x = 0; x <= 3; x++) {
            for (int y = 75; y <= 78; y++) {
                for (int z = 8; z <= 11; z++) world.setBlock(x, y, z, casing, metadata, 3);
            }
        }
        world.setBlockToAir(1, 78, 9);
        world.setBlock(3, 77, 7, casing, metadata, 3);
        vaultBase = place(world, 33, 70, 0, GTNGMachineID.SingularityDataHub.id);
        vaultBase.setOwnerName(player.getCommandSenderName());
        vaultBase.setOwnerUuid(player.getUniqueID());
        vaultBase.disableWorking();
        vault = (SingularityDataHub) vaultBase.getMetaTileEntity();
        vault.construct(new ItemStack(Items.stick), false);
        place(world, 32, 70, 0, GTNGMachineID.VaultPortHatch.id);
        checkNormalization();
        checkMasks();
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        advance(1);
    }

    private void checkNormalization() throws Exception {
        Set<Integer> tiles = new HashSet<>();
        for (int input = 0; input < 256; input++) {
            int expected = referenceNormalize(input);
            int actual = (Integer) normalizeMask.invoke(null, input);
            require(actual == expected, "normalized blob mask " + input + " retains only supported diagonals");
            tiles.add(actual);
        }
        require(tiles.size() == 47, "all 256 neighborhood combinations reduce to exactly 47 blob tiles");
    }

    private void checkMasks() throws Exception {
        for (int side = 0; side < 6; side++) {
            for (int input = 0; input < 256; input++) {
                FixtureAccess fixture = new FixtureAccess();
                fixture.put(0, 0, 0, casing, metadata, null);
                for (int neighbor = 0; neighbor < 8; neighbor++) {
                    if ((input & (1 << neighbor)) == 0) continue;
                    int[] offset = offset(side, OFFSETS[neighbor][0], OFFSETS[neighbor][1]);
                    fixture.put(offset[0], offset[1], offset[2], casing, metadata, null);
                }
                int actual = mask(fixture.access, side);
                require(actual == referenceNormalize(input),
                    "face " + side + " neighborhood " + input + " resolves to the correct bounded blob mask");
            }
            for (int neighbor = 0; neighbor < 4; neighbor++) {
                int[] offset = offset(side, OFFSETS[neighbor][0], OFFSETS[neighbor][1]);
                FixtureAccess fixture = new FixtureAccess();
                fixture.put(0, 0, 0, casing, metadata, null);
                fixture.put(offset[0], offset[1], offset[2], GregTechAPI.sBlockMachines, 0, controllerBase);
                require(mask(fixture.access, side) == 1 << neighbor, "face " + side + " connects its controller edge");
                fixture.put(offset[0], offset[1], offset[2], GregTechAPI.sBlockMachines, 0, portBase);
                require(mask(fixture.access, side) == 1 << neighbor, "face " + side + " connects its vault-port edge");
                fixture.put(offset[0], offset[1], offset[2], GregTechAPI.sBlockMachines, 0, ordinaryBase);
                require(mask(fixture.access, side) == 0, "face " + side + " rejects another GregTech machine");
                fixture.put(offset[0], offset[1], offset[2], casing, metadata + 1, null);
                require(mask(fixture.access, side) == 0, "face " + side + " rejects the neighboring casing metadata");
                fixture.put(offset[0], offset[1], offset[2], Blocks.stone, 0, null);
                require(mask(fixture.access, side) == 0, "face " + side + " rejects unrelated solid blocks");
                fixture.put(offset[0], offset[1], offset[2], casing, metadata, null);
                ForgeDirection normal = ForgeDirection.getOrientation(side);
                fixture.put(offset[0] + normal.offsetX, offset[1] + normal.offsetY, offset[2] + normal.offsetZ, casing,
                    metadata, null);
                require(mask(fixture.access, side) == 0,
                    "face " + side + " does not connect across a normal-facing step");
            }
            for (int corner = 0; corner < 4; corner++) {
                FixtureAccess fixture = new FixtureAccess();
                fixture.put(0, 0, 0, casing, metadata, null);
                for (int[] neighbor : OFFSETS) {
                    int[] offset = offset(side, neighbor[0], neighbor[1]);
                    fixture.put(offset[0], offset[1], offset[2], casing, metadata, null);
                }
                int[] diagonal = offset(side, OFFSETS[corner + 4][0], OFFSETS[corner + 4][1]);
                ForgeDirection normal = ForgeDirection.getOrientation(side);
                fixture.put(diagonal[0] + normal.offsetX, diagonal[1] + normal.offsetY, diagonal[2] + normal.offsetZ,
                    casing, metadata, null);
                require(mask(fixture.access, side) == (255 & ~(16 << corner)),
                    "face " + side + " keeps the concave corner when its diagonal face is occluded");
            }
        }
    }

    private int mask(IBlockAccess access, int side) throws Exception {
        return (Integer) getMask.invoke(null, access, 0, 0, 0, side);
    }

    private static int referenceNormalize(int input) {
        int result = input & 15;
        int[] gates = { 5, 6, 9, 10 };
        for (int corner = 0; corner < 4; corner++) {
            int bit = 16 << corner;
            if ((input & gates[corner]) == gates[corner] && (input & bit) != 0) result |= bit;
        }
        return result;
    }

    private static int[] offset(int side, int right, int up) {
        return new int[] { RIGHT[side][0] * right + UP[side][0] * up, RIGHT[side][1] * right + UP[side][1] * up,
            RIGHT[side][2] * right + UP[side][2] * up };
    }

    private void checkWorldTextures() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        IIcon inventory = casing.getIcon(2, metadata);
        require(inventory.getIconName().endsWith("stellar/sl_eddington"),
            "the wall item retains its original isolated inventory icon");
        Set<IIcon> connectedIcons = new HashSet<>();
        for (int mask = 0; mask < 256; mask++) {
            IIcon icon = StellarConnectedIcons.getIcon("sl_eddington", mask);
            require(
                icon.getIconName().endsWith("stellar/ctm/eddington_" + referenceNormalize(mask))
                    && mc.getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
                "blob mask " + mask + " selects its uploaded normalized CTM tile");
            connectedIcons.add(icon);
        }
        require(connectedIcons.size() == 47, "the real client atlas contains all 47 distinct CTM tiles");
        Set<String> icons = new HashSet<>();
        for (int x = 12; x <= 18; x++) {
            for (int y = 70; y <= 74; y++) {
                if (mc.theWorld.getBlock(x, y, 0) != casing) continue;
                IIcon icon = casing.getIcon(mc.theWorld, x, y, 0, 2);
                require(mc.getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
                    "the real flat wall selects a stitched CTM tile at " + x + "," + y);
                icons.add(icon.getIconName());
            }
        }
        require(icons.size() >= 9, "edges, holes, corners and interior select distinct CTM tiles on the flat wall");
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            SingularityDataHub controller = (SingularityDataHub) controllerBase.getMetaTileEntity();
            VaultPortHatch port = (VaultPortHatch) portBase.getMetaTileEntity();
            for (boolean active : new boolean[] { false, true }) {
                ITexture[] layers = controller.getTexture(controllerBase, side, ForgeDirection.NORTH, -1, active,
                    false);
                require(layers[0] == StellarCasingTextures.EDDINGTON,
                    "the controller shares the built-in Eddington texture on " + side);
                ITexture[] portLayers = port.getTexture(portBase, side, ForgeDirection.NORTH, -1, active, false);
                require(portLayers[0] == StellarCasingTextures.EDDINGTON,
                    "the vault port shares the built-in Eddington texture on " + side);
                if (side == ForgeDirection.NORTH) {
                    String suffix = active ? "_ACTIVE" : "";
                    require(
                        ((IIconTexture) layers[1]).getIcon(2, null).getIconName()
                            .endsWith("OVERLAY_FRONT_KERR_COMPRESSOR" + suffix),
                        "CTM preserves the original controller front in state " + active);
                    require(
                        ((IIconTexture) layers[2]).getIcon(2, null).getIconName()
                            .endsWith("OVERLAY_FRONT_KERR_COMPRESSOR" + suffix + "_GLOW"),
                        "CTM preserves the controller glow layer in state " + active);
                    if (active) {
                        require(
                            ((TextureAtlasSprite) ((IIconTexture) layers[1]).getIcon(2, null)).hasAnimationMetadata(),
                            "CTM retains the original active-controller animation");
                    }
                }
            }
            var context = new SBRContextHolder().getSBRWorldContext(15, 71, 0, GregTechAPI.sBlockMachines,
                GregTechAPI.sBlockMachines.getRenderType(), new RenderBlocks(mc.theWorld));
            IIcon icon = ((IIconTexture) StellarCasingTextures.EDDINGTON).getIcon(side.ordinal(), context);
            int expected = (Integer) getMask.invoke(null, mc.theWorld, 15, 71, 0, side.ordinal());
            require(
                icon.getIconName().endsWith("stellar/ctm/eddington_" + expected)
                    && mc.getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
                "the native GregTech world context selects a real CTM icon for the controller on " + side);
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || request <= completed || originalProbe == null) return;
        try {
            if (serverFailure != null) throw new AssertionError("CTM server checks failed", serverFailure);
            Minecraft mc = Minecraft.getMinecraft();
            if (++frames < 100) return;
            if (request == 2) {
                if (mc.theWorld.getBlock(16, 72, 0) != Blocks.air) return;
                require(casing.getIcon(mc.theWorld, 15, 72, 0, 2) != originalProbe,
                    "removing a wall block refreshes CTM across the chunk boundary");
            } else if (request == 3) {
                if (mc.theWorld.getBlock(16, 72, 0) != casing) return;
                require(casing.getIcon(mc.theWorld, 15, 72, 0, 2) == originalProbe,
                    "replacing the wall restores CTM across the chunk boundary");
            }
            String[] names = { "", "flat-wall", "flat-wall-removed", "flat-wall-restored", "cube-top-north-east",
                "cube-bottom-south-west", "ordinary-vault" };
            ScreenShotHelper.saveScreenshot(output(), names[request] + ".png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
            completed = request;
            frames = 0;
        } catch (Throwable error) {
            fail(error);
        }
    }

    private final class FixtureAccess {

        private final Map<String, Block> blocks = new HashMap<>();
        private final Map<String, Integer> metadata = new HashMap<>();
        private final Map<String, TileEntity> tiles = new HashMap<>();
        private final IBlockAccess access = (IBlockAccess) Proxy.newProxyInstance(IBlockAccess.class.getClassLoader(),
            new Class<?>[] { IBlockAccess.class }, (proxy, method, arguments) -> {
                if (arguments != null && arguments.length >= 3 && arguments[0] instanceof Integer) {
                    int x = (Integer) arguments[0], y = (Integer) arguments[1], z = (Integer) arguments[2];
                    if (Math.abs(x) > 1 || Math.abs(y) > 1 || Math.abs(z) > 1) {
                        throw new AssertionError("CTM queried beyond its bounded neighborhood");
                    }
                    String key = key(x, y, z);
                    return switch (method.getName()) {
                        case "getBlock" -> blocks.getOrDefault(key, Blocks.air);
                        case "getBlockMetadata" -> metadata.getOrDefault(key, 0);
                        case "getTileEntity" -> tiles.get(key);
                        case "isAirBlock" -> !blocks.containsKey(key);
                        case "isSideSolid" -> blocks.getOrDefault(key, Blocks.air).isSideSolid((IBlockAccess) proxy, x,
                            y, z, (ForgeDirection) arguments[3]);
                        default -> method.getReturnType() == boolean.class ? false
                            : method.getReturnType() == int.class ? 0 : null;
                    };
                }
                return method.getReturnType() == boolean.class ? false : method.getReturnType() == int.class ? 0 : null;
            });

        private void put(int x, int y, int z, Block block, int meta, TileEntity tile) {
            String key = key(x, y, z);
            blocks.put(key, block);
            metadata.put(key, meta);
            if (tile == null) tiles.remove(key);
            else tiles.put(key, tile);
        }
    }

    private static String key(int x, int y, int z) {
        return x + "," + y + "," + z;
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private void advance(int next) {
        stage = next;
        stageTicks = serverTicks;
    }

    private static File output() {
        return new File(System.getProperty("gtng.eddingtonCtm.qa.output", "."));
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("EDDINGTON_CTM_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        finish("FAIL; stage " + stage + "; " + error);
    }

    private void finish(String result) {
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("EDDINGTON_CTM_QA: " + result);
        finished = true;
    }
}
