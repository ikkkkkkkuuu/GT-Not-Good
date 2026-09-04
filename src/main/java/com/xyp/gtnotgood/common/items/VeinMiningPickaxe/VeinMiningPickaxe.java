package com.xyp.gtnotgood.common.items.VeinMiningPickaxe;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPickaxe;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.EnumHelper;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.oredict.OreDictionary;

import com.github.bsideup.jabel.Desugar;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.config.Config;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;
import com.xyp.gtnotgood.utils.item.ItemUtils;
import com.xyp.gtnotgood.utils.item.SubtitleDisplay;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.items.MetaGeneratedTool;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

public class VeinMiningPickaxe extends ItemPickaxe implements SubtitleDisplay {

    /** Players currently being processed by the synchronous vein-harvest pass. */
    private final Set<UUID> activePlayers = new HashSet<>();

    /** The six axis-aligned neighbors used by the breadth-first scan. */
    private static final int[][] NEIGHBOR_OFFSETS = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    // #tr item.VeinMiningPickaxe.name
    // # Vein Mining Pickaxe
    // # zh_CN 矿脉挖掘镐
    public VeinMiningPickaxe() {
        super(EnumHelper.addToolMaterial("VEIN", 15, 20000000, 15, 3, 10));
        this.setUnlocalizedName("VeinMiningPickaxe");
        this.setCreativeTab(GTNGCreativeTabs.GTNGItem);
        this.setTextureName(GTNotGood.RESOURCE_ROOT_ID + ":" + "SingularityPickaxe/SingularityPickaxe");
        this.setMaxStackSize(1);
        this.setMaxDamage(20000000);
        MinecraftForge.EVENT_BUS.register(this);
        FMLCommonHandler.instance()
            .bus()
            .register(this);
        GameRegistry.registerItem(this, getUnlocalizedName());
        GTNGItemList.VeinMiningPickaxe.set(new ItemStack(this, 1));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, EntityPlayer player, List<String> toolTip,
        boolean advancedToolTips) {
        NBTTagCompound tags = itemStack.getTagCompound();
        int range = 3;
        int amount = 327670;
        boolean preciseMode = false;

        if (tags != null) {
            if (tags.hasKey("range")) {
                range = Math.max(0, Math.min(Config.VeinMinerPickaxe.maxRange, tags.getInteger("range")));
            }
            if (tags.hasKey("amount")) {
                amount = Math.max(0, Math.min(Config.VeinMinerPickaxe.maxAmount, tags.getInteger("amount")));
            }
            if (tags.hasKey("preciseMode")) {
                preciseMode = tags.getBoolean("preciseMode");
            }
        }

        // #tr Tooltip_VeinMiningPickaxe_00
        // # Range: %d
        // # zh_CN 范围: %d
        toolTip.add(StatCollector.translateToLocalFormatted("Tooltip_VeinMiningPickaxe_00", range));

        // #tr Tooltip_VeinMiningPickaxe_01
        // # Amount: %d
        // # zh_CN 数量: %d
        toolTip.add(StatCollector.translateToLocalFormatted("Tooltip_VeinMiningPickaxe_01", amount));

        // #tr Tooltip_VeinMiningPickaxe_PreciseMode_On
        // # §aPrecise Mode: ON
        // # zh_CN §a精确模式: 开启
        // #tr Tooltip_VeinMiningPickaxe_PreciseMode_Off
        // # §cPrecise Mode: OFF
        // # zh_CN §c精确模式: 关闭
        toolTip.add(
            StatCollector.translateToLocal(
                preciseMode ? "Tooltip_VeinMiningPickaxe_PreciseMode_On"
                    : "Tooltip_VeinMiningPickaxe_PreciseMode_Off"));

        // #tr Tooltip_VeinMiningPickaxe_ShiftScroll
        // # §7Hold §eShift§7 + §eScroll§7 to adjust range
        // # zh_CN §7按住§eShift§7 + §e滚轮§7调整范围
        toolTip.add(StatCollector.translateToLocal("Tooltip_VeinMiningPickaxe_ShiftScroll"));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack, int pass) {
        NBTTagCompound nbt = stack.getTagCompound();
        return nbt != null && nbt.getBoolean("preciseMode");
    }

    @Override
    public void setDamage(ItemStack stack, int damage) {
        ItemUtils.setToolDamage(stack, damage);
    }

    @Override
    public int getDamage(ItemStack stack) {
        return Math.toIntExact(MetaGeneratedTool.getToolDamage(stack));
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return Math.toIntExact(MetaGeneratedTool.getToolMaxDamage(stack));
    }

    @Override
    public boolean canHarvestBlock(Block block, ItemStack stack) {
        return true;
    }

    @Override
    public float getDigSpeed(ItemStack stack, Block block, int meta) {
        return 20;
    }

    @Override
    public boolean isRepairable() {
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item aItem, CreativeTabs aCreativeTabs, List<ItemStack> aList) {
        ItemStack stack = new ItemStack(this, 1);
        ItemUtils.setToolMaxDamage(stack, 20000000);
        aList.add(stack);
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (player.isSneaking()) {
            NBTTagCompound tags = stack.getTagCompound();
            if (tags == null) {
                tags = new NBTTagCompound();
                stack.setTagCompound(tags);
            }

            boolean isPreciseMode = !tags.getBoolean("preciseMode");
            tags.setBoolean("preciseMode", isPreciseMode);
            player.swingItem();

            if (world.isRemote) {
                // #tr Tooltip_VeinMiningPickaxe_PreciseMode_On
                // # §aPrecise Mode: ON
                // # zh_CN §a精确模式: 开启
                // #tr Tooltip_VeinMiningPickaxe_PreciseMode_Off
                // # §cPrecise Mode: OFF
                // # zh_CN §c精确模式: 关闭
                String key = isPreciseMode ? StatCollector.translateToLocal("Tooltip_VeinMiningPickaxe_PreciseMode_On")
                    : StatCollector.translateToLocal("Tooltip_VeinMiningPickaxe_PreciseMode_Off");
                showSubtitle(key);
            }
        }
        return stack;
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        EntityPlayer player = event.getPlayer();
        World world = player.worldObj;
        if (world.isRemote || !(player instanceof EntityPlayerMP playerMP)) return;
        if (playerMP.isSneaking()) {
            ItemStack stack = playerMP.getCurrentEquippedItem();
            if (stack == null || !(stack.getItem() instanceof VeinMiningPickaxe)) {
                return;
            }
            int range = 3;
            int amount = 327670;
            boolean preciseMode = false;

            NBTTagCompound tags = stack.getTagCompound();
            if (tags != null) {
                if (tags.hasKey("range")) {
                    range = Math.max(-1, Math.min(Config.VeinMinerPickaxe.maxRange, tags.getInteger("range")));
                }
                if (tags.hasKey("preciseMode")) {
                    preciseMode = tags.getBoolean("preciseMode");
                }
                if (tags.hasKey("amount")) {
                    amount = Math.max(0, Math.min(Config.VeinMinerPickaxe.maxAmount, tags.getInteger("amount")));
                }
            }

            Block block = event.block;
            int meta = world.getBlockMetadata(event.x, event.y, event.z);

            if (block != null && range >= 0 && !activePlayers.contains(playerMP.getUniqueID())) {
                if (!activePlayers.add(playerMP.getUniqueID())) return;
                clearConnectedBlocks(
                    playerMP,
                    stack,
                    event.x,
                    event.y,
                    event.z,
                    block,
                    meta,
                    range,
                    amount,
                    preciseMode);
            }
        }
    }

    public void clearConnectedBlocks(EntityPlayerMP player, ItemStack stack, int x, int y, int z, Block targetBlock,
        int targetMeta, int maxGap, int amount, boolean preciseMode) {
        if (player.getFoodStats()
            .getFoodLevel() <= 0
            && player.getFoodStats()
                .getSaturationLevel() <= 0f
            && !player.capabilities.isCreativeMode) {
            activePlayers.remove(player.getUniqueID());
            return;
        }
        try {
            World world = player.worldObj;
            Queue<Node> queue = new ArrayDeque<>();
            Long2IntOpenHashMap bestGap = new Long2IntOpenHashMap();
            bestGap.defaultReturnValue(Integer.MAX_VALUE);
            int cleared = 0;
            int blocksSinceHunger = 0;
            int toolMaxDamage = Math.toIntExact(MetaGeneratedTool.getToolMaxDamage(stack));
            long toolDamage = MetaGeneratedTool.getToolDamage(stack);
            boolean silkTouch = EnchantmentHelper.getSilkTouchModifier(player);
            int fortune = EnchantmentHelper.getFortuneModifier(player);
            String[] targetOreNames = oreNames(new ItemStack(targetBlock, 1, targetMeta));
            Object2IntOpenHashMap<ItemStackWrapper> merged = new Object2IntOpenHashMap<>();

            long startKey = positionKey(x, y, z);
            bestGap.put(startKey, 0);
            queue.add(new Node(x, y, z, 0));

            while (!queue.isEmpty() && cleared < amount) {
                if (!player.isSneaking()) break;

                Node node = queue.poll();
                int px = node.x, py = node.y, pz = node.z;
                int gap = node.gap;

                long key = positionKey(px, py, pz);
                if (gap != bestGap.get(key)) continue;
                if (!world.blockExists(px, py, pz)) continue;

                int meta = world.getBlockMetadata(px, py, pz);
                Block block = world.getBlock(px, py, pz);
                if (block == null) continue;
                TileEntity tileEntity = block.hasTileEntity(meta) ? world.getTileEntity(px, py, pz) : null;

                boolean matches = false;
                if (block.getBlockHardness(world, px, py, pz) >= 0) {
                    if (tileEntity instanceof IGregTechTileEntity gtTE) {
                        meta = gtTE.getMetaTileID();
                    }
                    if (block == targetBlock && (!preciseMode || meta == targetMeta)) {
                        matches = true;
                    } else {
                        ItemStack stackAt = new ItemStack(block, 1, meta);
                        matches = matchesOre(stackAt, targetOreNames, preciseMode);
                    }
                }

                if (matches) {
                    List<ItemStack> drops = removeBlockAndGetDrops(
                        player,
                        stack,
                        world,
                        px,
                        py,
                        pz,
                        block,
                        silkTouch,
                        fortune);
                    if (!player.capabilities.isCreativeMode) {
                        for (ItemStack drop : drops) {
                            if (drop != null) merged.addTo(new ItemStackWrapper(drop), drop.stackSize);
                        }
                    }

                    cleared++;
                    blocksSinceHunger++;
                    gap = 0;

                    if (blocksSinceHunger >= 50) {
                        blocksSinceHunger = 0;
                        player.getFoodStats()
                            .addExhaustion(1f);
                    }

                    if (player.worldObj.rand.nextFloat() < 0.5f && !player.capabilities.isCreativeMode) {
                        if (toolMaxDamage > 0) {
                            if (toolDamage + 1 >= toolMaxDamage) {
                                world
                                    .playSoundEffect(player.posX, player.posY, player.posZ, "random.break", 1.0F, 1.0F);
                                if (stack.stackSize > 0) stack.stackSize--;
                                break;
                            } else {
                                toolDamage++;
                                ItemUtils.setToolDamage(stack, toolDamage);
                            }
                        }
                    }

                } else {
                    if (gap >= maxGap) continue;
                    gap++;
                }

                for (int[] offset : NEIGHBOR_OFFSETS) {
                    int nx = px + offset[0];
                    int ny = py + offset[1];
                    int nz = pz + offset[2];
                    long neighborKey = positionKey(nx, ny, nz);
                    if (gap < bestGap.get(neighborKey)) {
                        bestGap.put(neighborKey, gap);
                        queue.add(new Node(nx, ny, nz, gap));
                    }
                }
            }

            if (blocksSinceHunger > 0) {
                player.getFoodStats()
                    .addExhaustion(1f);
            }

            for (Object2IntMap.Entry<ItemStackWrapper> entry : merged.object2IntEntrySet()) {
                ItemStack dropStack = entry.getKey()
                    .stack()
                    .copy();
                dropStack.stackSize = entry.getIntValue();

                EntityItem entityItem = new EntityItem(world, player.posX, player.posY + 1, player.posZ, dropStack);
                entityItem.delayBeforeCanPickup = 0;
                entityItem.motionX = 0;
                entityItem.motionY = 0;
                entityItem.motionZ = 0;
                world.spawnEntityInWorld(entityItem);
            }
        } finally {
            activePlayers.remove(player.getUniqueID());
        }
    }

    public List<ItemStack> removeBlockAndGetDrops(EntityPlayerMP player, ItemStack stack, World world, int x, int y,
        int z, Block block, boolean silk, int fortune) {
        List<ItemStack> drops = new ArrayList<>();
        if (!world.blockExists(x, y, z)) return drops;

        Block blk = world.getBlock(x, y, z);
        int meta = world.getBlockMetadata(x, y, z);

        if (blk == null || blk.isAir(world, x, y, z)) return drops;
        if (block != null && blk != block) return drops;

        float hardness = blk.getBlockHardness(world, x, y, z);
        if (hardness < 0) return drops;

        if (player.theItemInWorldManager.tryHarvestBlock(x, y, z)) {
            if (silk) {
                Item item = Item.getItemFromBlock(blk);
                if (item != null) {
                    drops.add(new ItemStack(item, 1, blk.damageDropped(meta)));
                }
            } else {
                drops.addAll(blk.getDrops(world, x, y, z, meta, fortune));
            }
        }
        return drops;
    }

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.harvester == null || event.harvester.getCurrentEquippedItem() == null) return;
        ItemStack heldItem = event.harvester.getCurrentEquippedItem();
        if (!(heldItem.getItem() instanceof VeinMiningPickaxe veinMiningPickaxe)) return;
        EntityPlayer harvester = event.harvester;
        if (harvester instanceof EntityPlayerMP && veinMiningPickaxe.activePlayers.contains(harvester.getUniqueID())) {
            event.drops.clear();
        }
    }

    private static long positionKey(int x, int y, int z) {
        return (((long) x) & 0x3FFFFFF) << 38 | (((long) y) & 0xFFF) << 26 | (((long) z) & 0x3FFFFFF);
    }

    private static String[] oreNames(ItemStack stack) {
        int[] ids = OreDictionary.getOreIDs(stack);
        String[] names = new String[ids.length];
        for (int i = 0; i < ids.length; i++) names[i] = OreDictionary.getOreName(ids[i]);
        return names;
    }

    private static boolean matchesOre(ItemStack stack, String[] targetOreNames, boolean preciseMode) {
        for (int oreId : OreDictionary.getOreIDs(stack)) {
            String name = OreDictionary.getOreName(oreId);
            for (String targetName : targetOreNames) {
                if (preciseMode ? targetName.equals(name)
                    : (name.startsWith("ore") && targetName.startsWith("ore")) || targetName.startsWith(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Desugar
    private record Node(int x, int y, int z, int gap) {}

    @SideOnly(Side.CLIENT)
    @Override
    public void showSubtitle(String messageKey) {
        IChatComponent component = new ChatComponentTranslation(messageKey);
        component.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.WHITE));
        Minecraft.getMinecraft().ingameGUI.func_110326_a(component.getFormattedText(), true);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void showSubtitle(String messageKey, int range) {
        IChatComponent component = new ChatComponentTranslation(messageKey, range);
        component.setChatStyle(new ChatStyle().setColor(EnumChatFormatting.WHITE));
        Minecraft.getMinecraft().ingameGUI.func_110326_a(component.getFormattedText(), true);
    }

    @Desugar
    private record ItemStackWrapper(ItemStack stack) {

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof ItemStackWrapper other)) return false;
            return ItemStack.areItemStacksEqual(this.stack, other.stack);
        }

        @Override
        public int hashCode() {
            return stack == null ? 0
                : stack.getItem()
                    .hashCode() * 31 + stack.getItemDamage();
        }
    }
}
