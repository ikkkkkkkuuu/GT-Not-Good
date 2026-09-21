package com.xyp.gtnotgood.common.packaged;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import WayofTime.alchemicalWizardry.ModBlocks;
import WayofTime.alchemicalWizardry.api.altarRecipeRegistry.AltarRecipeRegistry;
import WayofTime.alchemicalWizardry.common.bloodAltarUpgrade.UpgradedAltars;
import WayofTime.alchemicalWizardry.common.tileEntity.TEAltar;
import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.definitions.IBlockDefinition;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.util.AEColor;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/** Opt-in manual world with real slate recipes; only this named playground receives switchable test LP. */
@Mod(modid = "bloodplayground", name = "Blood Altar Playground", version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class BloodAltarPlayground {

    private boolean launched, resumed, failed;
    private volatile boolean ready;
    private String save;
    private int ticks, frames;
    private TilePackagedProvider provider;
    private TEAltar altar;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.blood.playground")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        var mc = Minecraft.getMinecraft();
        if (!launched && mc.theWorld == null && mc.currentScreen != null) {
            launched = true;
            mc.gameSettings.guiScale = 2;
            mc.gameSettings.pauseOnLostFocus = false;
            com.cleanroommc.modularui.ModularUIConfig.guiDebugMode = false;
            save = System.getProperty("gtng.blood.playground.resume", "");
            resumed = !save.isEmpty();
            if (resumed && !save.matches("blood-manual-[0-9]+")) throw new IllegalArgumentException("Invalid test save");
            if (!resumed) save = "blood-manual-" + System.currentTimeMillis();
            Files.write(new File("blood-playground-save.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
            mc.launchIntegratedServer(save, "GTNG Blood Altar Manual Test", resumed ? null
                : new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
        if (ready && mc.theWorld != null && ++frames == 100) {
            ScreenShotHelper.saveScreenshot(mc.mcDataDir, "blood-playground.png", mc.displayWidth, mc.displayHeight,
                mc.getFramebuffer());
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || failed) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        var player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        var world = player.getServerForPlayer();
        if (!server.getFolderName().equals(save)) return;
        try {
            if (++ticks == 1) {
                if (!resumed) build(world, player);
                provider = (TilePackagedProvider) world.getTileEntity(0, 8, -4);
                altar = (TEAltar) world.getTileEntity(12, 12, 8);
                if (provider == null || altar == null) throw new IllegalStateException("Missing test station");
                provider.setOwnerName(player.getCommandSenderName());
            }
            // Test-only convenience, toggled by the labeled lever; never alters production altar behavior.
            if (ticks % 20 == 0 && world.getBlock(1, 8, -3) == Blocks.lever
                && (world.getBlockMetadata(1, 8, -3) & 8) != 0
                && world.getTileEntity(12, 12, 8) == altar) {
                altar.fillMainTank(Math.max(0, altar.getCapacity() - altar.getCurrentBlood()));
                altar.markDirty();
            }
            if (ticks == 100) {
                altar.checkAndSetAltar();
                if (altar.getTier() != 5) throw new IllegalStateException("Altar tier is " + altar.getTier());
                if (!provider.getProxy().isActive() || provider.getProxy().getCrafting().getCpus().isEmpty())
                    throw new IllegalStateException("AE power/channel/CPU missing");
                if (!resumed) {
                    var stock = AEItemStack.create(new ItemStack(Blocks.stone));
                    stock.setStackSize(4096);
                    if (provider.getProxy().getStorage().getItemInventory()
                        .injectItems(stock, Actionable.MODULATE, new MachineSource(provider)) != null)
                        throw new IllegalStateException("Stock rejected");
                }
                world.saveAllChunks(true, null);
                server.getConfigurationManager().saveAllPlayerData();
                player.addChatMessage(new ChatComponentText("血祭坛测试端就绪：在前方 ME 合成终端下单石板。"));
                player.addChatMessage(new ChatComponentText("五级祭坛已绑定，已备4096石头；拉杆控制测试补血，关闭后可测缺血等待。"));
                Files.write(new File("blood-playground-ready.txt").toPath(), save.getBytes(StandardCharsets.UTF_8));
                System.out.println("BLOOD_PLAYGROUND_READY: tier=5; AE powered; CPU available; save=" + save);
                ready = true;
            }
        } catch (Exception error) {
            failed = true;
            System.err.println("BLOOD_PLAYGROUND_FAILED");
            error.printStackTrace();
        }
    }

    private void build(World world, EntityPlayerMP player) {
        world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
        world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
        world.setWorldTime(6000);
        for (int x = -8; x <= 23; x++) for (int z = -8; z <= 19; z++) {
            world.setBlock(x, 7, z, (x + z) % 2 == 0 ? Blocks.stonebrick : Blocks.quartz_block);
        }
        for (var component : UpgradedAltars.getAltarUpgradeListForTier(5)) {
            world.setBlock(12 + component.x(), 12 + component.y(), 8 + component.z(),
                component.anyBlockMatches() ? Blocks.stonebrick : component.getBlock(),
                component.getMetadata() == 32767 ? 0 : component.getMetadata(), 3);
        }
        world.setBlock(12, 12, 8, ModBlocks.blockAltar);
        world.setBlock(0, 8, -4,
            net.minecraft.block.Block.getBlockFromItem(GTNGItemList.WirelessPackagedPatternProvider.getItem()));
        provider = (TilePackagedProvider) world.getTileEntity(0, 8, -4);
        provider.setOwnerName(player.getCommandSenderName());
        provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.BloodAltarCore.get(1));
        provider.autoReturn = true;
        if (!provider.bind(new PackagedTarget(world.provider.dimensionId, 12, 12, 8, 1)))
            throw new IllegalStateException("Binding failed");
        var blocks = AEApi.instance().definitions().blocks();
        place(world, -1, -4, blocks.controller());
        place(world, -1, -3, blocks.energyCellCreative());
        place(world, -2, -4, blocks.drive());
        ((TileDrive) world.getTileEntity(-2, 8, -4)).getInternalInventory().setInventorySlotContents(0,
            AEApi.instance().definitions().items().cell64k().maybeStack(1).get());
        place(world, -2, -3, blocks.craftingStorage64k());
        place(world, -3, -3, blocks.craftingAccelerator());
        terminal(world, player, -3, AEApi.instance().definitions().parts().craftingTerminal().maybeStack(1).get());
        terminal(world, player, -4, AEApi.instance().definitions().parts().patternTerminal().maybeStack(1).get());
        terminal(world, player, -5, AEApi.instance().definitions().parts().interfaceTerminal().maybeStack(1).get());
        ItemStack ingredient = new ItemStack(Blocks.stone);
        for (int slot = 0; slot < 5; slot++) {
            var recipe = AltarRecipeRegistry.getAltarRecipeForItemAndTier(ingredient, 5);
            if (recipe == null || recipe.getCanBeFilled() || recipe.getResult() == null)
                throw new IllegalStateException("Missing real slate recipe step " + slot);
            ItemStack output = recipe.getResult().copy();
            provider.setInventorySlotContents(slot, pattern(ingredient, output));
            System.out.println("BLOOD_PLAYGROUND_RECIPE: " + ingredient.getDisplayName() + " -> " + output.getDisplayName());
            ingredient = output.copy();
            ingredient.stackSize = 1;
        }
        world.setBlock(1, 8, -3, Blocks.lever, 13, 3);
        sign(world, 1, -5, "测试补血开关", "默认开启", "关闭后消耗存血", "用于测试缺血等待");
        sign(world, -3, -6, "ME 合成终端", "搜索石板 / slate", "已有五级石板链", "右侧样板终端");
        sign(world, 0, -6, "血祭坛封包供应器", "核心与样板已装", "祭坛已绑定", "自动回收开启");
        player.inventory.setInventorySlotContents(0, GTNGItemList.ItemWirelessConnector.get(1));
        player.inventory.setInventorySlotContents(1, GTNGItemList.BloodAltarCore.get(1));
        player.inventory.setInventorySlotContents(2, GTNGItemList.WirelessPackagedPatternProvider.get(1));
        player.inventory.setInventorySlotContents(3,
            AEApi.instance().definitions().materials().blankPattern().maybeStack(64).get());
        player.inventory.setInventorySlotContents(4, new ItemStack(ModBlocks.blockAltar));
        player.playerNetServerHandler.setPlayerLocation(-0.5, 9, -8.5, -15, 10);
        player.capabilities.isFlying = true;
        player.sendPlayerAbilities();
    }

    private static ItemStack pattern(ItemStack input, ItemStack output) {
        ItemStack stack = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList ins = new NBTTagList();
        ins.appendTag(input.writeToNBT(new NBTTagCompound()));
        NBTTagList outs = new NBTTagList();
        outs.appendTag(output.writeToNBT(new NBTTagCompound()));
        tag.setBoolean("crafting", false);
        tag.setTag("in", ins);
        tag.setTag("out", outs);
        stack.setTagCompound(tag);
        return stack;
    }

    private static void place(World world, int x, int z, IBlockDefinition definition) {
        world.setBlock(x, 8, z, definition.maybeBlock().get(), definition.maybeStack(1).get().getItemDamage(), 3);
    }

    private static void terminal(World world, EntityPlayerMP player, int x, ItemStack terminal) {
        place(world, x, -4, AEApi.instance().definitions().blocks().multiPart());
        var host = (IPartHost) world.getTileEntity(x, 8, -4);
        host.addPart(AEApi.instance().definitions().parts().cableGlass().stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN, player);
        if (host.addPart(terminal, ForgeDirection.NORTH, player) == null) throw new IllegalStateException("Terminal");
    }

    private static void sign(World world, int x, int z, String... lines) {
        world.setBlock(x, 8, z, Blocks.standing_sign, 8, 3);
        var sign = (TileEntitySign) world.getTileEntity(x, 8, z);
        System.arraycopy(lines, 0, sign.signText, 0, 4);
        sign.markDirty();
        world.markBlockForUpdate(x, 8, z);
    }
}
