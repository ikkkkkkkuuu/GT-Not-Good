package com.xyp.gtnotgood.common.user;

import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.cleanroommc.modularui.api.IGuiHolder;
import com.cleanroommc.modularui.factory.PosGuiData;
import com.cleanroommc.modularui.screen.ModularPanel;
import com.cleanroommc.modularui.screen.UISettings;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;

import cpw.mods.fml.common.eventhandler.Event;

/**
 * Server-owned nine-slot mechanical user with a separate speed-upgrade slot.
 * Each tile owns its fake player, so tools and container items cannot leak between machines.
 * Interactions use Forge events and survival inventory semantics; no chunks are force-loaded.
 */
public final class TileMechanicalUser extends TileEntity implements ISidedInventory, IGuiHolder<PosGuiData> {

    private static final int[] AUTOMATION_SLOTS = { 0, 1, 2, 3, 4, 5, 6, 7, 8 };
    private final ItemStack[] inventory = new ItemStack[10];
    int mode;
    boolean leftClick;
    boolean firstSlotOnly;
    private int cooldown;
    private FakePlayer player;
    private boolean working;

    @Override
    public void updateEntity() {
        if (worldObj == null || worldObj.isRemote || working) return;
        if (cooldown > 0) {
            cooldown--;
            return;
        }
        cooldown = interval() - 1;
        ForgeDirection direction = ForgeDirection.getOrientation(getBlockMetadata());
        if (direction == ForgeDirection.UNKNOWN) return;
        int x = xCoord + direction.offsetX, y = yCoord + direction.offsetY, z = zCoord + direction.offsetZ;
        if (!worldObj.blockExists(x, y, z)) return;
        int slot = selectSlot();
        working = true;
        ItemStack equipped = null;
        try {
            preparePlayer(direction);
            player.inventory.mainInventory[0] = inventory[slot];
            equipped = inventory[slot] == null ? null : inventory[slot].copy();
            inventory[slot] = null;
            if (equipped != null) player.getAttributeMap()
                .applyAttributeModifiers(equipped.getAttributeModifiers());
            perform(x, y, z, direction);
        } finally {
            if (player != null) {
                if (equipped != null) player.getAttributeMap()
                    .removeAttributeModifiers(equipped.getAttributeModifiers());
                inventory[slot] = clean(player.inventory.mainInventory[0]);
                player.inventory.mainInventory[0] = null;
                // Buckets, bottles and modded tools may place byproducts into other player slots.
                for (int i = 1; i < player.inventory.mainInventory.length; i++) {
                    returnRemainder(player.inventory.mainInventory[i]);
                    player.inventory.mainInventory[i] = null;
                }
                for (int i = 0; i < player.inventory.armorInventory.length; i++) {
                    returnRemainder(player.inventory.armorInventory[i]);
                    player.inventory.armorInventory[i] = null;
                }
                player.clearItemInUse();
                player.closeScreen();
                if (isInvalid()) {
                    for (int i = 0; i < inventory.length; i++) {
                        if (inventory[i] != null) worldObj.spawnEntityInWorld(
                            new EntityItem(worldObj, xCoord + 0.5, yCoord + 0.5, zCoord + 0.5, inventory[i]));
                        inventory[i] = null;
                    }
                    player = null;
                }
            }
            working = false;
            markDirty();
        }
    }

    /** @return operation interval in ticks, bounded to prevent more than one operation per tick */
    int interval() {
        ItemStack upgrades = inventory[9];
        int count = upgrades != null && upgrades.getItem() instanceof ItemUserSpeedUpgrade ? upgrades.stackSize : 0;
        return Math.max(1, 20 - Math.max(0, Math.min(20, count)));
    }

    private int selectSlot() {
        if (firstSlotOnly) return 0;
        int selected = 0, count = 0;
        for (int i = 0; i < 9; i++) {
            if (clean(inventory[i]) != null && worldObj.rand.nextInt(++count) == 0) selected = i;
        }
        return selected;
    }

    /** Position eyes just outside the working face for tools which perform their own ray tracing. */
    private void preparePlayer(ForgeDirection direction) {
        if (player == null) {
            player = new MechanicalUserPlayer((WorldServer) worldObj);
        }
        float yaw = direction == ForgeDirection.NORTH ? 180
            : direction == ForgeDirection.WEST ? 90 : direction == ForgeDirection.EAST ? -90 : 0;
        float pitch = direction == ForgeDirection.UP ? -90 : direction == ForgeDirection.DOWN ? 90 : 0;
        player.setPositionAndRotation(
            xCoord + 0.5 + direction.offsetX * 0.51,
            yCoord + 0.5 + direction.offsetY * 0.51 - player.getEyeHeight(),
            zCoord + 0.5 + direction.offsetZ * 0.51,
            yaw,
            pitch);
        player.rotationYawHead = yaw;
        player.inventory.currentItem = 0;
        player.setSneaking(false);
    }

    private void perform(int x, int y, int z, ForgeDirection direction) {
        if (mode == 5) {
            interactEntity(x, y, z);
            return;
        }
        ItemStack held = player.getHeldItem();
        int side = direction.getOpposite()
            .ordinal();
        float hitX = 0.5F - direction.offsetX * 0.5F;
        float hitY = 0.5F - direction.offsetY * 0.5F;
        float hitZ = 0.5F - direction.offsetZ * 0.5F;
        if (mode == 4) {
            if (leftClick || held == null) return;
            PlayerInteractEvent event = ForgeEventFactory
                .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, x, y, z, -1, worldObj);
            if (!event.isCanceled() && event.useItem != Event.Result.DENY) {
                player.theItemInWorldManager.tryUseItem(player, worldObj, held);
            }
            return;
        }
        if (leftClick) {
            // Match a mouse click, including block hooks; this is not an instant block breaker.
            player.theItemInWorldManager.onBlockClicked(x, y, z, side);
            player.theItemInWorldManager.cancelDestroyingBlock(x, y, z);
            return;
        }
        if (mode == 0) {
            if (worldObj.isAirBlock(x, y, z)) {
                PlayerInteractEvent event = ForgeEventFactory
                    .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_AIR, x, y, z, -1, worldObj);
                if (held != null && !event.isCanceled() && event.useItem != Event.Result.DENY) {
                    player.theItemInWorldManager.tryUseItem(player, worldObj, held);
                }
            } else {
                player.theItemInWorldManager
                    .activateBlockOrUseItem(player, worldObj, held, x, y, z, side, hitX, hitY, hitZ);
            }
            return;
        }
        if (mode == 1) {
            if (held == null || !(held.getItem() instanceof ItemBlock)
                || !worldObj.getBlock(x, y, z)
                    .isReplaceable(worldObj, x, y, z))
                return;
            // Click our working face, placing exactly into the adjacent target cell.
            x = xCoord;
            y = yCoord;
            z = zCoord;
            side = direction.ordinal();
            hitX = 0.5F + direction.offsetX * 0.5F;
            hitY = 0.5F + direction.offsetY * 0.5F;
            hitZ = 0.5F + direction.offsetZ * 0.5F;
        }
        PlayerInteractEvent event = ForgeEventFactory
            .onPlayerInteract(player, PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, x, y, z, side, worldObj);
        if (event.isCanceled()) return;
        if (mode == 3) {
            if (event.useBlock != Event.Result.DENY) {
                Block block = worldObj.getBlock(x, y, z);
                block.onBlockActivated(worldObj, x, y, z, player, side, hitX, hitY, hitZ);
            }
        } else if (held != null && event.useItem != Event.Result.DENY) {
            if (!held.getItem()
                .onItemUseFirst(held, player, worldObj, x, y, z, side, hitX, hitY, hitZ)) {
                held.tryPlaceItemIntoWorld(player, worldObj, x, y, z, side, hitX, hitY, hitZ);
            }
        }
    }

    private void interactEntity(int x, int y, int z) {
        List<?> entities = worldObj
            .getEntitiesWithinAABBExcludingEntity(player, AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1));
        for (Object object : entities) {
            Entity entity = (Entity) object;
            if (entity.isDead || entity instanceof EntityItem || entity instanceof EntityPlayer) continue;
            if (leftClick) player.attackTargetEntityWithCurrentItem(entity);
            else player.interactWith(entity);
            break;
        }
    }

    /** Return container items to the buffer, dropping only overflow in the world. */
    private void returnRemainder(ItemStack stack) {
        if (clean(stack) == null) return;
        for (int i = 0; i < 9 && stack.stackSize > 0; i++) {
            ItemStack existing = inventory[i];
            if (existing == null) {
                int amount = Math.min(stack.stackSize, Math.min(64, stack.getMaxStackSize()));
                inventory[i] = stack.splitStack(amount);
            } else if (existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int amount = Math.min(stack.stackSize, Math.max(0, existing.getMaxStackSize() - existing.stackSize));
                existing.stackSize += amount;
                stack.stackSize -= amount;
            }
        }
        if (stack.stackSize > 0)
            worldObj.spawnEntityInWorld(new EntityItem(worldObj, xCoord + 0.5, yCoord + 1, zCoord + 0.5, stack));
    }

    private static ItemStack clean(ItemStack stack) {
        return stack == null || stack.stackSize <= 0 ? null : stack;
    }

    @Override
    public ModularPanel buildUI(PosGuiData data, PanelSyncManager sync, UISettings settings) {
        return MechanicalUserGui.build(this, sync);
    }

    @Override
    public void invalidate() {
        if (!working) player = null;
        super.invalidate();
    }

    @Override
    public void onChunkUnload() {
        player = null;
        super.onChunkUnload();
    }

    @Override
    public int getSizeInventory() {
        return inventory.length;
    }

    @Override
    public ItemStack getStackInSlot(int slot) {
        return inventory[slot];
    }

    @Override
    public ItemStack decrStackSize(int slot, int amount) {
        if (inventory[slot] == null || amount <= 0) return null;
        ItemStack result = inventory[slot].splitStack(Math.min(amount, inventory[slot].stackSize));
        inventory[slot] = clean(inventory[slot]);
        markDirty();
        return result;
    }

    @Override
    public ItemStack getStackInSlotOnClosing(int slot) {
        ItemStack stack = inventory[slot];
        inventory[slot] = null;
        markDirty();
        return stack;
    }

    @Override
    public void setInventorySlotContents(int slot, ItemStack stack) {
        inventory[slot] = clean(stack);
        if (inventory[slot] != null) inventory[slot].stackSize = Math
            .min(stack.stackSize, Math.min(getInventoryStackLimit(), stack.getMaxStackSize()));
        markDirty();
    }

    @Override
    public String getInventoryName() {
        return "tile.mechanical_user.name";
    }

    @Override
    public boolean hasCustomInventoryName() {
        return false;
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUseableByPlayer(EntityPlayer entity) {
        return worldObj != null && worldObj.getTileEntity(xCoord, yCoord, zCoord) == this
            && entity.getDistanceSq(xCoord + 0.5, yCoord + 0.5, zCoord + 0.5) <= 64;
    }

    @Override
    public void openInventory() {}

    @Override
    public void closeInventory() {}

    @Override
    public boolean isItemValidForSlot(int slot, ItemStack stack) {
        return slot < 9 || stack != null && stack.getItem() instanceof ItemUserSpeedUpgrade;
    }

    @Override
    public int[] getAccessibleSlotsFromSide(int side) {
        return AUTOMATION_SLOTS.clone();
    }

    @Override
    public boolean canInsertItem(int slot, ItemStack stack, int side) {
        return slot < 9;
    }

    @Override
    public boolean canExtractItem(int slot, ItemStack stack, int side) {
        return slot < 9;
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        super.writeToNBT(tag);
        tag.setInteger("Mode", mode);
        tag.setBoolean("LeftClick", leftClick);
        tag.setBoolean("FirstSlotOnly", firstSlotOnly);
        NBTTagList items = new NBTTagList();
        for (int i = 0; i < inventory.length; i++) {
            if (clean(inventory[i]) == null) continue;
            NBTTagCompound entry = new NBTTagCompound();
            entry.setByte("Slot", (byte) i);
            inventory[i].writeToNBT(entry);
            items.appendTag(entry);
        }
        tag.setTag("Items", items);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        mode = Math.max(0, Math.min(5, tag.getInteger("Mode")));
        leftClick = tag.getBoolean("LeftClick");
        firstSlotOnly = tag.getBoolean("FirstSlotOnly");
        java.util.Arrays.fill(inventory, null);
        NBTTagList items = tag.getTagList("Items", 10);
        for (int i = 0; i < items.tagCount(); i++) {
            NBTTagCompound entry = items.getCompoundTagAt(i);
            int slot = entry.getByte("Slot") & 255;
            if (slot < inventory.length) inventory[slot] = clean(ItemStack.loadItemStackFromNBT(entry));
        }
        player = null;
        cooldown = 0;
    }
}
