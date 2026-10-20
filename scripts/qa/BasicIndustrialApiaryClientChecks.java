package com.xyp.gtnotgood.qa;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import com.xyp.gtnotgood.common.machines.basic.BasicIndustrialApiary;
import com.xyp.gtnotgood.common.machines.bee.ApiaryOutputBuffer;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import forestry.api.apiculture.BeeManager;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeModifier;
import forestry.apiculture.genetics.Bee;
import forestry.apiculture.genetics.BeeDefinition;
import gregtech.api.GregTechAPI;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTApiaryUpgrade;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;

@Mod(
    modid = "basicapiaryqa",
    name = "Basic Industrial Apiary QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class BasicIndustrialApiaryClientChecks {

    private BaseMetaTileEntity guiBase;
    private boolean started;
    private volatile boolean checked, finished, opened, clientReady;
    private int clientTicks, serverTicks, frames;
    private int fixtureX = 8;
    private final StringBuilder report = new StringBuilder();

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.basicApiary.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        try {
            require(++clientTicks < 6000, "client timeout");
            Minecraft mc = Minecraft.getMinecraft();
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.setSoundLevel(SoundCategory.MASTER, 0F);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("basic-apiary-qa-" + System.currentTimeMillis(), "Basic Industrial Apiary QA",
                    new WorldSettings(28618L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (
                checked && mc.theWorld != null
                    && mc.theWorld.getTileEntity(0, 20, 0) instanceof BaseMetaTileEntity tile
                    && tile.getMetaTileEntity() instanceof BasicIndustrialApiary
            ) clientReady = true;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            require(++serverTicks < 3600, "server timeout");
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            if (serverTicks == 20) {
                verifyRegistrationAndRecipe(player);
                verifyProduction(player, matedQueen(BeeDefinition.MODEST), false);
                verifyProduction(player, specialtyQueen(), true);
                verifyBreeding(player);
                verifyPowerAndPersistence(player);
                verifyCancellationAndMachineItem(player);
                verifyBufferAndAutomaticOutput(player);
                prepareGui(player);
                player.playerNetServerHandler.setPlayerLocation(0.5, 20, -3, 0, 0);
                System.out.println("BASIC_APIARY_QA: server checks passed\n" + report);
                checked = true;
            }
            if (clientReady && !opened) {
                ((BasicIndustrialApiary) guiBase.getMetaTileEntity()).onRightclick(guiBase, player);
                opened = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void verifyRegistrationAndRecipe(EntityPlayerMP player) {
        require(GTNGMachineID.BasicIndustrialApiary.id == 28618, "stable machine ID");
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        require(apiary.mTier == 1 && apiary.maxEUInput() == 32 && apiary.maxAmperesIn() == 1,
            "LV voltage and one input amp");
        require(apiary.mOutputItems.length == 54, "fifty-four output slots");
        require(apiary.mSpeed == 6, "base speed is sixty-four times");
        require(GTUtility.areStacksEqual(apiary.getStackForm(1), GTNGItemList.BasicIndustrialApiary.get(1)),
            "item enum resolves registered machine");
        apiary.addUpgrade(GTApiaryUpgrade.speed8.get(1));
        apiary.updateModifiers();
        require(apiary.mSpeed == 14 && apiary.getUpgrade(0) != null, "native speed upgrade adds eight levels");
        apiary.removeUpgrade(0, 1);
        apiary.updateModifiers();
        require(apiary.mSpeed == 6, "removing upgrade restores base speed");
        List<GTRecipe> recipes = RecipeMaps.assemblerRecipes.getAllRecipes().stream()
            .filter(r -> Arrays.stream(r.mOutputs)
                .anyMatch(s -> GTUtility.areStacksEqual(s, GTNGItemList.BasicIndustrialApiary.get(1))))
            .collect(Collectors.toList());
        require(recipes.size() == 1 && recipes.get(0).mEUt > 0 && recipes.get(0).mEUt <= 32, "one LV assembler recipe");
        report.append("Registration, LV power, native upgrades and LV assembler recipe passed\n");
    }

    private void verifyProduction(EntityPlayerMP player, ItemStack queen, boolean specialty) throws Exception {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        World world = player.worldObj;
        world.setWorldTime(18000);
        world.getWorldInfo().setRaining(true);
        world.getWorldInfo().setRainTime(6000);
        for (int x = tile.xCoord - 2; x <= tile.xCoord + 2; x++)
            for (int z = -2; z <= 2; z++) world.setBlock(x, 22, z, Blocks.stone, 0, 3);
        require(!world.canBlockSeeTheSky(tile.xCoord, tile.yCoord + 1, tile.zCoord), "covered fixture");
        IBee bee = BeeManager.beeRoot.getMember(queen);
        String species = bee.getGenome().getPrimary().getUID();
        apiary.setQueen(queen);
        apiary.updateModifiers();
        Map<GTUtility.ItemId, Float> expected = expectedProducts(apiary, bee);
        require(apiary.checkRecipe() == 2, species + " works at night in rain without sky or flowers");
        require(!apiary.hasErrors() && apiary.mEUt == 32, "unrestricted operation still uses LV power");
        float life = bee.getHealth() * apiary.getLifespanModifier(null, null, 1)
            * BeeManager.beeRoot.getBeekeepingMode(world).getBeeModifier().getLifespanModifier(null, null, 1);
        int duration = Math.max(1, (int) Math.ceil(life * 550F / 64));
        require(apiary.mMaxProgresstime == duration, "full native lifespan divided by sixty-four");
        Map<GTUtility.ItemId, Long> actual = pendingProducts(apiary);
        require(expected.keySet().containsAll(actual.keySet()), "ordinary and specialty product types are expected");
        for (Map.Entry<GTUtility.ItemId, Float> entry : expected.entrySet()) {
            double amount = entry.getValue().doubleValue() * 16;
            long count = actual.getOrDefault(entry.getKey(), 0L);
            require(count >= Math.floor(amount) && count <= Math.ceil(amount), "sixteenfold product rounding");
        }
        if (specialty) {
            require(!bee.getGenome().getPrimary().getSpecialtyChances().isEmpty(), "fixture has specialty production");
            require(
                bee.getGenome().getPrimary().getSpecialtyChances().keySet().stream()
                    .anyMatch(s -> actual.getOrDefault(GTUtility.ItemId.createNoCopy(s), 0L) > 0),
                "fixture produces an actual specialty item");
        }
        tile.setActive(true);
        finish(tile);
        require(apiary.getPendingItemCount() == 0, "completed product queue is committed");
        require(apiary.getQueen() != null && apiary.getDrone() != null, "native offspring are returned automatically");
        assertLegalStacks(tile, apiary);
        report.append(species).append(" production, specialty yield and offspring passed\n");
    }

    private Map<GTUtility.ItemId, Float> expectedProducts(BasicIndustrialApiary apiary, IBee bee) {
        IBeeGenome genome = bee.getGenome();
        IBeeModifier mode = BeeManager.beeRoot.getBeekeepingMode(apiary.getWorld()).getBeeModifier();
        float cycles = bee.getHealth() * apiary.getLifespanModifier(null, null, 1)
            * mode.getLifespanModifier(null, null, 1);
        float production = apiary.getProductionModifier(null, 0) + mode.getProductionModifier(null, 0);
        Map<GTUtility.ItemId, Float> drops = new HashMap<>();
        addExpected(drops, genome.getPrimary().getProductChances(), genome.getSpeed(), production, cycles, 1);
        addExpected(drops, genome.getSecondary().getProductChances(), genome.getSpeed(), production, cycles, 0.5F);
        addExpected(drops, genome.getPrimary().getSpecialtyChances(), genome.getSpeed(), production, cycles, 1);
        return drops;
    }

    private static void addExpected(Map<GTUtility.ItemId, Float> drops, Map<ItemStack, Float> products, float speed,
        float production, float cycles, float chanceFactor) {
        for (Map.Entry<ItemStack, Float> entry : products.entrySet()) {
            drops.merge(GTUtility.ItemId.createNoCopy(entry.getKey()),
                Bee.getFinalChance(entry.getValue() * chanceFactor, speed, production, 8F) * entry.getKey().stackSize
                    * cycles,
                Float::sum);
        }
    }

    private void verifyBreeding(EntityPlayerMP player) {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        apiary.setQueen(BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.PRINCESS));
        require(apiary.checkRecipe() != 2, "princess without drone cannot breed");
        apiary.setDrone(BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.DRONE));
        require(apiary.checkRecipe() == 2, "native breeding starts");
        require(apiary.mMaxProgresstime == 1 && apiary.mEUt == 32, "breeding takes one LV tick");
        require(apiary.getQueen() == null && apiary.getDrone() == null, "parents consumed once");
        tile.setActive(true);
        finish(tile);
        require(BeeManager.beeRoot.getType(apiary.getQueen()) == EnumBeeType.QUEEN,
            "offspring queen returned to input");
        report.append("Drone requirement and native one-tick breeding passed\n");
    }

    private void verifyPowerAndPersistence(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        ItemStack original = matedQueen(BeeDefinition.IMPERIAL);
        apiary.setQueen(original);
        require(apiary.checkRecipe() == 2, "power-loss operation starts");
        tile.setActive(true);
        NBTTagCompound before = save(apiary);
        long pending = apiary.getPendingItemCount();
        tile.setStoredEU(0);
        apiary.onPostTick(tile, 1);
        require(apiary.mProgresstime <= 0 && apiary.getPendingItemCount() == pending,
            "zero power retains pending outputs");
        require(ItemStack.areItemStacksEqual(original, apiary.getUsedQueen()),
            "zero power retains full queen genetics");
        NBTTagCompound saved = save(apiary);
        require(before.getTag("PendingProducts").equals(saved.getTag("PendingProducts")),
            "power loss preserves product NBT");
        BaseMetaTileEntity restoredTile = reload(player, tile);
        BasicIndustrialApiary restored = machine(restoredTile);
        require(restored.getPendingItemCount() == pending, "pending quantity survives tile NBT reload");
        require(ItemStack.areItemStacksEqual(original, restored.getUsedQueen()),
            "in-flight queen survives tile NBT reload");
        require(saved.getTag("PendingProducts").equals(save(restored).getTag("PendingProducts")),
            "all pending product types survive tile reload");
        fillOutputs(restoredTile, restored, new ItemStack(Items.stick, 64));
        restoredTile.enableWorking();
        restoredTile.setStoredEU(restoredTile.getEUCapacity());
        for (int i = 2; i < 500 && (restored.mStuttering || restored.mProgresstime < 0); i++)
            restored.onPostTick(restoredTile, i);
        restoredTile.disableWorking();
        finish(restoredTile);
        require(restored.getPendingItemCount() == 0 && restored.getBufferedItemCount() == pending,
            "full output stores every produced item after resume");
        BaseMetaTileEntity blockedTile = reload(player, restoredTile);
        BasicIndustrialApiary blocked = machine(blockedTile);
        require(blocked.getBufferedItemCount() == pending, "blocked products survive a second tile reload");
        clearOutputs(blockedTile, blocked);
        long drained = drainBuffer(blockedTile, blocked, null);
        require(drained == pending, "all completed products recover after clearing output");
        report.append("Power loss, in-flight queen NBT, full-output completion and two reloads passed\n");
    }

    private void verifyBufferAndAutomaticOutput(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        ItemStack product = new ItemStack(Items.sugar);
        product.setTagCompound(new NBTTagCompound());
        product.getTagCompound().setString("ApiaryQA", "retained");
        buffer(apiary).add(product, 100_123);
        BaseMetaTileEntity restoredTile = reload(player, tile);
        BasicIndustrialApiary restored = machine(restoredTile);
        require(restored.getBufferedItemCount() == 100_123, "large product count survives NBT");
        require(drainBuffer(restoredTile, restored, product) == 100_123,
            "large cache drains without loss or NBT changes");
        buffer(restored).add(product, BasicIndustrialApiary.BUFFER_LIMIT);
        restored.setQueen(BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.QUEEN));
        require(restored.checkRecipe() != 2 && restored.getQueen() != null,
            "buffer threshold retains queen and refuses new operation");
        buffer(restored).clear();
        restored.setQueen(null);
        World world = tile.getWorld();
        world.setBlock(restoredTile.xCoord, 20, -1, Blocks.chest, 0, 3);
        world.setBlock(restoredTile.xCoord + 1, 20, -1, Blocks.chest, 0, 3);
        TileEntityChest left = (TileEntityChest) world.getTileEntity(restoredTile.xCoord, 20, -1);
        TileEntityChest right = (TileEntityChest) world.getTileEntity(restoredTile.xCoord + 1, 20, -1);
        restored.setMainFacing(ForgeDirection.SOUTH);
        restoredTile.setFrontFacing(ForgeDirection.NORTH);
        fillOutputs(restoredTile, restored, new ItemStack(Items.sugar, 64));
        restored.mItemTransfer = false;
        restored.onPostTick(restoredTile, 1);
        require(inventoryCount(left) + inventoryCount(right) == 0, "disabled output does not transfer");
        restored.mItemTransfer = true;
        restored.onPostTick(restoredTile, 2);
        require(inventoryCount(left) + inventoryCount(right) == 54L * 64,
            "one tick transfers all fifty-four output stacks");
        require(outputCount(restoredTile, restored) == 0, "automatic output removes transferred stacks once");
        report.append("100123 tagged products, buffer threshold and per-tick fifty-four-stack export passed\n");
    }

    private void verifyCancellationAndMachineItem(EntityPlayerMP player) throws Exception {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        ItemStack queen = matedQueen(BeeDefinition.IMPERIAL);
        apiary.setQueen(queen);
        buffer(apiary).add(new ItemStack(Items.sugar), 17);
        require(apiary.checkRecipe() == 2 && apiary.getPendingItemCount() > 0, "cancel fixture starts production");
        tile.setActive(true);
        apiary.cancelProcess();
        apiary.cancelProcess();
        require(ItemStack.areItemStacksEqual(queen, apiary.getQueen()), "cancel returns complete queen exactly once");
        require(apiary.getPendingItemCount() == 0 && apiary.getBufferedItemCount() == 17,
            "cancel discards unearned products and retains completed products");
        require(apiary.mMaxProgresstime == 0 && !tile.isActive()
            && !tile.isAllowedToWork()
            && Arrays.stream(apiary.mOutputItems).allMatch(s -> s == null), "cancel clears process and offspring");
        verifyMachineItem(player, false);
        verifyMachineItem(player, true);
        report
            .append("Repeated cancellation and machine-item replacement preserve bees without duplicating products\n");
    }

    private void verifyMachineItem(EntityPlayerMP player, boolean breeding) throws Exception {
        BaseMetaTileEntity tile = fixture(player);
        BasicIndustrialApiary apiary = machine(tile);
        ItemStack queen = breeding ? BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.PRINCESS)
            : matedQueen(BeeDefinition.IMPERIAL);
        ItemStack drone = breeding ? BeeDefinition.MEADOWS.getMemberStack(EnumBeeType.DRONE) : null;
        apiary.setQueen(queen.copy());
        if (drone != null) apiary.setDrone(drone.copy());
        buffer(apiary).add(new ItemStack(Items.sugar), 17);
        require(apiary.checkRecipe() == 2, "machine-item fixture starts");
        tile.setActive(true);
        if (breeding) {
            tile = reload(player, tile);
            apiary = machine(tile);
            require(
                ItemStack.areItemStacksEqual(drone,
                    ItemStack.loadItemStackFromNBT(save(apiary).getCompoundTag("UsedDrone"))),
                "consumed breeding drone survives tile reload");
        }
        long pending = apiary.getPendingItemCount();
        NBTTagCompound itemData = new NBTTagCompound();
        apiary.setItemNBT(itemData);
        NBTTagCompound repeated = new NBTTagCompound();
        apiary.setItemNBT(repeated);
        require(itemData.equals(repeated), "serializing machine item twice does not duplicate cached bees");
        require(apiary.getPendingItemCount() == pending && apiary.mMaxProgresstime > 0,
            "item serialization does not mutate live operation");
        Map<GTUtility.ItemId, Long> expected = new HashMap<>();
        expected.put(GTUtility.ItemId.createNoCopy(new ItemStack(Items.sugar)), 17L);
        expected.put(GTUtility.ItemId.createNoCopy(queen), 1L);
        if (drone != null) expected.put(GTUtility.ItemId.createNoCopy(drone), 1L);
        require(readProducts(itemData, "BufferedProducts").equals(expected),
            "machine item contains completed products and original parents with full NBT");
        BaseMetaTileEntity replaced = fixture(player);
        replaced.setInitialValuesAsNBT(itemData, (short) GTNGMachineID.BasicIndustrialApiary.id);
        replaced.disableWorking();
        BasicIndustrialApiary restored = machine(replaced);
        require(restored.mMaxProgresstime == 0 && !replaced.isActive() && restored.getPendingItemCount() == 0,
            "replaced machine does not restore cancelled pending products");
        require(readProducts(save(restored), "BufferedProducts").equals(expected),
            "item placement restores every original parent and completed product");
        require(drainBuffer(replaced, restored, null) == (breeding ? 19 : 18),
            "replaced machine exports saved products and parents exactly once");
    }

    private void prepareGui(EntityPlayerMP player) {
        guiBase = place(player, 0);
        BasicIndustrialApiary apiary = machine(guiBase);
        apiary.setQueen(matedQueen(BeeDefinition.IMPERIAL));
        apiary.setDrone(BeeDefinition.IMPERIAL.getMemberStack(EnumBeeType.DRONE));
        apiary.addUpgrade(GTApiaryUpgrade.production.get(8));
        apiary.addUpgrade(GTApiaryUpgrade.speed8.get(1));
        apiary.updateModifiers();
        List<ItemStack> products = new ArrayList<>(
            BeeDefinition.IMPERIAL.getIndividual().getGenome().getPrimary().getProductChances().keySet());
        products.addAll(BeeDefinition.IMPERIAL.getIndividual().getGenome().getPrimary().getSpecialtyChances().keySet());
        for (int i = 0; i < products.size(); i++) {
            ItemStack product = products.get(i).copy();
            product.stackSize = product.getMaxStackSize();
            guiBase.setInventorySlotContents(apiary.getOutputSlot() + i, product);
        }
    }

    private static Map<GTUtility.ItemId, Long> pendingProducts(BasicIndustrialApiary apiary) {
        return readProducts(save(apiary), "PendingProducts");
    }

    private static Map<GTUtility.ItemId, Long> readProducts(NBTTagCompound saved, String key) {
        NBTTagList products = saved.getTagList(key, 10);
        Map<GTUtility.ItemId, Long> result = new HashMap<>();
        for (int i = 0; i < products.tagCount(); i++) {
            NBTTagCompound entry = products.getCompoundTagAt(i);
            result.merge(GTUtility.ItemId.createNoCopy(ItemStack.loadItemStackFromNBT(entry.getCompoundTag("Item"))),
                entry.getLong("Count"), Long::sum);
        }
        return result;
    }

    private static ApiaryOutputBuffer buffer(BasicIndustrialApiary apiary) throws Exception {
        Field field = BasicIndustrialApiary.class.getDeclaredField("bufferedProducts");
        field.setAccessible(true);
        return (ApiaryOutputBuffer) field.get(apiary);
    }

    private long drainBuffer(BaseMetaTileEntity tile, BasicIndustrialApiary apiary, ItemStack expected) {
        long total = 0;
        for (int tick = 1; tick < 1000; tick++) {
            apiary.onPostTick(tile, tick);
            assertLegalStacks(tile, apiary);
            for (int i = apiary.getOutputSlot(); i < apiary.getOutputSlot() + 54; i++) {
                ItemStack stack = tile.getStackInSlot(i);
                if (stack == null) continue;
                if (expected != null)
                    require(GTUtility.areStacksEqual(stack, expected), "product NBT survives queue drain");
                total += stack.stackSize;
                tile.setInventorySlotContents(i, null);
            }
            if (apiary.getBufferedItemCount() == 0) return total;
        }
        throw new AssertionError("buffer did not drain");
    }

    private static void finish(BaseMetaTileEntity tile) {
        BasicIndustrialApiary apiary = machine(tile);
        for (int tick = 1; tick < 40000 && apiary.mMaxProgresstime > 0; tick++) {
            tile.setStoredEU(tile.getEUCapacity());
            apiary.onPostTick(tile, tick);
        }
        require(apiary.mMaxProgresstime == 0, "operation completes with LV power");
    }

    private BaseMetaTileEntity fixture(EntityPlayerMP player) {
        BaseMetaTileEntity tile = place(player, fixtureX);
        fixtureX += 8;
        return tile;
    }

    private static BaseMetaTileEntity place(EntityPlayerMP player, int x) {
        World world = player.worldObj;
        world.setBlock(x, 20, 0, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, 20, 0);
        tile.setInitialValuesAsNBT(null, (short) GTNGMachineID.BasicIndustrialApiary.id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        tile.disableWorking();
        require(tile.getMetaTileEntity() instanceof BasicIndustrialApiary, "registered class");
        tile.setStoredEU(tile.getEUCapacity());
        return tile;
    }

    private BaseMetaTileEntity reload(EntityPlayerMP player, BaseMetaTileEntity source) {
        NBTTagCompound saved = new NBTTagCompound();
        source.writeToNBT(saved);
        BaseMetaTileEntity restored = fixture(player);
        saved.setInteger("x", restored.xCoord);
        saved.setInteger("y", restored.yCoord);
        saved.setInteger("z", restored.zCoord);
        restored.readFromNBT(saved);
        restored.disableWorking();
        return restored;
    }

    private static BasicIndustrialApiary machine(BaseMetaTileEntity tile) {
        return (BasicIndustrialApiary) tile.getMetaTileEntity();
    }

    private static ItemStack matedQueen(BeeDefinition species) {
        IBee bee = species.getIndividual();
        bee.mate(species.getIndividual());
        return BeeManager.beeRoot.getMemberStack(bee, EnumBeeType.QUEEN.ordinal());
    }

    private static ItemStack specialtyQueen() {
        for (BeeDefinition species : BeeDefinition.values()) {
            if (!species.getIndividual().getGenome().getPrimary().getSpecialtyChances().isEmpty())
                return matedQueen(species);
        }
        for (IBee template : BeeManager.beeRoot.getIndividualTemplates()) {
            if (template.getGenome().getPrimary().getSpecialtyChances().isEmpty()) continue;
            ItemStack stack = BeeManager.beeRoot.getMemberStack(template, EnumBeeType.QUEEN.ordinal());
            IBee queen = BeeManager.beeRoot.getMember(stack);
            queen.mate(BeeManager.beeRoot.getMember(stack));
            return BeeManager.beeRoot.getMemberStack(queen, EnumBeeType.QUEEN.ordinal());
        }
        throw new AssertionError("No registered bee has a specialty product");
    }

    private static NBTTagCompound save(BasicIndustrialApiary apiary) {
        NBTTagCompound tag = new NBTTagCompound();
        apiary.saveNBTData(tag);
        return tag;
    }

    private static void fillOutputs(BaseMetaTileEntity tile, BasicIndustrialApiary apiary, ItemStack stack) {
        for (int i = 0; i < 54; i++) tile.setInventorySlotContents(apiary.getOutputSlot() + i, stack.copy());
    }

    private static void clearOutputs(BaseMetaTileEntity tile, BasicIndustrialApiary apiary) {
        for (int i = 0; i < 54; i++) tile.setInventorySlotContents(apiary.getOutputSlot() + i, null);
    }

    private static long outputCount(BaseMetaTileEntity tile, BasicIndustrialApiary apiary) {
        long count = 0;
        for (int i = 0; i < 54; i++) {
            ItemStack stack = tile.getStackInSlot(apiary.getOutputSlot() + i);
            if (stack != null) count += stack.stackSize;
        }
        return count;
    }

    private static long inventoryCount(IInventory inventory) {
        long count = 0;
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            ItemStack stack = inventory.getStackInSlot(i);
            if (stack != null) count += stack.stackSize;
        }
        return count;
    }

    private static void assertLegalStacks(BaseMetaTileEntity tile, BasicIndustrialApiary apiary) {
        for (int i = 0; i < 54; i++) {
            ItemStack stack = tile.getStackInSlot(apiary.getOutputSlot() + i);
            require(stack == null || stack.stackSize > 0 && stack.stackSize <= stack.getMaxStackSize(),
                "output respects native stack limit");
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || !checked) return;
        try {
            require(++frames < 1800, "GUI timeout");
            Minecraft mc = Minecraft.getMinecraft();
            if (!(mc.currentScreen instanceof GuiContainer gui) || frames < 90) return;
            require("基础工业蜂箱".equals(GTNGItemList.BasicIndustrialApiary.get(1).getDisplayName()),
                "localized controller name");
            int outputs = 0, players = 0;
            for (Object entry : gui.inventorySlots.inventorySlots) {
                Slot slot = (Slot) entry;
                if (slot.inventory == mc.thePlayer.inventory) players++;
                else if (slot.getSlotIndex() >= 11 && slot.getSlotIndex() < 65) outputs++;
            }
            require(outputs == 54 && players == 36, "GUI exposes all outputs and player inventory");
            require(gui.inventorySlots.inventorySlots.size() >= 96, "bee and upgrade slots remain visible");
            ScreenShotHelper.saveScreenshot(output(), "basic-industrial-apiary-gui.png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            Files.write(new File(output(), "result.txt").toPath(),
                ("PASS\n" + report + "Chinese GUI with fifty-four outputs rendered\n")
                    .getBytes(StandardCharsets.UTF_8));
            System.out.println("BASIC_APIARY_QA: PASS\n" + report);
            finished = true;
            mc.shutdown();
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static File output() {
        File folder = new File(System.getProperty("gtng.basicApiary.output", "build/basic-apiary-qa"));
        folder.mkdirs();
        return folder;
    }

    private void fail(Throwable failure) {
        if (finished) return;
        finished = true;
        failure.printStackTrace();
        try {
            StringWriter trace = new StringWriter();
            failure.printStackTrace(new PrintWriter(trace));
            Files.write(new File(output(), "result.txt").toPath(), ("FAIL\n" + trace).getBytes(StandardCharsets.UTF_8));
        } catch (Exception reportFailure) {
            reportFailure.printStackTrace();
        }
        Minecraft.getMinecraft().shutdown();
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("BASIC_APIARY_QA: " + message);
    }
}
