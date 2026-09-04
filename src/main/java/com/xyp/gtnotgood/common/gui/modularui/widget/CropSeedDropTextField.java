package com.xyp.gtnotgood.common.gui.modularui.widget;

import net.minecraft.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import com.cleanroommc.modularui.integration.recipeviewer.RecipeViewerGhostIngredientSlot;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;
import com.gtnewhorizon.cropsnh.api.ICropCard;
import com.gtnewhorizon.cropsnh.farming.registries.CropRegistry;

/**
 * Text field that accepts CropsNH seed items dragged from NEI and fills in the crop id.
 */
public class CropSeedDropTextField extends TextFieldWidget implements RecipeViewerGhostIngredientSlot<ItemStack> {

    public CropSeedDropTextField() {
        autoUpdateOnChange(true);
    }

    @Override
    public boolean handleDragAndDrop(@NotNull ItemStack draggedStack, int button) {
        if (draggedStack == null || button != 0) return false;

        ICropCard crop = CropRegistry.instance.get(draggedStack);
        if (crop == null) return false;

        setText(crop.getId());
        onTextChanged();
        return true;
    }
}
