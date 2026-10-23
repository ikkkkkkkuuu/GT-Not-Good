// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.ExoticEnergy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationMath;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationRenderer;
import com.xyp.gtnotgood.common.blocks.lighthunter.LightHunterCasing;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LightHunterSpaceStationGui;
import com.xyp.gtnotgood.common.machines.hatch.CrossRecipeWirelessEnergyHatch;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.common.machines.multiblock.structure.LightHunterSpaceStationStructure;
import com.xyp.gtnotgood.common.machines.processing.LightHunterEnergy;
import com.xyp.gtnotgood.common.recipe.machine.LightHunterRecipeRules;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.render.IMTERenderer;
import io.netty.buffer.ByteBuf;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;

@IMetaTileEntity.SkipGenerateName
@IMetaTileEntity.SkipGenerateDescription
public final class LightHunterSpaceStation extends GTNGMultiBlockBase<LightHunterSpaceStation>
    implements ISurvivalConstructable, IMTERenderer {

    private static IStructureDefinition<LightHunterSpaceStation> definition;
    private static final IIconContainer FRONT = Textures.BlockIcons.custom(ModList.GTNotGood.getResourceLocation(),
        "lighthunter/station_front");
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GTNotGood.getResourceLocation(), "lighthunter/station_front_active");
    private static final IIconContainer FRONT_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "lighthunter/station_front_active_glow");
    private static final IIconContainer FRONT_IDLE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GTNotGood.getResourceLocation(), "lighthunter/station_front_glow");
    private int astralArrayCount;
    private boolean stationWorking;

    public LightHunterSpaceStation(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public LightHunterSpaceStation(String name) {
        super(name);
    }

    @Override
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new LightHunterSpaceStationGui(this);
    }

    @Override
    public String getLocalNameKey() {
        // #tr gtng.light_hunter.name
        // # Light Hunter Space Station
        // # zh_CN 猎光号空间站
        return "gtng.light_hunter.name";
    }

    @Override
    public String getLocalName() {
        return StatCollector.translateToLocal(getLocalNameKey());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new LightHunterSpaceStation(mName);
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (direction, rotation, flip) -> direction.offsetY == 0 && rotation.isNotRotated() && flip.isNotFlipped();
    }

    @Override
    public IStructureDefinition<LightHunterSpaceStation> getStructureDefinition() {
        if (definition == null) {
            var builder = StructureDefinition.<LightHunterSpaceStation>builder().addShape("main",
                transpose(LightHunterSpaceStationStructure.shape()));
            for (char symbol : LightHunterSpaceStationStructure.SYMBOLS.toCharArray()) {
                if (
                    symbol != 'd'
                ) builder.addElement(symbol,
                    "MaUWZ".indexOf(symbol) >= 0
                        ? GTStructureUtility.activeCoils(LightHunterSpaceStationStructure.element(symbol))
                        : LightHunterSpaceStationStructure.element(symbol));
            }
            builder.addElement('d',
                buildHatchAdder(LightHunterSpaceStation.class).atLeast(InputBus, Energy.or(ExoticEnergy))
                    .casingIndex(LightHunterCasing.HighPowerCasing.getTextureId()).hint(1)
                    .buildAndChain(LightHunterCasing.HighPowerCasing.asElement()));
            definition = builder.build();
        }
        return definition;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 36, 36, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 36, 36, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        if (!checkPiece("main", 36, 36, 0, errors)) return;
        checkHasInputBus(errors);
        checkHasAnyEnergy(errors);
        int energyHatches = LightHunterEnergy.countEnergyHatches(mEnergyHatches, mExoticEnergyHatches);
        if (energyHatches > 1) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, Energy, energyHatches, 1));
        int laserHatches = LightHunterEnergy.countLaserHatches(mExoticEnergyHatches);
        if (laserHatches > 0) errors.add(StructureErrors.hatchCount(ErrorType.TOO_MANY, ExoticEnergy, laserHatches, 0));
        fixAllIssues();
    }

    @Override
    public CheckRecipeResult checkProcessing() {
        if (
            LightHunterEnergy.totalInputPower(mEnergyHatches, mExoticEnergyHatches) < LightHunterRecipeRules.STATION_EUT
        ) return CheckRecipeResultRegistry.insufficientPower(LightHunterRecipeRules.STATION_EUT);
        ItemStack circuit = GTUtility.getIntegratedCircuit(1);
        boolean configured = GTUtility.areStacksEqual(getControllerSlot(), circuit);
        if (!configured) for (ItemStack input : getStoredInputs())
            if (input != null && input.stackSize > 0 && GTUtility.areStacksEqual(input, circuit)) {
                configured = true;
                break;
            }
        if (!configured) return CheckRecipeResultRegistry.NO_RECIPE;
        lEUt = -LightHunterRecipeRules.STATION_EUT;
        mMaxProgresstime = LightHunterRecipeRules.STATION_DURATION;
        mEfficiency = mEfficiencyIncrease = 10000;
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    @Override
    public void onPostTick(IGregTechTileEntity tile, long tick) {
        super.onPostTick(tile, tick);
        if (tile.isServerSide()) {
            boolean working = mMachine && tile.isActive() && mMaxProgresstime > 0;
            if (working != stationWorking) {
                stationWorking = working;
                tile.issueTileUpdate();
            }
        }
    }

    public boolean isStationWorking() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) return stationWorking;
        return tile != null && !tile.isDead() && mMachine && tile.isActive() && mMaxProgresstime > 0;
    }

    public int getAstralArrayCount() {
        return astralArrayCount;
    }

    public int installAstralArrays(int amount) {
        int accepted = Math.min(Math.max(0, amount), 64 - astralArrayCount);
        if (accepted > 0) {
            astralArrayCount += accepted;
            markDirty();
            getBaseMetaTileEntity().issueTileUpdate();
        }
        return accepted;
    }

    public List<int[]> getModulePositions() {
        List<int[]> positions = new ArrayList<>(20);
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        for (int side : new int[] { -9, 9 }) for (int distance = 41; distance <= 113; distance += 8) {
            int[] offset = moduleOffset(tile.getFrontFacing(), distance, side);
            positions.add(new int[] { tile.getXCoord() + offset[0], tile.getYCoord(), tile.getZCoord() + offset[2] });
        }
        return positions;
    }

    public static int[] moduleOffset(ForgeDirection facing, int distance, int side) {
        return switch (facing) {
            case EAST -> new int[] { -distance, 0, side };
            case SOUTH -> new int[] { -side, 0, -distance };
            case WEST -> new int[] { distance, 0, -side };
            default -> new int[] { side, 0, distance };
        };
    }

    public boolean acceptsModule(NexusSatelliteFactory module) {
        if (!mMachine) return false;
        IGregTechTileEntity stationTile = getBaseMetaTileEntity();
        if (
            stationTile == null || stationTile.isDead()
                || !stationTile.getWorld().blockExists(stationTile.getXCoord(), stationTile.getYCoord(),
                    stationTile.getZCoord())
                || stationTile.getWorld().getTileEntity(stationTile.getXCoord(), stationTile.getYCoord(),
                    stationTile.getZCoord()) != stationTile
        ) return false;
        IGregTechTileEntity tile = module.getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() != getBaseMetaTileEntity().getWorld()) return false;
        for (int[] position : getModulePositions())
            if (position[0] == tile.getXCoord() && position[1] == tile.getYCoord() && position[2] == tile.getZCoord()) {
                int dx = tile.getXCoord() - stationTile.getXCoord(), dz = tile.getZCoord() - stationTile.getZCoord();
                ForgeDirection facing = stationTile.getFrontFacing();
                ForgeDirection moduleFacing = tile.getFrontFacing();
                return facing.offsetX != 0 ? moduleFacing.offsetZ == -Integer.signum(dz)
                    : moduleFacing.offsetX == -Integer.signum(dx);
            }
        return false;
    }

    public int getConnectedModuleCount() {
        int count = 0;
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        for (int[] position : getModulePositions()) {
            if (!tile.getWorld().blockExists(position[0], position[1], position[2])) continue;
            TileEntity other = tile.getWorld().getTileEntity(position[0], position[1], position[2]);
            if (
                other instanceof IGregTechTileEntity gt
                    && gt.getMetaTileEntity() instanceof NexusSatelliteFactory module
                    && module.mMachine
                    && acceptsModule(module)
            ) count++;
        }
        return count;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("lightHunterAstralArrays", astralArrayCount);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        astralArrayCount = Math.max(0, Math.min(64, tag.getInteger("lightHunterAstralArrays")));
    }

    @Override
    public void writeToStream(ByteBuf buffer) {
        super.writeToStream(buffer);
        buffer.writeBoolean(stationWorking);
        buffer.writeByte(astralArrayCount);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void readFromStream(ByteBuf buffer) {
        super.readFromStream(buffer);
        stationWorking = buffer.readBoolean();
        astralArrayCount = buffer.readUnsignedByte();
    }

    @Override
    public void onRemoval() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isServerSide() && astralArrayCount > 0) {
            tile.getWorld().spawnEntityInWorld(new EntityItem(tile.getWorld(), tile.getXCoord() + 0.5,
                tile.getYCoord() + 0.5, tile.getZCoord() + 0.5, GTNGItemList.AstralArray.get(astralArrayCount)));
            astralArrayCount = 0;
        }
        stopEffect();
        super.onRemoval();
    }

    @Override
    public void onUnload() {
        stopEffect();
        super.onUnload();
    }

    private void stopEffect() {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        if (tile != null && tile.isClientSide()) stopClientEffect();
    }

    @SideOnly(Side.CLIENT)
    private void stopClientEffect() {
        LightHunterSpaceStationRenderer.stop(this);
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox(int x, int y, int z) {
        int[] star = moduleOffset(getBaseMetaTileEntity().getFrontFacing(), 206, 0);
        return AxisAlignedBB.getBoundingBox(Math.min(x - 37, x + star[0] - 56), y - 56,
            Math.min(z - 37, z + star[2] - 56), Math.max(x + 38, x + star[0] + 57), y + 57,
            Math.max(z + 38, z + star[2] + 57));
    }

    @Override
    public double getMaxRenderDistanceSquared() {
        return 384D * 384D;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderTESR(double x, double y, double z, float partialTicks) {
        IGregTechTileEntity tile = getBaseMetaTileEntity();
        long seed = LightHunterSpaceStationMath.positionSeed(tile.getXCoord(), tile.getYCoord(), tile.getZCoord());
        LightHunterSpaceStationRenderer.render(this, x, y, z, tile.getFrontFacing(), stationWorking, astralArrayCount,
            tile.getWorld().getTotalWorldTime(), partialTicks, seed);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = LightHunterCasing.HighPowerCasing.getTexture();
        return side == facing
            ? new ITexture[] { casing,
                TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT).extFacing().build(),
                TextureFactory.builder().addIcon(active ? FRONT_GLOW : FRONT_IDLE_GLOW).extFacing().glow().build() }
            : new ITexture[] { casing };
    }

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean addEnergyInputToMachineList(IGregTechTileEntity tile, int texture) {
        return !(tile.getMetaTileEntity() instanceof CrossRecipeWirelessEnergyHatch)
            && super.addEnergyInputToMachineList(tile, texture);
    }

    @Override
    public boolean addExoticEnergyInputToMachineList(IGregTechTileEntity tile, int texture) {
        return !(tile.getMetaTileEntity() instanceof MTEHatch hatch && LightHunterEnergy.isLaserHatch(hatch))
            && super.addExoticEnergyInputToMachineList(tile, texture);
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        tag.setInteger("lightHunterArrays", getAstralArrayCount());
        tag.setInteger("lightHunterModules", getConnectedModuleCount());
    }

    @Override
    public void getWailaBody(ItemStack stack, List<String> lines, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(stack, lines, accessor, config);
        NBTTagCompound tag = accessor.getNBTData();
        if (!tag.hasKey("lightHunterArrays")) return;
        lines.add(
            StatCollector.translateToLocalFormatted("gtng.light_hunter.arrays", tag.getInteger("lightHunterArrays")));
        lines.add(
            StatCollector.translateToLocalFormatted("gtng.light_hunter.modules", tag.getInteger("lightHunterModules")));
    }

    @Override
    public String[] getInfoData() {
        List<String> lines = new ArrayList<>(Arrays.asList(super.getInfoData()));
        // #tr gtng.light_hunter.arrays
        // # Astral Arrays: %s / 64
        // # zh_CN 已安装星阵：%s / 64
        lines.add(StatCollector.translateToLocalFormatted("gtng.light_hunter.arrays", astralArrayCount));
        // #tr gtng.light_hunter.modules
        // # Satellite factories: %s / 20
        // # zh_CN 卫星工厂：%s / 20
        lines.add(StatCollector.translateToLocalFormatted("gtng.light_hunter.modules", getConnectedModuleCount()));
        return lines.toArray(new String[0]);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.light_hunter.purpose
        // # Operates up to 20 Nexus Satellite Factories
        // # zh_CN 支持最多 20 座枢纽卫星工厂运行
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.purpose"));
        // #tr gtng.light_hunter.power
        // # Requires circuit 1 and 536,870,912 EU/t (UXV); circuit is not consumed
        // # zh_CN 需要配置电路 1 和 536,870,912 EU/t（UXV）；不消耗电路
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.power"));
        // #tr gtng.light_hunter.unlock
        // # Shift-right-click Astral Arrays to install; 64 unlock unrestricted recipe allocation
        // # zh_CN 手持星阵潜行右键安装；64 个星阵解除配方并行配额
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.unlock"));
        // #tr gtng.light_hunter.supply
        // # Each satellite factory needs its own power and input/output hatches
        // # zh_CN 每座卫星工厂独立供电，并使用自身输入输出仓室
        tt.addInfo(StatCollector.translateToLocal("gtng.light_hunter.supply"));
        tt.beginStructureBlock(73, 73, 189, false);
        // #tr gtng.light_hunter.hatches
        // # High Power Casings; 1 normal or multi-amp Energy Hatch
        // # zh_CN 高功率机械方块位置；1 个普通或多安能源仓
        String positions = StatCollector.translateToLocal("gtng.light_hunter.hatches");
        return tt.addInputBus(positions, 1).addEnergyHatch(positions, 1).toolTipFinisher();
    }

}
