package com.xyp.gtnotgood.common.items.compass;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiData;
import com.cleanroommc.modularui.factory.PlayerInventoryGuiFactory;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.ModularScreen;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** Server-authoritative two-target compass; each stack owns its own mode and destination. */
public final class StructureCompassItem extends GTNGItem implements IGuiHolder<PlayerInventoryGuiData> {

    public StructureCompassItem() {
        super("structure_compass");
        // #tr item.structure_compass.name
        // # Explorer's Compass
        // # zh_CN 遗迹探寻罗盘
        setUnlocalizedName("structure_compass");
        setMaxStackSize(1);
    }

    public static NBTTagCompound data(ItemStack stack) {
        if (!stack.hasTagCompound()) stack.setTagCompound(new NBTTagCompound());
        return stack.getTagCompound();
    }

    public static int mode(ItemStack stack) {
        return stack.hasTagCompound() && stack.getTagCompound().getInteger("Mode") == 1 ? 1 : 0;
    }

    public static String modeKey(int mode) {
        if (mode == 1) {
            // #tr compass.target.lootgames
            // # LootGames game area
            // # zh_CN LootGames 游戏区域
            return "compass.target.lootgames";
        }
        // #tr compass.target.house
        // # Roguelike red-brick entrance
        // # zh_CN Roguelike 红砖地牢入口
        return "compass.target.house";
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (world.isRemote) return stack;
        if (player.isSneaking()) selectMode(player, stack, 1 - mode(stack));
        else PlayerInventoryGuiFactory.INSTANCE.openFromMainHand(player);
        return stack;
    }

    @Override
    public ModularPanel buildUI(PlayerInventoryGuiData data, PanelSyncManager sync, UISettings settings) {
        return new StructureCompassGui(data, sync).build();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ModularScreen createScreen(PlayerInventoryGuiData data, ModularPanel panel) {
        return new ModularScreen(ModList.GTNotGood.getID(), panel);
    }

    /** Changes the server-owned target type and invalidates the previous search and destination. */
    public static void selectMode(EntityPlayer player, ItemStack stack, int mode) {
        if (player.worldObj.isRemote) return;
        StructureSearch.INSTANCE.cancel(player);
        data(stack).setInteger("Mode", mode == 1 ? 1 : 0);
        data(stack).removeTag("Found");
        player.inventory.markDirty();
    }

    /** Starts a search only when the selected optional mod is installed. */
    public static void search(EntityPlayer player, ItemStack stack) {
        if (player.worldObj.isRemote) return;
        ModList mod = mode(stack) == 0 ? ModList.Roguelike : ModList.LootGames;
        if (!mod.isModLoaded()) {
            // #tr compass.missing
            // # Required mod is not installed: %s
            // # zh_CN 未安装目标模组：%s
            player.addChatMessage(new ChatComponentTranslation("compass.missing", mod.getID()));
        } else StructureSearch.INSTANCE.start(player, stack);
    }

    /** Resolves seed candidates after normal player exploration has generated a matching structure nearby. */
    @Override
    public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean held) {
        if (
            world.isRemote || !held
                || !(entity instanceof EntityPlayer)
                || world.getTotalWorldTime() % 20 != 0
                || !stack.hasTagCompound()
        ) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.getBoolean("Found") || tag.getBoolean("Confirmed") || tag.getInteger("Dimension") != entity.dimension)
            return;
        StructureLocations.Location found = StructureLocations.get(world).nearest(mode(stack), tag.getInteger("X"),
            tag.getInteger("Z"), mode(stack) == 0 ? 128 : 24);
        if (found != null) StructureSearch.setTarget((EntityPlayer) entity, stack, found, true);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon("compass");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return Items.compass.getIconFromDamage(0);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List lines, boolean advanced) {
        lines.add(StatCollector.translateToLocal(modeKey(mode(stack))));
        // #tr compass.use
        // # Right-click: open compass. Sneak-right-click: switch/reset.
        // # zh_CN 右键打开罗盘界面；潜行右键切换并重置。
        lines.add(StatCollector.translateToLocal("compass.use"));
        // #tr compass.range
        // # Predicts seed candidates within 8192 blocks without generating terrain.
        // # zh_CN 按种子预测周围 8192 格候选区域，不生成地形。
        lines.add(StatCollector.translateToLocal("compass.range"));
        // #tr compass.historical
        // # Recorded structures may already be looted or changed.
        // # zh_CN 记录的遗迹可能已被探索或改动。
        lines.add(StatCollector.translateToLocal("compass.historical"));
        if (!stack.hasTagCompound() || !stack.getTagCompound().getBoolean("Found")) return;
        NBTTagCompound tag = stack.getTagCompound();
        if (!tag.getBoolean("Confirmed")) {
            // #tr compass.unconfirmed
            // # Unconfirmed candidate; terrain or tower type may prevent a match.
            // # zh_CN 待确认候选区域；地形或入口类型可能不符合目标。
            lines.add(StatCollector.translateToLocal("compass.unconfirmed"));
            // #tr compass.candidate_position
            // # Candidate: X %s, Z %s (dimension %s).
            // # zh_CN 候选：X %s，Z %s（维度 %s）。
            lines.add(StatCollector.translateToLocalFormatted("compass.candidate_position", tag.getInteger("X"),
                tag.getInteger("Z"), tag.getInteger("Dimension")));
        } else {
            // #tr compass.position
            // # Target: %s, %s, %s (dimension %s)
            // # zh_CN 目标：%s，%s，%s（维度 %s）
            lines.add(StatCollector.translateToLocalFormatted("compass.position", tag.getInteger("X"),
                tag.getInteger("Y"), tag.getInteger("Z"), tag.getInteger("Dimension")));
        }
        if (player.dimension == tag.getInteger("Dimension")) {
            double dx = tag.getInteger("X") + .5 - player.posX, dz = tag.getInteger("Z") + .5 - player.posZ;
            // #tr compass.distance
            // # Horizontal distance: %s blocks
            // # zh_CN 水平距离：%s 格
            lines.add(StatCollector.translateToLocalFormatted("compass.distance", (int) Math.sqrt(dx * dx + dz * dz)));
        } else {
            // #tr compass.dimension
            // # Target is in another dimension. Search again here.
            // # zh_CN 目标在其他维度，请在当前维度重新搜索。
            lines.add(StatCollector.translateToLocal("compass.dimension"));
        }
    }
}
