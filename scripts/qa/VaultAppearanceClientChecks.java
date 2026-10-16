package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.common.machines.hatch.VaultPortHatch;
import com.xyp.gtnotgood.common.machines.multiblock.SingularityDataHub;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.ITexture;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.common.render.IIconTexture;

@Mod(
    modid = "vaultappearanceqa",
    name = "Vault Appearance QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class VaultAppearanceClientChecks {

    private final List<int[]> casings = new ArrayList<>();
    private BaseMetaTileEntity base;
    private BaseMetaTileEntity portBase;
    private SingularityDataHub machine;
    private VaultPortHatch port;
    private Block casing;
    private int casingMeta;
    private int stage;
    private int serverTicks;
    private int stageTick;
    private int clientTicks;
    private int frames;
    private int checks;
    private int firstAnimationFrame;
    private TextureAtlasSprite activeSprite;
    private boolean started;
    private volatile boolean finished;
    private volatile Throwable serverFailure;
    private volatile int captureRequested;
    private volatile int captureCompleted;
    private volatile boolean initialReady;
    private boolean texturesChecked;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.vaultAppearance.qa")) FMLCommonHandler.instance().bus().register(this);
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
            if (serverFailure != null) throw new AssertionError("Vault server checks failed", serverFailure);
            if (++clientTicks > 4200) throw new AssertionError("Vault client timed out at stage " + stage);
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                mc.launchIntegratedServer("vault-appearance-qa-" + System.currentTimeMillis(), "Vault Appearance QA",
                    new WorldSettings(92L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (initialReady && !texturesChecked && mc.theWorld != null && mc.thePlayer != null) {
                checkTextures();
                texturesChecked = true;
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
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
                    if (!machine.mMachine) {
                        await(260, "new casing structure forms through normal server ticks");
                        return;
                    }
                    require(machine.mCountCasing == 10 && machine.portHatch == port && port.controller == machine,
                        "3x2x2 vault forms with exactly ten Eddington walls and one bound vault port");
                    require(!base.isActive(), "disabled formed vault displays its idle controller");
                    initialReady = true;
                    captureRequested = 1;
                    advance(2);
                }
                case 2 -> {
                    if (captureCompleted < 1) return;
                    base.enableWorking();
                    advance(3);
                }
                case 3 -> {
                    if (!base.isActive() || machine.locked) {
                        await(160, "formed vault enters its normal active storage cycle");
                        return;
                    }
                    checkStorage();
                    captureRequested = 2;
                    advance(4);
                }
                case 4 -> {
                    if (captureCompleted < 2) return;
                    int[] position = casings.get(casings.size() - 1);
                    world.setBlockToAir(position[0], position[1], position[2]);
                    advance(5);
                }
                case 5 -> {
                    if (machine.mMachine) {
                        await(80, "broken Eddington wall invalidates the vault automatically");
                        return;
                    }
                    require(machine.locked && !base.isActive(),
                        "broken structure stops and locks the vault after " + (serverTicks - stageTick) + " ticks");
                    require(port.controller == null, "broken structure unbinds the existing vault port");
                    require(machine.injectItems(new ItemStack(Items.iron_ingot), false) == 0,
                        "unformed vault refuses further storage insertion");
                    requireStored("breaking the appearance casing preserves stored contents");
                    int[] position = casings.get(casings.size() - 1);
                    world.setBlock(position[0], position[1], position[2], casing, casingMeta, 3);
                    advance(6);
                }
                case 6 -> {
                    if (!machine.mMachine) {
                        await(100, "restored Eddington wall reforms the vault automatically");
                        return;
                    }
                    require(port.controller == machine && machine.portHatch == port,
                        "restored structure rebinds the same vault port");
                    requireStored("restoring the casing retains the stored items and fluids");
                    base.enableWorking();
                    advance(8);
                }
                case 8 -> {
                    if (!base.isActive() || machine.locked) {
                        await(80, "reformed vault resumes storage after its normal restart action");
                        return;
                    }
                    require(machine.injectItems(new ItemStack(Items.iron_ingot), false) == 1,
                        "reformed vault accepts storage again after its normal restart action");
                    for (int[] position : casings) {
                        world.setBlock(position[0], position[1], position[2], GregTechAPI.sBlockCasings2, 0, 3);
                    }
                    require(!machine.checkStructure(true, base), "legacy solid steel casing cannot form the new vault");
                    for (int[] position : casings) {
                        world.setBlock(position[0], position[1], position[2], casing, casingMeta, 3);
                    }
                    advance(7);
                }
                case 7 -> {
                    if (!machine.mMachine) {
                        await(100, "Eddington walls form again after legacy-casing rejection");
                        return;
                    }
                    requireStored("legacy-casing rejection and repeated reformation preserve storage");
                    finish("PASS; " + checks + " vault appearance checks");
                }
                default -> throw new AssertionError("Unknown vault stage " + stage);
            }
        } catch (Throwable error) {
            serverFailure = error;
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world.setWorldTime(6000);
        for (int x = -4; x <= 4; x++) {
            for (int z = -3; z <= 5; z++) world.setBlock(x, 69, z, Blocks.stone, 0, 3);
        }
        ItemStack casingStack = GTNGItemList.EddingtonWallCasing.get(1);
        casing = Block.getBlockFromItem(casingStack.getItem());
        casingMeta = casingStack.getItemDamage();
        require(casingMeta == 7, "the vault uses the registered Eddington wall metadata");
        base = place(world, 0, 70, 0, GTNGMachineID.SingularityDataHub.id);
        base.setOwnerName(player.getCommandSenderName());
        base.setOwnerUuid(player.getUniqueID());
        base.disableWorking();
        machine = (SingularityDataHub) base.getMetaTileEntity();
        machine.construct(new ItemStack(Items.stick), false);
        List<int[]> constructed = new ArrayList<>();
        for (int x = -2; x <= 2; x++) {
            for (int y = 70; y <= 72; y++) {
                for (int z = -2; z <= 2; z++) {
                    if (world.getBlock(x, y, z) == casing && world.getBlockMetadata(x, y, z) == casingMeta) {
                        constructed.add(new int[] { x, y, z });
                    }
                }
            }
        }
        require(constructed.size() == 11, "StructureLib construction places eleven Eddington casing candidates");
        int[] portPosition = constructed.stream().filter(position -> position[1] == 70 && position[2] == 0).findFirst()
            .orElseThrow(() -> new AssertionError("Missing front-bottom hatch candidate"));
        portBase = place(world, portPosition[0], portPosition[1], portPosition[2], GTNGMachineID.VaultPortHatch.id);
        portBase.setOwnerName(player.getCommandSenderName());
        portBase.setOwnerUuid(player.getUniqueID());
        port = (VaultPortHatch) portBase.getMetaTileEntity();
        for (int[] position : constructed) {
            if (position == portPosition) continue;
            casings.add(position);
            require(world.getTileEntity(position[0], position[1], position[2]) == null,
                "the actual Eddington structure wall remains a plain block");
        }
        player.capabilities.allowFlying = true;
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
        player.playerNetServerHandler.setPlayerLocation(-3.8, 71.7, -5.5, -35, 22);
        advance(1);
    }

    private void checkStorage() {
        ItemStack iron = new ItemStack(Items.iron_ingot, 32);
        FluidStack water = new FluidStack(FluidRegistry.WATER, 4096);
        long before = machine.getUsedStorageBytes();
        require(machine.injectItems(iron, false) == 32 && machine.injectFluids(water, false) == 4096,
            "formed vault simulates ordinary item and fluid insertion");
        require(machine.getUsedStorageBytes() == before, "storage simulations do not consume shared bytes");
        require(machine.injectItems(iron, true) == 32 && machine.injectFluids(water, true) == 4096,
            "formed vault accepts ordinary item and fluid insertion");
        require(
            machine.extractItems(AEItemStack.create(new ItemStack(Items.iron_ingot, 8)), true) == 8
                && machine.extractFluids(AEFluidStack.create(new FluidStack(FluidRegistry.WATER, 512)), true) == 512,
            "formed vault extracts items and fluids after its casing change");
        requireStored("shared storage accounting remains correct after extraction");
    }

    private void requireStored(String description) {
        var iron = machine.getStoredItem(new ItemStack(Items.iron_ingot));
        var water = machine.getStoredFluid(new FluidStack(FluidRegistry.WATER, 1));
        require(iron != null && iron.getStackSize() == 24
            && water != null
            && water.getStackSize() == 3584
            && machine.getUsedStorageBytes() == 21, description);
    }

    private void checkTextures() throws Exception {
        IIcon expected = casing.getIcon(ForgeDirection.NORTH.ordinal(), casingMeta);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            for (boolean active : new boolean[] { false, true }) {
                ITexture[] textures = machine.getTexture(base, side, ForgeDirection.NORTH, -1, active, false);
                require(icon(textures[0], side) == expected,
                    "controller " + side + " uses Eddington wall as its base in state " + active);
                if (side != ForgeDirection.NORTH) {
                    require(textures.length == 1, "non-front controller face has no machine overlay");
                } else {
                    String suffix = active ? "_ACTIVE" : "";
                    require(
                        textures.length == 3
                            && icon(textures[1], side).getIconName().endsWith("OVERLAY_FRONT_KERR_COMPRESSOR" + suffix)
                            && icon(textures[2], side).getIconName()
                                .endsWith("OVERLAY_FRONT_KERR_COMPRESSOR" + suffix + "_GLOW"),
                        "controller front uses the original Kerr compressor overlay and glow in state " + active);
                    require((Boolean) field(textures[2], "glow"),
                        "the controller glow layer renders at full brightness");
                    if (active) {
                        activeSprite = (TextureAtlasSprite) icon(textures[1], side);
                        require(activeSprite.hasAnimationMetadata(),
                            "the active controller retains upstream animation metadata");
                    }
                }
                ITexture[] portTextures = port.getTexture(portBase, side, ForgeDirection.NORTH, -1, active, false);
                require(icon(portTextures[0], side) == expected,
                    "vault port " + side + " uses the Eddington wall background in state " + active);
            }
        }
    }

    private IIcon icon(ITexture texture, ForgeDirection side) {
        if (!(texture instanceof IIconTexture)) throw new AssertionError("Unexpected texture layer " + texture);
        IIcon icon = ((IIconTexture) texture).getIcon(side.ordinal(), null);
        require(
            icon != null && Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
            "controller or port texture is present in the real block atlas");
        return icon;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !texturesChecked) return;
        try {
            if (serverFailure != null) throw new AssertionError("Vault server checks failed", serverFailure);
            if (captureRequested <= captureCompleted) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.thePlayer == null || mc.theWorld == null || ++frames < 80) return;
            if (captureRequested == 1) {
                screenshot("vault-idle.png");
                captureCompleted = 1;
                frames = 0;
            } else if (captureRequested == 2) {
                if (frames == 80) {
                    screenshot("vault-active.png");
                    firstAnimationFrame = (Integer) field(activeSprite, "frameCounter");
                }
                int frame = (Integer) field(activeSprite, "frameCounter");
                if (frame != firstAnimationFrame) {
                    require(true, "the visible controller advances its real upstream texture animation");
                    screenshot("vault-active-next-frame.png");
                    captureCompleted = 2;
                    frames = 0;
                } else if (frames > 600) {
                    throw new AssertionError("Visible controller animation did not advance");
                }
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private static Object field(Object target, String name) throws Exception {
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field.get(target);
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        return tile;
    }

    private void await(int limit, String description) {
        if (serverTicks - stageTick > limit) {
            throw new AssertionError(description + " timed out; formed="
                + machine.mMachine
                + ", active="
                + base.isActive()
                + ", allowed="
                + base.isAllowedToWork()
                + ", locked="
                + machine.locked
                + ", mUpdate="
                + machine.mUpdate
                + ", mUpdated="
                + machine.mUpdated
                + ", mStartUpCheck="
                + machine.mStartUpCheck);
        }
    }

    private void advance(int next) {
        stage = next;
        stageTick = serverTicks;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        return new File(System.getProperty("gtng.vaultAppearance.qa.output", "."));
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("VAULT_APPEARANCE_QA: " + message);
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
        System.out.println("VAULT_APPEARANCE_QA: " + result);
        finished = true;
    }
}
