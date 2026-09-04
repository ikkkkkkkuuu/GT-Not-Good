package com.xyp.gtnotgood.ae2thing.nei;

import static com.xyp.gtnotgood.ae2thing.proxy.ClientProxy.mouseHandlers;
import static net.minecraft.client.gui.GuiScreen.isShiftKeyDown;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.common.item.ItemFluidPacket;
import com.xyp.gtnotgood.ae2thing.AE2Thing;
import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.nei.object.OrderStack;
import com.xyp.gtnotgood.ae2thing.network.CPacketTransferRecipe;
import com.xyp.gtnotgood.ae2thing.proxy.ClientProxy;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.client.gui.AEBaseGui;
import appeng.container.slot.SlotFake;
import appeng.util.Platform;
import codechicken.nei.PositionedStack;
import codechicken.nei.recipe.GuiOverlayButton;
import codechicken.nei.recipe.TemplateRecipeHandler;
import cpw.mods.fml.relauncher.ReflectionHelper;

/** Keeps Shift+wheel ingredient cycling for native AE2 pattern terminals without depending on the removed dual GUI. */
public final class NativePatternIngredientCycler {

    private static boolean registered;
    private static final Field THE_SLOT = ReflectionHelper.findField(GuiContainer.class, "theSlot", "field_147006_u");

    private NativePatternIngredientCycler() {}

    public static void register() {
        if (registered) return;
        registered = true;
        mouseHandlers.add((event, overlayButton) -> {
            GuiScreen screen = Minecraft.getMinecraft().currentScreen;
            if (!(screen instanceof AEBaseGui gui) || overlayButton == null || !GuiScreen.isShiftKeyDown())
                return false;
            GuiOverlayButton button = ClientProxy.getOverlayButton();
            Slot hoverSlot = getHoveredSlot(gui);
            if (button == null || !(hoverSlot instanceof SlotFake slot)) return false;
            ItemStack slotItem = slot.getStack();
            if (slotItem == null) return false;

            Constants.MouseWheel wheel = event.scrollAmount == -1 ? Constants.MouseWheel.NEXT
                : Constants.MouseWheel.PREVIEW;
            List<PositionedStack> ingredients = button.handlerRef.handler
                .getIngredientStacks(button.handlerRef.recipeIndex);
            FluidStack slotFluid = extractFluid(slotItem);
            if (slotFluid != null) {
                for (PositionedStack ingredient : ingredients) {
                    FluidStack replacement = findAdjacentFluid(ingredient.items, slotFluid, wheel);
                    if (replacement != null && replacement.getFluid() != slotFluid.getFluid()) {
                        sendReplacement(slotFluid, replacement, shouldCraft(button));
                        return true;
                    }
                }
                return false;
            }

            for (PositionedStack ingredient : ingredients) {
                ItemStack replacement = findAdjacentItem(ingredient.items, slotItem, wheel);
                if (replacement != null) {
                    sendReplacement(slotItem, replacement, shouldCraft(button));
                    return true;
                }
            }
            return false;
        });
    }

    private static Slot getHoveredSlot(GuiContainer gui) {
        try {
            return (Slot) THE_SLOT.get(gui);
        } catch (IllegalAccessException ignored) {
            return null;
        }
    }

    private static ItemStack findAdjacentItem(ItemStack[] candidates, ItemStack item, Constants.MouseWheel wheel) {
        for (int index = 0; index < candidates.length; index++) {
            if (Platform.isSameItemPrecise(item, candidates[index])) {
                int adjacent = index + wheel.direction;
                return candidates[adjacent < 0 ? candidates.length - 1 : adjacent % candidates.length];
            }
        }
        return null;
    }

    private static FluidStack findAdjacentFluid(ItemStack[] candidates, FluidStack fluid, Constants.MouseWheel wheel) {
        for (int index = 0; index < candidates.length; index++) {
            FluidStack candidate = extractFluid(candidates[index]);
            if (candidate != null && candidate.getFluid() == fluid.getFluid()) {
                int adjacent = index + wheel.direction;
                return extractFluid(candidates[adjacent < 0 ? candidates.length - 1 : adjacent % candidates.length]);
            }
        }
        return null;
    }

    private static FluidStack extractFluid(ItemStack stack) {
        if (stack == null || stack.getItem() == null) return null;
        if (stack.getItem() instanceof gregtech.common.items.ItemFluidDisplay) {
            if (stack.getTagCompound() == null) return null;
            Fluid fluid = FluidRegistry.getFluid(stack.getItemDamage());
            int amount = (int) stack.getTagCompound()
                .getLong("mFluidDisplayAmount");
            return amount > 0 && fluid != null ? new FluidStack(fluid, amount) : null;
        }
        if (FluidDropCompat.isFluidDrop(stack)) return FluidDropCompat.getFluidStack(stack);
        if (stack.getItem() instanceof ItemFluidPacket) return ItemFluidPacket.getFluidStack(stack);
        return null;
    }

    private static void sendReplacement(Object current, Object replacement, boolean crafting) {
        List<OrderStack<?>> inputs = new ArrayList<>();
        List<OrderStack<?>> outputs = new ArrayList<>();
        inputs.add(new OrderStack<>(current, 0));
        outputs.add(new OrderStack<>(replacement, 0));
        AE2Thing.proxy.netHandler.sendToServer(
            new CPacketTransferRecipe(inputs, outputs, crafting, isShiftKeyDown(), Constants.NEI_MOUSE_WHEEL));
    }

    private static boolean shouldCraft(GuiOverlayButton button) {
        if (!(button.handlerRef.handler instanceof TemplateRecipeHandler recipe)) return false;
        String identifier = recipe.getOverlayIdentifier();
        return "crafting".equals(identifier) || "crafting2x2".equals(identifier);
    }
}
