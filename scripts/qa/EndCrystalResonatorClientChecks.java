package com.xyp.gtnotgood.qa;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.item.EntityEnderCrystal;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.basic.EndCrystalResonator;
import com.xyp.gtnotgood.loader.EndCrystalResonatorLoader;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTUtility;
import gregtech.common.misc.WirelessNetworkManager;

@Mod(
    modid = "crystalresonatorqa",
    name = "End Crystal Resonator QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class EndCrystalResonatorClientChecks {

    private BaseMetaTileEntity natural;
    private BigInteger initialEU;
    private boolean started;
    private int ticks;
    private int frames;
    private int checks;
    private volatile boolean visuals;
    private volatile boolean finished;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.crystalresonator.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.pauseOnLostFocus = false;
        if (finished) FMLCommonHandler.instance().exitJava(0, false);
        else if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("crystal-resonator-qa-" + System.currentTimeMillis(), "Crystal Resonator QA",
                new WorldSettings(71L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || visuals || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            ticks++;
            if (ticks == 1) setup(player);
            if (ticks == 101) {
                BigInteger total = WirelessNetworkManager.getUserEU(player.getUniqueID())
                    .add(BigInteger.valueOf(pending(natural)));
                equal(BigInteger.valueOf(EndCrystalResonator.generationForTier(1) * 100), total.subtract(initialEU),
                    "100 actual server ticks produce exactly 100 ticks of EU");
                natural.disableWorking();
                player.capabilities.allowFlying = true;
                player.capabilities.isFlying = true;
                player.sendPlayerAbilities();
                player.setPositionAndUpdate(12.5, 18, -18);
                player.rotationYaw = 0;
                player.rotationPitch = 18;
                visuals = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void setup(EntityPlayerMP player) {
        World world = player.worldObj;
        world.setWorldTime(1000);
        require(ModsItemlist.PlaceableEndCrystal.get(1) != null, "existing Et Futurum crystal item resolves");
        for (int tier = 1; tier <= 14; tier++) {
            int x = (tier - 1) % 7 * 4, z = (tier - 1) / 7 * 4;
            ItemStack stack = EndCrystalResonatorLoader.machine(tier).get(1);
            long recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream().filter(recipe -> {
                for (ItemStack output : recipe.mOutputs) if (GTUtility.areStacksEqual(output, stack)) return true;
                return false;
            }).count();
            require(recipes == 1, "one acquisition recipe at tier " + tier);
            BaseMetaTileEntity base = place(world, player, x, z, stack);
            EndCrystalResonator machine = (EndCrystalResonator) base.getMetaTileEntity();
            EntityEnderCrystal crystal = new EntityEnderCrystal(world, x + 0.5, 11, z + 0.5);
            require(world.spawnEntityInWorld(crystal), "spawn crystal at tier " + tier);
            machine.onFirstTick(base);
            base.enableWorking();
            long rate = GTValues.V[tier] * (tier == 1 ? 32L : 128L);
            BigInteger before = WirelessNetworkManager.getUserEU(player.getUniqueID());
            run(machine, base, 1, 20);
            equal(BigInteger.valueOf(rate * 20), balance(player).subtract(before), "exact wireless generation " + tier);
            require(!crystal.isDead, "generation preserves crystal " + tier);
            require(!machine.isEnetInput() && !machine.isEnetOutput(), "wireless only " + tier);
            run(machine, base, 21, 27);
            NBTTagCompound saved = new NBTTagCompound();
            machine.saveNBTData(saved);
            require(saved.getLong("resonatorPendingEU") == rate * 7, "pending energy saves exactly " + tier);
            machine.loadNBTData(saved);
            machine.onFirstTick(base);
            before = balance(player);
            run(machine, base, 28, 40);
            equal(BigInteger.valueOf(rate * 20), balance(player).subtract(before),
                "NBT reload preserves pending " + tier);
            base.disableWorking();
            before = balance(player);
            run(machine, base, 41, 60);
            equal(before, balance(player), "disabled generation stops " + tier);
            require(!base.isActive(), "inactive when disabled " + tier);
            base.enableWorking();
            crystal.setPosition(x + 1.01, 11, z + 0.5);
            run(machine, base, 80, 100);
            equal(before, balance(player), "overlapping neighboring crystal is rejected " + tier);
            crystal.setDead();
            run(machine, base, 120, 140);
            equal(before, balance(player), "dead crystal produces no energy " + tier);
            placeCrystal(player, base, machine);
            machine.onFirstTick(base);
            run(machine, base, 141, 147);
            before = balance(player);
            world.setBlockToAir(x, 10, z);
            equal(BigInteger.valueOf(rate * 7), balance(player).subtract(before),
                "removal settles partial second " + tier);
            base = place(world, player, x, z, stack);
            ((EndCrystalResonator) base.getMetaTileEntity()).onFirstTick(base);
            if (tier == 1) natural = base;
        }
        natural.enableWorking();
        initialEU = balance(player).add(BigInteger.valueOf(pending(natural)));
    }

    private void placeCrystal(EntityPlayerMP player, BaseMetaTileEntity base, EndCrystalResonator machine) {
        boolean creative = player.capabilities.isCreativeMode;
        ItemStack previous = player.getHeldItem();
        ItemStack crystals = ModsItemlist.PlaceableEndCrystal.get(2);
        player.inventory.setInventorySlotContents(player.inventory.currentItem, crystals);
        player.capabilities.isCreativeMode = false;
        crystals.stackSize = 0;
        require(!machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "empty stack rejected");
        crystals.stackSize = 2;
        FakePlayer denied = new FakePlayer((WorldServer) player.worldObj, player.getGameProfile()) {

            @Override
            public boolean canPlayerEdit(int x, int y, int z, int side, ItemStack held) {
                return false;
            }
        };
        denied.inventory.setInventorySlotContents(0, crystals.copy());
        require(!machine.onRightclick(base, denied, ForgeDirection.UP, 0.5F, 1, 0.5F), "edit permission enforced");
        base.getWorld().setBlock(base.getXCoord(), 11, base.getZCoord(), Blocks.stone);
        require(!machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "occupied top rejected");
        base.getWorld().setBlockToAir(base.getXCoord(), 11, base.getZCoord());
        require(!machine.onRightclick(base, player, ForgeDirection.NORTH, 0.5F, 0.5F, 0.5F), "wrong side rejected");
        require(crystals.stackSize == 2, "rejected click consumes nothing");
        require(machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "top places crystal in survival");
        require(crystals.stackSize == 1, "successful placement consumes one item");
        require(!machine.onRightclick(base, player, ForgeDirection.UP, 0.5F, 1, 0.5F), "duplicate placement rejected");
        require(crystals.stackSize == 1, "duplicate consumes nothing");
        player.capabilities.isCreativeMode = creative;
        player.inventory.setInventorySlotContents(player.inventory.currentItem, previous);
    }

    private static BaseMetaTileEntity place(World world, EntityPlayerMP player, int x, int z, ItemStack stack) {
        world.setBlock(x, 10, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, 10, z);
        base.setInitialValuesAsNBT(null, (short) stack.getItemDamage());
        base.setOwnerUuid(player.getUniqueID());
        base.setFrontFacing(ForgeDirection.NORTH);
        base.disableWorking();
        return base;
    }

    private static void run(EndCrystalResonator machine, BaseMetaTileEntity base, int from, int to) {
        for (int tick = from; tick <= to; tick++) machine.onPostTick(base, tick);
    }

    private static long pending(BaseMetaTileEntity base) {
        NBTTagCompound tag = new NBTTagCompound();
        base.getMetaTileEntity().saveNBTData(tag);
        return tag.getLong("resonatorPendingEU");
    }

    private static BigInteger balance(EntityPlayerMP player) {
        return WirelessNetworkManager.getUserEU(player.getUniqueID());
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !visuals || finished) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            frames++;
            if (frames == 40) {
                mc.gameSettings.hideGUI = true;
                mc.displayGuiScreen(null);
            } else if (frames == 80) {
                capture("resonator-world.png");
                mc.displayGuiScreen(new TooltipScreen(1));
            } else if (frames == 100) {
                capture("resonator-lv-tooltip.png");
                mc.displayGuiScreen(new TooltipScreen(14));
            } else if (frames == 120) {
                capture("resonator-max-tooltip.png");
                Files.write(new File(output(), "result.txt").toPath(), "PASS".getBytes(StandardCharsets.UTF_8));
                System.out.println("CRYSTAL_RESONATOR_QA: PASS (" + checks + " checks)");
                finished = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        return new File(System.getProperty("gtng.crystalresonator.qa.output"));
    }

    private void equal(BigInteger expected, BigInteger actual, String label) {
        require(expected.equals(actual), label + ": expected " + expected + ", got " + actual);
    }

    private void require(boolean condition, String label) {
        if (!condition) throw new AssertionError(label);
        checks++;
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        try {
            Files.write(new File(output(), "result.txt").toPath(),
                ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) {}
        finished = true;
    }

    private static final class TooltipScreen extends GuiScreen {

        private final int tier;

        private TooltipScreen(int tier) {
            this.tier = tier;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTicks) {
            drawDefaultBackground();
            fontRendererObj.drawString("GTL Enhanced / GTNH - End Crystal Resonators", 20, 18, 0xFFFFFF);
            RenderHelper.enableGUIStandardItemLighting();
            for (int index = 1; index <= 14; index++) {
                itemRender.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(),
                    EndCrystalResonatorLoader.machine(index).get(1), 20 + (index - 1) * 26, 42);
            }
            RenderHelper.disableStandardItemLighting();
            ItemStack stack = EndCrystalResonatorLoader.machine(tier).get(1);
            renderToolTip(stack, 26, 95);
        }
    }
}
