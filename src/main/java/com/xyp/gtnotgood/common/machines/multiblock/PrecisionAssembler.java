// SPDX-License-Identifier: LGPL-3.0-or-later
// Adapted from GT5-Unofficial's MTEPreciseAssembler; see META-INF/licenses/precision-assembler/NOTICE.txt.
package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.HatchElement.*;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTUtility.validMTEList;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.annotation.Nonnull;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.commons.lang3.tuple.Pair;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.base.GTNGModernMultiBlockBaseGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import goodgenerator.api.recipe.GoodGeneratorRecipeMaps;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.Materials;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.enums.VoltageIndex;
import gregtech.api.interfaces.IIconContainer;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.GregTechTileClientEvents;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.modularui2.GTGuiTextures;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.misc.GTStructureChannels;

public final class PrecisionAssembler extends GTNGMultiBlockBase<PrecisionAssembler> implements ISurvivalConstructable {

    private static final int MODE_PRECISE = 0;
    private static final int MODE_ASSEMBLER = 1;
    private static final Casings[] MECHANICAL_CASINGS = { Casings.SolidSteelMachineCasing,
        Casings.ChemicallyInertMachineCasing, Casings.CleanStainlessSteelMachineCasing,
        Casings.StableTitaniumMachineCasing, Casings.RobustTungstenSteelMachineCasing };
    private static final IIconContainer FRONT_ACTIVE = Textures.BlockIcons
        .custom(ModList.GregTech.getResourceLocation(), "iconsets/OVERLAY_QTANK");
    private static final IIconContainer FRONT_ACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GregTech.getResourceLocation(), "iconsets/OVERLAY_QTANK_GLOW");
    private static final IIconContainer FRONT_INACTIVE = Textures.BlockIcons
        .custom(ModList.GregTech.getResourceLocation(), "iconsets/OVERLAY_QCHEST");
    private static final IIconContainer FRONT_INACTIVE_GLOW = Textures.BlockIcons
        .customOptional(ModList.GregTech.getResourceLocation(), "iconsets/OVERLAY_QCHEST_GLOW");
    private static final String[][] SHAPE = { { "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC", "CCCCCCCCC" },
        { "F       F", "CGGGGGGGC", "C-------C", "CGGGGGGGC", "F       F" },
        { "F       F", "CGGGGGGGC", "C-------C", "CGGGGGGGC", "F       F" },
        { "F       F", "CGGGGGGGC", "C-------C", "CGGGGGGGC", "F       F" },
        { "CCCC~CCCC", "CMMMMMMMC", "CMMMMMMMC", "CMMMMMMMC", "CCCCCCCCC" } };

    private IStructureDefinition<PrecisionAssembler> definition;
    private int mechanicalCasingTier = -1;
    private int machineCasingTier = -1;
    private int casingCount;

    public PrecisionAssembler(int id, String name, String regionalName) {
        super(id, name, regionalName);
    }

    public PrecisionAssembler(String name) {
        super(name);
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity tile) {
        return new PrecisionAssembler(mName);
    }

    @Override
    public IStructureDefinition<PrecisionAssembler> getStructureDefinition() {
        if (definition == null) {
            definition = StructureDefinition.<PrecisionAssembler>builder().addShape("main", transpose(SHAPE))
                .addElement('C',
                    GTStructureChannels.PRASS_UNIT_CASING
                        .use(
                            buildHatchAdder(PrecisionAssembler.class)
                                .atLeast(InputBus, InputHatch, OutputBus, Maintenance, Muffler, Energy.or(ExoticEnergy))
                                .casingIndex(MECHANICAL_CASINGS[0].getTextureId()).hint(1)
                                .buildAndChain(onElementPass(machine -> machine.casingCount++,
                                    ofBlocksTiered(PrecisionAssembler::getMechanicalCasingBlockTier,
                                        Arrays.stream(MECHANICAL_CASINGS)
                                            .map(casing -> Pair.of(casing.getBlock(), casing.getBlockMeta()))
                                            .collect(Collectors.toList()),
                                        -1, (machine, tier) -> machine.mechanicalCasingTier = tier,
                                        machine -> machine.mechanicalCasingTier)))))
                .addElement('F', GTStructureUtility.ofFrame(Materials.TungstenSteel))
                .addElement('G', ofBlock(Block.getBlockFromItem(ModsItemlist.BorosilicateGlass.get(1).getItem()), 0))
                .addElement('M',
                    GTStructureChannels.TIER_MACHINE_CASING.use(ofBlocksTiered(
                        (block, meta) -> block == GregTechAPI.sBlockCasings1 && meta >= 0 && meta <= 9 ? meta : null,
                        IntStream.range(0, 10).mapToObj(meta -> Pair.of(GregTechAPI.sBlockCasings1, meta))
                            .collect(Collectors.toList()),
                        -1, (machine, tier) -> machine.machineCasingTier = tier, machine -> machine.machineCasingTier)))
                .build();
        }
        return definition;
    }

    private static Integer getMechanicalCasingBlockTier(Block block, int meta) {
        for (int tier = 0; tier < MECHANICAL_CASINGS.length; tier++) {
            Casings casing = MECHANICAL_CASINGS[tier];
            if (block == casing.getBlock() && meta == casing.getBlockMeta()) return tier;
        }
        return null;
    }

    @Override
    public void construct(ItemStack stack, boolean hintsOnly) {
        buildPiece("main", stack, hintsOnly, 4, 4, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stack, int budget, ISurvivalBuildEnvironment env) {
        return mMachine ? -1 : survivalBuildPiece("main", stack, 4, 4, 0, budget, env, false, true);
    }

    @Override
    public void checkMachine(IGregTechTileEntity tile, ItemStack stack, List<StructureError> errors) {
        mechanicalCasingTier = -1;
        machineCasingTier = -1;
        casingCount = 0;
        if (!checkPiece("main", 4, 4, 0, errors)) return;
        updateHatchTextures();
        tile.sendBlockEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, getUpdateData());
        checkCasingMin(errors, casingCount, 42);
        checkHasAnyEnergy(errors);
        checkOneMaintenanceHatch(errors);
        checkHasMufflerHatch(errors);
        checkHasInputBus(errors);
        checkHasOutputBus(errors);
    }

    public int getMechanicalCasingTier() {
        return mechanicalCasingTier;
    }

    public int getMachineCasingTier() {
        return machineCasingTier;
    }

    public int getCasingTextureId() {
        return MECHANICAL_CASINGS[Math.max(0, Math.min(mechanicalCasingTier, MECHANICAL_CASINGS.length - 1))]
            .getTextureId();
    }

    private void updateHatchTextures() {
        int texture = getCasingTextureId();
        for (var hatch : mDualInputHatches) {
            if (hatch instanceof MetaTileEntity tile && tile.isValid()) hatch.updateTexture(texture);
        }
        for (List<? extends MTEHatch> hatches : Arrays.<List<? extends MTEHatch>>asList(mInputHatches, mInputBusses,
            mOutputBusses, mEnergyHatches, mExoticEnergyHatches, mMaintenanceHatches, mMufflerHatches)) {
            for (MTEHatch hatch : validMTEList(hatches)) hatch.updateTexture(texture);
        }
    }

    @Override
    public byte getUpdateData() {
        return (byte) (mechanicalCasingTier + 1);
    }

    @Override
    public void receiveClientEvent(byte event, byte value) {
        super.receiveClientEvent(event, value);
        if (event == GregTechTileClientEvents.CHANGE_CUSTOM_DATA && value >= 0 && value <= MECHANICAL_CASINGS.length)
            mechanicalCasingTier = value - 1;
    }

    @Override
    public void saveNBTData(NBTTagCompound tag) {
        super.saveNBTData(tag);
        tag.setInteger("mechanicalCasingTier", mechanicalCasingTier);
        tag.setInteger("machineCasingTier", machineCasingTier);
    }

    @Override
    public void loadNBTData(NBTTagCompound tag) {
        super.loadNBTData(tag);
        mechanicalCasingTier = Math.max(-1, Math.min(tag.getInteger("mechanicalCasingTier"), 4));
        machineCasingTier = Math.max(-1, Math.min(tag.getInteger("machineCasingTier"), 9));
        machineMode = machineMode == MODE_ASSEMBLER ? MODE_ASSEMBLER : MODE_PRECISE;
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        return machineMode == MODE_PRECISE ? GoodGeneratorRecipeMaps.preciseAssemblerRecipes
            : RecipeMaps.assemblerRecipes;
    }

    @Override
    public Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return Arrays.asList(GoodGeneratorRecipeMaps.preciseAssemblerRecipes, RecipeMaps.assemblerRecipes);
    }

    @Override
    public boolean supportsMachineModeSwitch() {
        return true;
    }

    @Override
    public int nextMachineMode() {
        return machineMode == MODE_PRECISE ? MODE_ASSEMBLER : MODE_PRECISE;
    }

    @Override
    public void setMachineMode(int index) {
        super.setMachineMode(index == MODE_ASSEMBLER ? MODE_ASSEMBLER : MODE_PRECISE);
        for (var hatch : mInputHatches) hatch.mRecipeMap = getRecipeMap();
        for (var bus : mInputBusses) bus.mRecipeMap = getRecipeMap();
    }

    @Override
    public String getMachineModeKey() {
        // #tr gtng.precision.mode.normal
        // # Normal
        // # zh_CN 常规
        if (machineMode == MODE_ASSEMBLER) return "gtng.precision.mode.normal";
        // #tr gtng.precision.mode.precise
        // # Precise
        // # zh_CN 精密
        return "gtng.precision.mode.precise";
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (getBaseMetaTileEntity().isClientSide()) return;
        if (player.isSneaking()) super.onScrewdriverRightClick(side, player, x, y, z, tool);
        else {
            setMachineMode(nextMachineMode());
            GTUtility.sendChatTrans(player, "gt.interact.desc.mb.mode", getMachineModeName());
        }
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @Override
            @Nonnull
            protected CheckRecipeResult validateRecipe(@Nonnull GTRecipe recipe) {
                if (machineMode == MODE_PRECISE && recipe.mSpecialValue > mechanicalCasingTier)
                    return CheckRecipeResultRegistry.insufficientMachineTier(recipe.mSpecialValue);
                if (machineCasingTier < 0) return CheckRecipeResultRegistry.insufficientMachineTier(0);
                if (machineCasingTier < VoltageIndex.UHV && recipe.mEUt > GTValues.V[machineCasingTier])
                    return CheckRecipeResultRegistry.insufficientMachineTier(GTUtility.getTier(recipe.mEUt));
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @Override
            @Nonnull
            protected OverclockCalculator createOverclockCalculator(@Nonnull GTRecipe recipe) {
                return super.createOverclockCalculator(recipe)
                    .setDurationModifier(machineMode == MODE_PRECISE ? 1 : 0.5);
            }
        }.enablePerfectOverclock().setMaxParallelSupplier(this::getTrueParallel);
    }

    @Override
    public int getMaxParallelRecipes() {
        return machineMode == MODE_PRECISE ? 1 : 16 << Math.max(0, mechanicalCasingTier);
    }

    private static String colorTiers(String text) {
        EnumChatFormatting[] colors = { EnumChatFormatting.GREEN, EnumChatFormatting.BLUE,
            EnumChatFormatting.LIGHT_PURPLE, EnumChatFormatting.GOLD, EnumChatFormatting.RED };
        String[] tiers = text.split("/");
        for (int tier = 0; tier < tiers.length; tier++)
            tiers[tier] = colors[Math.min(tier, colors.length - 1)] + tiers[tier].trim();
        return String.join(EnumChatFormatting.GRAY + "/", tiers) + EnumChatFormatting.GRAY;
    }

    @Override
    public int getPollutionPerSecond(ItemStack stack) {
        return 780;
    }

    @Override
    @SideOnly(Side.CLIENT)
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GT_MACHINES_MULTI_PRECISE_LOOP;
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity tile, ForgeDirection side, ForgeDirection facing, int color,
        boolean active, boolean redstone) {
        ITexture casing = Textures.BlockIcons.getCasingTextureForId(getCasingTextureId());
        if (side != facing) return new ITexture[] { casing };
        return new ITexture[] { casing,
            TextureFactory.builder().addIcon(active ? FRONT_ACTIVE : FRONT_INACTIVE).extFacing().build(), TextureFactory
                .builder().addIcon(active ? FRONT_ACTIVE_GLOW : FRONT_INACTIVE_GLOW).extFacing().glow().build() };
    }

    @Override
    @Nonnull
    protected MTEMultiBlockBaseGui<?> getGui() {
        return new GTNGModernMultiBlockBaseGui<>(this).withMachineModeIcons(GTGuiTextures.OVERLAY_BUTTON_PRECISE_MODE,
            GTGuiTextures.OVERLAY_BUTTON_ASSEMBLER_MODE);
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr gtng.precision.casing
        // # Solid Steel / Chemically Inert / Clean Stainless Steel / Stable Titanium / Robust Tungstensteel
        // # zh_CN 脱氧钢／化学惰性／洁净不锈钢／加强钛／强化钨钢机械方块
        String casing = StatCollector.translateToLocal("gtng.precision.casing");
        // #tr gtng.precision.hatch_location
        // # Any listed mechanical casing
        // # zh_CN 任意上述机械方块
        String hatchLocation = StatCollector.translateToLocal("gtng.precision.hatch_location");
        // #tr gtng.precision.tiered
        // # {\\AQUA}(Tiered)
        // # zh_CN {\\AQUA}（分级）
        String tiered = " " + StatCollector.translateToLocal("gtng.precision.tiered");
        // #tr gtng.precision.machine_type
        // # Precise Assembler, Assembler, PrAss
        // # zh_CN 精密组装机，组装机，PrAss
        tt.addMachineType(StatCollector.translateToLocal("gtng.precision.machine_type"))
            // #tr gtng.precision.tolerance
            // # Tolerance is not more than 7nm
            // # zh_CN 误差不超过7nm
            .addInfo(StatCollector.translateToLocal("gtng.precision.tolerance"))
            // #tr gtng.precision.modes
            // # Has two modes: Precise and Normal
            // # zh_CN 有两种模式：精密与常规
            .addInfo(StatCollector.translateToLocal("gtng.precision.modes"))
            // #tr gtng.precision.switch
            // # Use a screwdriver to switch modes
            // # zh_CN 使用螺丝刀切换模式
            .addInfo(StatCollector.translateToLocal("gtng.precision.switch")).addSeparator()
            // #tr gtng.precision.precise
            // # Precise Mode unlocks the ability to assemble precise components
            // # zh_CN 精密模式下解锁组装精密元件的能力
            .addInfo(StatCollector.translateToLocal("gtng.precision.precise"))
            // #tr gtng.precision.recipe_tier
            // # Structure casing tier determines the maximum precise recipe tier
            // # zh_CN 结构外壳等级决定最大精密配方等级
            .addInfo(StatCollector.translateToLocal("gtng.precision.recipe_tier")).addSeparator()
            // #tr gtng.precision.normal
            // # Normal Mode supports standard Assembler recipes
            // # zh_CN 常规模式支持标准的组装配方
            .addInfo(StatCollector.translateToLocal("gtng.precision.normal"))
            // #tr gtng.precision.parallel
            // # Structure casing tier determines the number of {\\YELLOW}parallels
            // # zh_CN 结构外壳的等级决定了{\\YELLOW}并行{\\GRAY}的数量
            .addInfo(StatCollector.translateToLocal("gtng.precision.parallel"))
            // #tr gtng.precision.parallel_tiers
            // # Steel / Inert / Stainless Steel / Titanium / Tungstensteel
            // # zh_CN 脱氧钢 / 化学惰性 / 洁净不锈钢 / 加强钛 / 强化钨钢
            .addInfo(colorTiers(StatCollector.translateToLocal("gtng.precision.parallel_tiers")))
            // #tr gtng.precision.parallel_values
            // # %s parallels
            // # zh_CN %s 并行
            .addInfo(StatCollector.translateToLocalFormatted("gtng.precision.parallel_values",
                colorTiers("16/32/64/128/256")))
            .addStaticSpeedInfo(2f).addSeparator()
            // #tr gtng.precision.voltage
            // # Internal LV/MV/HV/etc. machine casings limit recipe tiers; %s unlocks all recipes
            // # zh_CN 内部 LV／MV／HV 等机械方块限制配方等级，%s级解锁所有配方。
            .addInfo(StatCollector.translateToLocalFormatted("gtng.precision.voltage",
                EnumChatFormatting.DARK_RED + "" + EnumChatFormatting.UNDERLINE + "UHV" + EnumChatFormatting.GRAY))
            // #tr gtng.precision.overclock_power
            // # Machine casings do not limit energy hatch tiers or overclocking voltage
            // # zh_CN 内部机械方块不限制能源仓等级及超频电压
            .addInfo(StatCollector.translateToLocal("gtng.precision.overclock_power"))
            // #tr gtng.precision.hatch_support
            // # {\\GREEN}Supports Multi-Amp and Laser Hatches!
            // # zh_CN {\\GREEN}支持多安能源仓和激光仓！
            .addInfo(StatCollector.translateToLocal("gtng.precision.hatch_support"))
            // #tr gtng.precision.power
            // # Supports lower-tier recipes and voltage boosting with two energy hatches
            // # zh_CN 支持运行低等级配方，也可双仓升压
            .addInfo(StatCollector.translateToLocal("gtng.precision.power")).addPerfectOCInfo()
            .addPollutionAmount(getPollutionPerSecond(null)).beginStructureBlock(9, 5, 5, true)
            // #tr gtng.precision.controller
            // # Front bottom center
            // # zh_CN 正面底部中央
            .addController(StatCollector.translateToLocal("gtng.precision.controller"))
            .addCasing("42-81", casing + tiered, false)
            // #tr gtng.precision.glass
            // # Borosilicate Glass (standard variant only)
            // # zh_CN 硼玻璃（仅普通型号）
            .addCasing("42", StatCollector.translateToLocal("gtng.precision.glass"), false)
            // #tr gtng.precision.machine_casing
            // # Internal Machine Casings (ULV-UHV)
            // # zh_CN 内部机械方块（ULV-UHV）
            .addCasing("21", StatCollector.translateToLocal("gtng.precision.machine_casing") + tiered, false)
            // #tr gtng.precision.frame
            // # Tungstensteel Frame Box
            // # zh_CN 钨钢框架
            .addCasing("12", StatCollector.translateToLocal("gtng.precision.frame"), false)
            .addEnergyHatch("1+", hatchLocation, 1).addMaintenanceHatch("1", hatchLocation, 1)
            .addMufflerHatch("1", hatchLocation, 1).addInputBus("1+", hatchLocation, 1)
            .addInputHatch("0+", hatchLocation, 1).addOutputBus("1+", hatchLocation, 1)
            // #tr gtng.precision.channel.casing
            // # Subchannel {\\AQUA}%s{\\GRAY}: structure casing tier (1-5)
            // # zh_CN 子信道 {\\AQUA}%s{\\GRAY}：结构外壳等级（1-5）
            .addStructureInfo(StatCollector.translateToLocalFormatted("gtng.precision.channel.casing",
                GTStructureChannels.PRASS_UNIT_CASING.get()))
            // #tr gtng.precision.channel.machine
            // # Subchannel {\\AQUA}%s{\\GRAY}: internal machine casing tier (1-10, ULV-UHV)
            // # zh_CN 子信道 {\\AQUA}%s{\\GRAY}：内部机械方块等级（1-10，ULV-UHV）
            .addStructureInfo(StatCollector.translateToLocalFormatted("gtng.precision.channel.machine",
                GTStructureChannels.TIER_MACHINE_CASING.get()))
            .toolTipFinisher();
        return tt;
    }
}
