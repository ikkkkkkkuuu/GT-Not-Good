package com.xyp.gtnotgood.common.packaged;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;

import appeng.api.AEApi;
import appeng.api.storage.data.IAEItemStack;
import appeng.tile.inventory.IAEStackInventory;
import thaumcraft.common.lib.crafting.ThaumcraftCraftingManager;
import thaumcraft.common.tiles.TileArcaneWorkbench;
import thaumcraft.common.tiles.TileMagicWorkbench;

/** Records a real workbench's nine positions in an ordinary AE processing pattern without consuming its example. */
public final class ArcaneWorkbenchPatterns {

    /** Nine vanilla ItemStack NBT entries shared by captured patterns and terminal-imported patterns. */
    public static final String GRID = "GTNGArcaneGrid";

    private ArcaneWorkbenchPatterns() {}

    /**
     * Verifies a saved layout against the current processing bill before attaching it to a newly encoded pattern.
     * Condensing/reordering identical items is allowed; fluids, extra items and changed quantities are rejected.
     *
     * @param layout nine vanilla ItemStack NBT entries, including empty cells
     * @param inputs current native AE input inventory
     * @return whether the inventory supplies exactly one item for each occupied layout cell
     */
    public static boolean matchesInputs(NBTTagList layout, IAEStackInventory inputs) {
        if (layout == null || layout.tagCount() != 9) return false;
        long[] remaining = new long[inputs.getSizeInventory()];
        long total = 0;
        for (int i = 0; i < remaining.length; i++) {
            var stack = inputs.getAEStackInSlot(i);
            if (stack == null) continue;
            if (!stack.isItem() || stack.getStackSize() <= 0 || stack.getStackSize() > 9) return false;
            remaining[i] = stack.getStackSize();
            total += remaining[i];
            if (total > 9) return false;
        }
        if (total == 0) return false;
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = layout.getCompoundTagAt(i);
            if (entry.hasNoTags()) continue;
            ItemStack required = ItemStack.loadItemStackFromNBT(entry);
            if (required == null || required.stackSize != 1) return false;
            int match = -1;
            for (int j = 0; j < remaining.length; j++) {
                if (remaining[j] == 0) continue;
                ItemStack supplied = ((IAEItemStack) inputs.getAEStackInSlot(j)).getItemStack();
                if (required.isItemEqual(supplied) && ItemStack.areItemStackTagsEqual(required, supplied)) {
                    match = j;
                    break;
                }
            }
            if (match < 0) return false;
            remaining[match]--;
            total--;
        }
        return total == 0;
    }

    /** Validates untrusted pattern NBT; every occupied slot describes exactly one ingredient per craft. */
    static TileMagicWorkbench grid(ItemStack pattern, World world) {
        if (pattern == null || !pattern.hasTagCompound()) return null;
        NBTTagList entries = pattern.getTagCompound()
            .getTagList(GRID, 10);
        if (entries.tagCount() != 9) return null;
        TileMagicWorkbench grid = new TileMagicWorkbench();
        grid.setWorldObj(world);
        boolean occupied = false;
        for (int i = 0; i < 9; i++) {
            NBTTagCompound entry = entries.getCompoundTagAt(i);
            ItemStack stack = ItemStack.loadItemStackFromNBT(entry);
            if (stack == null && entry.hasKey("id") || stack != null && stack.stackSize != 1) return null;
            grid.setInventorySlotContentsSoftly(i, stack);
            occupied |= stack != null;
        }
        return occupied ? grid : null;
    }

    /**
     * Server-only target-row action. Requires ownership, research, an idle target, a free provider slot and a blank
     * pattern in the player's main inventory. The example ingredients and wand remain on the table.
     */
    static void capture(TilePackagedProvider provider, EntityPlayer player, int index) {
        if (!provider.isServerSide() || !provider.canConfigure(player)
            || index < 0
            || index >= provider.targets.size()
            || !(PackagedCoreRegistry
                .get(provider.getStackInSlot(TilePackagedProvider.CORE)) instanceof ArcaneWorkbenchAdapter))
            return;
        PackagedTarget target = provider.targets.get(index);
        if (provider.busy(target) || !(target.resolve(provider.getWorldObj()) instanceof TileArcaneWorkbench workbench)
            || !player.worldObj.canMineBlock(player, target.x, target.y, target.z)) return;
        TileMagicWorkbench example = new TileMagicWorkbench();
        example.setWorldObj(player.worldObj);
        example.xCoord = workbench.xCoord;
        example.yCoord = workbench.yCoord;
        example.zCoord = workbench.zCoord;
        ItemStack wand = workbench.getStackInSlot(10);
        example.setInventorySlotContentsSoftly(10, wand == null ? null : wand.copy());
        for (int i = 0; i < 9; i++) {
            ItemStack input = workbench.getStackInSlot(i);
            if (input != null) {
                input = input.copy();
                input.stackSize = 1;
                example.setInventorySlotContentsSoftly(i, input);
            }
        }
        ItemStack result = ThaumcraftCraftingManager.findMatchingArcaneRecipe(example, player);
        int destination = -1, blankSlot = -1;
        for (int i = 0; i < TilePackagedProvider.PATTERNS; i++) {
            if (provider.getStackInSlot(i) == null) {
                destination = i;
                break;
            }
        }
        ItemStack blank = AEApi.instance()
            .definitions()
            .materials()
            .blankPattern()
            .maybeStack(1)
            .get();
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            if (TilePackagedProvider.sameItem(blank, player.inventory.mainInventory[i])) {
                blankSlot = i;
                break;
            }
        }
        if (result == null || result.stackSize <= 0 || destination < 0 || blankSlot < 0) {
            // #tr gui.packaged.arcane_capture_failed
            // # Check research, table recipe, an empty pattern slot and a blank pattern in your inventory.
            // # zh_CN 请检查研究、台上配方、供应器空样板槽和背包中的空白样板。
            player.addChatMessage(new ChatComponentTranslation("gui.packaged.arcane_capture_failed"));
            return;
        }
        ItemStack encoded = encode(example, result);
        provider.setInventorySlotContents(destination, encoded);
        player.inventory.decrStackSize(blankSlot, 1);
        player.inventory.markDirty();
        // #tr gui.packaged.arcane_captured
        // # Recipe recorded. Clear the example ingredients, leave the wand, and close the workbench.
        // # zh_CN 配方已录入。请清空示例材料，保留法杖并关闭工作台。
        player.addChatMessage(new ChatComponentTranslation("gui.packaged.arcane_captured"));
    }

    /** Encodes both AE's unordered bill of materials and the exact TC4 matching layout. */
    static ItemStack encode(TileMagicWorkbench example, ItemStack result) {
        ItemStack encoded = AEApi.instance()
            .definitions()
            .items()
            .encodedPattern()
            .maybeStack(1)
            .get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList layout = new NBTTagList();
        NBTTagList inputs = new NBTTagList();
        for (int i = 0; i < 9; i++) {
            ItemStack input = example.getStackInSlot(i);
            NBTTagCompound entry = new NBTTagCompound();
            if (input != null) {
                input = input.copy();
                input.stackSize = 1;
                input.writeToNBT(entry);
                inputs.appendTag(entry.copy());
            }
            layout.appendTag(entry);
        }
        NBTTagList outputs = new NBTTagList();
        outputs.appendTag(result.writeToNBT(new NBTTagCompound()));
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setTag(GRID, layout);
        tag.setBoolean("crafting", false);
        tag.setBoolean("substitute", false);
        encoded.setTagCompound(tag);
        return encoded;
    }
}
