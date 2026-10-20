package com.xyp.gtnotgood.common.items.electrode;

import static com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace.ArcFurnaceElectrodeAccess.formatModifier;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;

import com.xyp.gtnotgood.common.items.GTNGItem;
import com.xyp.gtnotgood.mixins.late.gregtech.arcfurnace.ArcFurnaceElectrodeAccess;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.enums.Materials;
import kubatech.api.arcfurnace.ArcFurnaceProcessingEvent;
import kubatech.loaders.ArcFurnaceElectrode;
import kubatech.loaders.ArcFurnaceLoader;
import kubatech.tileentity.gregtech.multiblock.MTEIndustrialArcFurnace;

public final class IronElectrode extends GTNGItem {

    // The furnace persists ordinal() and restores by ID; keep both equal without changing native enum values.
    public static final int ELECTRODE_ID = 32766;
    private static final int STARTING_PARALLEL = 4096;
    private static final String INFINITY_PARALLEL_KEY = "infinityTargetParallel";
    private static final String DURABILITY_KEY = "durability";
    private static final ArcFurnaceElectrode DEFINITION = ArcFurnaceElectrodeAccess.create("IronElectrode",
        ELECTRODE_ID, ELECTRODE_ID, Materials.Iron, 50d, STARTING_PARALLEL, 12d, 4d, Integer.MAX_VALUE, 0.5d, 0d,
        IronElectrode::applySpecialEffect);

    public IronElectrode() {
        // #tr item.gtnotgood.iron_electrode.name
        // # Iron Electrode
        // # zh_CN 铁电极
        super("iron_electrode");
        setMaxStackSize(1);
    }

    public static ArcFurnaceElectrode getDefinition() {
        return DEFINITION;
    }

    public static boolean damage(ItemStack stack, int amount) {
        long used = Math.max(0L, (long) ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.usedDurability(stack) + amount);
        if (used >= DEFINITION.durability) return true;
        if (stack.getTagCompound() == null) stack.setTagCompound(new NBTTagCompound());
        stack.getTagCompound().setInteger(DURABILITY_KEY, (int) used);
        return false;
    }

    private static void applySpecialEffect(ArcFurnaceProcessingEvent event) {
        NBTTagCompound state = event.arcFurnace.getEffectState();
        ensureStartingParallel(state);
        if (
            event instanceof ArcFurnaceProcessingEvent.EventStartIgnition ignition
                && event.arcFurnace instanceof MTEIndustrialArcFurnace furnace
        ) {
            ignition.eut = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, furnace.getAverageInputVoltage()));
        }
        ArcFurnaceElectrode.InfinityElectrode.specialEffect.accept(event);
        ArcFurnaceElectrode.NetheriteElectrode.specialEffect.accept(event);
        if (event instanceof ArcFurnaceProcessingEvent.EventReset) ensureStartingParallel(state);
    }

    private static void ensureStartingParallel(NBTTagCompound state) {
        state.setInteger(INFINITY_PARALLEL_KEY, Math.max(STARTING_PARALLEL, state.getInteger(INFINITY_PARALLEL_KEY)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
        int durability = ArcFurnaceLoader.ARC_FURNACE_ELECTRODE.remainingDurability(stack);
        // #tr item.gtnotgood.iron_electrode.durability
        // # Durability: %s/%s
        // # zh_CN 耐久：%s/%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.durability",
            formatModifier(durability, 100, 500, 700) + EnumChatFormatting.RESET + EnumChatFormatting.GRAY,
            formatModifier(DEFINITION.durability, 100, 500, 700)));
        // #tr item.gtnotgood.iron_electrode.speed
        // # Speed: %s
        // # zh_CN 速度：%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.speed",
            formatModifier(DEFINITION.speedModifier * 100, false, 50d, 100d, 200d, "", "%")));
        // #tr item.gtnotgood.iron_electrode.parallel
        // # Parallels: %s
        // # zh_CN 并行：%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.parallel",
            formatModifier(DEFINITION.parallelLimit, 4, 64, 128)));
        // #tr item.gtnotgood.iron_electrode.overclock
        // # OC: %s/%s
        // # zh_CN 超频系数：%s/%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.overclock",
            formatModifier(DEFINITION.OCSpeedFactor, false, 1d, 2d, 4d, "", "") + EnumChatFormatting.GRAY,
            formatModifier(DEFINITION.OCPowerFactor, false, 2d, 4d, 6d, "", "")));
        // #tr item.gtnotgood.iron_electrode.eu_modifier
        // # EU Modifier: %s
        // # zh_CN EU修正：%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.eu_modifier",
            formatModifier(DEFINITION.euModifier, true, 0.6d, 1d, 1.25d, "", "x")));
        // #tr item.gtnotgood.iron_electrode.startup_penalty
        // # Startup Penalty: %s
        // # zh_CN 启动惩罚：%s
        tooltip.add(StatCollector.translateToLocalFormatted("item.gtnotgood.iron_electrode.startup_penalty",
            formatModifier(DEFINITION.startupPenalty + 1, true, 0.25d, 0.5d, 1d, "x", "")));
        // #tr item.gtnotgood.iron_electrode.startup_amperage
        // # Startup Amperage: 1 A
        // # zh_CN 启动电流：1A
        tooltip.add(StatCollector.translateToLocal("item.gtnotgood.iron_electrode.startup_amperage"));
        // #tr item.arc_furnace_electrode.tip.special_effect.IronElectrode
        // # {\\GOLD}To Infinity!: Starts at 4096 parallels; doubles each run; resets on shutdown.
        // # zh_CN {\\GOLD}迈向无尽：从4096并行开始，每次完成作业后翻倍；停机后重置为4096。
        addEffectTooltip(tooltip, "item.arc_furnace_electrode.tip.special_effect.IronElectrode");
        // #tr item.gtnotgood.iron_electrode.hot_restart
        // # {\\GOLD}Hot Restart: No shutdown cooldown; skips ignition within 4s of the last ignition.
        // # zh_CN {\\GOLD}热重启：无停机冷却；距上次点火完成不足4秒时跳过启动。
        addEffectTooltip(tooltip, "item.gtnotgood.iron_electrode.hot_restart");
    }

    @SideOnly(Side.CLIENT)
    private static void addEffectTooltip(List<String> tooltip, String key) {
        tooltip.addAll(
            Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(StatCollector.translateToLocal(key), 300));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister register) {
        itemIcon = register.registerIcon(ModList.KubaTech.getResourcePath("arcfurnace/electrode"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        short[] color = Materials.Iron.getRGBA();
        return (color[0] & 255) << 16 | (color[1] & 255) << 8 | color[2] & 255;
    }
}
