package com.xyp.gtnotgood.client.radialmenu.client.script;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;

import com.xyp.gtnotgood.client.radialmenu.config.AccentConfig;
import com.xyp.gtnotgood.client.radialmenu.core.model.AccentCoefficients;
import com.xyp.gtnotgood.client.radialmenu.core.script.LookTarget;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptContext;
import com.xyp.gtnotgood.client.radialmenu.core.script.ScriptItem;

public final class ClientScriptContext implements ScriptContext {

    public static final ClientScriptContext INSTANCE = new ClientScriptContext();

    private ClientScriptContext() {}

    private static EntityPlayer player() {
        return Minecraft.getMinecraft().thePlayer;
    }

    @Override
    public String playerName() {
        EntityPlayer player = player();
        return player == null ? "" : player.getCommandSenderName();
    }

    @Override
    public int dimension() {
        EntityPlayer player = player();
        return player == null ? 0 : player.dimension;
    }

    @Override
    public int blockX() {
        EntityPlayer player = player();
        return player == null ? 0 : (int) Math.floor(player.posX);
    }

    @Override
    public int blockY() {
        EntityPlayer player = player();
        return player == null ? 0 : (int) Math.floor(player.posY);
    }

    @Override
    public int blockZ() {
        EntityPlayer player = player();
        return player == null ? 0 : (int) Math.floor(player.posZ);
    }

    @Override
    public double yaw() {
        EntityPlayer player = player();
        return player == null ? 0.0 : player.rotationYaw;
    }

    @Override
    public double pitch() {
        EntityPlayer player = player();
        return player == null ? 0.0 : player.rotationPitch;
    }

    @Override
    public double health() {
        EntityPlayer player = player();
        return player == null ? 0.0 : player.getHealth();
    }

    @Override
    public int food() {
        EntityPlayer player = player();
        return player == null ? 0 : player.getFoodStats().getFoodLevel();
    }

    @Override
    public int air() {
        EntityPlayer player = player();
        return player == null ? 0 : player.getAir();
    }

    @Override
    public ScriptItem heldItem() {
        EntityPlayer player = player();
        return player == null ? null : itemOf(player.getHeldItem(), 0);
    }

    @Override
    public List<ScriptItem> inventory() {
        EntityPlayer player = player();
        if (player == null || player.inventory == null) {
            return Collections.emptyList();
        }
        ItemStack[] slots = player.inventory.mainInventory;
        List<ScriptItem> items = new ArrayList<>();
        for (int i = 0; i < slots.length; i++) {
            ScriptItem item = itemOf(slots[i], i + 1);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    @Override
    public LookTarget lookingAt() {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition hit = mc.objectMouseOver;
        if (hit == null || mc.theWorld == null) {
            return null;
        }

        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            Block block = mc.theWorld.getBlock(hit.blockX, hit.blockY, hit.blockZ);
            if (block == null) {
                return null;
            }
            int meta = mc.theWorld.getBlockMetadata(hit.blockX, hit.blockY, hit.blockZ);
            return LookTarget.block(name(Block.blockRegistry.getNameForObject(block)), blockLabel(block, meta), meta,
                hit.blockX, hit.blockY, hit.blockZ);
        }

        if (hit.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && hit.entityHit != null) {
            Entity entity = hit.entityHit;
            String id = EntityList.getEntityString(entity);
            String label = entity.getCommandSenderName();
            return LookTarget.entity(id == null || id.isEmpty() ? name(label) : id, label,
                (int) Math.floor(entity.posX), (int) Math.floor(entity.posY), (int) Math.floor(entity.posZ));
        }
        return null;
    }

    @Override
    public long worldTime() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc.theWorld == null ? 0L : mc.theWorld.getWorldTime();
    }

    @Override
    public String worldName() {
        Minecraft mc = Minecraft.getMinecraft();

        ServerData server = mc.func_147104_D();
        if (server != null && server.serverIP != null) {
            return server.serverIP;
        }
        if (mc.getIntegratedServer() != null) {
            return name(mc.getIntegratedServer().getFolderName());
        }
        return "";
    }

    @Override
    public String storeGet(String key) {
        return ScriptStore.get(key);
    }

    private static ScriptItem itemOf(ItemStack stack, int slot) {
        if (stack == null || stack.getItem() == null) {
            return null;
        }
        String id = name(Item.itemRegistry.getNameForObject(stack.getItem()));
        String label = id;
        try {
            label = stack.getDisplayName();
        } catch (Throwable ignored) {}
        return new ScriptItem(id, stack.getItemDamage(), stack.stackSize, label, slot);
    }

    private static String blockLabel(Block block, int meta) {
        try {
            return new ItemStack(block, 1, meta).getDisplayName();
        } catch (Throwable ignored) {}
        try {
            return block.getLocalizedName();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String name(Object value) {
        return value == null ? "" : value.toString();
    }

    @Override
    public AccentCoefficients accentCoefficients() {
        return AccentConfig.coefficients();
    }
}
