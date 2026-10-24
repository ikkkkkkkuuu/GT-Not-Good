package com.xyp.gtnotgood.qa;

import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalItemSource;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.MachineSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AEColor;
import appeng.items.misc.ItemMEStackPacket;
import appeng.tile.grid.AENetworkTile;
import appeng.tile.misc.TileSecurity;
import appeng.tile.storage.TileDrive;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;

/** Advances real ME tiles and caches through normal server ticks; no mocked storage or permissions. */
public final class TerminalStorageServerChecks {

    private static final int X = 256, Y = 80, Z = 256;
    private final MinecraftServer server;
    private final EntityPlayerMP player;
    private final WorldServer world;
    private final ItemStack[] savedInventory;
    private final int savedSlot;
    private final ItemStack terminal;
    private final TerminalItemSource source;
    private final TileDrive drive;
    private TileSecurity security;
    private int ticks, stage, stageStarted, checks;
    private boolean done;

    public TerminalStorageServerChecks(MinecraftServer server, EntityPlayerMP player) throws Exception {
        this.server = server;
        this.player = player;
        this.world = (WorldServer) player.worldObj;
        savedInventory = Arrays.stream(player.inventory.mainInventory).map(stack -> stack == null ? null : stack.copy())
            .toArray(ItemStack[]::new);
        savedSlot = player.inventory.currentItem;
        Arrays.fill(player.inventory.mainInventory, null);
        player.inventory.currentItem = 0;
        terminal = MultiblockTerminalServerChecks.terminal();
        TerminalSettings.setAe(terminal, true);
        player.inventory.mainInventory[0] = terminal;
        source = new TerminalItemSource(terminal, player);

        keepFixtureLoaded();
        // The isolated server world persists between runs; remove only this fixture's old tiles and parts.
        for (int dx = -1; dx <= 1; dx++) for (int dy = 0; dy <= 1; dy++)
            for (int dz = -1; dz <= 1; dz++) world.setBlockToAir(X + dx, Y + dy, Z + dz);
        var definitions = AEApi.instance().definitions();
        world.setBlock(X, Y, Z, definitions.blocks().multiPart().maybeBlock().get(), 0, 3);
        IPartHost cable = (IPartHost) world.getTileEntity(X, Y, Z);
        require(cable.addPart(definitions.parts().cableGlass().stack(AEColor.Transparent, 1), ForgeDirection.UNKNOWN,
            player) != null, "real ME glass cable placed");
        placePower();
        world.setBlock(X - 1, Y, Z, definitions.blocks().drive().maybeBlock().get(), 0, 3);
        drive = (TileDrive) world.getTileEntity(X - 1, Y, Z);
        drive.getProxy().setOwner(player);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        drive.getInternalInventory().setInventorySlotContents(1, new ItemStack(ItemAndBlockHolder.CELL16384KM));
        TerminalWirelessServerChecks.prepare(world, player, X, Y + 1, Z);
        require(world.getTileEntity(X + 1, Y, Z) instanceof AENetworkTile,
            "native creative energy cell and physical item/fluid cells placed");
    }

    public int checks() {
        return checks;
    }

    public boolean tick() throws Exception {
        if (done) return true;
        keepFixtureLoaded();
        if (++ticks > 500) throw new AssertionError("terminal storage QA timed out at stage " + stage);
        int elapsed = ticks - stageStarted;
        if (stage == 0 && elapsed >= 70 && ready()) {
            verifyPoweredStorage();
            checks += TerminalWirelessServerChecks.verify(server, player, terminal, grid(), X, Y + 1, Z);
            require(
                player.inventory.mainInventory[0] == terminal
                    && Arrays.stream(player.inventory.mainInventory).skip(1).allMatch(stack -> stack == null),
                "wireless QA restores the original bound terminal without residual fallback material");
            world.setBlockToAir(X + 1, Y, Z);
            advance(1);
        } else if (stage == 1) {
            grid().<IEnergyGrid>getCache(IEnergyGrid.class).extractAEPower(1_000_000, Actionable.MODULATE,
                PowerMultiplier.ONE);
            if (elapsed >= 40 && !ready()) {
                verifyUnpoweredStorage();
                placePower();
                advance(2);
            }
        } else if (stage == 2 && elapsed >= 70 && ready()) {
            require(ironAmount() == 64 && waterAmount() == 8000 && goldAmount() == 0,
                "restored power exposes the exact original cell contents after all denied offline transfers");
            world.setBlock(X, Y, Z - 1, AEApi.instance().definitions().blocks().security().maybeBlock().get(), 0, 3);
            security = (TileSecurity) world.getTileEntity(X, Y, Z - 1);
            security.getProxy().setOwner(player);
            advance(3);
        } else if (stage == 3 && elapsed >= 70 && ready() && security.isSecurityEnabled()) {
            ISecurityGrid permissions = grid().getCache(ISecurityGrid.class);
            if (!permissions.isAvailable()) return false;
            verifySecurity(permissions);
            checks += TerminalWirelessServerChecks.verifySecured(server, player, terminal, grid(), X, Y + 1, Z);
            verifyRemovedNode();
            restoreInventory();
            done = true;
        }
        return done;
    }

    private void verifyPoweredStorage() {
        require(grid().<IEnergyGrid>getCache(IEnergyGrid.class).isNetworkPowered(),
            "physical network becomes powered through normal server ticks");
        var fixtureSource = new MachineSource(drive);
        require(items().injectItems(AEItemStack.create(iron(64)), Actionable.MODULATE, fixtureSource) == null,
            "native drive accepts initial item stock");
        require(fluids().injectItems(AEFluidStack.create(water(8000)), Actionable.MODULATE, fixtureSource) == null,
            "native drive accepts initial fluid stock");
        require(TerminalItemSource.bind(terminal, player, world, X - 1, Y, Z, ForgeDirection.UNKNOWN),
            "terminal binds to a real active ME drive node");
        require(amount(source.take(stack -> stack.getItem() == Items.iron_ingot, true, 5)) == 5 && ironAmount() == 64,
            "simulated AE extraction returns the requested material without consuming it");
        require(amount(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 5)) == 5 && ironAmount() == 59,
            "real terminal extraction consumes exactly five items from the native cell");
        source.give(iron(5));
        require(ironAmount() == 64 && inventoryItems(player, Items.iron_ingot) == 0,
            "material return reaches AE before the player inventory");
        require(source.takeFluid(water(1000), true).amount == 1000 && waterAmount() == 8000,
            "simulated native fluid extraction does not consume fluid");
        require(source.takeFluid(water(1000), false).amount == 1000 && waterAmount() == 7000,
            "terminal extracts exactly one bucket from the native fluid cell");
        source.giveFluid(water(1000), false);
        require(waterAmount() == 8000, "terminal returns fluid to the physical ME fluid cell");
        source.giveFluid(water(500), true);
        require(waterAmount() == 8000, "simulated fluid insertion leaves storage unchanged");

        ItemStack anotherDimension = terminal.copy();
        NBTTagCompound address = anotherDimension.getTagCompound().getCompoundTag("BoundAE");
        address.setInteger("Dimension", world.provider.dimensionId + 1);
        TerminalItemSource foreignSource = new TerminalItemSource(anotherDimension, player);
        require(
            foreignSource.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == 64,
            "binding address from another dimension cannot extract items");
        require(foreignSource.takeFluid(water(1000), false) == null && waterAmount() == 8000,
            "binding address from another dimension cannot extract fluids");
        TerminalSettings.setAe(terminal, false);
        require(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty() && ironAmount() == 64,
            "disabled AE mode does not access a bound network");
        TerminalSettings.setAe(terminal, true);
    }

    private void verifyUnpoweredStorage() {
        require(!grid().<IEnergyGrid>getCache(IEnergyGrid.class).isNetworkPowered() && !ready(),
            "removing the real power cell deactivates the bound node");
        ItemStack itemCell = drive.getInternalInventory().getStackInSlot(0).copy();
        ItemStack fluidCell = drive.getInternalInventory().getStackInSlot(1).copy();
        require(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty(),
            "unpowered bound network cannot supply material");
        require(source.takeFluid(water(1000), false) == null, "unpowered bound network cannot supply fluid");
        source.give(new ItemStack(Items.gold_ingot, 6));
        require(inventoryItems(player, Items.gold_ingot) == 6,
            "item return falls back to player inventory while the node is unpowered");
        require(
            ItemStack.areItemStacksEqual(itemCell, drive.getInternalInventory().getStackInSlot(0))
                && ItemStack.areItemStacksEqual(fluidCell, drive.getInternalInventory().getStackInSlot(1)),
            "denied offline extraction and insertion preserve both physical cell stacks and their complete NBT");
        clearFallbackInventory(player);
    }

    private void verifySecurity(ISecurityGrid permissions) {
        require(
            permissions.hasPermission(player, SecurityPermissions.EXTRACT)
                && permissions.hasPermission(player, SecurityPermissions.INJECT),
            "real security terminal grants its owner extraction and insertion");
        EntityPlayerMP stranger = new EntityPlayerMP(server, world,
            new GameProfile(UUID.fromString("9c5a13ae-e21b-46f8-a7aa-87078854971c"), "TerminalUnauthorizedQA"),
            new ItemInWorldManager(world));
        new NetHandlerPlayServer(server, new NetworkManager(false), stranger);
        stranger.setGameType(WorldSettings.GameType.SURVIVAL);
        stranger.setPosition(X, Y, Z + 3);
        require(
            !permissions.hasPermission(stranger, SecurityPermissions.EXTRACT)
                && !permissions.hasPermission(stranger, SecurityPermissions.INJECT),
            "real security cache denies an unknown player without a biometric card");
        ItemStack fresh = MultiblockTerminalServerChecks.terminal();
        TerminalSettings.setAe(fresh, true);
        require(!TerminalItemSource.bind(fresh, stranger, world, X - 1, Y, Z, ForgeDirection.UNKNOWN),
            "unauthorized player cannot bind to the protected physical network");
        ItemStack copiedBinding = terminal.copy();
        stranger.inventory.mainInventory[0] = copiedBinding;
        TerminalItemSource denied = new TerminalItemSource(copiedBinding, stranger);
        require(denied.take(stack -> stack.getItem() == Items.iron_ingot, false, 4).isEmpty() && ironAmount() == 64,
            "copied binding still checks the current player's extraction permission");
        require(denied.takeFluid(water(1000), false) == null && waterAmount() == 8000,
            "unauthorized copied binding cannot extract stored fluid");
        denied.give(iron(7));
        require(inventoryItems(stranger, Items.iron_ingot) == 7 && ironAmount() == 64,
            "denied insertion returns items to the player without altering protected storage");
        denied.giveFluid(water(250), false);
        require(packetWater(stranger) == 250 && waterAmount() == 8000,
            "denied fluid insertion creates the native fluid packet without altering protected storage");
        require(amount(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1)) == 1 && ironAmount() == 63,
            "authorized owner can still extract after protection is enabled");
        source.give(iron(1));
        require(ironAmount() == 64, "authorized owner can still return material after protection is enabled");
    }

    private void verifyRemovedNode() {
        world.setBlockToAir(X - 1, Y, Z);
        require(world.getTileEntity(X - 1, Y, Z) == null, "bound physical drive node is removed");
        require(source.take(stack -> stack.getItem() == Items.iron_ingot, false, 1).isEmpty(),
            "stale binding cannot extract from the removed drive");
        require(source.takeFluid(water(1000), false) == null,
            "stale binding cannot extract fluids from the removed drive");
        require(!TerminalItemSource.bind(terminal.copy(), player, world, X - 1, Y, Z, ForgeDirection.UNKNOWN),
            "removed node cannot be bound again");
    }

    private void placePower() {
        world.setBlock(X + 1, Y, Z, AEApi.instance().definitions().blocks().energyCellCreative().maybeBlock().get(), 0,
            3);
        TileEntity tile = world.getTileEntity(X + 1, Y, Z);
        if (tile instanceof AENetworkTile networkTile) networkTile.getProxy().setOwner(player);
    }

    private void keepFixtureLoaded() {
        for (int chunkX = (X - 1) >> 4; chunkX <= (X + 1) >> 4; chunkX++)
            for (int chunkZ = (Z - 1) >> 4; chunkZ <= Z >> 4; chunkZ++)
                world.theChunkProviderServer.loadChunk(chunkX, chunkZ);
    }

    private boolean ready() {
        IGridNode node = drive.getActionableNode();
        return node != null && node.isActive();
    }

    private IGrid grid() {
        return drive.getActionableNode().getGrid();
    }

    private IMEMonitor<IAEItemStack> items() {
        return grid().<IStorageGrid>getCache(IStorageGrid.class).getItemInventory();
    }

    private IMEMonitor<IAEFluidStack> fluids() {
        return grid().<IStorageGrid>getCache(IStorageGrid.class).getFluidInventory();
    }

    private long ironAmount() {
        IAEItemStack stored = items().getStorageList().findPrecise(AEItemStack.create(iron(1)));
        return stored == null ? 0 : stored.getStackSize();
    }

    private long goldAmount() {
        IAEItemStack stored = items().getStorageList().findPrecise(AEItemStack.create(new ItemStack(Items.gold_ingot)));
        return stored == null ? 0 : stored.getStackSize();
    }

    private long waterAmount() {
        IAEFluidStack stored = fluids().getStorageList().findPrecise(AEFluidStack.create(water(1)));
        return stored == null ? 0 : stored.getStackSize();
    }

    private static ItemStack iron(int count) {
        return new ItemStack(Items.iron_ingot, count);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(FluidRegistry.WATER, amount);
    }

    private static int amount(Map<ItemStack, Integer> stacks) {
        return stacks.values().stream().mapToInt(Integer::intValue).sum();
    }

    private static int inventoryItems(EntityPlayerMP player, Item item) {
        int result = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.getItem() == item) result += stack.stackSize;
        return result;
    }

    private static long packetWater(EntityPlayerMP player) {
        long result = 0;
        for (ItemStack stack : player.inventory.mainInventory) {
            if (stack == null) continue;
            var packet = ItemMEStackPacket.toAEStack(stack);
            if (packet instanceof IAEFluidStack fluid && fluid.isSameType(AEFluidStack.create(water(1))))
                result += fluid.getStackSize();
        }
        return result;
    }

    private static void clearFallbackInventory(EntityPlayerMP player) {
        for (int slot = 1; slot < player.inventory.mainInventory.length; slot++)
            player.inventory.mainInventory[slot] = null;
    }

    private void restoreInventory() {
        System.arraycopy(savedInventory, 0, player.inventory.mainInventory, 0, savedInventory.length);
        player.inventory.currentItem = savedSlot;
        player.inventory.markDirty();
    }

    private void advance(int next) {
        stage = next;
        stageStarted = ticks;
    }

    private void require(boolean condition, String description) {
        if (!condition) throw new AssertionError(description);
        checks++;
        System.out.println("TERMINAL_STORAGE_QA: PASS " + description);
    }
}
