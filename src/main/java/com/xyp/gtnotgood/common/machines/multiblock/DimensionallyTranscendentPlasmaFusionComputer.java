package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksTiered;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static gregtech.api.enums.GTValues.VN;
import static gregtech.common.misc.WirelessNetworkManager.getUserEU;
import static net.minecraft.util.StatCollector.translateToLocal;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.DimensionallyTranscendentPlasmaFusionComputerGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGCleanWirelessMultiMachineBase;

import goodgenerator.loader.Loaders;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.HatchElement;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.logic.ProcessingLogic;
import gregtech.api.metatileentity.GregTechTileClientEvents;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.render.TextureFactory;
import gregtech.api.structure.error.ErrorType;
import gregtech.api.structure.error.StructureError;
import gregtech.api.structure.error.StructureErrors;
import gregtech.api.structure.error.TranslatableText;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTUtility;
import gregtech.api.util.HatchElementBuilder;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.api.util.OverclockCalculator;
import gregtech.api.util.ParallelHelper;
import gtPlusPlus.core.block.ModBlocks;
import gtPlusPlus.xmod.gregtech.common.blocks.textures.TexturesGtBlock;
import mcp.mobius.waila.api.IWailaConfigHandler;
import mcp.mobius.waila.api.IWailaDataAccessor;
import tectech.thing.CustomItemList;

public class DimensionallyTranscendentPlasmaFusionComputer
    extends GTNGCleanWirelessMultiMachineBase<DimensionallyTranscendentPlasmaFusionComputer>
    implements ISurvivalConstructable, ICasingTextureProvider {

    // #tr structure.gtnotgood.dtpf.reactor_coil
    // # Reactor Coil Block (A)
    // # zh_CN 反应堆线圈方块 (A)
    private static final String REACTOR_COIL_STRUCTURE_KEY = "structure.gtnotgood.dtpf.reactor_coil";
    // #tr structure.gtnotgood.dtpf.fusion_casing
    // # Fusion Machine Casing (B)
    // # zh_CN 聚变机械方块 (B)
    private static final String FUSION_CASING_STRUCTURE_KEY = "structure.gtnotgood.dtpf.fusion_casing";
    // #tr structure.error.gtnotgood.dtpf.tier_mismatch
    // # Reactor coil tier (%1$s) does not match fusion machine casing tier (%2$s)
    // # zh_CN 反应堆线圈等级（%1$s）与聚变机械方块等级（%2$s）不匹配
    private static final String TIER_MISMATCH_STRUCTURE_KEY = "structure.error.gtnotgood.dtpf.tier_mismatch";

    // #tr machine.gtnotgood.dtpf.runtime
    // # Runtime
    // # zh_CN 运行时间
    private static final String RUNTIME_TEXT = translateToLocal("machine.gtnotgood.dtpf.runtime");
    // #tr machine.gtnotgood.dtpf.machine_level
    // # Machine Level
    // # zh_CN 机器等级
    private static final String MACHINE_LEVEL_TEXT = translateToLocal("machine.gtnotgood.dtpf.machine_level");
    // #tr machine.gtnotgood.dtpf.overclock
    // # Overclock Status
    // # zh_CN 超频状态
    private static final String OVERCLOCK_TEXT = translateToLocal("machine.gtnotgood.dtpf.overclock");
    // #tr machine.gtnotgood.dtpf.overclock.perfect
    // # Perfect Overclock
    // # zh_CN 无损超频
    private static final String PERFECT_OVERCLOCK_TEXT = translateToLocal("machine.gtnotgood.dtpf.overclock.perfect");
    // #tr machine.gtnotgood.dtpf.overclock.perfect_mkv
    // # Perfect Overclock (MK-V)
    // # zh_CN MK-V 无损超频
    private static final String PERFECT_OVERCLOCK_MKV_TEXT = translateToLocal(
        "machine.gtnotgood.dtpf.overclock.perfect_mkv");
    // #tr machine.gtnotgood.dtpf.overclock.imperfect
    // # Imperfect Overclock
    // # zh_CN 有损超频
    private static final String IMPERFECT_OVERCLOCK_TEXT = translateToLocal(
        "machine.gtnotgood.dtpf.overclock.imperfect");
    // #tr machine.gtnotgood.dtpf.tooltip.runtime
    // # Full efficiency after 3600 seconds of continuous running
    // # zh_CN 持续运行 3600 秒后达到满效率
    private static final String TOOLTIP_RUNTIME = translateToLocal("machine.gtnotgood.dtpf.tooltip.runtime");
    // #tr machine.gtnotgood.dtpf.tooltip.ramp
    // # Below 3600 seconds: no EU or duration reduction
    // # zh_CN 未满 3600 秒时没有耗电或时长减免
    private static final String TOOLTIP_RAMP = translateToLocal("machine.gtnotgood.dtpf.tooltip.ramp");
    // #tr machine.gtnotgood.dtpf.tooltip.parallel
    // # Maximum parallel: (1 + machine tier - recipe tier) x 64
    // # zh_CN 最大并行：(1 + 机器等级 - 配方等级) x 64
    private static final String TOOLTIP_PARALLEL = translateToLocal("machine.gtnotgood.dtpf.tooltip.parallel");
    // #tr machine.gtnotgood.dtpf.tooltip.efficiency
    // # At full efficiency: EU and duration are both halved
    // # zh_CN 满效率时耗电和配方时长均减半
    private static final String TOOLTIP_EFFICIENCY = translateToLocal("machine.gtnotgood.dtpf.tooltip.efficiency");
    // #tr machine.gtnotgood.dtpf.tooltip.wireless_parallel
    // # Wireless mode: configurable single-recipe parallel
    // # zh_CN 无线模式：可配置单配方并行
    private static final String TOOLTIP_WIRELESS_PARALLEL = translateToLocal(
        "machine.gtnotgood.dtpf.tooltip.wireless_parallel");
    // #tr machine.gtnotgood.dtpf.tooltip.wireless_discount
    // # Wireless mode: fixed 0.75 EU and duration modifier, runtime ramp disabled
    // # zh_CN 无线模式：固定七五折耗电和时长，禁用运行时间增益
    private static final String TOOLTIP_WIRELESS_DISCOUNT = translateToLocal(
        "machine.gtnotgood.dtpf.tooltip.wireless_discount");

    public DimensionallyTranscendentPlasmaFusionComputer(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public DimensionallyTranscendentPlasmaFusionComputer(String aName) {
        super(aName);
    }

    /**
     * 控制器槽位是否放入星阵
     * 控制器槽是同步的库存槽,客户端永远最新,GUI 按钮以此作为门槛
     */
    private void refreshWirelessAvailability() {
        setWirelessModeAvailable(hasAAF() && isTierAtLeast(5));
    }

    public boolean hasAAF() {
        ItemStack aGuiStack = this.getControllerSlot();
        return aGuiStack != null && GTUtility.areStacksEqual(aGuiStack, CustomItemList.astralArrayFabricator.get(1));
    }

    // region Structure piece offsets (structure_string is transposed; controller '~' ends up at x=16, y=29, z=16)
    private static final int HORIZONTAL_OFFSET = 16;
    private static final int VERTICAL_OFFSET = 29;
    private static final int DEPTH_OFFSET = 16;

    // endregion

    // region LevelTier
    /**
     * Reactor tiers of this machine.
     * <p>
     * Every tier pairs a reactor coil block (A) with a fusion machine casing block (B); the machine is only
     * considered formed when the tier derived from A exactly matches the tier derived from B. The blocks marked in
     * the structure comment are {@link #TIER1}. Anything unrecognised is {@link #INVALID} (tier -1).
     * <p>
     * voltageTier is the matching GregTech voltage tier used for the recipe restriction
     * (6 = LuV / Mk-I, 7 = ZPM / Mk-II, 8 = UV / Mk-III, 9 = UHV / Mk-IV, 10 = UEV / Mk-V).
     */
    public enum LevelTier {

        TIER1(1, 6, () -> Loaders.compactFusionCoil, 0, () -> GregTechAPI.sBlockCasings1, 6),
        TIER2(2, 7, () -> Loaders.compactFusionCoil, 1, () -> GregTechAPI.sBlockCasings4, 6),
        TIER3(3, 8, () -> Loaders.compactFusionCoil, 2, () -> GregTechAPI.sBlockCasings4, 8),
        TIER4(4, 9, () -> Loaders.compactFusionCoil, 3, () -> ModBlocks.blockCasings3Misc, 12),
        TIER5(5, 10, () -> Loaders.compactFusionCoil, 4, () -> ModBlocks.blockCasings6Misc, 0),
        INVALID(-1, -1, () -> null, -1, () -> null, -1);

        /** Sentinel meaning "no / unmatched / unrecognised tier". */
        public static final int INVALID_TIER = -1;

        /** 1..5, or -1 for {@link #INVALID}. */
        public final int tier;
        /** GregTech voltage tier index (6=LuV, 7=ZPM, 8=UV, 9=UHV, 10=UEV), or -1 for {@link #INVALID}. */
        public final int voltageTier;
        /** A: reactor coil block resolver for this tier. */
        private final Supplier<Block> coilBlockSupplier;
        public final int coilMeta;
        /** B: fusion machine casing block resolver for this tier. */
        private final Supplier<Block> machineBlockSupplier;
        public final int machineMeta;

        LevelTier(int tier, int voltageTier, Supplier<Block> coilBlockSupplier, int coilMeta,
            Supplier<Block> machineBlockSupplier, int machineMeta) {
            this.tier = tier;
            this.voltageTier = voltageTier;
            this.coilBlockSupplier = coilBlockSupplier;
            this.coilMeta = coilMeta;
            this.machineBlockSupplier = machineBlockSupplier;
            this.machineMeta = machineMeta;
        }

        /**
         * Resolves the Good Generator reactor coil after all dependency blocks have been registered.
         *
         * @return current reactor coil block instance, or {@code null} only before dependency registration completes
         */
        public Block getCoilBlock() {
            return coilBlockSupplier.get();
        }

        /**
         * Resolves the GregTech or GT++ fusion casing after all dependency blocks have been registered.
         *
         * @return current fusion casing block instance, or {@code null} only before dependency registration completes
         */
        public Block getMachineBlock() {
            return machineBlockSupplier.get();
        }

        public boolean isValid() {
            return tier != INVALID_TIER;
        }

        public String getTierName() {
            if (!isValid()) return "INVALID";
            return switch (tier) {
                case 1 -> "I";
                case 2 -> "II";
                case 3 -> "III";
                case 4 -> "IV";
                case 5 -> "V";
                default -> String.valueOf(tier);
            };
        }

        public String getVoltageName() {
            return isValid() ? VN[voltageTier] : "INVALID";
        }

        /**
         * Is this tier greater than or equal to {@code other}? {@link #INVALID} is never "at least" a real tier.
         */
        public boolean isAtLeast(LevelTier other) {
            return isValid() && other != null && other.isValid() && tier >= other.tier;
        }

        /** Is this tier greater than or equal to the given tier number (1..5)? {@link #INVALID} is never. */
        public boolean isAtLeast(int minTier) {
            return isValid() && tier >= minTier;
        }

        public static LevelTier fromTier(int tier) {
            for (LevelTier level : values()) {
                if (level.tier == tier) return level;
            }
            return INVALID;
        }

        public static LevelTier getFromCoilBlock(Block block, int meta) {
            for (LevelTier level : values()) {
                if (level.isValid() && level.getCoilBlock() == block && level.coilMeta == meta) return level;
            }
            return INVALID;
        }

        public static LevelTier getFromMachineBlock(Block block, int meta) {
            for (LevelTier level : values()) {
                if (level.isValid() && level.getMachineBlock() == block && level.machineMeta == meta) return level;
            }
            return INVALID;
        }

        /**
         * Tier fetcher for StructureLib's {@code StructureUtility#ofBlocksTiered} used by structure element 'A'.
         *
         * @return the reactor coil tier of the given block, or {@code null} if it is not a recognised reactor coil.
         */
        public static Integer getCoilBlockTier(Block block, int meta) {
            LevelTier level = getFromCoilBlock(block, meta);
            return level.isValid() ? level.tier : null;
        }

        /**
         * Tier fetcher for StructureLib's {@code StructureUtility#ofBlocksTiered} used by structure element 'B'.
         *
         * @return the fusion machine casing tier of the given block, or {@code null} if it is not a recognised
         *         fusion machine casing.
         */
        public static Integer getMachineBlockTier(Block block, int meta) {
            LevelTier level = getFromMachineBlock(block, meta);
            return level.isValid() ? level.tier : null;
        }

        public static List<Pair<Block, Integer>> getCoilList() {
            return Arrays.asList(
                Pair.of(TIER1.getCoilBlock(), TIER1.coilMeta),
                Pair.of(TIER2.getCoilBlock(), TIER2.coilMeta),
                Pair.of(TIER3.getCoilBlock(), TIER3.coilMeta),
                Pair.of(TIER4.getCoilBlock(), TIER4.coilMeta),
                Pair.of(TIER5.getCoilBlock(), TIER5.coilMeta));
        }

        public static List<Pair<Block, Integer>> getMachineList() {
            return Arrays.asList(
                Pair.of(TIER1.getMachineBlock(), TIER1.machineMeta),
                Pair.of(TIER2.getMachineBlock(), TIER2.machineMeta),
                Pair.of(TIER3.getMachineBlock(), TIER3.machineMeta),
                Pair.of(TIER4.getMachineBlock(), TIER4.machineMeta),
                Pair.of(TIER5.getMachineBlock(), TIER5.machineMeta));
        }
    }
    // endregion

    // region Reactor coil (A) & fusion machine casing (B) tiers (independent int fields)
    private int fusionCoilTier = LevelTier.INVALID_TIER;
    private int fusionMachineTier = LevelTier.INVALID_TIER;
    private LevelTier levelTier = LevelTier.INVALID;
    /** Client-synced tier used for the controller's side texture; INVALID is rendered as MKI (1). */
    private int renderTier = 1;

    /** Structure tier as a plain int (1..5), or -1 ({@link LevelTier#INVALID_TIER}) if not formed / mismatched. */
    public int getStructureTier() {
        return levelTier.tier;
    }

    /**
     * Whether the current structure tier is at least the given tier.
     * <p>
     * Example: {@code if (isTierAtLeast(LevelTier.TIER3)) ...} returns {@code true} for TIER3/4/5 and
     * {@code false} when the machine is not formed (INVALID).
     */
    public boolean isTierAtLeast(LevelTier required) {
        return levelTier.isAtLeast(required);
    }

    /**
     * Whether the current structure tier is at least the given tier number (1..5).
     * <p>
     * Example: {@code if (isTierAtLeast(3)) ...} returns {@code true} for tiers 3..5 and {@code false}
     * for tiers 1..2 or when the machine is not formed (INVALID).
     */
    public boolean isTierAtLeast(int required) {
        return levelTier.isAtLeast(required);
    }

    /** A: reactor coil block of the current matched tier. */
    public Block getFusionCoilBlock() {
        return levelTier.getCoilBlock();
    }

    public int getFusionCoilMeta() {
        return levelTier.coilMeta;
    }

    /** B: fusion machine casing block of the current matched tier. */
    public Block getFusionMachineBlock() {
        return levelTier.getMachineBlock();
    }

    public int getFusionMachineMeta() {
        return levelTier.machineMeta;
    }
    // endregion

    // region DTPF-style GUI and runtime state
    private boolean convergence = false;
    private int catalystTypeForRecipesWithoutCatalyst = 1;

    // DTPF-style runtime efficiency scaling (3600s to full efficiency)
    private long running_time = 0;
    private static final double MAX_EFFICIENCY_TIME_TICKS = 3600 * 20; // 3600s in ticks
    private static final int EFFICIENCY_DECAY_RATE = 100; // same as MTEPlasmaForge

    public boolean getConvergenceStatus() {
        return convergence;
    }

    public void setConvergenceStatus(boolean value) {
        this.convergence = value;
    }

    public long getRunningTime() {
        return running_time;
    }

    /** Runtime progress 0..1: 0 = fresh, 1 = 3600s full efficiency. */
    private double getRuntimeProgress() {
        return Math.min(1.0, running_time / MAX_EFFICIENCY_TIME_TICKS);
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        aNBT.setInteger("catalystType", catalystTypeForRecipesWithoutCatalyst);
        aNBT.setBoolean("convergence", convergence);
        aNBT.setLong("eRunningTime", running_time);
        super.saveNBTData(aNBT);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        if (aNBT.hasKey("catalystType")) catalystTypeForRecipesWithoutCatalyst = aNBT.getInteger("catalystType");
        convergence = aNBT.getBoolean("convergence");
        if (aNBT.hasKey("eRunningTime")) running_time = aNBT.getLong("eRunningTime");
        super.loadNBTData(aNBT);
    }

    // DTPF (Plasma Forge) style GUI with the convergence / catalyst buttons.
    @Override
    protected @NotNull DimensionallyTranscendentPlasmaFusionComputerGui getGui() {
        return new DimensionallyTranscendentPlasmaFusionComputerGui(this);
    }
    // endregion

    @Override
    protected boolean isEnablePerfectOverclock() {
        return isTierAtLeast(5);
    }

    @Override
    protected float getEuModifier() {
        // Wireless mode: fixed 0.75 EU discount, independent of the 3600s runtime ramp.
        if (isEnableWireless()) return 0.75F;
        // Wired mode: EU/t bonus only kicks in after 3600s: 1.0 (no reduction) until then, 0.5 at/after full runtime.
        return getRuntimeProgress() >= 1.0 ? 0.5F : 1.0F;
    }

    @Override
    protected float getSpeedBonus() {
        // Wireless mode: fixed 0.75 duration modifier, independent of the 3600s runtime ramp.
        if (isEnableWireless()) return 0.75F;
        // Wired mode: durationModifier: 1.0 = normal time; only at 3600s does it become 0.5 (half recipe time).
        return getRuntimeProgress() >= 1.0 ? 0.5F : 1.0F;
    }

    @Override
    public int getMaxParallelRecipes() {
        // Wireless mode uses the player-selected parallel cap (like the Transcendent Plasma Mixer).
        if (isEnableWireless()) return Math.max(1, wirelessParallel);
        // Upper-bound fallback for the GUI / before a recipe is selected.
        // The actual parallel is set per recipe in validateRecipe:
        // maxParallel = (1 + machineTier - recipeTier) * 64, min 1.
        return Math.max(1, (1 + Math.max(1, getStructureTier())) * 64);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        if (mMachine) return -1;
        return survivalBuildPiece(
            STRUCTURE_PIECE_MAIN,
            stackSize,
            HORIZONTAL_OFFSET,
            VERTICAL_OFFSET,
            DEPTH_OFFSET,
            elementBudget,
            env,
            false,
            true);
    }

    @Override
    public IStructureDefinition<DimensionallyTranscendentPlasmaFusionComputer> getStructureDefinition() {
        if (structureDefinition == null) {
            structureDefinition = createStructureDefinition();
        }
        return structureDefinition;
    }

    // spotless:off
    @SuppressWarnings("SpellCheckingInspection")
    private static final String[][] structure_string = new String[][]{
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "},
        {"                                 ","         B   B     B   B         ","         B   B     B   B         ","         B   B     B   B         ","                                 ","                                 ","                                 ","         B   B     B   B         ","         B   B     B   B         "," BBB   BBB   B     B   BBB   BBB ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 "," BBB   BBB   B     B   BBB   BBB ","         B   B     B   B         ","         B   B     B   B         ","                                 ","                                 ","                                 ","         B   B     B   B         ","         B   B     B   B         ","         B   B     B   B         ","                                 "},
        {"         B   B     B   B         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         B   B     B   B         ","                                 ","         B   B     B   B         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","BDDDB BDDBAAAD     DAAABDDB BDDDB"," AAA   AAA   B     B   AAA   AAA "," AAA   AAA             AAA   AAA "," AAA   AAA             AAA   AAA ","BDDDB BDDDB           BDDDB BDDDB","                                 ","                                 ","                                 ","                                 ","                                 ","BDDDB BDDDB           BDDDB BDDDB"," AAA   AAA             AAA   AAA "," AAA   AAA             AAA   AAA "," AAA   AAA   B     B   AAA   AAA ","BDDDB BDDBAAAD     DAAABDDB BDDDB","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         B   B     B   B         ","                                 ","         B   B     B   B         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         B   B     B   B         "},
        {"         B   B     B   B         ","         DAAAD     DAAAD         ","      BBBDDDDDBBCBBDDDDDBBB      ","    CC   DAAAD     DAAAD   CC    ","   C     B   B     B   B     C   ","   C                         C   ","  B      B   B     B   B      B  ","  B      DAAAD     DAAAD      B  ","  B     CDDDDDBBCBBDDDDDC     B  ","BDDDB BDDBAAAD     DAAABDDB BDDDB"," ADA   ADA   B     B   ADA   ADA "," ADA   ADA             ADA   ADA "," ADA   ADA             ADA   ADA ","BDDDB BDDDB           BDDDB BDDDB","  B     B               B     B  ","  B     B               B     B  ","  C     C               C     C  ","  B     B               B     B  ","  B     B               B     B  ","BDDDB BDDDB           BDDDB BDDDB"," ADA   ADA             ADA   ADA "," ADA   ADA             ADA   ADA "," ADA   ADA   B     B   ADA   ADA ","BDDDB BDDBAAAD     DAAABDDB BDDDB","  B     CDDDDDBBCBBDDDDDC     B  ","  B      DAAAD     DAAAD      B  ","  B      B   B     B   B      B  ","   C                         C   ","   C     B   B     B   B     C   ","    CC   DAAAD     DAAAD   CC    ","      BBBDDDDDBBCBBDDDDDBBB      ","         DAAAD     DAAAD         ","         B   B     B   B         "},
        {"         B   B     B   B         ","         DAAAD     DAAAD         ","    CC   DAAAD     DAAAD   CC    ","         DAAAD     DAAAD         ","  C      BAAAB     BAAAB      C  ","  C      BAAAB     BAAAB      C  ","         BAAAB     BAAAB         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB"," AAAAAAAAA   B     B   AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA ","BDDDBBBDDDB           BDDDBBBDDDB","                                 ","                                 ","                                 ","                                 ","                                 ","BDDDBBBDDDB           BDDDBBBDDDB"," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA   B     B   AAAAAAAAA ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         BAAAB     BAAAB         ","  C      BAAAB     BAAAB      C  ","  C      BAAAB     BAAAB      C  ","         DAAAD     DAAAD         ","    CC   DAAAD     DAAAD   CC    ","         DAAAD     DAAAD         ","         B   B     B   B         "},
        {"                                 ","         B   B     B   B         ","   C     B   B     B   B     C   ","  C      BAAAB     BAAAB      C  ","                                 ","                                 ","                                 ","         BAAAB     BAAAB         ","         B   B     B   B         "," BBB   BBB   B     B   BBB   BBB ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," BBB   BBB   B     B   BBB   BBB ","         B   B     B   B         ","         BAAAB     BAAAB         ","                                 ","                                 ","                                 ","  C      BAAAB     BAAAB      C  ","   C     B   B     B   B     C   ","         B   B     B   B         ","                                 "},
        {"                                 ","                                 ","   C                         C   ","  C      BAAAB     BAAAB      C  ","                                 ","                                 ","                                 ","         BAAAB     BAAAB         ","                                 ","   B   B                 B   B   ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   ","   B   B                 B   B   ","                                 ","                                 ","                                 ","                                 ","                                 ","   B   B                 B   B   ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   ","   B   B                 B   B   ","                                 ","         BAAAB     BAAAB         ","                                 ","                                 ","                                 ","  C      BAAAB     BAAAB      C  ","   C                         C   ","                                 ","                                 "},
        {"                                 ","         B   B     B   B         ","  B      B   B     B   B      B  ","         BAAAB     BAAAB         ","                                 ","                                 ","                                 ","         BAAAB     BAAAB         ","         B   B     B   B         "," BBB   BBB   B     B   BBB   BBB ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," BBB   BBB   B     B   BBB   BBB ","         B   B     B   B         ","         BAAAB     BAAAB         ","                                 ","                                 ","                                 ","         BAAAB     BAAAB         ","  B      B   B     B   B      B  ","         B   B     B   B         ","                                 "},
        {"         B   B     B   B         ","         DAAAD     DAAAD         ","  B      DAAAD     DAAAD      B  ","         DAAAD     DAAAD         ","         BAAAB     BAAAB         ","         BAAAB     BAAAB         ","         BAAAB     BAAAB         ","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB"," AAAAAAAAA   B     B   AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA ","BDDDBBBDDDB           BDDDBBBDDDB","                                 ","                                 ","                                 ","                                 ","                                 ","BDDDBBBDDDB           BDDDBBBDDDB"," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA             AAAAAAAAA "," AAAAAAAAA   B     B   AAAAAAAAA ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB","         DAAAD     DAAAD         ","         DAAAD     DAAAD         ","         BAAAB     BAAAB         ","         BAAAB     BAAAB         ","         BAAAB     BAAAB         ","         DAAAD     DAAAD         ","  B      DAAAD     DAAAD      B  ","         DAAAD     DAAAD         ","         B   B     B   B         "},
        {"         B   B     B   B         ","         DAAAD     DAAAD         ","  B     CDDDDDBBCBBDDDDDC     B  ","         DAAAD     DAAAD         ","         B   B     B   B         ","                                 ","         B   B     B   B         ","         DAAAD     DAAAD         ","  C     CDDDDDBBCBBDDDDDC     C  ","BDDDB BDDBAAAD     DAAABDDB BDDDB"," ADA   ADA   B     B   ADA   ADA "," ADA   ADA             ADA   ADA "," ADA   ADA             ADA   ADA ","BDDDB BDDDB           BDDDB BDDDB","  B     B               B     B  ","  B     B               B     B  ","  C     C               C     C  ","  B     B               B     B  ","  B     B               B     B  ","BDDDB BDDDB           BDDDB BDDDB"," ADA   ADA             ADA   ADA "," ADA   ADA             ADA   ADA "," ADA   ADA   B     B   ADA   ADA ","BDDDB BDDBAAAD     DAAABDDB BDDDB","  C     CDDDDDBBCBBDDDDDC     C  ","         DAAAD     DAAAD         ","         B   B     B   B         ","                                 ","         B   B     B   B         ","         DAAAD     DAAAD         ","  B     CDDDDDBBCBBDDDDDC     B  ","         DAAAD     DAAAD         ","         B   B     B   B         "},
        {" BBB   BBB   B     B   BBB   BBB ","BDDDB BDDBAAAD     DAAABDDB BDDDB","BDDDB BDDBAAAD     DAAABDDB BDDDB","BDDDBBBDDBAAAD     DAAABDDBBBDDDB"," BBB   BB    B     B    BB   BBB ","   B   B                 B   B   "," BBB   BB    B     B    BB   BBB ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB","BDDDB BDDBAAAD     DAAABDDB BDDDB","BBBB   BBBAAAD     DAAABBB   BBBB"," AAA   AAA   B     B   AAA   AAA "," AAA   AAA             AAA   AAA "," AAA   AAA             AAA   AAA ","BDDDB BDDDB           BDDDB BDDDB","                                 ","                                 ","                                 ","                                 ","                                 ","BDDDB BDDDB           BDDDB BDDDB"," AAA   AAA             AAA   AAA "," AAA   AAA             AAA   AAA "," AAA   AAA   B     B   AAA   AAA ","BBBB   BBBAAAD     DAAABBB   BBBB","BDDDB BDDBAAAD     DAAABDDB BDDDB","BDDDBBBDDBAAAD     DAAABDDBBBDDDB"," BBB   BB    B     B    BB   BBB ","   B   B                 B   B   "," BBB   BB    B     B    BB   BBB ","BDDDBBBDDBAAAD     DAAABDDBBBDDDB","BDDDB BDDBAAAD     DAAABDDB BDDDB","BDDDB BDDBAAAD     DAAABDDB BDDDB"," BBB   BBB   B     B   BBB   BBB "},
        {"                                 "," AAA   AAA   B     B   AAA   AAA "," ADA   ADA   B     B   ADA   ADA "," AAAAAAAAA   B     B   AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA   B     B   AAAAAAAAA "," ADA   ADA   B     B   ADA   ADA "," AAA   AAA   B     B   AAA   AAA ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 "," AAA   AAA   B     B   AAA   AAA "," ADA   ADA   B     B   ADA   ADA "," AAAAAAAAA   B     B   AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA   B     B   AAAAAAAAA "," ADA   ADA   B     B   ADA   ADA "," AAA   AAA   B     B   AAA   AAA ","                                 "},
        {"                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 "},
        {"                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 "},
        {" BBB   BBB             BBB   BBB ","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDBBBDDDB           BDDDBBBDDDB"," BBB   BBB             BBB   BBB ","   B   B                 B   B   "," BBB   BBB             BBB   BBB ","BDDDBBBDDDB           BDDDBBBDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB"," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDBBBDDDB           BDDDBBBDDDB"," BBB   BBB             BBB   BBB ","   B   B                 B   B   "," BBB   BBB             BBB   BBB ","BDDDBBBDDDB           BDDDBBBDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB"," BBB   BBB             BBB   BBB "},
        {"                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 "},
        {"                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 "},
        {"                                 ","                                 ","  C     C               C     C  ","                                 ","                                 ","                                 ","                                 ","                                 ","  C     C               C     C  ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","  C     C               C     C  ","                                 ","                                 ","                                 ","                                 ","                                 ","  C     C               C     C  ","                                 ","                                 "},
        {"                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 "},
        {"                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 ","                                 ","                                 ","  B     B               B     B  "," BBB   BBB             BBB   BBB ","  B     B               B     B  ","                                 "},
        {" BBB   BBB             BBB   BBB ","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDBBBDDDB           BDDDBBBDDDB"," BBB   BBB             BBB   BBB ","   B   B                 B   B   "," BBB   BBB             BBB   BBB ","BDDDBBBDDDB           BDDDBBBDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB"," BBB   BBB             BBB   BBB ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," BBB   BBB             BBB   BBB ","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDBBBDDDB           BDDDBBBDDDB"," BBB   BBB             BBB   BBB ","   B   B                 B   B   "," BBB   BBB             BBB   BBB ","BDDDBBBDDDB           BDDDBBBDDDB","BDDDB BDDDB           BDDDB BDDDB","BDDDB BDDDB           BDDDB BDDDB"," BBB   BBB             BBB   BBB "},
        {"                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 "},
        {"                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 ","                ~                ","                                 ","                                 ","                                 ","                                 ","                                 ","                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 "},
        {"                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 ","                                 ","                                 ","                                 ","                                 ","                B                ","               BBB               ","                B                ","                                 ","                                 ","                                 ","                                 ","                                 "," AAA   AAA             AAA   AAA "," ADA   ADA             ADA   ADA "," AAAAAAAAA             AAAAAAAAA ","   A   A                 A   A   ","   A   A                 A   A   ","   A   A                 A   A   "," AAAAAAAAA             AAAAAAAAA "," ADA   ADA             ADA   ADA "," AAA   AAA             AAA   AAA ","                                 "},
        {" BBB   BBB             BBB   BBB ","BDDDB BDDDB    B B    BDDDB BDDDB","BDDDB BDDDBBBBBCBCBBBBBDDDB BDDDB","BDDDBBBDDDB    BDB    BDDDBBBDDDB"," BBB   BBB     BDB     BBB   BBB ","   B   B       BDB       B   B   "," BBB   BBB     BDB     BBB   BBB ","BDDDBBBDDDB    BDB    BDDDBBBDDDB","BDDDB BDDDBBBBBCBCBBBBBDDDB BDDDB","BDDDB BDDDB    BDB    BDDDB BDDDB"," BBB   BBB     BDB     BBB   BBB ","  B     B      BDB      B     B  ","  B     B      BDB      B     B  ","  B     B     BCBCB     B     B  ","  B     B    BDDDDDB    B     B  "," BCBBBBBCBBBBCDDDDDCBBBBCBBBBBCB ","  BDDDDDBDDDDBDDDDDBDDDDBDDDDDB  "," BCBBBBBCBBBBCDDDDDCBBBBCBBBBBCB ","  B     B    BDDDDDB    B     B  ","  B     B     BCBCB     B     B  ","  B     B      BDB      B     B  ","  B     B      BDB      B     B  "," BBB   BBB     BDB     BBB   BBB ","BDDDB BDDDB    BDB    BDDDB BDDDB","BDDDB BDDDBBBBBCBCBBBBBDDDB BDDDB","BDDDBBBDDDB    BDB    BDDDBBBDDDB"," BBB   BBB     BDB     BBB   BBB ","   B   B       BDB       B   B   "," BBB   BBB     BDB     BBB   BBB ","BDDDBBBDDDB    BDB    BDDDBBBDDDB","BDDDB BDDDBBBBBCBCBBBBBDDDB BDDDB","BDDDB BDDDB    B B    BDDDB BDDDB"," BBB   BBB             BBB   BBB "}
    };
    // spotless:on

    protected static final String STRUCTURE_PIECE_MAIN = "main";
    /*
     * Blocks:
     * A -> ofBlock...(compactFusionCoil, 0, ...);
     * B -> ofBlock...(gt.blockcasings, 6, ...);
     * C -> ofBlock...(gt.blockcasings.cyclotron_coils, 5, ...);
     * D -> ofBlock...(gt.blockcasings8, 7, ...);
     */

    private static IStructureDefinition<DimensionallyTranscendentPlasmaFusionComputer> structureDefinition;

    /**
     * Builds the StructureLib definition lazily, after Good Generator and GT++ have registered their block fields.
     * Building this during GT Not Good's pre-initialization used to permanently capture null dependency blocks, which
     * crashed the client when StructureLib attempted to render construction hints.
     *
     * @return fully resolved DTPF structure definition
     */
    private static IStructureDefinition<DimensionallyTranscendentPlasmaFusionComputer> createStructureDefinition() {
        return StructureDefinition.<DimensionallyTranscendentPlasmaFusionComputer>builder()
            .addShape(STRUCTURE_PIECE_MAIN, transpose(structure_string))
            // A: reactor coil block, tier is auto-detected & recorded into the fusionCoilTier int field.
            // All coil blocks must share the same tier, otherwise the piece check itself fails.
            .addElement(
                'A',
                ofBlocksTiered(
                    LevelTier::getCoilBlockTier,
                    LevelTier.getCoilList(),
                    LevelTier.INVALID_TIER,
                    DimensionallyTranscendentPlasmaFusionComputer::setFusionCoilTier,
                    DimensionallyTranscendentPlasmaFusionComputer::getFusionCoilTier,
                    Collections.singletonList(REACTOR_COIL_STRUCTURE_KEY)))
            // B: fusion machine casing block, same auto-detect scheme into the fusionMachineTier int field.
            .addElement(
                'B',
                ofBlocksTiered(
                    LevelTier::getMachineBlockTier,
                    LevelTier.getMachineList(),
                    LevelTier.INVALID_TIER,
                    DimensionallyTranscendentPlasmaFusionComputer::setFusionMachineTier,
                    DimensionallyTranscendentPlasmaFusionComputer::getFusionMachineTier,
                    Collections.singletonList(FUSION_CASING_STRUCTURE_KEY)))
            // C: static fixed-id block (gt.blockcasings.cyclotron_coils : 5 = ZPM Solenoid Superconductor Coil).
            .addElement('C', ofBlock(GregTechAPI.sSolenoidCoilCasings, 5))
            // D: functional block - compartments for input/output bus/hatch and energy hatch.
            // The fill casing is gt.blockcasings8 : 7.
            .addElement(
                'D',
                HatchElementBuilder.<DimensionallyTranscendentPlasmaFusionComputer>builder()
                    .anyOf(
                        HatchElement.InputBus,
                        HatchElement.OutputBus,
                        HatchElement.InputHatch,
                        HatchElement.OutputHatch,
                        HatchElement.Energy,
                        HatchElement.ExoticEnergy)
                    .casingIndex(GTUtility.getCasingTextureIndex(GregTechAPI.sBlockCasings8, 7))
                    .hint(1)
                    .buildAndChain(ofBlock(GregTechAPI.sBlockCasings8, 7)))
            .build();
    }

    @Override
    public RecipeMap<?> getRecipeMap() {
        // Use the Large Fusion recipe pool.
        return RecipeMaps.fusionRecipes;
    }

    @Override
    public @NotNull Collection<RecipeMap<?>> getAvailableRecipeMaps() {
        return super.getAvailableRecipeMaps();
    }

    @Override
    public void getWailaNBTData(EntityPlayerMP player, TileEntity tile, NBTTagCompound tag, World world, int x, int y,
        int z) {
        super.getWailaNBTData(player, tile, tag, world, x, y, z);
        final IGregTechTileEntity tileEntity = getBaseMetaTileEntity();
        if (tileEntity != null) {
            tag.setBoolean("enablePOC", isTierAtLeast(4));
            tag.setBoolean("pocspec", getLevelTier() == LevelTier.TIER5);
            tag.setInteger("machineTier", getStructureTier());
            tag.setLong("runningTime", running_time);
        }
    }

    @Override
    public ItemStack getWailaStack(IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return super.getWailaStack(accessor, config);
    }

    @Override
    public void getWailaBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaBody(itemStack, currentTip, accessor, config);
        final NBTTagCompound tag = accessor.getNBTData();
        // Only add the lines when the server actually wrote the keys (same "hasKey" gate as MTEPlasmaForge).
        if (tag.hasKey("runningTime")) {
            currentTip.add(
                RUNTIME_TEXT + ": " + EnumChatFormatting.WHITE + tag.getLong("runningTime") + EnumChatFormatting.RESET);
        }
        if (tag.hasKey("machineTier")) {
            int tier = tag.getInteger("machineTier");
            LevelTier level = LevelTier.fromTier(tier);
            String levelName = level.isValid() ? "MK" + level.getTierName() : "-";
            currentTip.add(MACHINE_LEVEL_TEXT + ": " + EnumChatFormatting.AQUA + levelName + EnumChatFormatting.RESET);
        }
        if (tag.hasKey("enablePOC")) {
            boolean pocEnabled = tag.getBoolean("enablePOC");
            boolean PocStatus = tag.getBoolean("pocspec");
            currentTip.add(
                OVERCLOCK_TEXT + ": "
                    + (pocEnabled
                        ? EnumChatFormatting.YELLOW + (PocStatus ? PERFECT_OVERCLOCK_MKV_TEXT : PERFECT_OVERCLOCK_TEXT)
                        : EnumChatFormatting.GREEN + IMPERFECT_OVERCLOCK_TEXT)
                    + EnumChatFormatting.RESET);
        }
    }

    @Override
    public boolean hasWailaAdvancedBody(ItemStack itemStack, IWailaDataAccessor accessor, IWailaConfigHandler config) {
        return super.hasWailaAdvancedBody(itemStack, accessor, config);
    }

    @Override
    public void getWailaAdvancedBody(ItemStack itemStack, List<String> currentTip, IWailaDataAccessor accessor,
        IWailaConfigHandler config) {
        super.getWailaAdvancedBody(itemStack, currentTip, accessor, config);
    }

    @Override
    public String[] getInfoData() {
        List<String> infoData = new ArrayList<>();
        if (getLevelTier() == LevelTier.TIER5) {
            infoData.add(
                OVERCLOCK_TEXT + ": "
                    + EnumChatFormatting.YELLOW
                    + PERFECT_OVERCLOCK_MKV_TEXT
                    + EnumChatFormatting.RESET);
        } else if (getLevelTier() == LevelTier.TIER4) {
            infoData.add(
                OVERCLOCK_TEXT + ": " + EnumChatFormatting.YELLOW + PERFECT_OVERCLOCK_TEXT + EnumChatFormatting.RESET);
        } else {
            infoData.add(
                OVERCLOCK_TEXT + ": " + EnumChatFormatting.GREEN + IMPERFECT_OVERCLOCK_TEXT + EnumChatFormatting.RESET);
        }

        return infoData.toArray(new String[0]);
    }

    @Override
    public void getExtraInfoData(List<String> info) {
        super.getExtraInfoData(info);
    }

    @Override
    protected ProcessingLogic createProcessingLogic() {
        return new ProcessingLogic() {

            @NotNull
            @Override
            public CheckRecipeResult process() {
                setEuModifier(getEuModifier());
                setSpeedBonus(getSpeedBonus());
                setOverclock(isEnablePerfectOverclock() ? 4 : 2, isTierAtLeast(4) ? 2 : 4);
                // level 5->4 speed 2 power
                // level 4->2 speed 2 power
                // level 1-3->2 speed 4 power
                return super.process();
            }

            @NotNull
            @Override
            protected ParallelHelper createParallelHelper(@NotNull GTRecipe recipe) {
                ParallelHelper helper = super.createParallelHelper(recipe);
                if (isEnableWireless()) {
                    // Wireless power is consumed directly from the network in onRecipeStart,
                    // so do not let ParallelHelper consume inputs before that check.
                    helper.setConsumption(false);
                }
                return helper;
            }

            @NotNull
            @Override
            protected OverclockCalculator createOverclockCalculator(@NotNull GTRecipe recipe) {
                // Wireless mode follows MTETranscendentPlasmaMixer's no-overclock behaviour, but keeps
                // DimensionallyTranscendentPlasmaFusionComputer's runtime efficiency modifiers so the wireless cost
                // matches what is actually paid.
                if (isEnableWireless()) {
                    return OverclockCalculator.ofNoOverclock(recipe)
                        .setEUtDiscount(getEuModifier())
                        .setDurationModifier(getSpeedBonus());
                }
                return super.createOverclockCalculator(recipe);
            }

            @NotNull
            @Override
            protected CheckRecipeResult validateRecipe(@NotNull GTRecipe recipe) {
                LevelTier current = getLevelTier();
                if (!current.isValid()) {
                    return CheckRecipeResultRegistry.insufficientPower(recipe.mEUt);
                }

                if (isEnableWireless()) {
                    // Wireless mode uses the player-selected parallel cap (like the Transcendent Plasma Mixer).
                    maxParallel = Math.max(1, wirelessParallel);
                } else {
                    // Parallel = (1 + machine tier - recipe tier) * 64, min 1.
                    // Recipe tier is the GT voltage tier mapped to MKI..MKV scale (LuV=1, ZPM=2, ... UEV=5).
                    int recipeTier = Math.max(1, GTUtility.getTier(recipe.mEUt) - 5);
                    maxParallel = Math.max(1, (1 + current.tier - recipeTier) * 64);
                }

                // Same restriction as the Fusion Computer, but without the startup-energy system:
                // a tier can only do recipes of its own voltage tier and below.
                if (recipe.mEUt > GTValues.V[current.voltageTier]) {
                    return CheckRecipeResultRegistry.insufficientPower(recipe.mEUt);
                }

                // Wireless mode: mimic MTETranscendentPlasmaMixer and cap the parallel to what the
                // global wireless balance can actually pay for. This check happens before any input is consumed.
                long wirelessEUt = recipe.mEUt;
                int wirelessDuration = recipe.mDuration;
                if (isEnableWireless()) {
                    wirelessEUt = (long) Math.ceil(recipe.mEUt * getEuModifier());
                    wirelessDuration = (int) Math.ceil(recipe.mDuration * getSpeedBonus());
                    BigInteger recipeEU = BigInteger.valueOf(wirelessEUt)
                        .multiply(BigInteger.valueOf(wirelessDuration));
                    if (ownerUUID == null || getUserEU(ownerUUID).compareTo(recipeEU) < 0) {
                        return CheckRecipeResultRegistry.insufficientStartupPower(recipeEU);
                    }
                    maxParallel = getUserEU(ownerUUID).divide(recipeEU)
                        .min(BigInteger.valueOf(maxParallel))
                        .intValue();
                }

                // Wireless mode: final shared safety check before input consumption.
                CheckRecipeResult wirelessResult = validateWirelessPowerForRecipe(
                    wirelessEUt,
                    wirelessDuration,
                    maxParallel);
                if (!wirelessResult.wasSuccessful()) {
                    return wirelessResult;
                }
                return CheckRecipeResultRegistry.SUCCESSFUL;
            }

            @NotNull
            @Override
            protected CheckRecipeResult onRecipeStart(@NotNull GTRecipe recipe) {
                if (isEnableWireless()) {
                    CheckRecipeResult wirelessResult = startWirelessRecipe(
                        recipe,
                        calculatedParallels,
                        calculatedEut,
                        duration,
                        inputFluids,
                        inputItems);
                    if (!wirelessResult.wasSuccessful()) {
                        return wirelessResult;
                    }
                    // Power was already deducted from the wireless network in one lump; don't also drain hatches.
                    overwriteCalculatedEut(0);
                }
                return super.onRecipeStart(recipe);
            }
        }.setMaxParallelSupplier(this::getLimitedMaxParallel);
    }

    @Override
    protected void setProcessingLogicPower(ProcessingLogic logic) {
        if (isEnableWireless()) {
            // Wireless mode pulls directly from the global wireless network, so the machine has no
            // energy-hatch voltage limit to respect (same idea as MTETranscendentPlasmaMixer).
            logic.setAvailableVoltage(Long.MAX_VALUE);
            logic.setAvailableAmperage(1);
            logic.setAmperageOC(false);
            logic.setUnlimitedTierSkips();
        } else {
            super.setProcessingLogicPower(logic);
        }
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        tt.addMachineType("Plasma Fusion, DTPF")
            .addSupportAny()
            .addInfo(EnumChatFormatting.YELLOW + TOOLTIP_RUNTIME)
            .addInfo(EnumChatFormatting.GRAY + TOOLTIP_RAMP)
            .addInfo(EnumChatFormatting.GOLD + TOOLTIP_PARALLEL)
            .addInfo(EnumChatFormatting.AQUA + TOOLTIP_EFFICIENCY)
            .addInfo(EnumChatFormatting.LIGHT_PURPLE + TOOLTIP_WIRELESS_PARALLEL)
            .addInfo(EnumChatFormatting.LIGHT_PURPLE + TOOLTIP_WIRELESS_DISCOUNT)
            .addStructureInfo("")
            .toolTipFinisher();
        return tt;
    }

    @NotNull
    @Override
    public CheckRecipeResult checkProcessing() {
        refreshWirelessAvailability();
        CheckRecipeResult result = super.checkProcessing();
        if (result.wasSuccessful() && !isEnableWireless()) {
            running_time += mMaxProgresstime;
        }
        return result;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide() && mMaxProgresstime == 0 && !isEnableWireless()) {
            running_time = Math.max(0, running_time - EFFICIENCY_DECAY_RATE);
        }
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        // Reset per-check state; the structure elements write the tier ints while walking the structure.
        setFusionCoilTier(LevelTier.INVALID_TIER);
        setFusionMachineTier(LevelTier.INVALID_TIER);
        levelTier = LevelTier.INVALID;
        renderTier = 1; // INVALID is rendered as MKI
        aBaseMetaTileEntity.sendBlockEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, getUpdateData());

        if (!checkPiece(STRUCTURE_PIECE_MAIN, HORIZONTAL_OFFSET, VERTICAL_OFFSET, DEPTH_OFFSET, errors)) return;

        LevelTier coilLevel = LevelTier.fromTier(getFusionCoilTier());
        LevelTier machineLevel = LevelTier.fromTier(getFusionMachineTier());

        // A (reactor coil) and B (fusion machine casing) must match their tier exactly.
        if (coilLevel == LevelTier.INVALID || machineLevel == LevelTier.INVALID || coilLevel != machineLevel) {
            errors.add(
                StructureErrors.of(
                    TIER_MISMATCH_STRUCTURE_KEY,
                    TranslatableText.literal(coilLevel.getVoltageName()),
                    TranslatableText.literal(machineLevel.getVoltageName())));
            return;
        }

        levelTier = coilLevel; // coilLevel == machineLevel
        renderTier = coilLevel.tier;
        refreshWirelessAvailability();
        aBaseMetaTileEntity.sendBlockEvent(GregTechTileClientEvents.CHANGE_CUSTOM_DATA, getUpdateData());

        // D compartments: input/output bus & hatch, and energy hatch requirements.
        // In wireless mode the machine draws directly from the global wireless network, so normal
        // energy hatches are forbidden (and are not used).
        checkHasAnyInput(errors);
        checkHasAnyOutput(errors);
        if (isWirelessModeActive()) {
            if (!areEnergyHatchesEmpty()) {
                errors.add(
                    StructureErrors.hatchCount(
                        ErrorType.TOO_MANY,
                        HatchElement.Energy,
                        mEnergyHatches.size() + mExoticEnergyHatches.size(),
                        0));
                return;
            }
        } else {
            checkHasAnyEnergy(errors);
        }
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new DimensionallyTranscendentPlasmaFusionComputer(mName);
    }

    @Override
    public boolean addOutput(FluidStack aLiquid) {
        if (aLiquid == null) return false;
        FluidStack tLiquid = aLiquid.copy();
        addOutputPartial(tLiquid);
        return tLiquid.amount == 0;
    }

    /**
     * Route the normal recipe-completion fluid output through {@link #addOutput(FluidStack)} (DTPF-style partial
     * ejection). This avoids the generic path being the only one used and matches how DTPF handles fluid output.
     */
    @Override
    protected boolean addFluidOutputs(FluidStack[] outputFluids) {
        boolean succeed = true;
        if (outputFluids == null) return true;
        for (FluidStack output : outputFluids) {
            if (output == null) continue;
            if (!addOutput(output)) {
                succeed = false;
            }
        }
        return succeed;
    }

    @Override
    public byte getUpdateData() {
        // Send the render tier (1..5) to the client; INVALID is sent as 1 (MKI).
        return (byte) renderTier;
    }

    @Override
    public void receiveClientEvent(byte aEventID, byte aValue) {
        super.receiveClientEvent(aEventID, aValue);
        if (aEventID == GregTechTileClientEvents.CHANGE_CUSTOM_DATA && aValue >= 1 && aValue <= 5) {
            renderTier = aValue;
        }
    }

    /**
     * Front face (side == facing) uses the Large Fusion Computer Mk-V style; other faces use the
     * fusion machine casing texture matching the current machine tier (INVALID falls back to MKI),
     * similar to how MTEPreciseAssembler varies its side texture by casing tier.
     */
    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection facing,
        int colorIndex, boolean aActive, boolean aRedstone) {
        if (side == facing) {
            // Front face: Large Fusion Computer Mk-V (fusion glass + MK-V screen overlay).
            return new ITexture[] { TextureFactory.builder()
                .addIcon(Textures.BlockIcons.MACHINE_CASING_FUSION_GLASS)
                .extFacing()
                .build(), getTextureOverlay() };
        }
        // Other faces: tier-dependent fusion machine casing (INVALID -> MKI).
        // Use TextureFactory.of(block, meta) directly so the side uses the actual block icon;
        // this avoids missing casing-texture entries for GT++/higher-tier casings.
        LevelTier textureTier = LevelTier.fromTier(renderTier);
        if (!textureTier.isValid()) {
            textureTier = LevelTier.TIER1;
        }
        return new ITexture[] { TextureFactory.of(textureTier.getMachineBlock(), textureTier.machineMeta) };
    }

    /**
     * Front face overlay, exactly like MTELargeFusionComputer5#getTextureOverlay:
     * rainbow screen while active, random screen while idle.
     */
    private ITexture getTextureOverlay() {
        return TextureFactory.of(
            TextureFactory.builder()
                .addIcon(
                    getBaseMetaTileEntity() != null && getBaseMetaTileEntity().isActive()
                        ? TexturesGtBlock.Casing_Machine_Screen_Rainbow
                        : TexturesGtBlock.Casing_Machine_Screen_1)
                .extFacing()
                .build());
    }

    @Override
    public ITexture getCasingTexture() {
        // Same casing as MTEPlasmaForge: casingTexturePages[0][DIM_BRIDGE_CASING] (DIM_BRIDGE_CASING == 14).
        return Textures.BlockIcons.casingTexturePages[0][14];
    }

    /** Returns the structure-derived matched reactor tier. */
    public LevelTier getLevelTier() {
        return levelTier;
    }

    public int getFusionCoilTier() {
        return fusionCoilTier;
    }

    public void setFusionCoilTier(int tier) {
        fusionCoilTier = tier;
    }

    public int getFusionMachineTier() {
        return fusionMachineTier;
    }

    public void setFusionMachineTier(int tier) {
        fusionMachineTier = tier;
    }

    public int getCatalystTypeForRecipesWithoutCatalyst() {
        return catalystTypeForRecipesWithoutCatalyst;
    }

    public void setCatalystTypeForRecipesWithoutCatalyst(int catalystType) {
        catalystTypeForRecipesWithoutCatalyst = catalystType;
    }

    /**
     * Toggles wireless intent on the server and immediately revalidates the structure so the energy-hatch rule
     * changes in the same interaction.
     */
    public void toggleWirelessModeFromServer() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide() || !isWirelessModeAvailable()) return;
        setWirelessModeEnabled(!isWirelessModeEnabled());
        checkStructure(true, base);
    }

    /** DTPF intentionally performs single-recipe parallelism only. */
    @Override
    protected boolean supportsCrossRecipeParallel() {
        return false;
    }

    @Override
    public void checkMaintenance() {}

    @Override
    public boolean getDefaultHasMaintenanceChecks() {
        return false;
    }

    @Override
    public boolean shouldCheckMaintenance() {
        return false;
    }

}
