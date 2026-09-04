package com.xyp.gtnotgood.common.items.wildcard;

import java.util.List;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.common.gui.modularui.wildcard.WildcardPatternGui;

import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.items.misc.ItemEncodedPattern;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Configurable AE2 encoded pattern item that expands material wildcards into concrete crafting patterns.
 */
public class WildcardPatternItem extends ItemEncodedPattern implements IGuiHolder<PlayerInventoryGuiData> {

    public static final String UNLOCALIZED_NAME = "wildcardpattern";
    public static final String ITEM_NAME = "WildcardPattern";

    public WildcardPatternItem() {
        super();
        setMaxStackSize(1);
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        // #tr item.wildcardpattern.name
        // # Wildcard Pattern
        // # zh_CN 通配样板符
        setUnlocalizedName(UNLOCALIZED_NAME);
        setTextureName(GTNotGood.RESOURCE_ROOT_ID + ":wildcard_pattern");
    }

    @Override
    public void onCreated(ItemStack stack, World world, EntityPlayer player) {
        WildcardPatternGenerator.markAsWildcard(stack);
    }

    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean isHeld) {
        if (stack != null && stack.stackTagCompound == null) {
            WildcardPatternGenerator.markAsWildcard(stack);
        }
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (stack == null) return stack;

        WildcardPatternGenerator.markAsWildcard(stack);
        if (!world.isRemote) {
            try {
                PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return stack;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager syncManager, UISettings settings) {
        return new WildcardPatternGui(data.getSlotIndex()).buildUI(data, syncManager, settings);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public com.cleanroommc.modularui.screen.ModularScreen createScreen(PlayerInventoryGuiData data,
        ModularPanel mainPanel) {
        return new com.cleanroommc.modularui.screen.ModularScreen(GTNotGood.MODID, mainPanel);
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        WildcardPatternGenerator.markAsWildcard(stack);
        return false;
    }

    @Override
    public ICraftingPatternDetails getPatternForItem(ItemStack stack, World world) {
        return WildcardPatternGenerator.getDetailsForItem(stack, world);
    }

    @Override
    public ItemStack getOutput(ItemStack item) {
        if (!WildcardPatternGenerator.isWildcardPattern(item)) {
            return super.getOutput(item);
        }
        return WildcardPatternGenerator.getOutputForItem(item, null);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addCheckedInformation(ItemStack stack, EntityPlayer player, List<String> lines,
        boolean advancedTooltips) {
        if (!WildcardPatternGenerator.isWildcardPattern(stack)) {
            WildcardPatternGenerator.markAsWildcard(stack);
        }

        // 实时计算考虑排除规则后的实际配方数量(添加异常保护)
        int actualCount = 0;
        try {
            actualCount = WildcardPatternGenerator.countActualPatternsAfterExclude(stack);
        } catch (Exception e) {
            // 如果计算失败,使用保存的固定值作为fallback
            actualCount = WildcardPatternState.getExpandedPatternCount(stack);
        }

        // #tr tooltip.wildcardpattern.expand_count
        // # Expanded Patterns:
        // # zh_CN 展开配方数:
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.expand_count") + " " + actualCount);

        // #tr tooltip.wildcardpattern.usage
        // # Right-click to configure
        // # zh_CN 右键配置
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.usage"));

        // #tr tooltip.wildcardpattern.desc_axis
        // # §7Generates one pattern per material that passes the filter.
        // # zh_CN §7为每个通过过滤的材料各生成一个样板。
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.desc_axis"));

        // #tr tooltip.wildcardpattern.desc_io
        // # §7Input/Output: prefix (ingot/plate...), fluid, or fixed item.
        // # zh_CN §7输入/输出: 前缀(锭/板...)、流体、或固定物品。
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.desc_io"));

        // #tr tooltip.wildcardpattern.desc_filter
        // # §7Filter materials by property, tag, or name.
        // # zh_CN §7按属性、标签或名称过滤材料。
        lines.add(StatCollector.translateToLocal("tooltip.wildcardpattern.desc_filter"));
    }
}
