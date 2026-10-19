package com.xyp.gtnotgood.common.blocks.packaged;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import bartworks.common.loaders.ItemRegistry;
import bartworks.common.tileentities.multis.MTECircuitAssemblyLine;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.ItemList;
import gregtech.api.graphs.paths.CableReadout;
import gregtech.api.graphs.paths.PowerNodePath;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaPipeEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTECable;
import gregtech.api.metatileentity.implementations.MTEHatchEnergy;
import gregtech.api.metatileentity.implementations.MTEHatchInput;
import gregtech.api.metatileentity.implementations.MTEHatchInputBus;
import gregtech.api.metatileentity.implementations.MTEHatchMaintenance;
import gregtech.api.metatileentity.implementations.MTEHatchOutputBus;
import tectech.thing.metaTileEntity.single.MTEDebugPowerGenerator;

final class CircuitAssemblyLineQaStructure {

    static Rig build(EntityPlayerMP player, int x, int y, int z, int inputCount, int energyTier) {
        require(inputCount >= 1 && inputCount <= 6, "CAL supports one to six ordered input buses");
        require(energyTier >= 1 && energyTier < GTValues.V.length, "valid energy tier");
        World world = player.worldObj;
        BaseMetaTileEntity base = machine(player, x, y, z, ItemRegistry.cal, ForgeDirection.NORTH);
        base.disableWorking();
        MTECircuitAssemblyLine line = (MTECircuitAssemblyLine) base.getMetaTileEntity();
        MTEHatchInputBus[] inputs = new MTEHatchInputBus[inputCount];
        for (int slice = 0; slice <= inputCount; slice++) {
            int sx = x - slice;
            for (int depth = 0; depth < 3; depth++) {
                if (slice != 0 || depth != 0) block(world, sx, y, z + depth, GregTechAPI.sBlockCasings3, 10);
                block(world, sx, y - 2, z + depth, GregTechAPI.sBlockCasings2, 0);
            }
            block(world, sx, y - 1, z, ItemRegistry.bw_realglas, 1);
            block(world, sx, y - 1, z + 1, GregTechAPI.sBlockCasings2, 5);
            block(world, sx, y - 1, z + 2, ItemRegistry.bw_realglas, 1);
            if (slice < inputCount) {
                inputs[slice] = (MTEHatchInputBus) machine(player, sx, y - 2, z + 1, ItemList.Hatch_Input_Bus_IV.get(1),
                    ForgeDirection.DOWN).getMetaTileEntity();
            }
        }
        BaseMetaTileEntity outputBase = machine(player, x - inputCount, y - 2, z + 1,
            ItemList.Hatch_Output_Bus_IV.get(1), ForgeDirection.DOWN);
        BaseMetaTileEntity inputBase = machine(player, x, y - 2, z + 2, ItemList.Hatch_Input_IV.get(1),
            ForgeDirection.SOUTH);
        BaseMetaTileEntity maintenanceBase = machine(player, x, y - 2, z, ItemList.Hatch_Maintenance.get(1),
            ForgeDirection.NORTH);
        MTEHatchMaintenance maintenance = (MTEHatchMaintenance) maintenanceBase.getMetaTileEntity();
        ItemStack held = player.inventory.getCurrentItem();
        try {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, ItemList.Duct_Tape.get(1));
            require(maintenance.onRightclick(maintenanceBase, player, ForgeDirection.NORTH, .5f, .5f, .5f),
                "native maintenance hatch accepts duct tape");
            require(player.inventory.getCurrentItem() == null, "native maintenance consumes duct tape");
        } finally {
            player.inventory.setInventorySlotContents(player.inventory.currentItem, held);
        }
        IMetaTileEntity energyPrototype = null;
        IMetaTileEntity generatorPrototype = null;
        MTECable cablePrototype = null;
        for (IMetaTileEntity prototype : GregTechAPI.METATILEENTITIES) {
            if (prototype == null) continue;
            if (prototype.getClass() == MTEHatchEnergy.class && ((MTEHatchEnergy) prototype).mTier == energyTier) {
                energyPrototype = prototype;
            }
            if (prototype instanceof MTEDebugPowerGenerator) generatorPrototype = prototype;
            if (
                prototype instanceof MTECable candidate && candidate.mInsulated
                    && candidate.mVoltage == GTValues.V[energyTier]
                    && candidate.mAmperage >= 2
                    && (cablePrototype == null || candidate.mAmperage < cablePrototype.mAmperage
                        || candidate.mAmperage == cablePrototype.mAmperage
                            && candidate.mCableLossPerMeter < cablePrototype.mCableLossPerMeter)
            ) {
                cablePrototype = candidate;
            }
        }
        require(energyPrototype != null, "ordinary energy hatch exists at selected recipe tier");
        require(generatorPrototype != null, "native debug power generator exists");
        require(cablePrototype != null, "real insulated cable supports selected voltage and two amperes");
        BaseMetaTileEntity energyBase = machine(player, x, y, z + 2, energyPrototype.getStackForm(1),
            ForgeDirection.UP);
        BaseMetaTileEntity generatorBase = machine(player, x, y + 2, z + 2, generatorPrototype.getStackForm(1),
            ForgeDirection.NORTH);
        MTEDebugPowerGenerator generator = (MTEDebugPowerGenerator) generatorBase.getMetaTileEntity();
        generator.setUsingTiers(true);
        generator.setVoltageTier((byte) energyTier);
        generator.setAmperage(2);
        generator.setLASER(false);
        generator.setProducing(true);
        generatorBase.enableWorking();
        IGregTechTileEntity cableBase = place(player, x, y + 1, z + 2, cablePrototype.getStackForm(1));
        MTECable cable = (MTECable) cableBase.getMetaTileEntity();
        require(cable.connect(ForgeDirection.UP) > 0 && cable.connect(ForgeDirection.DOWN) > 0,
            "native cable connects generator to ordinary energy hatch");
        base.issueBlockUpdate();
        return new Rig(base, line, inputs, (MTEHatchInput) inputBase.getMetaTileEntity(),
            (MTEHatchOutputBus) outputBase.getMetaTileEntity(), energyBase, generatorBase,
            (BaseMetaPipeEntity) cableBase, cable, generator);
    }

    static final class Rig {

        final BaseMetaTileEntity base;
        final MTECircuitAssemblyLine line;
        final MTEHatchInputBus[] inputs;
        final MTEHatchInput input;
        final MTEHatchOutputBus output;
        final BaseMetaTileEntity energyBase;
        final BaseMetaTileEntity generatorBase;
        final BaseMetaPipeEntity cableBase;
        final MTECable cable;
        final MTEDebugPowerGenerator generator;

        private Rig(BaseMetaTileEntity base, MTECircuitAssemblyLine line, MTEHatchInputBus[] inputs,
            MTEHatchInput input, MTEHatchOutputBus output, BaseMetaTileEntity energyBase,
            BaseMetaTileEntity generatorBase, BaseMetaPipeEntity cableBase, MTECable cable,
            MTEDebugPowerGenerator generator) {
            this.base = base;
            this.line = line;
            this.inputs = inputs;
            this.input = input;
            this.output = output;
            this.energyBase = energyBase;
            this.generatorBase = generatorBase;
            this.cableBase = cableBase;
            this.cable = cable;
            this.generator = generator;
        }

        void verifyStructure() {
            require(line.mMachine, "native controller tick forms the complete CAL");
            require(line.mInputBusses.size() == inputs.length && line.mInputHatches.size() == 1
                && line.mOutputBusses.size() == 1
                && line.mEnergyHatches.size() == 1
                && line.mMaintenanceHatches.size() == 1, "native structure identifies all real hatches");
            for (int i = 0; i < inputs.length; i++) {
                require(line.mInputBusses.get(i) == inputs[i], "native structure preserves input lane order " + i);
            }
            require(
                line.mInputHatches.get(0) == input && line.mOutputBusses.get(0) == output
                    && line.mEnergyHatches.get(0).getBaseMetaTileEntity() == energyBase,
                "native hatch lists contain the physically built hatches");
        }

        void setPower(boolean enabled) {
            generator.setProducing(enabled);
            if (enabled) generatorBase.enableWorking();
            else generatorBase.disableWorking();
        }

        long storedEnergy() {
            return energyBase.getStoredEU();
        }

        CableReadout cableReadout() {
            return cableBase.getNodePath() instanceof PowerNodePath path ? path.getReadout() : null;
        }
    }

    private static BaseMetaTileEntity machine(EntityPlayerMP player, int x, int y, int z, ItemStack stack,
        ForgeDirection facing) {
        BaseMetaTileEntity tile = (BaseMetaTileEntity) place(player, x, y, z, stack);
        tile.setFrontFacing(facing);
        return tile;
    }

    private static IGregTechTileEntity place(EntityPlayerMP player, int x, int y, int z, ItemStack stack) {
        IMetaTileEntity prototype = GregTechAPI.METATILEENTITIES[stack.getItemDamage()];
        require(prototype != null, "registered GT fixture block");
        World world = player.worldObj;
        require(world.setBlock(x, y, z, GregTechAPI.sBlockMachines, prototype.getTileEntityBaseType(), 3),
            "place GT fixture at " + x + "," + y + "," + z);
        IGregTechTileEntity tile = (IGregTechTileEntity) world.getTileEntity(x, y, z);
        require(tile != null, "real GT tile exists");
        tile.setInitialValuesAsNBT(stack.getTagCompound(), (short) stack.getItemDamage());
        tile.setOwnerName(player.getCommandSenderName());
        tile.setOwnerUuid(player.getUniqueID());
        return tile;
    }

    private static void block(World world, int x, int y, int z, Block block, int metadata) {
        require(world.setBlock(x, y, z, block, metadata, 3), "place native structure block");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError("CIRCUIT_ASSEMBLY_QA: " + message);
    }
}
