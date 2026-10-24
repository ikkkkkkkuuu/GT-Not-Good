package com.xyp.gtnotgood.common.items.multiblockterminal;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.gtnewhorizon.structurelib.item.ItemConstructableTrigger;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public final class MultiblockTerminalItem extends ItemConstructableTrigger
    implements IGuiHolder<PlayerInventoryGuiData> {

    public MultiblockTerminalItem() {
        // #tr item.multiblock_terminal.name
        // # Multiblock Terminal
        // # zh_CN 多方块终端
        setUnlocalizedName("multiblock_terminal");
        setTextureName(ModList.GTNotGood.getResourcePath("multiblock_terminal"));
        setCreativeTab(GTNGCreativeTabs.GTNGItem);
        setMaxStackSize(1);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) return super.onItemRightClick(stack, world, player);
        if (!world.isRemote && getMovingObjectPositionFromPlayer(world, player, true) == null)
            PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
        return stack;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        if (!player.isSneaking()) return TerminalOperations.project(stack, player, world, x, y, z, side);
        // Let PlayerControllerMP send the placement packet; the server consumes the interaction.
        if (world.isRemote) return false;
        if (!(player instanceof EntityPlayerMP serverPlayer)) return false;
        if (TerminalItemSource.bind(stack, serverPlayer, world, x, y, z, ForgeDirection.getOrientation(side)))
            return true;
        return TerminalOperations.execute(stack, serverPlayer, world, x, y, z, side);
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        return new MultiblockTerminalGui(data, sync).build();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return new ModularScreen(ModList.GTNotGood.getID(), panel);
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        return TerminalBlockRecovery.isHarvesting() || super.canHarvestBlock(block, stack);
    }

    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> lines, boolean advanced) {
        // #tr tooltip.multiblock_terminal.project
        // # Right-click a controller to show its structure projection.
        // # zh_CN 右键控制器显示结构投影。
        lines.add(StatCollector.translateToLocal("tooltip.multiblock_terminal.project"));
        // #tr tooltip.multiblock_terminal.operate
        // # Shift-right-click a controller to build or dismantle; right-click air for settings.
        // # zh_CN Shift 右键控制器建造或拆除；右键空气打开设置。
        lines.add(StatCollector.translateToLocal("tooltip.multiblock_terminal.operate"));
        // #tr tooltip.multiblock_terminal.channels
        // # Supports projector channels, middle-click picking and material source priorities.
        // # zh_CN 支持投影仪频道、中键拾取与材料源优先级。
        lines.add(StatCollector.translateToLocal("tooltip.multiblock_terminal.channels"));
        // #tr tooltip.multiblock_terminal.ae
        // # Shift-right-click an ME node to bind it; enable AE mode to use that network.
        // # zh_CN Shift 右键 ME 节点绑定，启用 AE 模式后从该网络存取材料。
        lines.add(StatCollector.translateToLocal("tooltip.multiblock_terminal.ae"));
        if (advanced || player.isSneaking()) super.addInformation(stack, player, lines, advanced);
    }
}
