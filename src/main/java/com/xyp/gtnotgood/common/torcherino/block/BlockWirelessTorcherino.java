package com.xyp.gtnotgood.common.torcherino.block;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

import com.cleanroommc.modularui.factory.TileEntityGuiFactory;
import com.xyp.gtnotgood.common.torcherino.tile.TileWirelessTorcherinoBase;
import com.xyp.gtnotgood.config.Config;

import gregtech.api.enums.ItemList;
import gregtech.api.metatileentity.BaseMetaTileEntity;

/**
 * Wireless Torcherino block that binds machines from a GT Data Stick and opens its bound-machine GUI.
 */
public class BlockWirelessTorcherino extends BlockTorcherinoBase {

    private static final String DATA_STICK_TYPE = "GTNotGoodWirelessTorcherino";

    private final TorcherinoTileFactory tileFactory;

    /**
     * Creates one wireless Torcherino tier.
     *
     * @param blockName   unlocalized and registry name
     * @param iconName    texture name
     * @param light       emitted light
     * @param tileFactory tile factory for this tier
     */
    public BlockWirelessTorcherino(String blockName, String iconName, float light, TorcherinoTileFactory tileFactory) {
        super(blockName, iconName, light);
        this.tileFactory = tileFactory;
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return tileFactory.create();
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int side, float hitX,
        float hitY, float hitZ) {
        if (world.isRemote) return true;

        TileEntity tile = world.getTileEntity(x, y, z);
        if (!(tile instanceof TileWirelessTorcherinoBase)) return true;
        TileWirelessTorcherinoBase torch = (TileWirelessTorcherinoBase) tile;
        if (handleDataStickInteraction(world, player, torch)) return true;

        TileEntityGuiFactory.INSTANCE.open(player, x, y, z);
        return true;
    }

    /**
     * Writes a wireless Torcherino target into the held GT Data Stick.
     *
     * @param world  world containing the target
     * @param x      target X coordinate
     * @param y      target Y coordinate
     * @param z      target Z coordinate
     * @param player player holding the data stick
     * @param held   held data stick stack
     * @param target target tile entity
     */
    public static void writeBindingToDataStick(World world, int x, int y, int z, EntityPlayer player, ItemStack held,
        TileEntity target) {
        NBTTagCompound tag = held.stackTagCompound;
        if (tag == null) {
            tag = new NBTTagCompound();
            held.stackTagCompound = tag;
        }

        String machineName = getMachineName(world, x, y, z, target);
        tag.setString("type", DATA_STICK_TYPE);
        tag.setInteger("machineX", x);
        tag.setInteger("machineY", y);
        tag.setInteger("machineZ", z);
        tag.setInteger("machineDim", world.provider.dimensionId);
        tag.setString("machineName", machineName);
        tag.removeTag("torchX");
        tag.removeTag("torchY");
        tag.removeTag("torchZ");

        // #tr gtnotgood.torcherino.wireless.datastick.name
        // # Wireless Torcherino Link [%s]
        // # zh_CN 无线加速火把链接 [%s]
        held.setStackDisplayName(
            StatCollector.translateToLocalFormatted("gtnotgood.torcherino.wireless.datastick.name", machineName));
        // #tr gtnotgood.torcherino.wireless.datastick.machine_saved
        // # Saved "%s" at (%d, %d, %d). Right-click a wireless Torcherino to bind it.
        // # zh_CN 已保存 "%s" 坐标 (%d, %d, %d),右键无线加速火把完成绑定。
        player.addChatMessage(
            new ChatComponentText(
                StatCollector.translateToLocalFormatted(
                    "gtnotgood.torcherino.wireless.datastick.machine_saved",
                    machineName,
                    x,
                    y,
                    z)));
    }

    /**
     * Consumes wireless-link NBT from the player's GT Data Stick and binds the saved machine to a torch.
     *
     * @param world  world containing the torch and target
     * @param player player using the data stick
     * @param torch  wireless Torcherino tile
     * @return {@code true} when the held item was handled as a wireless-link data stick
     */
    public static boolean handleDataStickInteraction(World world, EntityPlayer player,
        TileWirelessTorcherinoBase torch) {
        if (!Config.enableWirelessTorcherino) return false;

        ItemStack held = player.getHeldItem();
        if (held == null || !ItemList.Tool_DataStick.isStackEqual(held, false, true)) return false;

        NBTTagCompound tag = held.stackTagCompound;
        if (tag == null || !DATA_STICK_TYPE.equals(tag.getString("type"))) return false;
        if (!tag.hasKey("machineX") || !tag.hasKey("machineY") || !tag.hasKey("machineZ")) return false;

        int machineX = tag.getInteger("machineX");
        int machineY = tag.getInteger("machineY");
        int machineZ = tag.getInteger("machineZ");
        int machineDim = tag.hasKey("machineDim") ? tag.getInteger("machineDim") : world.provider.dimensionId;

        if (machineDim != world.provider.dimensionId) {
            // #tr gtnotgood.torcherino.wireless.datastick.wrong_dim
            // # Saved machine is in another dimension. Link data cleared.
            // # zh_CN 已保存的机器在其他维度,已清除链接数据。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector.translateToLocal("gtnotgood.torcherino.wireless.datastick.wrong_dim")));
            clearDataStick(held, tag);
            return true;
        }

        TileEntity targetTe = world.getTileEntity(machineX, machineY, machineZ);
        if (targetTe == null || targetTe.isInvalid()) {
            // #tr gtnotgood.torcherino.wireless.datastick.machine_gone
            // # Saved machine no longer exists. Link data cleared.
            // # zh_CN 已保存的机器不存在了,已清除链接数据。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector.translateToLocal("gtnotgood.torcherino.wireless.datastick.machine_gone")));
            clearDataStick(held, tag);
            return true;
        }

        if (!torch.isInRange(machineX, machineY, machineZ)) {
            // #tr gtnotgood.torcherino.wireless.datastick.out_of_range
            // # Target is outside this wireless Torcherino's range.
            // # zh_CN 目标超出这个无线加速火把的绑定范围。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector.translateToLocal("gtnotgood.torcherino.wireless.datastick.out_of_range")));
            clearDataStick(held, tag);
            return true;
        }

        if (machineX == torch.xCoord && machineY == torch.yCoord && machineZ == torch.zCoord) {
            // #tr gtnotgood.torcherino.wireless.datastick.cannot_bind_self
            // # A wireless Torcherino cannot bind itself.
            // # zh_CN 无线加速火把不能绑定自己。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector.translateToLocal("gtnotgood.torcherino.wireless.datastick.cannot_bind_self")));
            clearDataStick(held, tag);
            return true;
        }

        if (torch.addBoundMachine(machineX, machineY, machineZ, machineDim)) {
            String machineName = tag.hasKey("machineName") ? tag.getString("machineName") : "Unknown";
            // #tr gtnotgood.torcherino.wireless.datastick.bound
            // # Bound machine "%s" successfully.
            // # zh_CN 机器 "%s" 绑定成功。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector
                        .translateToLocalFormatted("gtnotgood.torcherino.wireless.datastick.bound", machineName)));
        } else {
            // #tr gtnotgood.torcherino.wireless.datastick.bind_failed
            // # Binding failed. The list may be full.
            // # zh_CN 绑定失败,列表可能已满。
            player.addChatMessage(
                new ChatComponentText(
                    StatCollector.translateToLocal("gtnotgood.torcherino.wireless.datastick.bind_failed")));
        }

        clearDataStick(held, tag);
        return true;
    }

    /**
     * Extracts a localized display name from a tile entity for data-stick binding messages.
     *
     * @param world world containing the target
     * @param x     target X coordinate
     * @param y     target Y coordinate
     * @param z     target Z coordinate
     * @param te    target tile entity
     * @return best-effort localized machine name
     */
    public static String getMachineName(World world, int x, int y, int z, TileEntity te) {
        if (te instanceof BaseMetaTileEntity) {
            BaseMetaTileEntity baseMetaTileEntity = (BaseMetaTileEntity) te;
            if (baseMetaTileEntity.getMetaTileEntity() == null) return getBlockName(world, x, y, z);
            String name = baseMetaTileEntity.getMetaTileEntity()
                .getLocalName();
            if (name != null && !name.isEmpty()) {
                String translated = StatCollector.translateToLocal(name);
                return translated.isEmpty() ? name : translated;
            }
        }

        return getBlockName(world, x, y, z);
    }

    private static String getBlockName(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block != null) {
            String name = block.getLocalizedName();
            if (name != null && !name.isEmpty()) {
                String translated = StatCollector.translateToLocal(name);
                return translated.isEmpty() ? name : translated;
            }
        }
        return "Unknown";
    }

    /**
     * Clears temporary wireless-link data from a GT Data Stick.
     *
     * @param held stack to clean
     * @param tag  current stack tag
     */
    public static void clearDataStick(ItemStack held, NBTTagCompound tag) {
        tag.removeTag("type");
        tag.removeTag("machineX");
        tag.removeTag("machineY");
        tag.removeTag("machineZ");
        tag.removeTag("machineDim");
        tag.removeTag("machineName");
        tag.removeTag("torchX");
        tag.removeTag("torchY");
        tag.removeTag("torchZ");

        if (held.stackTagCompound != null && held.stackTagCompound.hasKey("display")) {
            NBTTagCompound display = held.stackTagCompound.getCompoundTag("display");
            display.removeTag("Name");
            if (display.hasNoTags()) {
                held.stackTagCompound.removeTag("display");
            }
        }
        if (held.stackTagCompound != null && held.stackTagCompound.hasNoTags()) {
            held.stackTagCompound = null;
        }
    }
}
