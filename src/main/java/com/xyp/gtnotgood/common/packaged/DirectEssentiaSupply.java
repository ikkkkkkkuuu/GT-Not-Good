// SPDX-License-Identifier: LGPL-3.0-only
// New GTNG TC4 integration (c) 2026 GTNG contributors.
package com.xyp.gtnotgood.common.packaged;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import com.xyp.gtnotgood.utils.enums.ModList;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.api.aspects.AspectList;
import thaumcraft.common.tiles.TileInfusionMatrix;

/**
 * Persistent, job-scoped source routing for real TC4 consumption calls. No source scan or chunk loading is needed.
 * A missing/unpowered Provider pauses direct supply; it never silently consumes nearby jars instead.
 */
public final class DirectEssentiaSupply {

    public static final String TAG = "GTNGDirectEssentia";

    private DirectEssentiaSupply() {}

    /**
     * After TC4 consumes one successfully supplied unit, consume at most the configured extra units of that aspect.
     * The original cycle remains responsible for pedestal consumption, instability, duration and completion.
     * 
     * @return TC4's original balance-reduction result
     */
    public static boolean reduceBatch(TileInfusionMatrix matrix, AspectList remaining, Aspect aspect, int amount) {
        boolean reduced = remaining.reduce(aspect, amount);
        if (!reduced || amount != 1 || !(matrix instanceof InfusionSourceAccess access)) return reduced;
        NBTTagCompound route = access.gtnotgood$getEssentiaSource();
        var world = matrix.getWorldObj();
        if (route == null || world == null || world.isRemote) return reduced;
        int x = route.getInteger("X"), z = route.getInteger("Z");
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) return reduced;
        if (!(world.getTileEntity(x, route.getInteger("Y"), z) instanceof TilePackagedProvider provider))
            return reduced;
        for (int i = 1; i < Math.min(32, provider.essentiaSpeed) && remaining.getAmount(aspect) > 0; i++) {
            if (!Boolean.TRUE.equals(drain(matrix, aspect))) break;
            remaining.reduce(aspect, 1);
        }
        return reduced;
    }

    /** Attach only after TC4 accepts a new job. The matrix mixin persists the reference across world reloads. */
    public static void attach(TileInfusionMatrix matrix, TilePackagedProvider provider) {
        if (!provider.networkEssentia) return;
        NBTTagCompound route = new NBTTagCompound();
        route.setInteger("X", provider.xCoord);
        route.setInteger("Y", provider.yCoord);
        route.setInteger("Z", provider.zCoord);
        route.setString("Identity", provider.essentiaIdentity());
        ((InfusionSourceAccess) matrix).gtnotgood$setEssentiaSource(route);
        matrix.markDirty();
    }

    /**
     * @param tile   TC4's actual consuming tile
     * @param aspect the single aspect unit requested by TC4
     * @return null for ordinary TC4 supply, otherwise the result of this job's exclusive AE supply
     */
    public static Boolean drain(TileEntity tile, Aspect aspect) {
        if (!(tile instanceof TileInfusionMatrix matrix) || !(tile instanceof InfusionSourceAccess access)) return null;
        NBTTagCompound route = access.gtnotgood$getEssentiaSource();
        if (route == null) return null;
        var world = matrix.getWorldObj();
        if (world == null || world.isRemote || !matrix.crafting || aspect == null) return false;
        int x = route.getInteger("X"), y = route.getInteger("Y"), z = route.getInteger("Z");
        if (!world.getChunkProvider()
            .chunkExists(x >> 4, z >> 4)) return false;
        if (!(world.getTileEntity(x, y, z) instanceof TilePackagedProvider provider) || provider.isInvalid()
            || !provider.networkEssentia
            || !provider.essentiaIdentity()
                .equals(route.getString("Identity"))
            || !provider
                .busy(new PackagedTarget(world.provider.dimensionId, matrix.xCoord, matrix.yCoord, matrix.zCoord, 1)))
            return false;
        if (!provider.getProxy()
            .isActive() || !ModList.ThaumicEnergistics.isModLoaded()) {
            provider.altarStatus = AltarStatus.ESSENTIA;
            return false;
        }
        boolean supplied = ThaumicEnergisticsSupply.extractOne(provider, aspect);
        provider.altarStatus = supplied ? AltarStatus.RUNNING : AltarStatus.ESSENTIA;
        return supplied;
    }
}
