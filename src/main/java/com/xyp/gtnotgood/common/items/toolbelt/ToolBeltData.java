package com.xyp.gtnotgood.common.items.toolbelt;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.packet.SyncToolBeltData;

import cpw.mods.fml.common.network.NetworkRegistry;

/**
 * 工具腰带数据 - 存储在玩家身上，固定10个槽位，无需物品
 * Tool belt data - stored on player, fixed 10 slots, no item needed
 */
public class ToolBeltData implements IExtendedEntityProperties {

    public static final String PROP_NAME = "ToolBeltData";
    public static final int SLOT_COUNT = 10;
    private static final String NBT_ITEMS = "Items";

    private final EntityLivingBase owner;
    private final ItemStack[] items = new ItemStack[SLOT_COUNT];

    public ToolBeltData(EntityLivingBase owner) {
        this.owner = owner;
    }

    public static ToolBeltData get(EntityLivingBase entity) {
        if (entity == null) return null;
        return (ToolBeltData) entity.getExtendedProperties(PROP_NAME);
    }

    public static void register(EntityLivingBase entity) {
        entity.registerExtendedProperties(PROP_NAME, new ToolBeltData(entity));
    }

    public EntityLivingBase getOwner() {
        return owner;
    }

    public int getSlotCount() {
        return SLOT_COUNT;
    }

    public ItemStack getStackInSlot(int slot) {
        if (slot < 0 || slot >= SLOT_COUNT) return null;
        return items[slot];
    }

    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items[slot] = stack;
        onContentsChanged();
    }

    /**
     * 无声设置槽位（不触发同步，用于客户端接收服务器同步数据）
     */
    public void setStackInSlotSilent(int slot, ItemStack stack) {
        if (slot < 0 || slot >= SLOT_COUNT) return;
        items[slot] = stack;
    }

    /**
     * 尝试将物品插入腰带，优先合并已有堆叠，其次放入空槽
     * Try to insert a stack into the belt, merge first, then empty slot
     * 
     * @return remaining stack (null if fully inserted)
     */
    public ItemStack tryInsert(ItemStack stack) {
        if (stack == null || stack.stackSize <= 0) return null;

        // Try to merge with existing stacks
        for (int i = 0; i < SLOT_COUNT; i++) {
            ItemStack existing = items[i];
            if (existing != null && existing.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(existing, stack)) {
                int max = existing.getMaxStackSize();
                int total = existing.stackSize + stack.stackSize;
                if (total <= max) {
                    existing.stackSize = total;
                    onContentsChanged();
                    return null;
                } else {
                    existing.stackSize = max;
                    stack.stackSize = total - max;
                }
            }
        }

        // Try to find an empty slot
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items[i] == null) {
                items[i] = stack.copy();
                onContentsChanged();
                return null;
            }
        }

        // Belt is full
        return stack;
    }

    public void onContentsChanged() {
        if (!owner.worldObj.isRemote) {
            syncToTracking();
        }
    }

    public void syncToTracking() {
        if (!(owner instanceof EntityPlayerMP)) return;
        EntityPlayerMP player = (EntityPlayerMP) owner;
        NetworkRegistry.TargetPoint point = new NetworkRegistry.TargetPoint(
            owner.dimension,
            owner.posX,
            owner.posY,
            owner.posZ,
            64.0);
        GTNotGood.channel.sendToAllAround(new SyncToolBeltData(owner, this), point);
    }

    public void syncToSelf() {
        if (!(owner instanceof EntityPlayerMP)) return;
        GTNotGood.channel.sendTo(new SyncToolBeltData(owner, this), (EntityPlayerMP) owner);
    }

    @Override
    public void saveNBTData(NBTTagCompound compound) {
        NBTTagCompound data = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < SLOT_COUNT; i++) {
            if (items[i] != null) {
                NBTTagCompound slotTag = new NBTTagCompound();
                slotTag.setByte("Slot", (byte) i);
                items[i].writeToNBT(slotTag);
                list.appendTag(slotTag);
            }
        }
        if (list.tagCount() > 0) {
            data.setTag(NBT_ITEMS, list);
        }
        compound.setTag(PROP_NAME, data);
    }

    @Override
    public void loadNBTData(NBTTagCompound compound) {
        NBTTagCompound data = compound.getCompoundTag(PROP_NAME);
        if (data.hasKey(NBT_ITEMS)) {
            NBTTagList list = data.getTagList(NBT_ITEMS, Constants.NBT.TAG_COMPOUND);
            for (int i = 0; i < list.tagCount(); i++) {
                NBTTagCompound slotTag = list.getCompoundTagAt(i);
                int slot = slotTag.getByte("Slot") & 255;
                if (slot >= 0 && slot < SLOT_COUNT) {
                    items[slot] = ItemStack.loadItemStackFromNBT(slotTag);
                }
            }
        }
    }

    @Override
    public void init(Entity entity, World world) {}
}
