package com.xyp.gtnotgood.qa;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalItemSource;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.implementations.items.IAEItemPowerStorage;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.misc.TileSecurity;
import appeng.tile.networking.TileWireless;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import baubles.api.BaublesApi;

public final class TerminalWirelessServerChecks {

    private final MinecraftServer server;
    private final EntityPlayerMP player;
    private final WorldServer world;
    private final IGrid grid;
    private final int x, y, z, savedSlot;
    private final double savedX, savedY, savedZ;
    private final ItemStack[] savedInventory, savedBaubles;
    private final IInventory baubles;
    private final ItemStack terminal, wireless;
    private final IWirelessTermHandler handler;
    private final IAEItemPowerStorage battery;
    private int checks;

    private TerminalWirelessServerChecks(MinecraftServer server, EntityPlayerMP player, ItemStack terminal, IGrid grid,
        int x, int y, int z) {
        this.server = server;
        this.player = player;
        this.world = (WorldServer) player.worldObj;
        this.grid = grid;
        this.x = x;
        this.y = y;
        this.z = z;
        savedX = player.posX;
        savedY = player.posY;
        savedZ = player.posZ;
        savedSlot = player.inventory.currentItem;
        savedInventory = player.inventory.mainInventory.clone();
        baubles = ModList.Baubles.isModLoaded() ? BaublesApi.getBaubles(player) : null;
        savedBaubles = baubles == null ? null : new ItemStack[baubles.getSizeInventory()];
        if (baubles != null) {
            for (int slot = 0; slot < savedBaubles.length; slot++) {
                savedBaubles[slot] = baubles.getStackInSlot(slot);
                baubles.setInventorySlotContents(slot, null);
            }
        }
        Arrays.fill(player.inventory.mainInventory, null);
        player.inventory.currentItem = 0;
        this.terminal = terminal.copy();
        TerminalSettings.setAe(this.terminal, true);
        NBTTagCompound invalid = new NBTTagCompound();
        invalid.setInteger("Dimension", world.provider.dimensionId + 1);
        this.terminal.getTagCompound().setTag("BoundAE", invalid);
        player.inventory.mainInventory[0] = this.terminal;
        wireless = AEApi.instance().definitions().items().wirelessTerminal().maybeStack(1).get();
        handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(wireless);
        battery = (IAEItemPowerStorage) wireless.getItem();
        player.inventory.mainInventory[1] = wireless;
        near();
    }

    public static TileWireless prepare(WorldServer world, EntityPlayerMP player, int x, int y, int z) {
        world.setBlock(x, y, z, AEApi.instance().definitions().blocks().wireless().maybeBlock().get(), 0, 3);
        TileWireless accessPoint = (TileWireless) world.getTileEntity(x, y, z);
        accessPoint.setOrientation(ForgeDirection.UP, ForgeDirection.NORTH);
        accessPoint.getProxy().setOwner(player);
        return accessPoint;
    }

    public static int verify(MinecraftServer server, EntityPlayerMP player, ItemStack terminal, IGrid grid, int x,
        int y, int z) {
        TerminalWirelessServerChecks check = new TerminalWirelessServerChecks(server, player, terminal, grid, x, y, z);
        try {
            check.normal();
            return check.checks;
        } finally {
            check.restore();
        }
    }

    public static int verifySecured(MinecraftServer server, EntityPlayerMP player, ItemStack terminal, IGrid grid,
        int x, int y, int z) {
        TerminalWirelessServerChecks check = new TerminalWirelessServerChecks(server, player, terminal, grid, x, y, z);
        try {
            check.secured();
            return check.checks;
        } finally {
            check.restore();
        }
    }

    private void normal() {
        TileWireless accessPoint = (TileWireless) world.getTileEntity(x, y, z);
        require(accessPoint.isActive() && accessPoint.getActionableNode().getGrid() == grid,
            "wireless access point joined the physical powered storage grid through normal ticks");
        require(handler != null && !handler.hasInfinityPower(wireless) && !handler.hasInfinityRange(wireless),
            "native AE wireless terminal has its registered handler and ordinary battery/range rules");
        pair(wireless, accessPoint.getLocatableSerial());
        require(
            AEApi.instance().registries().locatable().getLocatableBy(accessPoint.getLocatableSerial()) == accessPoint,
            "terminal key points to the real registered access point");
        require(AEApi.instance().registries().wireless().checkRange(wireless, player),
            "native wireless range check accepts the nearby paired terminal");
        long ironBefore = ironAmount(), waterBefore = waterAmount();
        require(ironBefore >= 8 && waterBefore >= 2000, "real item and fluid cells contain wireless QA material");
        TerminalItemSource source = source();
        double power = battery.getAECurrentPower(wireless);
        require(
            amount(source.take(stack -> stack.getItem() == Items.iron_ingot, true, 3)) == 3
                && ironAmount() == ironBefore
                && equal(power, battery.getAECurrentPower(wireless)),
            "wireless simulation leaves physical stock and terminal charge unchanged");
        require(
            amount(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 3)) == 3
                && ironAmount() == ironBefore - 3,
            "invalid direct binding falls back to the real paired wireless terminal");
        require(equal(power - 3, battery.getAECurrentPower(wireless)),
            "wireless item extraction consumes three real AE units from the terminal battery");
        source.give(iron(3));
        require(ironAmount() == ironBefore && equal(power - 6, battery.getAECurrentPower(wireless)),
            "wireless item return restores the cell and consumes its own terminal charge");
        power = battery.getAECurrentPower(wireless);
        FluidStack extracted = source.takeFluid(water(1000), false);
        require(extracted != null && extracted.amount == 1000 && waterAmount() == waterBefore - 1000,
            "wireless fallback extracts one bucket from the physical fluid cell");
        require(equal(power - 1, battery.getAECurrentPower(wireless)),
            "wireless fluid extraction pays the native one-bucket terminal energy cost");
        source.giveFluid(water(1000), false);
        require(waterAmount() == waterBefore && equal(power - 2, battery.getAECurrentPower(wireless)),
            "wireless fluid return preserves the exact physical stock and drains the terminal battery");

        battery.extractAEPower(wireless, Double.MAX_VALUE);
        require(equal(0, battery.getAECurrentPower(wireless)), "native terminal battery is genuinely depleted");
        require(
            source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == ironBefore
                && source.takeFluid(water(1000), false) == null
                && waterAmount() == waterBefore,
            "depleted wireless terminal cannot extract items or fluid despite a powered network");
        battery.injectAEPower(wireless, 1000);
        power = battery.getAECurrentPower(wireless);
        player.setPosition(x + accessPoint.getRange() + 64, y, z);
        require(!AEApi.instance().registries().wireless().checkRange(wireless, player),
            "real access point range rejects the distant player");
        require(
            source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == ironBefore
                && equal(power, battery.getAECurrentPower(wireless)),
            "out-of-range wireless fallback neither extracts nor drains the terminal");
        near();
        ItemStack invalidKey = wireless.copy();
        handler.setEncryptionKey(invalidKey, "terminal-qa-invalid-key", "Invalid QA key");
        player.inventory.mainInventory[1] = invalidKey;
        require(
            source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == ironBefore
                && equal(power, battery.getAECurrentPower(invalidKey))
                && equal(power, battery.getAECurrentPower(wireless)),
            "malformed terminal key is rejected without extraction, energy loss or a parse crash");
        player.inventory.mainInventory[1] = wireless;
        TerminalSettings.setAe(terminal, false);
        power = battery.getAECurrentPower(wireless);
        require(
            source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == ironBefore
                && equal(power, battery.getAECurrentPower(wireless)),
            "disabled AE mode also disables wireless fallback");
        TerminalSettings.setAe(terminal, true);
        verifyBaubles(accessPoint.getLocatableSerial(), ironBefore);
        require(ironAmount() == ironBefore && waterAmount() == waterBefore,
            "wireless QA restores physical item and fluid cell contents");
    }

    private void verifyBaubles(long key, long ironBefore) {
        if (baubles == null) return;
        ItemStack wearable = wireless;
        int slot = baubleSlot(wearable);
        if (slot < 0) {
            wearable = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
            slot = baubleSlot(wearable);
        }
        require(slot >= 0, "installed Baubles inventory exposes a valid slot for a real wireless terminal");
        pair(wearable, key);
        player.inventory.mainInventory[1] = null;
        baubles.setInventorySlotContents(slot, wearable);
        require(BaublesApi.getBaubles(player).getStackInSlot(slot) == wearable,
            "wireless fixture occupies the actual player Baubles slot outside the main inventory");
        IWirelessTermHandler registered = AEApi.instance().registries().wireless().getWirelessTerminalHandler(wearable);
        require(
            Long.toString(key).equals(registered.getEncryptionKey(wearable))
                && AEApi.instance().registries().wireless().checkRange(wearable, player),
            "worn terminal retains its real active network key and passes native wireless range validation");
        IAEItemPowerStorage powerStorage = (IAEItemPowerStorage) wearable.getItem();
        double power = powerStorage.getAECurrentPower(wearable);
        TerminalItemSource source = source();
        require(source.takeOne(iron(1), false) && ironAmount() == ironBefore - 1,
            "real wireless terminal in a valid Baubles slot supplies construction material");
        require(equal(power - 1, powerStorage.getAECurrentPower(baubles.getStackInSlot(slot))),
            "Baubles wireless extraction writes charge back to the actual worn stack");
        source.give(iron(1));
        require(ironAmount() == ironBefore, "Baubles wireless return restores the physical cell");
        baubles.setInventorySlotContents(slot, null);
        require(!source.takeOne(iron(1), false) && ironAmount() == ironBefore,
            "removing the worn wireless terminal invalidates the cached fallback session");
    }

    private void secured() {
        TileSecurity security = null;
        for (IGridNode node : grid.getMachines(TileSecurity.class))
            if (node.getMachine() instanceof TileSecurity station) security = station;
        require(security != null && security.isSecurityEnabled(),
            "secured wireless QA uses the real active native security station");
        pair(wireless, security.getLocatableSerial());
        require(AEApi.instance().registries().locatable().getLocatableBy(security.getLocatableSerial()) == security,
            "wireless terminal is paired through the native security-station encryption key");
        long before = ironAmount();
        TerminalItemSource owner = source();
        require(owner.takeOne(iron(1), false) && ironAmount() == before - 1,
            "real security owner can use the paired wireless fallback");
        owner.give(iron(1));
        require(ironAmount() == before, "authorized wireless return restores protected storage");

        EntityPlayerMP stranger = new EntityPlayerMP(server, world,
            new GameProfile(UUID.fromString("1b4eadc5-1eea-4435-b78a-84c0d842218a"), "TerminalWirelessDeniedQA"),
            new ItemInWorldManager(world));
        new NetHandlerPlayServer(server, new NetworkManager(false), stranger);
        stranger.setGameType(WorldSettings.GameType.SURVIVAL);
        stranger.setPosition(x + 1, y, z + 1);
        ItemStack copied = wireless.copy();
        stranger.inventory.mainInventory[0] = terminal.copy();
        stranger.inventory.mainInventory[1] = copied;
        ISecurityGrid permissions = grid.getCache(ISecurityGrid.class);
        require(
            !permissions.hasPermission(stranger, SecurityPermissions.EXTRACT)
                && !permissions.hasPermission(stranger, SecurityPermissions.INJECT),
            "native security cache denies the unlisted player both wireless transfer permissions");
        require(AEApi.instance().registries().wireless().checkRange(copied, stranger),
            "unauthorized terminal is genuinely paired and in range before permission rejection");
        double power = battery.getAECurrentPower(copied);
        long waterBefore = waterAmount();
        TerminalItemSource denied = new TerminalItemSource(stranger.inventory.mainInventory[0], stranger);
        require(
            denied.take(stack -> stack.getItem() == Items.iron_ingot, false, 2).isEmpty() && ironAmount() == before
                && equal(power, battery.getAECurrentPower(copied)),
            "paired unauthorized wireless terminal cannot extract or spend energy");
        require(denied.takeFluid(water(1000), false) == null && waterAmount() == waterBefore,
            "wireless fluid fallback respects the current player's extraction permission");
        denied.give(iron(2));
        require(
            ironAmount() == before && stranger.inventory.mainInventory[2] != null
                && stranger.inventory.mainInventory[2].getItem() == Items.iron_ingot
                && stranger.inventory.mainInventory[2].stackSize == 2,
            "denied wireless insertion returns material to the player without altering protected storage");
        require(equal(power, battery.getAECurrentPower(copied)),
            "denied wireless insertion does not drain the copied terminal");
    }

    private void pair(ItemStack stack, long key) {
        IWirelessTermHandler registered = AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
        if (registered == null) throw new AssertionError("real wireless terminal is not registered");
        registered.setEncryptionKey(stack, Long.toString(key), "Terminal wireless QA");
        if (!Long.toString(key).equals(registered.getEncryptionKey(stack)))
            throw new AssertionError("native wireless pairing did not select the requested active network key");
        ((IAEItemPowerStorage) stack.getItem()).injectAEPower(stack, 1000);
    }

    private int baubleSlot(ItemStack stack) {
        for (int slot = 0; slot < baubles.getSizeInventory(); slot++)
            if (baubles.isItemValidForSlot(slot, stack)) return slot;
        return -1;
    }

    private TerminalItemSource source() {
        return new TerminalItemSource(terminal, player);
    }

    private long ironAmount() {
        IAEItemStack stored = grid.<IStorageGrid>getCache(IStorageGrid.class).getItemInventory().getStorageList()
            .findPrecise(AEItemStack.create(iron(1)));
        return stored == null ? 0 : stored.getStackSize();
    }

    private long waterAmount() {
        IAEFluidStack stored = grid.<IStorageGrid>getCache(IStorageGrid.class).getFluidInventory().getStorageList()
            .findPrecise(AEFluidStack.create(water(1)));
        return stored == null ? 0 : stored.getStackSize();
    }

    private void near() {
        player.setPosition(x + 1, y, z + 1);
    }

    private void restore() {
        System.arraycopy(savedInventory, 0, player.inventory.mainInventory, 0, savedInventory.length);
        player.inventory.currentItem = savedSlot;
        if (baubles != null) for (int slot = 0; slot < savedBaubles.length; slot++)
            baubles.setInventorySlotContents(slot, savedBaubles[slot]);
        player.setPosition(savedX, savedY, savedZ);
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
    }

    private static ItemStack iron(int amount) {
        return new ItemStack(Items.iron_ingot, amount);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static int amount(Map<ItemStack, Integer> stacks) {
        return stacks.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static boolean equal(double expected, double actual) {
        return Math.abs(expected - actual) < 0.000001;
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("TERMINAL_WIRELESS_QA: PASS " + message);
    }
}
