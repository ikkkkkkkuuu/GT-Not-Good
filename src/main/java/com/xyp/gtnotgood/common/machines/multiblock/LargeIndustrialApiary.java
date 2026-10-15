/*
 * spotless:off
 * KubaTech - Gregtech Addon
 * Copyright (C) 2022 - 2024  kuba6000
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library. If not, see <https://www.gnu.org/licenses/>.
 * spotless:on
 */

package com.xyp.gtnotgood.common.machines.multiblock;

import static com.gtnewhorizon.structurelib.structure.StructureUtility.isAir;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlock;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlockAnyMeta;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofBlocksMap;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.ofChain;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.onElementPass;
import static com.gtnewhorizon.structurelib.structure.StructureUtility.transpose;
import static forestry.api.apiculture.BeeManager.beeRoot;
import static gregtech.api.enums.HatchElement.Energy;
import static gregtech.api.enums.HatchElement.InputBus;
import static gregtech.api.enums.HatchElement.Maintenance;
import static gregtech.api.enums.HatchElement.OutputBus;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW;
import static gregtech.api.enums.Textures.BlockIcons.OVERLAY_FRONT_DISTILLATION_TOWER_GLOW;
import static gregtech.api.util.GTStructureUtility.buildHatchAdder;
import static gregtech.api.util.GTStructureUtility.ofAnyWater;
import static gregtech.api.util.GTStructureUtility.ofOreDictBlockMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.structurelib.StructureLibAPI;
import com.gtnewhorizon.structurelib.alignment.IAlignmentLimits;
import com.gtnewhorizon.structurelib.alignment.constructable.ISurvivalConstructable;
import com.gtnewhorizon.structurelib.structure.IStructureDefinition;
import com.gtnewhorizon.structurelib.structure.IStructureElementNoPlacement;
import com.gtnewhorizon.structurelib.structure.ISurvivalBuildEnvironment;
import com.gtnewhorizon.structurelib.structure.StructureDefinition;
import com.xyp.gtnotgood.common.gui.modularui.multiblock.LargeIndustrialApiaryGui;
import com.xyp.gtnotgood.common.machines.multiblock.multiMachineBase.GTNGMultiBlockBase;
import com.xyp.gtnotgood.utils.enums.ModsItemlist;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import forestry.api.apiculture.EnumBeeType;
import forestry.api.apiculture.FlowerManager;
import forestry.api.apiculture.IAlleleBeeSpecies;
import forestry.api.apiculture.IBee;
import forestry.api.apiculture.IBeeGenome;
import forestry.api.apiculture.IBeeModifier;
import forestry.api.apiculture.IBeekeepingMode;
import forestry.apiculture.blocks.BlockAlveary;
import forestry.apiculture.blocks.BlockApicultureType;
import forestry.apiculture.genetics.Bee;
import forestry.plugins.PluginApiculture;
import gregtech.api.GregTechAPI;
import gregtech.api.casing.Casings;
import gregtech.api.enums.GTValues;
import gregtech.api.enums.SoundResource;
import gregtech.api.enums.Textures;
import gregtech.api.interfaces.ITexture;
import gregtech.api.interfaces.metatileentity.IMetaTileEntity;
import gregtech.api.interfaces.tileentity.ICasingTextureProvider;
import gregtech.api.interfaces.tileentity.IGregTechDeviceInformation;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.recipe.check.CheckRecipeResult;
import gregtech.api.recipe.check.CheckRecipeResultRegistry;
import gregtech.api.recipe.check.SimpleCheckRecipeResult;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTRecipe;
import gregtech.api.util.GTRecipeBuilder;
import gregtech.api.util.GTStructureUtility;
import gregtech.api.util.GTUtility;
import gregtech.api.util.GTUtility.ItemId;
import gregtech.api.util.ItemEjectionHelper;
import gregtech.api.util.MultiblockTooltipBuilder;
import gregtech.common.gui.modularui.multiblock.base.MTEMultiBlockBaseGui;
import gregtech.common.tileentities.machines.RecipeCheckReason;
import kubatech.client.effect.MegaApiaryBeesRenderer;

@IMetaTileEntity.SkipGenerateDescription
public class LargeIndustrialApiary extends GTNGMultiBlockBase<LargeIndustrialApiary>
    implements ISurvivalConstructable, ICasingTextureProvider {

    protected int casingCount = 0;
    public int maxSlots = 0;
    private boolean needsWaterFill = false;

    public int primaryMode = MODE_PRIMARY_INPUT;
    public int secondaryMode = MODE_SECONDARY_NORMAL;

    public final ArrayList<BeeSimulator> storage = new ArrayList<>();
    public final HashMap<ItemId, Double> dropProgress = new HashMap<>();

    public static final int MODE_PRIMARY_INPUT = 0;
    public static final int MODE_PRIMARY_OUTPUT = 1;
    public static final int MODE_PRIMARY_OPERATING = 2;

    public static final int MODE_SECONDARY_NORMAL = 0;
    public static final int MODE_SECONDARY_SWARMER = 1;

    protected static final ItemStack royalJelly = PluginApiculture.items.royalJelly.getItemStack(1);

    private static final GTRecipe SWARMER_JELLY_RECIPE = GTRecipeBuilder.empty()
        .itemInputsUnsafe(GTUtility.copyAmountUnsafe(100, royalJelly)).nbtSensitive().build()
        .orElseThrow(IllegalStateException::new);

    protected static final int CASING_INDEX = 10;
    protected static final String STRUCTURE_PIECE_MAIN = "main";
    protected static final String STRUCTURE_PIECE_FLOWERS = "flowers";
    protected static final String STRUCTURE_PIECE_MAIN_SURVIVAL = "mainsurvival";

    protected static final int STORAGE_VERSION = 2;

    private static final String[][] struct = transpose(new String[][] { // spotless:off
        {"               ","               ","               ","      HHH      ","    HHAAAHH    ","    HAPLPAH    ","   HAPAAAPAH   ","   HALAAALAH   ","   HAPAAAPAH   ","    HAPLPAH    ","    HHAAAHH    ","      HHH      ","               ","               ","               "},
        {"               ","               ","      GGG      ","    GG   GG    ","   G       G   ","   G       G   ","  G         G  ","  G         G  ","  G         G  ","   G       G   ","   G       G   ","    GG   GG    ","      GGG      ","               ","               "},
        {"               ","      HHH      ","   HHH   HHH   ","  HG       GH  ","  H         H  ","  H         H  "," H           H "," H           H "," H           H ","  H         H  ","  H         H  ","  HG       GH  ","   HHH   HHH   ","      HHH      ","               "},
        {"      GGG      ","   GGG   GGG   ","  G         G  "," G           G "," G           G "," G           G ","G             G","G             G","G             G"," G           G "," G           G "," G           G ","  G         G  ","   GGG   GGG   ","      GGG      "},
        {"      AAA      ","   OLA   ALO   ","  P         P  "," O           O "," L           L "," A           A ","A             A","A             A","A             A"," A           A "," L           L "," O           O ","  P         P  ","   OLA   ALO   ","      AAA      "},
        {"     AAAAA     ","   NA     AO   ","  P         P  "," N           O "," A           A ","A             A","A     III     A","A     III     A","A     III     A","A             A"," A           A "," N           N ","  P         P  ","   NA     AN   ","     AAAAA     "},
        {"     AAAAA     ","   NA FFF AO   ","  PFF     FFP  "," NFF       FFO "," AF         FA ","A             A","AF    JJJ    FA","AF    JKJ    FA","AF    JJJ    FA","A             A"," AF         FA "," NFF       FFN ","  PFF     FFP  ","   NA FFF AN   ","     AAAAA     "},
        {"      AAA      ","   OLAFFFALO   ","  PFFFFFFFFFP  "," OFFFF   FFFFO "," LFF       FFL "," AFF FFFFF  FA ","AFF  FKKKFF FFA","AFF FFKKKFF FFA","AFF FFKKKF  FFA"," AF  FFFFF  FA "," LFF   FF  FFL "," OFFFF    FFFO ","  PFFFFFFFFFP  ","   OLAFFFALO   ","      AAA      "},
        {"      G~G      ","   GGGBBBGGG   ","  GBBFFFFFBBG  "," GBBFF   FFBBG "," GBF       FBG "," GFF FFFFF  FG ","GBF  FKKKFF FBG","GBF FFKJKFF FBG","GBF FFKKKF  FBG"," GF  FFFFF  FG "," GBF   FF  FBG "," GBBFF    FBBG ","  GBBFFFFFBBG  ","   GGGBBBGGG   ","      GGG      "},
        {"      HHH      ","    HHBBBHH    ","  HHBBBBBBBHH  ","  HBBBWWWBBBH  "," HBBWWWWWWWBBH "," HBBWBBBBBWWBH ","HBBWWBBBBBBWBBH","HBBWBBBBBBBWBBH","HBBWBBBBBBWWBBH"," HBWWBBBBBWWBH "," HBBWWWBBWWBBH ","  HBBBWWWWBBH  ","  HHBBBBBBBHH  ","    HHBBBHH    ","      HHH      "},
        {"               ","     GGGGG     ","   GGBBBBBGG   ","  GBBBBBBBBBG  ","  GBBBBBBBBBG  "," GBBBBBBBBBBBG "," GBBBBBBBBBBBG "," GBBBBBBBBBBBG "," GBBBBBBBBBBBG "," GBBBBBBBBBBBG ","  GBBBBBBBBBG  ","  GBBBBBBBBBG  ","   GGBBBBBGG   ","     GGGGG     ","               "},
        {"               ","      HHH      ","    HHBBBHH    ","   HBBBBBBBH   ","  HBBBBBBBBBH  ","  HBBBBBBBBBH  "," HBBBBBBBBBBBH "," HBBBBBBBBBBBH "," HBBBBBBBBBBBH ","  HBBBBBBBBBH  ","  HBBBBBBBBBH  ","   HBBBBBBBH   ","    HHBBBHH    ","      HHH      ","               "},
        {"               ","               ","      GGG      ","    GGBBBGG    ","   GBBBBBBBG   ","   GBBBBBBBG   ","  GBBBBBBBBBG  ","  GBBBBBBBBBG  ","  GBBBBBBBBBG  ","   GBBBBBBBG   ","   GBBBBBBBG   ","    GGBBBGG    ","      GGG      ","               ","               "},
        {"               ","               ","       H       ","     HHBHH     ","    HBBBBBH    ","   HBBBBBBBH   ","   HBBBBBBBH   ","  HBBBBBBBBBH  ","   HBBBBBBBH   ","   HBBBBBBBH   ","    HBBBBBH    ","     HHBHH     ","       H       ","               ","               "},
        {"               ","               ","               ","       G       ","     GGBGG     ","    GBBBBBG    ","    GBBBBBG    ","   GBBBBBBBG   ","    GBBBBBG    ","    GBBBBBG    ","     GGBGG     ","       G       ","               ","               ","               "},
        {"               ","               ","               ","               ","      HHH      ","     HHHHH     ","    HHBBBHH    ","    HHBBBHH    ","    HHBBBHH    ","     HHHHH     ","      HHH      ","               ","               ","               ","               "},
        {"               ","               ","               ","               ","               ","               ","      GGG      ","      GHG      ","      GGG      ","               ","               ","               ","               ","               ","               "}
    }); // spotless:on

    private static final IStructureDefinition<LargeIndustrialApiary> STRUCTURE_DEFINITION = StructureDefinition
        .<LargeIndustrialApiary>builder().addShape(STRUCTURE_PIECE_MAIN, struct)
        .addShape(STRUCTURE_PIECE_MAIN_SURVIVAL,
            Arrays.stream(struct).map(sa -> Arrays.stream(sa).map(s -> s.replace("F", " ")).toArray(String[]::new))
                .toArray(String[][]::new))
        .addShape(STRUCTURE_PIECE_FLOWERS,
            Arrays.stream(struct)
                .map(sa -> Arrays.stream(sa).map(s -> s.replaceAll("[^F]", " ")).toArray(String[]::new))
                .toArray(String[][]::new))
        .addElement('A', ofBlock(Block.getBlockFromItem(ModsItemlist.BorosilicateGlass.get(1).getItem()), 0))
        .addElement('B', ofChain(ofBlockAnyMeta(Blocks.dirt, 0), ofBlock(Blocks.grass, 0)))
        .addElement('G',
            buildHatchAdder(LargeIndustrialApiary.class).atLeast(InputBus, OutputBus, Energy, Maintenance)
                .casingIndex(CASING_INDEX).hint(1)
                .buildAndChain(onElementPass(t -> t.casingCount++, ofBlock(GregTechAPI.sBlockCasings1, 10))))
        .addElement('H', ofBlocksMap(ofOreDictBlockMap("plankWood"), Blocks.planks, 0))
        .addElement('I', ofBlocksMap(ofOreDictBlockMap("slabWood"), Blocks.wooden_slab, 0))
        .addElement('J', ofBlock(PluginApiculture.blocks.apiculture, BlockApicultureType.APIARY.getMeta()))
        .addElement('K', ofBlock(PluginApiculture.blocks.alveary, BlockAlveary.Type.PLAIN.ordinal()))
        .addElement('L', ofBlock(PluginApiculture.blocks.alveary, BlockAlveary.Type.HYGRO.ordinal()))
        .addElement('N', ofBlock(PluginApiculture.blocks.alveary, BlockAlveary.Type.STABILIZER.ordinal()))
        .addElement('O', ofBlock(PluginApiculture.blocks.alveary, BlockAlveary.Type.HEATER.ordinal()))
        .addElement('P', ofBlock(PluginApiculture.blocks.alveary, BlockAlveary.Type.FAN.ordinal()))
        .addElement('W', ofChain(ofAnyWater(false), isAir())).addElement('F', new IStructureElementNoPlacement<>() {

            @Override
            public boolean check(LargeIndustrialApiary mte, World world, int x, int y, int z) {
                mte.flowerCheck(world, x, y, z);
                return true;
            }

            @Override
            public boolean spawnHint(LargeIndustrialApiary mte, World world, int x, int y, int z, ItemStack trigger) {
                StructureLibAPI.hintParticle(world, x, y, z, StructureLibAPI.getBlockHint(), 2 - 1);
                return true;
            }
        }).build();

    public LargeIndustrialApiary(int aID, String aName, String aNameRegional) {
        super(aID, aName, aNameRegional);
    }

    public LargeIndustrialApiary(String aName) {
        super(aName);
    }

    @Override
    public void onRemoval() {
        super.onRemoval();
        IGregTechTileEntity bmte = getBaseMetaTileEntity();
        if (!bmte.isServerSide()) return;
        for (BeeSimulator s : storage) {
            EntityItem item = new EntityItem(bmte.getWorld(), bmte.getXCoord(), bmte.getYCoord(), bmte.getZCoord(),
                s.queenStack);
            item.delayBeforeCanPickup = 10;
            bmte.getWorld().spawnEntityInWorld(item);
        }
        storage.clear();
    }

    private boolean missingFlowers = true;
    private boolean needsTVarUpdate = true;
    private int storageVersion;
    private long lastInputPower = -1;
    private List<String> flowerTypes = Collections.emptyList();

    private void flowerCheck(World world, int x, int y, int z) {
        if (!missingFlowers || world.isAirBlock(x, y, z)) return;
        for (String flowerType : flowerTypes) {
            if (FlowerManager.flowerRegistry.isAcceptedFlower(flowerType, world, x, y, z)) {
                missingFlowers = false;
                return;
            }
        }
    }

    public void onStorageContentChanged(boolean ignoreFlowerCheck) {
        if (!ignoreFlowerCheck) checkRequiredFlowers();
        onStateChanged();
    }

    private void onStateChanged() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        if (base == null || !base.isServerSide()) return;
        markDirty();
        scheduleRecipeCheck(RecipeCheckReason.IMMEDIATE);
    }

    @Override
    public void construct(ItemStack stackSize, boolean hintsOnly) {
        buildPiece(STRUCTURE_PIECE_MAIN, stackSize, hintsOnly, 7, 8, 0);
    }

    @Override
    public int survivalConstruct(ItemStack stackSize, int elementBudget, ISurvivalBuildEnvironment env) {
        int built = survivalBuildPiece(STRUCTURE_PIECE_MAIN_SURVIVAL, stackSize, 7, 8, 0, elementBudget, env, true);
        if (built == -1) {
            GTUtility.sendChatToPlayer(env.getActor(),
                // #tr chat.gtnotgood.largeIndustrialApiary.place_flowers
                // # Structure built. Place any flower in a flower position.
                // # zh_CN 结构建造完成，请在花位放置任意花源。
                StatCollector.translateToLocal("chat.gtnotgood.largeIndustrialApiary.place_flowers"));
            return 0;
        }
        return built;
    }

    @Override
    public IStructureDefinition<LargeIndustrialApiary> getStructureDefinition() {
        return STRUCTURE_DEFINITION;
    }

    @Override
    protected IAlignmentLimits getInitialAlignmentLimits() {
        return (d, r, f) -> d.offsetY == 0 && r.isNotRotated();
    }

    @Override
    protected MultiblockTooltipBuilder createTooltip() {
        MultiblockTooltipBuilder tt = new MultiblockTooltipBuilder();
        // #tr tooltip.gtnotgood.largeIndustrialApiary.type
        // # Industrial Apiary
        // # zh_CN 工业蜂箱
        tt.addMachineType(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.type"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.capacity
        // # Queen capacity: floor(total input EU/t / 32) x 8.
        // # zh_CN 蜂后容量：向下取整（总输入功率 / 32 EU/t）× 8。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.capacity"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.energy
        // # Normal mode uses 32 EU/t per stored queen; maintenance penalties apply.
        // # zh_CN 普通模式每只已存蜂后基础消耗 32 EU/t，维护故障会增加实际耗电。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.energy"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.power
        // # All stored queens must fit the capacity and available power to run.
        // # zh_CN 全部已存蜂后必须同时满足容量和供电要求，才能开始生产。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.power"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.production
        // # Produces every 5 seconds at 64x speed; queens do not age.
        // # zh_CN 每 5 秒结算一次，以 64 倍时间加速生产，蜂后不会衰老。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.production"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.upgrades
        // # Uses 8 production upgrades; total input power also boosts production.
        // # zh_CN 内置相当于 8 个生产升级的加成，总输入功率也会影响产量。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.upgrades"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.environment
        // # Ignores climate, weather, time and specialty conditions.
        // # zh_CN 忽略温湿度、天气、昼夜和特产欣喜条件。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.environment"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.flowers
        // # Normal mode needs any one registered flower source in a flower position.
        // # zh_CN 普通模式只需任一花位有任意已注册花源，无需匹配蜂后基因。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.flowers"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.jelly
        // # Up to 40 Royal Jelly per queen per cycle boosts drops up to 3x.
        // # zh_CN 每轮每只蜂后最多消耗 40 个蜂王浆，按实际用量将产出提升至最多 3 倍。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.jelly"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.swarmer
        // # Swarmer: 1 queen, 100 Royal Jelly, 60 s, 8190 EU/t; normal overclocking.
        // # zh_CN 分蜂：1 只蜂后、100 蜂王浆、60 秒、8190 EU/t，支持普通超频。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.swarmer"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.princess
        // # Swarmer produces an ignoble princess copy and preserves the queen.
        // # zh_CN 分蜂输出保留基因的劣等公主蜂副本，原蜂后保留。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.princess"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.modes
        // # Screwdriver: input/output/run; sneak to switch normal/swarmer when empty.
        // # zh_CN 螺丝刀切换输入/输出/运行；空仓时潜行使用可切换普通/分蜂。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.modes"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.glass
        // # Requires standard Borosilicate Glass. Energy hatches have no tier restriction.
        // # zh_CN 玻璃固定使用普通硼玻璃方块，能源仓无等级限制。
        tt.addInfo(StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.glass"));
        tt.beginStructureBlock(15, 17, 15, true);
        tt.addController(StatCollector.translateToLocal("gt.mbtt.structure.front_center_9th_layer"));
        // #tr tooltip.gtnotgood.largeIndustrialApiary.dirt
        // # Dirt or Grass
        // # zh_CN 泥土或草方块
        tt.addCasing("457", StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.dirt"), false);
        tt.addCasing("190-200", Casings.BronzePlatedBricks.getLocalizedName(), false);
        // #tr tooltip.gtnotgood.largeIndustrialApiary.planks
        // # Wooden Planks
        // # zh_CN 木板
        tt.addCasing("185", StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.planks"), false);
        tt.addCasing("121", ModsItemlist.BorosilicateGlass.get(1).getDisplayName(), false);
        // #tr tooltip.gtnotgood.largeIndustrialApiary.water
        // # Water (automatically filled)
        // # zh_CN 水（自动补充，无需输入）
        tt.addCasing("34", StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.water"), false);
        tt.addCasing("24",
            new ItemStack(PluginApiculture.blocks.alveary, 1, BlockAlveary.Type.FAN.ordinal()).getDisplayName(), false);
        tt.addCasing("20",
            new ItemStack(PluginApiculture.blocks.alveary, 1, BlockAlveary.Type.HEATER.ordinal()).getDisplayName(),
            false);
        tt.addCasing("20",
            new ItemStack(PluginApiculture.blocks.alveary, 1, BlockAlveary.Type.HYGRO.ordinal()).getDisplayName(),
            false);
        tt.addCasing("18",
            new ItemStack(PluginApiculture.blocks.alveary, 1, BlockAlveary.Type.PLAIN.ordinal()).getDisplayName(),
            false);
        tt.addCasing("12",
            new ItemStack(PluginApiculture.blocks.alveary, 1, BlockAlveary.Type.STABILIZER.ordinal()).getDisplayName(),
            false);
        tt.addCasing("9",
            new ItemStack(PluginApiculture.blocks.apiculture, 1, BlockApicultureType.APIARY.getMeta()).getDisplayName(),
            false);
        // #tr tooltip.gtnotgood.largeIndustrialApiary.slabs
        // # Wooden Slabs
        // # zh_CN 木台阶
        tt.addCasing("9", StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.slabs"), false);
        // #tr tooltip.gtnotgood.largeIndustrialApiary.flower_position
        // # Any registered flower source (normal mode)
        // # zh_CN 任意已注册花源（普通模式需要）
        tt.addCasing("1+", StatCollector.translateToLocal("tooltip.gtnotgood.largeIndustrialApiary.flower_position"),
            false);
        tt.addEnergyHatch("1+", StatCollector.translateToLocal("gt.mbtt.structure.any_casing"), 1);
        tt.addMaintenanceHatch("1+", StatCollector.translateToLocal("gt.mbtt.structure.any_casing"), 1);
        tt.addInputBus("0+", StatCollector.translateToLocal("gt.mbtt.structure.any_casing"), 1);
        tt.addOutputBus("1+", StatCollector.translateToLocal("gt.mbtt.structure.any_casing"), 1);
        tt.toolTipFinisher();
        return tt;
    }

    @Override
    public void saveNBTData(NBTTagCompound aNBT) {
        super.saveNBTData(aNBT);
        aNBT.setInteger("primaryMode", primaryMode);
        aNBT.setInteger("secondaryMode", secondaryMode);
        aNBT.setInteger("mStorageSize", storage.size());
        for (int i = 0; i < storage.size(); i++) aNBT.setTag("storage." + i, storage.get(i).toNBTTagCompound());
        aNBT.setInteger("STORAGE_VERSION", STORAGE_VERSION);
        NBTTagList progress = new NBTTagList();
        dropProgress.forEach((id, amount) -> {
            NBTTagCompound entry = new NBTTagCompound();
            entry.setTag("stack", id.getItemStack(1).writeToNBT(new NBTTagCompound()));
            entry.setDouble("amount", amount);
            progress.appendTag(entry);
        });
        aNBT.setTag("dropProgress", progress);
    }

    @Override
    public void loadNBTData(NBTTagCompound aNBT) {
        super.loadNBTData(aNBT);
        primaryMode = aNBT.getInteger("primaryMode");
        secondaryMode = aNBT.getInteger("secondaryMode");
        primaryMode = Math.max(MODE_PRIMARY_INPUT, Math.min(MODE_PRIMARY_OPERATING, primaryMode));
        secondaryMode = Math.max(MODE_SECONDARY_NORMAL, Math.min(MODE_SECONDARY_SWARMER, secondaryMode));
        storage.clear();
        for (int i = 0, isize = aNBT.getInteger("mStorageSize"); i < isize; i++)
            storage.add(new BeeSimulator(aNBT.getCompoundTag("storage." + i)));
        storageVersion = aNBT.getInteger("STORAGE_VERSION");
        dropProgress.clear();
        NBTTagList progress = aNBT.getTagList("dropProgress", 10);
        for (int i = 0; i < progress.tagCount(); i++) {
            NBTTagCompound entry = progress.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(entry.getCompoundTag("stack"));
            double amount = entry.getDouble("amount");
            if (stack != null && Double.isFinite(amount) && amount >= 0) {
                dropProgress.put(ItemId.createNoCopy(stack), amount);
            }
        }
        needsTVarUpdate = true;
        onStorageContentChanged(true);
    }

    public boolean setPrimaryMode(int mode) {
        if (mode < MODE_PRIMARY_INPUT || mode > MODE_PRIMARY_OPERATING || !canChangeMode()) return false;
        primaryMode = mode;
        onStateChanged();
        return true;
    }

    public boolean setSecondaryMode(int mode) {
        if (
            mode < MODE_SECONDARY_NORMAL || mode > MODE_SECONDARY_SWARMER
                || !canChangeMode()
                || !canChangeSecondaryMode()
        ) return false;
        secondaryMode = mode;
        updateMaxSlots();
        onStateChanged();
        return true;
    }

    private boolean canChangeMode() {
        IGregTechTileEntity base = getBaseMetaTileEntity();
        return base != null && base.isServerSide() && mMaxProgresstime <= 0;
    }

    public boolean canChangeSecondaryMode() {
        return mMaxProgresstime <= 0 && storage.isEmpty();
    }

    @Override
    public void onScrewdriverRightClick(ForgeDirection side, EntityPlayer player, float x, float y, float z,
        ItemStack tool) {
        if (player.isSneaking()) {
            if (!setSecondaryMode((secondaryMode + 1) % 2)) {
                // #tr chat.gtnotgood.largeIndustrialApiary.empty_idle
                // # Stop the machine and empty its queen storage before changing the operating mode.
                // # zh_CN 请先停止机器并清空蜂后库存，再切换普通/分蜂模式。
                GTUtility.sendChatToPlayer(player,
                    StatCollector.translateToLocal("chat.gtnotgood.largeIndustrialApiary.empty_idle"));
                return;
            }
        } else if (!setPrimaryMode((primaryMode + 1) % 3)) {
            // #tr chat.gtnotgood.largeIndustrialApiary.running
            // # Stop the machine before changing its mode.
            // # zh_CN 请先停止机器，再切换模式。
            GTUtility.sendChatToPlayer(player,
                StatCollector.translateToLocal("chat.gtnotgood.largeIndustrialApiary.running"));
            return;
        }
        GTUtility.sendChatToPlayer(player, StatCollector.translateToLocal(getModeKey()));
    }

    public static int calculateCapacity(long inputPower) {
        long slots = Math.max(0, inputPower) / GTValues.V[1];
        return (int) Math.min(Integer.MAX_VALUE, Math.min(slots, Integer.MAX_VALUE) * 8L);
    }

    private void updateMaxSlots() {
        long inputPower = getMaxInputEu();
        maxSlots = secondaryMode == MODE_SECONDARY_NORMAL ? calculateCapacity(inputPower)
            : inputPower >= GTValues.V[1] ? 1 : 0;
        if (lastInputPower != inputPower) {
            needsTVarUpdate = true;
            lastInputPower = inputPower;
        }
    }

    public double getVoltageTierExact() {
        return Math.log(Math.max(1, getMaxInputEu()) / 8d) / Math.log(4d) + 1e-8d;
    }

    @Override
    public boolean needsClientTick() {
        return true;
    }

    @Override
    public void onPostTick(IGregTechTileEntity aBaseMetaTileEntity, long aTick) {
        super.onPostTick(aBaseMetaTileEntity, aTick);
        if (aBaseMetaTileEntity.isServerSide()) {
            if (needsWaterFill && aTick % 20 == 0) {
                if (
                    GTStructureUtility.fillStructureWithWater(aBaseMetaTileEntity, getExtendedFacing(), struct, 7, 8, 0,
                        'W')
                ) {
                    needsWaterFill = false;
                }
            }
            if (mUpdate < -550) mUpdate = 50;
        } else {
            if (aBaseMetaTileEntity.isActive() && aTick % 100 == 0) {
                int[] abc = new int[] { 0, -2, 7 };
                int[] xyz = new int[] { 0, 0, 0 };
                this.getExtendedFacing().getWorldOffset(abc, xyz);
                xyz[0] += aBaseMetaTileEntity.getXCoord();
                xyz[1] += aBaseMetaTileEntity.getYCoord();
                xyz[2] += aBaseMetaTileEntity.getZCoord();
                showBees(aBaseMetaTileEntity.getWorld(), xyz[0], xyz[1], xyz[2], 100);
            }
        }
    }

    @SideOnly(Side.CLIENT)
    private void showBees(World world, int x, int y, int z, int age) {
        MegaApiaryBeesRenderer bee = new MegaApiaryBeesRenderer(world, x, y, z, age);
        Minecraft.getMinecraft().effectRenderer.addEffect(bee);
    }

    @Override
    @NotNull
    public CheckRecipeResult checkProcessing() {
        updateMaxSlots();
        if (primaryMode == MODE_PRIMARY_INPUT) {
            if (storage.size() >= maxSlots) return CheckRecipeResultRegistry.NO_RECIPE;
            World world = getBaseMetaTileEntity().getWorld();
            float tier = (float) getVoltageTierExact();
            boolean changed = false;
            for (ItemStack input : getStoredInputs()) {
                if (input.stackSize > 0 && beeRoot.getType(input) == EnumBeeType.QUEEN) {
                    BeeSimulator bee = new BeeSimulator(input, world, tier);
                    if (bee.isValid) {
                        storage.add(bee);
                        changed = true;
                    }
                }
                if (storage.size() >= maxSlots) break;
            }
            if (!changed) return CheckRecipeResultRegistry.NO_RECIPE;
            onStorageContentChanged(false);
            updateSlots();
            startCycle(10, 0);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
        if (primaryMode == MODE_PRIMARY_OUTPUT) {
            if (storage.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
            boolean changed = false;
            while (!storage.isEmpty()) {
                ItemEjectionHelper output = new ItemEjectionHelper(getOutputBusses(), true);
                if (output.ejectItems(Collections.singletonList(storage.get(0).queenStack), 1) == 0) break;
                output.commit();
                storage.remove(0);
                changed = true;
            }
            if (!changed) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
            onStorageContentChanged(false);
            startCycle(10, 0);
            return CheckRecipeResultRegistry.SUCCESSFUL;
        }
        if (primaryMode != MODE_PRIMARY_OPERATING || storage.isEmpty()) return CheckRecipeResultRegistry.NO_RECIPE;
        if (storage.size() > maxSlots) {
            // #tr GT5U.gui.text.recipe_result.LargeIndustrialApiary_slot_overflow
            // # Too many queens for the available capacity
            // # zh_CN 蜂后数量超过当前容量
            return SimpleCheckRecipeResult.ofFailure("LargeIndustrialApiary_slot_overflow");
        }
        return secondaryMode == MODE_SECONDARY_NORMAL ? processNormal() : processSwarmer();
    }

    private CheckRecipeResult processNormal() {
        long requiredPower = GTValues.V[1] * storage.size();
        long actualPower = getPowerWithMaintenance(requiredPower);
        if (getMaxInputEu() < actualPower) return CheckRecipeResultRegistry.insufficientPower(actualPower);
        if (missingFlowers) {
            // #tr GT5U.gui.text.recipe_result.LargeIndustrialApiary_missing_flowers
            // # Place any registered flower source in a flower position
            // # zh_CN 请在任一花位放置任意已注册花源
            return SimpleCheckRecipeResult.ofFailure("LargeIndustrialApiary_missing_flowers");
        }
        World world = getBaseMetaTileEntity().getWorld();
        float tier = (float) getVoltageTierExact();
        if (storageVersion != STORAGE_VERSION) {
            storageVersion = STORAGE_VERSION;
            storage.forEach(bee -> bee.generate(world, tier));
        } else if (needsTVarUpdate) {
            storage.forEach(bee -> bee.updateTVar(world, tier));
        }
        needsTVarUpdate = false;

        long maxJelly = storage.size() * 40L;
        long jelly = 0;
        ArrayList<ItemStack> inputs = getStoredInputs();
        for (ItemStack input : inputs) {
            if (input.stackSize > 0 && input.isItemEqual(royalJelly)) {
                jelly += Math.min(input.stackSize, maxJelly - jelly);
                if (jelly == maxJelly) break;
            }
        }
        double boost = 1d + 2d * jelly / maxJelly;
        HashMap<ItemId, Double> nextProgress = new HashMap<>(dropProgress);
        for (BeeSimulator bee : storage) bee.accumulateDrops(nextProgress, 6400d * boost);
        ItemStack[] outputs = collectDrops(nextProgress);
        if (!canOutput(outputs)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        long remaining = jelly;
        for (ItemStack input : inputs) {
            if (input.stackSize <= 0 || !input.isItemEqual(royalJelly)) continue;
            int consumed = (int) Math.min(input.stackSize, remaining);
            input.stackSize -= consumed;
            remaining -= consumed;
            if (remaining == 0) break;
        }
        dropProgress.clear();
        dropProgress.putAll(nextProgress);
        mOutputItems = outputs;
        updateSlots();
        startCycle(100, requiredPower);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private CheckRecipeResult processSwarmer() {
        long power = GTValues.V[5] - 2L;
        long available = getMaxInputEu();
        int duration = 1200;
        if (available < getPowerWithMaintenance(power)) {
            return CheckRecipeResultRegistry.insufficientPower(getPowerWithMaintenance(power));
        }
        for (int overclocks = 0; overclocks < GTUtility.log2ceil(1200) && power <= available / 4; overclocks++) {
            power *= 4;
            duration = Math.max(1, duration / 2);
        }
        if (available < getPowerWithMaintenance(power)) {
            return CheckRecipeResultRegistry.insufficientPower(getPowerWithMaintenance(power));
        }
        ItemStack[] outputs = { storage.get(0).createIgnobleCopy() };
        if (!canOutput(outputs)) return CheckRecipeResultRegistry.ITEM_OUTPUT_FULL;
        if (!SWARMER_JELLY_RECIPE.isRecipeInputEqual(true, null, getStoredInputs().toArray(new ItemStack[0]))) {
            return CheckRecipeResultRegistry.NO_RECIPE;
        }
        mOutputItems = outputs;
        updateSlots();
        startCycle(duration, power);
        return CheckRecipeResultRegistry.SUCCESSFUL;
    }

    private boolean canOutput(ItemStack[] outputs) {
        return !protectsExcessItem() || outputs.length == 0
            || new ItemEjectionHelper(getOutputBusses(), true, true).ejectItems(Arrays.asList(outputs), 1) > 0;
    }

    private long getPowerWithMaintenance(long power) {
        int efficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        return (long) (power * (10000d / Math.max(1000, efficiency)));
    }

    private void startCycle(int duration, long power) {
        mMaxProgresstime = duration;
        mEfficiency = 10000 - (getIdealStatus() - getRepairStatus()) * 1000;
        mEfficiencyIncrease = 10000;
        lEUt = -power;
    }

    private static ItemStack[] collectDrops(Map<ItemId, Double> progress) {
        List<ItemStack> outputs = new ArrayList<>();
        for (Map.Entry<ItemId, Double> entry : progress.entrySet()) {
            double amount = entry.getValue();
            if (amount < 1d) continue;
            int count = (int) Math.min(Integer.MAX_VALUE, amount);
            outputs.add(entry.getKey().getItemStack(count));
            entry.setValue(amount - count);
        }
        return outputs.toArray(new ItemStack[0]);
    }

    private String getModeKey() {
        if (primaryMode == MODE_PRIMARY_INPUT) {
            // #tr info.gtnotgood.largeIndustrialApiary.input
            // # Mode: Input
            // # zh_CN 模式：输入
            return "info.gtnotgood.largeIndustrialApiary.input";
        }
        if (primaryMode == MODE_PRIMARY_OUTPUT) {
            // #tr info.gtnotgood.largeIndustrialApiary.output
            // # Mode: Output
            // # zh_CN 模式：输出
            return "info.gtnotgood.largeIndustrialApiary.output";
        }
        if (secondaryMode == MODE_SECONDARY_NORMAL) {
            // #tr info.gtnotgood.largeIndustrialApiary.normal
            // # Mode: Normal Production
            // # zh_CN 模式：普通生产
            return "info.gtnotgood.largeIndustrialApiary.normal";
        }
        // #tr info.gtnotgood.largeIndustrialApiary.swarmer
        // # Mode: Swarmer
        // # zh_CN 模式：分蜂
        return "info.gtnotgood.largeIndustrialApiary.swarmer";
    }

    @Override
    public String[] getInfoData() {
        ArrayList<String> info = new ArrayList<>(Arrays.asList(super.getInfoData()));
        info.add(getModeKey());
        // #tr info.gtnotgood.largeIndustrialApiary.storage
        // # Queens: %s / %s
        // # zh_CN 蜂后：%s / %s
        info.add(IGregTechDeviceInformation.encode("info.gtnotgood.largeIndustrialApiary.storage",
            Integer.toString(storage.size()), Integer.toString(maxSlots)));
        return info.toArray(new String[0]);
    }

    @Override
    public void checkMachine(IGregTechTileEntity aBaseMetaTileEntity, ItemStack aStack, List<StructureError> errors) {
        needsWaterFill = false;
        casingCount = 0;
        if (!checkPiece(STRUCTURE_PIECE_MAIN, 7, 8, 0, errors)) {
            needsWaterFill = GTStructureUtility.hasWaterAtStructurePosition(aBaseMetaTileEntity, getExtendedFacing(),
                struct, 7, 8, 0, 'W');
            return;
        }
        checkCasingMin(errors, casingCount, 190);
        checkHasEnergyHatch(errors);
        checkHasMaintenanceHatch(errors);
        checkHasOutputBus(errors);
        if (errors.isEmpty()) {
            updateMaxSlots();
            needsWaterFill = true;
        }
        checkRequiredFlowers();
    }

    protected void checkRequiredFlowers() {
        missingFlowers = true;
        flowerTypes = new ArrayList<>(FlowerManager.flowerRegistry.getFlowerTypes());
        checkPiece(STRUCTURE_PIECE_FLOWERS, 7, 8, 0, new ArrayList<>());
    }

    @Override
    public IMetaTileEntity newMetaEntity(IGregTechTileEntity aTileEntity) {
        return new LargeIndustrialApiary(this.mName);
    }

    @Override
    public ITexture[] getTexture(IGregTechTileEntity aBaseMetaTileEntity, ForgeDirection side, ForgeDirection aFacing,
        int colorIndex, boolean aActive, boolean redstoneLevel) {
        return Textures.BlockIcons.createTextureWithCasing(this, side, aFacing, aActive,
            OVERLAY_FRONT_DISTILLATION_TOWER, OVERLAY_FRONT_DISTILLATION_TOWER_GLOW,
            OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE, OVERLAY_FRONT_DISTILLATION_TOWER_ACTIVE_GLOW);
    }

    @Override
    public ITexture getCasingTexture() {
        return Textures.BlockIcons.getCasingTextureForId(CASING_INDEX);
    }

    @Override
    protected @NotNull MTEMultiBlockBaseGui<?> getGui() {
        return new LargeIndustrialApiaryGui(this);
    }

    public static class BeeSimulator {

        public final ItemStack queenStack;
        public boolean isValid;
        List<BeeDrop> drops = new ArrayList<>();
        List<BeeDrop> specialDrops = new ArrayList<>();
        float beeSpeed;

        float maxBeeCycles;
        String flowerType;
        String flowerTypeDescription;
        public String speciesKey;
        private static IBeekeepingMode mode;

        public BeeSimulator(ItemStack queenStack, World world, float t) {
            isValid = false;
            this.queenStack = queenStack.copy();
            this.queenStack.stackSize = 1;
            if (generate(world, t)) {
                isValid = true;
                queenStack.stackSize--;
            }
        }

        public boolean generate(World world, float t) {
            if (mode == null) mode = beeRoot.getBeekeepingMode(world);
            drops.clear();
            specialDrops.clear();
            if (beeRoot.getType(this.queenStack) != EnumBeeType.QUEEN) return false;
            IBee queen = beeRoot.getMember(this.queenStack);
            IBeeModifier beeModifier = mode.getBeeModifier();
            float mod = beeModifier.getLifespanModifier(null, null, 1.f);
            int h = queen.getMaxHealth();
            maxBeeCycles = (float) h / (1.f / mod);
            IBeeGenome genome = queen.getGenome();
            this.flowerType = genome.getFlowerProvider().getFlowerType();
            this.flowerTypeDescription = genome.getFlowerProvider().getDescription();
            IAlleleBeeSpecies primary = genome.getPrimary();
            beeSpeed = genome.getSpeed();
            speciesKey = primary.getUID() + "\0" + genome.getSecondary().getUID() + "\0" + beeSpeed;
            genome.getPrimary().getProductChances()
                .forEach((key, value) -> drops.add(new BeeDrop(key, value, beeSpeed, t)));
            genome.getSecondary().getProductChances()
                .forEach((key, value) -> drops.add(new BeeDrop(key, value / 2.f, beeSpeed, t)));
            primary.getSpecialtyChances()
                .forEach((key, value) -> specialDrops.add(new BeeDrop(key, value, beeSpeed, t)));
            return true;
        }

        public BeeSimulator(NBTTagCompound tag) {
            queenStack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("queenStack"));
            isValid = tag.getBoolean("isValid");
            drops = new ArrayList<>();
            specialDrops = new ArrayList<>();
            for (int i = 0, isize = tag.getInteger("dropssize"); i < isize; i++)
                drops.add(new BeeDrop(tag.getCompoundTag("drops" + i)));
            for (int i = 0, isize = tag.getInteger("specialDropssize"); i < isize; i++)
                specialDrops.add(new BeeDrop(tag.getCompoundTag("specialDrops" + i)));
            beeSpeed = tag.getFloat("beeSpeed");
            maxBeeCycles = tag.getFloat("maxBeeCycles");
            IBee queen = beeRoot.getMember(this.queenStack);
            IBeeGenome genome = queen.getGenome();
            speciesKey = genome.getPrimary().getUID() + "\0" + genome.getSecondary().getUID() + "\0" + beeSpeed;
            if (tag.hasKey("flowerType") && tag.hasKey("flowerTypeDescription")) {
                flowerType = tag.getString("flowerType");
                flowerTypeDescription = tag.getString("flowerTypeDescription");
            } else {
                this.flowerType = genome.getFlowerProvider().getFlowerType();
                this.flowerTypeDescription = genome.getFlowerProvider().getDescription();
            }
        }

        public NBTTagCompound toNBTTagCompound() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("queenStack", queenStack.writeToNBT(new NBTTagCompound()));
            tag.setBoolean("isValid", isValid);
            tag.setInteger("dropssize", drops.size());
            for (int i = 0; i < drops.size(); i++) tag.setTag("drops" + i, drops.get(i).toNBTTagCompound());
            tag.setInteger("specialDropssize", specialDrops.size());
            for (int i = 0; i < specialDrops.size(); i++)
                tag.setTag("specialDrops" + i, specialDrops.get(i).toNBTTagCompound());
            tag.setFloat("beeSpeed", beeSpeed);
            tag.setFloat("maxBeeCycles", maxBeeCycles);
            tag.setString("flowerType", flowerType);
            tag.setString("flowerTypeDescription", flowerTypeDescription);
            return tag;
        }

        public void accumulateDrops(Map<ItemId, Double> progress, double timePassed) {
            double cycles = timePassed / 550d;
            drops.forEach(drop -> progress.merge(drop.id, drop.getAmount(cycles), Double::sum));
            specialDrops.forEach(drop -> progress.merge(drop.id, drop.getAmount(cycles), Double::sum));
        }

        public ItemStack createIgnobleCopy() {
            IBee princess = beeRoot.getMember(queenStack);
            princess.setIsNatural(false);
            return beeRoot.getMemberStack(princess, EnumBeeType.PRINCESS.ordinal());
        }

        public void updateTVar(World world, float t) {
            if (mode == null) mode = beeRoot.getBeekeepingMode(world);
            drops.forEach(d -> d.updateTVar(t));
            specialDrops.forEach(d -> d.updateTVar(t));
        }

        public String getFlowerType() {
            return flowerType;
        }

        public String getFlowerTypeDescription() {
            return flowerTypeDescription;
        }

        private static class BeeDrop {

            private static final float MAX_PRODUCTION_MODIFIER_FROM_UPGRADES = 17.19926784f; // 4*1.2^8
            final ItemStack stack;
            double amount;
            final ItemId id;

            final float chance;
            final float beeSpeed;
            float t;

            public BeeDrop(ItemStack stack, float chance, float beeSpeed, float t) {
                this.stack = stack;
                this.chance = chance;
                this.beeSpeed = beeSpeed;
                this.t = t;
                id = ItemId.createNoCopy(this.stack);
                evaluate();
            }

            public void updateTVar(float t) {
                if (this.t != t) {
                    this.t = t;
                    evaluate();
                }
            }

            public void evaluate() {
                this.amount = Bee.getFinalChance(chance, beeSpeed, MAX_PRODUCTION_MODIFIER_FROM_UPGRADES
                    + mode.getBeeModifier().getProductionModifier(null, MAX_PRODUCTION_MODIFIER_FROM_UPGRADES), t);
            }

            public double getAmount(double speedModifier) {
                return amount * speedModifier;
            }

            public ItemStack get(int amount) {
                ItemStack r = stack.copy();
                r.stackSize = amount;
                return r;
            }

            public BeeDrop(NBTTagCompound tag) {
                stack = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("stack"));
                chance = tag.getFloat("chance");
                beeSpeed = tag.getFloat("beeSpeed");
                t = tag.getFloat("t");
                amount = tag.getDouble("amount");
                id = ItemId.createNoCopy(stack);
            }

            public NBTTagCompound toNBTTagCompound() {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setTag("stack", stack.writeToNBT(new NBTTagCompound()));
                tag.setFloat("chance", chance);
                tag.setFloat("beeSpeed", beeSpeed);
                tag.setFloat("t", t);
                tag.setDouble("amount", amount);
                return tag;
            }

            @Override
            public int hashCode() {
                return id.hashCode();
            }
        }
    }

    @SideOnly(Side.CLIENT)
    @Override
    protected SoundResource getActivitySoundLoop() {
        return SoundResource.GT_MACHINES_MEGA_INDUSTRIAL_APIARY_LOOP;
    }

    @Override
    public boolean supportsBatchMode() {
        return false;
    }

    @Override
    public boolean supportsInputSeparation() {
        return false;
    }

    @Override
    public boolean supportsSingleRecipeLocking() {
        return false;
    }
}
