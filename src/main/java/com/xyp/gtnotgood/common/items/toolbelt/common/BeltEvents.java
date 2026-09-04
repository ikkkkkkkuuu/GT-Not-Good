package com.xyp.gtnotgood.common.items.toolbelt.common;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;

import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;

public class BeltEvents {

    @SubscribeEvent
    public void onEntityConstructing(net.minecraftforge.event.entity.EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityLivingBase) {
            ToolBeltData.register((EntityLivingBase) event.entity);
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data != null) {
            data.syncToSelf();
        }
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        EntityPlayer player = event.player;
        if (player.worldObj.isRemote) return;
        ToolBeltData data = ToolBeltData.get(player);
        if (data != null) {
            data.syncToSelf();
        }
    }

    @SubscribeEvent
    public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
        if (event.target instanceof EntityPlayer) {
            ToolBeltData data = ToolBeltData.get((EntityPlayer) event.target);
            if (data != null) {
                data.syncToSelf();
            }
        }
    }

    @SubscribeEvent
    public void onLivingDrops(net.minecraftforge.event.entity.living.LivingDropsEvent event) {
        EntityLivingBase entity = event.entityLiving;
        ToolBeltData data = ToolBeltData.get(entity);
        if (data == null) return;

        // Drop all items from tool belt on death
        for (int i = 0; i < ToolBeltData.SLOT_COUNT; i++) {
            ItemStack stack = data.getStackInSlot(i);
            if (stack != null) {
                if (entity instanceof EntityPlayer) {
                    EntityPlayer player = (EntityPlayer) entity;
                    if (!player.worldObj.getGameRules()
                        .getGameRuleBooleanValue("keepInventory") && !player.capabilities.isCreativeMode) {
                        event.drops
                            .add(new EntityItem(entity.worldObj, entity.posX, entity.posY, entity.posZ, stack.copy()));
                        data.setStackInSlot(i, null);
                    }
                } else {
                    event.drops
                        .add(new EntityItem(entity.worldObj, entity.posX, entity.posY, entity.posZ, stack.copy()));
                    data.setStackInSlot(i, null);
                }
            }
        }
    }

    @SubscribeEvent
    public void onPlayerClone(net.minecraftforge.event.entity.player.PlayerEvent.Clone event) {
        EntityPlayer oldPlayer = event.original;
        EntityPlayer newPlayer = event.entityPlayer;

        ToolBeltData oldData = ToolBeltData.get(oldPlayer);
        ToolBeltData newData = ToolBeltData.get(newPlayer);

        if (oldData != null && newData != null) {
            for (int i = 0; i < ToolBeltData.SLOT_COUNT; i++) {
                ItemStack stack = oldData.getStackInSlot(i);
                newData.setStackInSlot(i, stack != null ? stack.copy() : null);
            }
        }
    }
}
