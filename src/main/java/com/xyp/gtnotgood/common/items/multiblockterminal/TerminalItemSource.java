package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.gtnewhorizon.gtnhlib.util.map.ItemStackMap;
import com.gtnewhorizon.structurelib.structure.IItemSource;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyGrid;
import appeng.api.networking.energy.IEnergySource;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.security.PlayerSource;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.parts.IPart;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStackType;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.items.misc.ItemMEStackPacket;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import baubles.api.BaublesApi;
import baubles.common.container.InventoryBaubles;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import it.unimi.dsi.fastutil.objects.ObjectLongPair;

public final class TerminalItemSource implements IItemSource {

    private static final String BOUND_AE = "BoundAE";
    private final ItemStack terminal;
    private final EntityPlayerMP player;
    private final IItemSource playerSource;
    private final PlayerSource actionSource;
    private WirelessAccess wirelessAccess;

    public TerminalItemSource(ItemStack terminal, EntityPlayerMP player) {
        this.terminal = terminal;
        this.player = player;
        this.playerSource = IItemSource.fromPlayer(player);
        this.actionSource = new PlayerSource(player, null);
    }

    @Override
    @Nonnull
    public Map<ItemStack, Integer> take(Predicate<ItemStack> predicate, boolean simulate, int count) {
        if (predicate == null) throw new IllegalArgumentException();
        Map<ItemStack, Integer> result = new ItemStackMap<>(true);
        if (count <= 0) return result;

        Connection connection = connection(SecurityPermissions.EXTRACT);
        int remaining = count;
        if (connection != null) {
            IGrid grid = connection.grid();
            var inventory = grid.<IStorageGrid>getCache(IStorageGrid.class).getItemInventory();
            IEnergySource energy = connection.energy();
            int powerBudget = simulate
                ? (int) Math.min(count, energy.extractAEPower(count, Actionable.SIMULATE, PowerMultiplier.CONFIG) + 0.9)
                : count;
            List<IAEItemStack> candidates = new ArrayList<>();
            for (IAEItemStack available : inventory.getStorageList()) {
                if (available.getStackSize() > 0 && predicate.test(available.getItemStack()))
                    candidates.add(available.copy());
            }
            for (IAEItemStack available : candidates) {
                if (powerBudget <= 0) break;
                IAEItemStack request = available
                    .setStackSize(Math.min(Math.min(remaining, powerBudget), available.getStackSize()));
                IAEItemStack extracted = Platform.poweredExtraction(energy, inventory, request, actionSource,
                    simulate ? Actionable.SIMULATE : Actionable.MODULATE);
                if (extracted != null && extracted.getStackSize() > 0) {
                    int amount = (int) Math.min(remaining, extracted.getStackSize());
                    add(result, extracted.getItemStack(), amount);
                    remaining -= amount;
                    powerBudget -= amount;
                }
                if (remaining == 0) return result;
            }
        }
        for (var entry : playerSource.take(predicate, simulate, remaining).entrySet()) {
            add(result, entry.getKey(), entry.getValue());
            remaining -= entry.getValue();
        }
        if (remaining > 0 && ModList.Forestry.isModLoaded())
            ForestryBackpacks.take(player, predicate, simulate, remaining, result);
        return result;
    }

    @Override
    public boolean takeOne(ItemStack stack, boolean simulate) {
        if (stack == null || stack.getItem() == null || stack.stackSize != 1) throw new IllegalArgumentException();
        Connection connection = connection(SecurityPermissions.EXTRACT);
        if (connection != null) {
            IGrid grid = connection.grid();
            var inventory = grid.<IStorageGrid>getCache(IStorageGrid.class).getItemInventory();
            IAEItemStack extracted = Platform.poweredExtraction(connection.energy(), inventory,
                AEItemStack.create(stack), actionSource, simulate ? Actionable.SIMULATE : Actionable.MODULATE);
            if (extracted != null && extracted.getStackSize() > 0) return true;
        }
        if (playerSource.takeOne(stack, simulate)) return true;
        return ModList.Forestry.isModLoaded() && ForestryBackpacks.take(player,
            available -> available.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(available, stack), simulate, 1,
            new ItemStackMap<>(true)) == 0;
    }

    public void give(ItemStack stack) {
        if (stack == null || stack.getItem() == null || stack.stackSize <= 0) return;
        ItemStack remainder = stack.copy();
        Connection connection = connection(SecurityPermissions.INJECT);
        if (connection != null) {
            IGrid grid = connection.grid();
            var inventory = grid.<IStorageGrid>getCache(IStorageGrid.class).getItemInventory();
            IAEItemStack rest = Platform.poweredInsert(connection.energy(), inventory, AEItemStack.create(remainder),
                actionSource);
            remainder = rest == null ? null : rest.getItemStack();
        }
        if (remainder != null && remainder.stackSize > 0) {
            if (ModList.Forestry.isModLoaded()) ForestryBackpacks.stow(player, remainder);
            player.inventory.addItemStackToInventory(remainder);
            if (remainder.stackSize > 0) player.dropPlayerItemWithRandomChoice(remainder, false);
        }
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
    }

    public FluidStack takeFluid(FluidStack resource, boolean simulate) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) return null;
        IAEFluidStack request = AEFluidStack.create(resource);
        Connection connection = connection(SecurityPermissions.EXTRACT);
        if (connection != null) {
            IGrid grid = connection.grid();
            var inventory = grid.<IStorageGrid>getCache(IStorageGrid.class).getFluidInventory();
            IEnergySource energy = connection.energy();
            IAEFluidStack available = Platform.poweredExtraction(energy, inventory, request, actionSource,
                Actionable.SIMULATE);
            if (available != null && available.getStackSize() >= resource.amount) {
                if (simulate) return resource.copy();
                IAEFluidStack extracted = Platform.poweredExtraction(energy, inventory, request, actionSource);
                return extracted == null ? null : extracted.getFluidStack();
            }
        }

        ItemStack packet = takeOne(stack -> {
            var stored = ItemMEStackPacket.toAEStack(stack);
            return stored instanceof IAEFluidStack fluid && fluid.isSameType(request)
                && fluid.getStackSize() >= resource.amount;
        }, true);
        if (packet != null) {
            if (simulate) return resource.copy();
            if (!takeOne(packet, false)) return null;
            IAEFluidStack remainder = (IAEFluidStack) ItemMEStackPacket.toAEStack(packet);
            remainder.setStackSize(remainder.getStackSize() - resource.amount);
            if (remainder.getStackSize() > 0) Platform.handleLeftover(player, remainder);
            return resource.copy();
        }
        IAEStackType<IAEFluidStack> type = request.getStackType();
        ItemStack container = takeOne(stack -> drains(type, stack, request) != null, true);
        if (container == null) return null;
        ObjectLongPair<ItemStack> drained = drains(type, container, request);
        if (drained == null) return null;
        if (simulate) return resource.copy();
        if (!takeOne(container, false)) return null;
        give(drained.left());
        return resource.copy();
    }

    public int giveFluid(FluidStack resource, boolean simulate) {
        if (resource == null || resource.getFluid() == null || resource.amount <= 0) return 0;
        if (simulate) return resource.amount;
        IAEFluidStack remainder = AEFluidStack.create(resource);
        Connection connection = connection(SecurityPermissions.INJECT);
        if (connection != null) {
            IGrid grid = connection.grid();
            var inventory = grid.<IStorageGrid>getCache(IStorageGrid.class).getFluidInventory();
            remainder = Platform.poweredInsert(connection.energy(), inventory, remainder, actionSource);
        }
        if (remainder != null && remainder.getStackSize() > 0) {
            IAEFluidStack requested = remainder;
            IAEStackType<IAEFluidStack> type = requested.getStackType();
            ItemStack container = takeOne(stack -> fills(type, stack, requested) != null, true);
            ObjectLongPair<ItemStack> filled = container == null ? null : fills(type, container, requested);
            if (filled != null && takeOne(container, false)) give(filled.left());
            else Platform.handleLeftover(player, remainder);
        }
        return resource.amount;
    }

    private static ObjectLongPair<ItemStack> drains(IAEStackType<IAEFluidStack> type, ItemStack stack,
        IAEFluidStack request) {
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (!type.isContainerItemForType(single)) return null;
        IAEFluidStack contained = type.getStackFromContainerItem(single);
        if (contained == null || !contained.isSameType(request) || contained.getStackSize() < request.getStackSize())
            return null;
        ObjectLongPair<ItemStack> drained = type.drainStackFromContainer(single, request.copy());
        return drained.left() != null && drained.rightLong() == request.getStackSize() ? drained : null;
    }

    private static ObjectLongPair<ItemStack> fills(IAEStackType<IAEFluidStack> type, ItemStack stack,
        IAEFluidStack request) {
        ItemStack single = stack.copy();
        single.stackSize = 1;
        if (!type.isContainerItemForType(single)) return null;
        ObjectLongPair<ItemStack> filled = type.fillContainer(single, request.copy());
        if (filled.left() == null || filled.rightLong() != request.getStackSize()) return null;
        IAEFluidStack actual = type.getStackFromContainerItem(filled.left());
        return actual != null && actual.isSameType(request) && actual.getStackSize() >= request.getStackSize() ? filled
            : null;
    }

    public static boolean bind(ItemStack terminal, EntityPlayerMP player, World world, int x, int y, int z,
        ForgeDirection side) {
        if (
            terminal == null || world == null
                || world.isRemote
                || player.worldObj != world
                || !world.blockExists(x, y, z)
        ) return false;
        ForgeDirection requestedSide = side == null ? ForgeDirection.UNKNOWN : side;
        IGridNode node = node(world.getTileEntity(x, y, z), requestedSide);
        if (node == null || !node.isActive()) return false;
        IGrid grid = node.getGrid();
        if (
            grid == null || !(permitted(grid, player, SecurityPermissions.EXTRACT)
                || permitted(grid, player, SecurityPermissions.INJECT))
        ) return false;

        NBTTagCompound address = new NBTTagCompound();
        address.setInteger("Dimension", world.provider.dimensionId);
        address.setInteger("X", x);
        address.setInteger("Y", y);
        address.setInteger("Z", z);
        address.setInteger("Side", requestedSide.ordinal());
        if (!terminal.hasTagCompound()) terminal.setTagCompound(new NBTTagCompound());
        terminal.getTagCompound().setTag(BOUND_AE, address);
        // #tr item.gtnotgood.multiblock_terminal.ae_bound
        // # Bound to ME node at %s, %s, %s
        // # zh_CN 已绑定 ME 节点：%s，%s，%s
        player.addChatMessage(new ChatComponentTranslation("item.gtnotgood.multiblock_terminal.ae_bound", x, y, z));
        player.inventory.markDirty();
        player.openContainer.detectAndSendChanges();
        return true;
    }

    private Connection connection(SecurityPermissions permission) {
        if (!TerminalSettings.ae(terminal)) return null;
        IGrid bound = boundGrid(permission);
        if (bound != null) return new Connection(bound, bound.getCache(IEnergyGrid.class));
        Connection cached = wirelessAccess == null ? null : wirelessAccess.connection(permission);
        if (cached != null) return cached;
        Connection connection = wireless(player.inventory, permission);
        if (connection != null) return connection;
        return ModList.Baubles.isModLoaded() ? wireless(BaublesApi.getBaubles(player), permission) : null;
    }

    private Connection wireless(IInventory inventory, SecurityPermissions permission) {
        if (inventory == null) return null;
        for (int slot = 0; slot < inventory.getSizeInventory(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack == null || stack.getItem() == null || stack.stackSize <= 0) continue;
            IWirelessTermHandler handler = AEApi.instance().registries().wireless().getWirelessTerminalHandler(stack);
            if (handler == null) continue;
            WirelessAccess access = new WirelessAccess(inventory, slot, stack, handler);
            Connection connection = access.connection(permission);
            if (connection != null) {
                wirelessAccess = access;
                return connection;
            }
        }
        return null;
    }

    private IGrid boundGrid(SecurityPermissions permission) {
        if (!terminal.hasTagCompound()) return null;
        NBTTagCompound address = terminal.getTagCompound().getCompoundTag(BOUND_AE);
        World world = player.worldObj;
        if (
            !terminal.getTagCompound().hasKey(BOUND_AE, 10)
                || address.getInteger("Dimension") != world.provider.dimensionId
        ) return null;
        int x = address.getInteger("X"), y = address.getInteger("Y"), z = address.getInteger("Z");
        if (!world.blockExists(x, y, z)) return null;
        IGridNode node = node(world.getTileEntity(x, y, z), ForgeDirection.getOrientation(address.getInteger("Side")));
        if (node == null || !node.isActive()) return null;
        IGrid grid = node.getGrid();
        return grid != null && permitted(grid, player, permission) ? grid : null;
    }

    private static boolean permitted(IGrid grid, EntityPlayerMP player, SecurityPermissions permission) {
        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        return security != null && security.hasPermission(player, permission);
    }

    private static IGridNode node(TileEntity tile, ForgeDirection side) {
        IPart part = Platform.getPartFromTE(tile, side);
        if (part != null && part.getGridNode() != null) return part.getGridNode();
        if (tile instanceof IGregTechTileEntity gtTile) {
            IMetaTileEntity machine = gtTile.getMetaTileEntity();
            if (machine instanceof IGridHost host) {
                IGridNode node = node(host, side);
                if (node != null) return node;
            }
        }
        return tile instanceof IGridHost host ? node(host, side) : null;
    }

    private static IGridNode node(IGridHost host, ForgeDirection side) {
        IGridNode node = host.getGridNode(side);
        return node != null || side == ForgeDirection.UNKNOWN ? node : host.getGridNode(ForgeDirection.UNKNOWN);
    }

    private static void add(Map<ItemStack, Integer> result, ItemStack stack, int amount) {
        ItemStack key = stack.copy();
        key.stackSize = 1;
        result.merge(key, amount, Integer::sum);
    }

    private record Connection(IGrid grid, IEnergySource energy) {}

    private final class WirelessAccess {

        private final IInventory inventory;
        private final int slot;
        private final ItemStack stack;
        private final IWirelessTermHandler handler;
        private WirelessTerminalGuiObject host;
        private String linkedKey;

        private WirelessAccess(IInventory inventory, int slot, ItemStack stack, IWirelessTermHandler handler) {
            this.inventory = inventory;
            this.slot = slot;
            this.stack = stack;
            this.handler = handler;
        }

        private Connection connection(SecurityPermissions permission) {
            if (inventory.getStackInSlot(slot) != stack) return null;
            String key = handler.getEncryptionKey(stack);
            long encryptionKey;
            try {
                encryptionKey = Long.parseLong(key);
            } catch (NumberFormatException exception) {
                return null;
            }
            Object locatable = AEApi.instance().registries().locatable().getLocatableBy(encryptionKey);
            if (!(locatable instanceof IGridHost gridHost)) return null;
            IGridNode node = gridHost.getGridNode(ForgeDirection.UNKNOWN);
            if (node == null || !node.isActive()) return null;
            if (
                !AEApi.instance().registries().wireless().checkRange(stack, player)
                    || !handler.hasInfinityPower(stack) && !handler.hasPower(player, 0.5, stack)
            ) return null;
            if (host == null || !key.equals(linkedKey) || host.getGrid() != node.getGrid()) {
                host = new WirelessTerminalGuiObject(handler, stack, player, player.worldObj, slot, 0, 0);
                linkedKey = key;
            }
            IGrid grid = host.getGrid();
            if (grid == null || !permitted(grid, player, permission)) return null;
            return new Connection(grid, (amount, mode, multiplier) -> {
                double extracted = host.extractAEPower(amount, mode, multiplier);
                if (mode == Actionable.MODULATE && extracted > 0) {
                    inventory.markDirty();
                    player.inventory.markDirty();
                    if (
                        !handler.hasInfinityPower(stack) && ModList.Baubles.isModLoaded()
                            && inventory instanceof InventoryBaubles baubles
                    ) baubles.syncSlotToClients(slot);
                }
                return extracted;
            });
        }
    }

    private static final class ForestryBackpacks {

        private static final BackpackApi API = api();

        private static BackpackApi api() {
            try {
                Class<?> backpack = Class.forName("forestry.storage.items.ItemBackpack");
                return new BackpackApi(
                    backpack.getMethod("tryStowing", EntityPlayer.class, ItemStack.class, ItemStack.class),
                    backpack.getMethod("getBackpackSize"), backpack.getMethod("getMode", ItemStack.class),
                    Class.forName("forestry.storage.inventory.ItemInventoryBackpack").getConstructor(EntityPlayer.class,
                        int.class, ItemStack.class));
            } catch (ReflectiveOperationException | LinkageError exception) {
                GTNotGood.LOG.warn("Forestry backpack access is unavailable", exception);
                return null;
            }
        }

        private static void stow(EntityPlayerMP player, ItemStack stack) {
            if (API == null) return;
            for (ItemStack held : player.inventory.mainInventory) {
                if (stack.stackSize <= 0) return;
                if (isBackpack(held)) {
                    try {
                        API.stow().invoke(null, player, held, stack);
                    } catch (ReflectiveOperationException exception) {
                        GTNotGood.LOG.warn("Forestry backpack insertion failed", exception);
                        return;
                    }
                }
            }
        }

        private static int take(EntityPlayerMP player, Predicate<ItemStack> predicate, boolean simulate, int remaining,
            Map<ItemStack, Integer> result) {
            if (API == null) return remaining;
            for (ItemStack held : player.inventory.mainInventory) {
                if (remaining <= 0) break;
                if (!isBackpack(held)) continue;
                try {
                    String mode = ((Enum<?>) API.mode().invoke(null, held)).name();
                    if (!mode.equals("NORMAL") && !mode.equals("RESUPPLY")) continue;
                    // The public inventory constructor assigns a UID and can migrate legacy NBT even when only read.
                    ItemStack parent = simulate ? held.copy() : held;
                    IInventory inventory = (IInventory) API.inventory().newInstance(player,
                        (Integer) API.size().invoke(held.getItem()), parent);
                    boolean changed = false;
                    for (int slot = 0; slot < inventory.getSizeInventory() && remaining > 0; slot++) {
                        ItemStack available = inventory.getStackInSlot(slot);
                        if (available == null || available.stackSize <= 0 || !predicate.test(available)) continue;
                        int amount = Math.min(remaining, available.stackSize);
                        if (simulate) {
                            add(result, available, amount);
                        } else {
                            ItemStack extracted = inventory.decrStackSize(slot, amount);
                            if (extracted == null) continue;
                            amount = extracted.stackSize;
                            add(result, extracted, amount);
                            changed = true;
                        }
                        remaining -= amount;
                    }
                    if (changed) {
                        inventory.markDirty();
                        player.inventory.markDirty();
                    }
                } catch (ReflectiveOperationException exception) {
                    GTNotGood.LOG.warn("Forestry backpack extraction failed", exception);
                    return remaining;
                }
            }
            return remaining;
        }

        private static boolean isBackpack(ItemStack stack) {
            return stack != null && stack.stackSize > 0 && API.stow().getDeclaringClass().isInstance(stack.getItem());
        }

        private record BackpackApi(Method stow, Method size, Method mode, Constructor<?> inventory) {}
    }
}
