// SPDX-License-Identifier: LGPL-3.0-only
// Adapted from GT-Not-Leisure PR #627 at 651396c75d108e6ff5399a44c56c12214ad27d3d.

package com.xyp.gtnotgood.common.wirelessfrequency;

import static com.gtnewhorizon.gtnhlib.capability.Capabilities.getCapability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.IChatComponent;
import net.minecraftforge.common.util.ForgeDirection;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.common.blocks.stockio.TileStockIOInterface;
import com.xyp.gtnotgood.common.parts.advancedio.PartAdvancedIOBus;
import com.xyp.gtnotgood.common.parts.stockio.PartStockIOInterface;

import appeng.api.AEApi;
import appeng.api.crafting.ICraftingIconProvider;
import appeng.api.implementations.tiles.ICraftingMachine;
import appeng.api.interfaces.IInterfaceNameProvider;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IStorageBus;
import appeng.api.util.IInterfaceViewable;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import appeng.util.InventoryAdaptor;
import appeng.util.inv.AdaptorConduitBandle;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;

public class WirelessInterfaceView {

    private final Map<IInterfaceHost, List<MTEHatch>> targets = new IdentityHashMap<>();
    private final Map<Object, Name> targetNames = new IdentityHashMap<>();
    private final Map<IWirelessLinkView, List<Name>> names = new IdentityHashMap<>();

    @Desugar
    record Name(String rawName, String suffix, ItemStack icon) {

        Name(String rawName, IChatComponent suffix, ItemStack icon) {
            this(rawName, suffix == null ? "" : IChatComponent.Serializer.func_150696_a(suffix), icon);
        }

    }

    List<MTEHatch> hatches(IInterfaceHost host) {
        if (!targets.containsKey(host)) targets.put(host, readHatches(host));
        return targets.get(host);
    }

    private static List<MTEHatch> readHatches(IInterfaceHost host) {
        var tile = host.getTileEntity();
        if (tile == null || tile.isInvalid() || tile.getWorldObj() == null) return null;
        var world = tile.getWorldObj();
        List<MTEHatch> hatches = new ArrayList<>();
        for (ForgeDirection side : host.getTargets()) {
            if (side == ForgeDirection.UNKNOWN) continue;
            int x = tile.xCoord + side.offsetX, y = tile.yCoord + side.offsetY, z = tile.zCoord + side.offsetZ;
            if (!world.blockExists(x, y, z)) return null;
            var target = world.getTileEntity(x, y, z);
            if (target != null && target.isInvalid()) return null;
            // AE checks all horizontal neighbors when resolving double chests.
            if (target instanceof TileEntityChest && !world.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1))
                return null;
            if (target instanceof IGregTechTileEntity base && base.getMetaTileEntity() instanceof MTEHatch hatch)
                hatches.add(hatch);
        }
        return hatches;
    }

    List<Name> names(IWirelessLinkView link) {
        return names.computeIfAbsent(link, this::readNames);
    }

    ItemStack displayIcon(Object host) {
        Object machine = host instanceof IGregTechTileEntity base ? base.getMetaTileEntity() : host;
        if (machine instanceof MTEHatch) return null;
        Name name = targetName(host);
        return name == null ? null : name.icon();
    }

    private Name targetName(Object machine) {
        if (!(machine instanceof IInterfaceViewable) && machine instanceof IGregTechTileEntity base)
            machine = base.getMetaTileEntity();
        if (!targetNames.containsKey(machine)) targetNames.put(machine, readTargetName(machine));
        return targetNames.get(machine);
    }

    private Name readTargetName(Object machine) {
        if (machine instanceof IInterfaceHost host) {
            return ((WirelessInterfaceNameOverride) host.getInterfaceDuality())
                .wirelessFrequency$withoutCustomName(() -> readInterfaceName(host));
        }
        if (machine instanceof IInterfaceViewable host) return readInterfaceName(host);
        if (machine instanceof IStorageBus bus && machine instanceof AEBasePart part) return readStorageName(bus, part);
        if (machine instanceof TileStockIOInterface tile) return readMachineName(tile.getTarget());
        if (machine instanceof PartStockIOInterface || machine instanceof PartAdvancedIOBus) {
            AEBasePart part = (AEBasePart) machine;
            var tile = part.getTile();
            ForgeDirection side = part.getSide();
            if (
                tile == null || tile.isInvalid()
                    || tile.getWorldObj() == null
                    || side == null
                    || side == ForgeDirection.UNKNOWN
            ) return null;
            int x = tile.xCoord + side.offsetX, y = tile.yCoord + side.offsetY, z = tile.zCoord + side.offsetZ;
            return tile.getWorldObj().blockExists(x, y, z) ? readMachineName(tile.getWorldObj().getTileEntity(x, y, z))
                : null;
        }
        return null;
    }

    private static Name readMachineName(TileEntity target) {
        if (
            target == null || target.isInvalid()
                || !(target instanceof IGregTechTileEntity base)
                || base.getMetaTileEntity() == null
        ) return null;
        ItemStack icon = base.getMetaTileEntity().getStackForm(1);
        if (icon == null || icon.getItem() == null) return null;
        var nameProvider = getCapability(target, IInterfaceNameProvider.class);
        return new Name(icon.getUnlocalizedName(), nameProvider == null ? null : nameProvider.getInterfaceNameSuffix(),
            icon);
    }

    private List<Name> readNames(IWirelessLinkView link) {
        Set<Object> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        var names = new TreeMap<String, Name>();
        for (var node : link.members()) {
            Object machine = node.getMachine();
            if (!(machine instanceof IInterfaceViewable) && machine instanceof IGregTechTileEntity base)
                machine = base.getMetaTileEntity();
            if (!seen.add(machine)) continue;
            Name name = targetName(machine);
            if (name != null) names.putIfAbsent(name.rawName() + '\0' + name.suffix(), name);
        }
        return Collections.unmodifiableList(new ArrayList<>(names.values()));
    }

    private Name readInterfaceName(IInterfaceViewable host) {
        var tile = host.getTileEntity();
        if (tile == null || tile.isInvalid() || tile.getWorldObj() == null) return null;
        if (
            host instanceof IInterfaceHost iface
                && (hatches(iface) == null || !canReadIcons(iface) || !hasSingleTarget(iface))
        ) return null;
        ItemStack icon = host.getDisplayRep();
        // An interface without a target reports "Nothing"; retain its normal device label instead.
        if (host instanceof IInterfaceHost && (icon == null || icon.getItem() == null)) return null;
        if (icon == null) icon = host.getSelfRep();
        String rawName = host.getRawName();
        return rawName == null || rawName.isEmpty() ? null : new Name(rawName, host.getNameSuffix(), icon);
    }

    private static boolean hasSingleTarget(IInterfaceHost host) {
        var tile = host.getTileEntity();
        int targets = 0;
        for (ForgeDirection side : host.getTargets()) {
            if (side == ForgeDirection.UNKNOWN) continue;
            var target = tile.getWorldObj().getTileEntity(tile.xCoord + side.offsetX, tile.yCoord + side.offsetY,
                tile.zCoord + side.offsetZ);
            if (!isInterfaceTarget(target, side.getOpposite())) continue;
            if (++targets > 1) return false;
        }
        return targets == 1;
    }

    private static boolean isInterfaceTarget(TileEntity target, ForgeDirection side) {
        if (target == null || target.isInvalid() || target instanceof IInterfaceHost) return false;
        var provider = getCapability(target, ICraftingIconProvider.class);
        if (provider != null && provider.getMachineCraftingIcon() != null) return true;
        var adaptor = InventoryAdaptor.getAdaptor(target, side);
        if (adaptor instanceof AdaptorConduitBandle || target instanceof BaseMetaPipeEntity) return false;
        if (!(target instanceof ICraftingMachine) && adaptor == null) return false;
        if (target instanceof IInventory inventory && inventory.getSizeInventory() == 0) return false;
        if (target instanceof ISidedInventory inventory) {
            int[] slots = inventory.getAccessibleSlotsFromSide(side.ordinal());
            if (slots == null || slots.length == 0) return false;
        }
        return true;
    }

    private static Name readStorageName(IStorageBus bus, AEBasePart part) {
        var host = part.getHost();
        var tile = host == null ? null : host.getTile();
        ForgeDirection side = part.getSide();
        if (
            tile == null || tile.isInvalid()
                || tile.getWorldObj() == null
                || side == null
                || side == ForgeDirection.UNKNOWN
        ) return null;
        var world = tile.getWorldObj();
        int x = tile.xCoord + side.offsetX, y = tile.yCoord + side.offsetY, z = tile.zCoord + side.offsetZ;
        if (!world.blockExists(x, y, z)) return null;
        var target = world.getTileEntity(x, y, z);
        if (target == null || target.isInvalid()) return null;
        if (target instanceof TileEntityChest && !world.checkChunksExist(x - 1, y, z - 1, x + 1, y, z + 1)) return null;
        // Select AE's adapter without constructing an inventory or resetting the bus's live storage handler.
        if (
            AEApi.instance().registries().externalStorage().getHandler(target, side.getOpposite(), bus.getStackType(),
                new MachineSource(part)) == null
        ) return null;
        var iconProvider = getCapability(target, ICraftingIconProvider.class);
        ItemStack icon = iconProvider == null ? null : iconProvider.getMachineCraftingIcon();
        if (icon == null || icon.getItem() == null) {
            var block = world.getBlock(x, y, z);
            icon = new ItemStack(block, 1, block.getDamageValue(world, x, y, z));
        }
        if (icon.getItem() == null) return null;
        var nameProvider = getCapability(target, IInterfaceNameProvider.class);
        return new Name(icon.getUnlocalizedName(), nameProvider == null ? null : nameProvider.getInterfaceNameSuffix(),
            icon);
    }

    private static boolean canReadIcons(IInterfaceHost host) {
        var tile = host.getTileEntity();
        for (ForgeDirection side : host.getTargets()) {
            int x = tile.xCoord + side.offsetX * 2, z = tile.zCoord + side.offsetZ * 2;
            // AE's fallback icon ray trace extends one block beyond the already checked neighbor.
            if (
                ((x >> 4) != ((x - side.offsetX) >> 4) || (z >> 4) != ((z - side.offsetZ) >> 4))
                    && !tile.getWorldObj().blockExists(x, tile.yCoord + side.offsetY * 2, z)
            ) return false;
        }
        return true;
    }
}
