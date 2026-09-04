package com.xyp.gtnotgood.ae2thing.util;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.concurrent.Future;

import javax.annotation.Nonnull;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import org.lwjgl.input.Mouse;

import com.glodblock.github.client.gui.FCGuiTextField;
import com.xyp.gtnotgood.ae2thing.api.Constants;
import com.xyp.gtnotgood.ae2thing.common.item.ItemWirelessDualInterfaceTerminal;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingGrid;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IDisplayRepo;
import appeng.api.util.DimensionalCoord;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.client.me.ItemRepo;
import appeng.container.implementations.ContainerCraftConfirm;
import appeng.core.AELog;
import appeng.crafting.v2.CraftingJobV2;
import appeng.integration.modules.NEI;
import appeng.items.tools.powered.ToolWirelessTerminal;
import appeng.me.cache.CraftingGridCache;
import appeng.util.Platform;
import codechicken.nei.recipe.StackInfo;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.common.registry.GameRegistry;

public class Util {

    private static int AE_VERSION = -1;

    /**
     * Caches the resolved display-repo {@link Field} per concrete GUI class. {@code getDisplayRepo} is called per
     * fake/pattern slot every render frame; without this it re-walks getDeclaredFields()+setAccessible up the whole
     * superclass chain each time. A sentinel {@link #NO_REPO_FIELD} marks classes that have no repo field so the
     * miss is cached too.
     */
    private static final java.util.Map<Class<?>, Field> DISPLAY_REPO_FIELD_CACHE = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Field NO_REPO_FIELD;
    static {
        Field sentinel = null;
        try {
            sentinel = Util.class.getDeclaredField("AE_VERSION");
        } catch (NoSuchFieldException ignored) {}
        NO_REPO_FIELD = sentinel;
    }

    public static int getAEVersion() {
        if (AE_VERSION == -1) {
            Optional<ModContainer> mod = Loader.instance()
                .getActiveModList()
                .stream()
                .filter(
                    x -> x.getModId()
                        .equals("appliedenergistics2"))
                .findFirst();
            if (mod.isPresent()) {
                try {
                    AE_VERSION = Integer.parseInt(
                        mod.get()
                            .getVersion()
                            .split("-")[2]);
                } catch (Exception ignored) {
                    AE_VERSION = 0;
                }
            } else {
                AE_VERSION = 0;
            }
        }
        return AE_VERSION;
    }

    public static boolean replan(EntityPlayer player, appeng.container.implementations.ContainerCraftConfirm c){
        ICraftingJob job = Ae2Reflect.getJob(c);
        if(job instanceof CraftingJobV2 jobV2 && jobV2.isDone()){
            c.simulation = true;
            c.bytesUsed = 0;
        }else{
            return false;
        }
        Object target;
        target = c.getTarget();
        if (target instanceof final IGridHost gh) {
            final IGridNode gn = gh.getGridNode(ForgeDirection.UNKNOWN);

            if (gn == null) {
                return false;
            }

            final IGrid g = gn.getGrid();
            if (g == null || c.getItemToCraft() == null) {
                return false;
            }

            Future<ICraftingJob> futureJob = null;
            try {
                final ICraftingGrid cg = g.getCache(ICraftingGrid.class);
                // Convert the target so fluids resolve to their native IAEFluidStack. getItemToCraft() returns an
                // ItemFluidDrop IAEItemStack for fluid crafts, and the IAEStack beginCraftingJob overload does NOT
                // convertStack (only the IAEItemStack one does), so without this a replan re-runs with the fluid_drop
                // and the plan collapses back to a single un-expanded drop. Mirrors the CPacketCraftRequest fix.
                appeng.api.storage.data.IAEStack<?> craftTarget = Platform.convertStack((IAEItemStack) c.getItemToCraft());
                if (cg instanceof CraftingGridCache cgc) {
                    futureJob = cgc.beginCraftingJob(
                        c.getWorld(),
                        g,
                        c.getActionSource(),
                        craftTarget,
                        null);
                }

                if (player.openContainer instanceof final ContainerCraftConfirm ccc) {
                    ccc.setJob(futureJob);
                    ccc.detectAndSendChanges();
                }
                return true;
            } catch (final Throwable e) {
                if (futureJob != null) {
                    futureJob.cancel(true);
                }
                AELog.debug(e);
            }
        }
        return false;
    }

    /**
     * Writes a terminal ItemStack back into the slot it lives in. Handles both a normal main-inventory slot and a
     * Baubles slot encoded with {@link com.xyp.gtnotgood.ae2thing.api.Constants#BAUBLE_SLOT_OFFSET}. Guards against
     * out-of-range indices so a stale/encoded slot can never crash the server.
     */
    public static void writeBackTerminal(EntityPlayer player, int slot, ItemStack stack) {
        if (slot == -1) {
            player.inventory.setItemStack(stack);
            return;
        }
        if (slot >= com.xyp.gtnotgood.ae2thing.api.Constants.BAUBLE_SLOT_OFFSET) {
            net.minecraft.inventory.IInventory baublesInv = baubles.api.BaublesApi.getBaubles(player);
            int bSlot = slot - com.xyp.gtnotgood.ae2thing.api.Constants.BAUBLE_SLOT_OFFSET;
            if (baublesInv != null && bSlot >= 0 && bSlot < baublesInv.getSizeInventory()) {
                baublesInv.setInventorySlotContents(bSlot, stack);
                // Baubles only serializes its in-memory stackList into player.getEntityData()'s "Baubles.Inventory"
                // tag at world-save time (PlayerHandler.savePlayerBaubles -> InventoryBaubles.saveNBT). A worn
                // terminal's NBT is edited in place (e.g. encoding a blank pattern into the pattern slot), which never
                // touches that durable snapshot. Any mid-session readNBT (or stackList reconstruction from a sync)
                // then restores the STALE snapshot and drops the change ("opened a few times and it's gone"), and the
                // same stale snapshot is what persists on relog. Flush the authoritative inventory to entityData now
                // so the durable snapshot always carries the latest terminal NBT.
                flushBaublesToEntityData(player, baublesInv);
            }
            return;
        }
        if (slot >= 0 && slot < player.inventory.getSizeInventory()) {
            player.inventory.setInventorySlotContents(slot, stack);
        }
    }

    /**
     * Calls {@code InventoryBaubles.saveNBT(player.getEntityData())} via reflection so we don't hard-depend on Baubles
     * internals. This writes the live baubles stackList into the durable {@code "Baubles.Inventory"} snapshot that
     * Baubles saves/loads, keeping a worn terminal's in-place NBT edits from being lost. Fails silently if Baubles is
     * absent or its API changes.
     */
    private static void flushBaublesToEntityData(EntityPlayer player, net.minecraft.inventory.IInventory baublesInv) {
        try {
            java.lang.reflect.Method saveNBT = baublesInv.getClass()
                .getMethod("saveNBT", NBTTagCompound.class);
            saveNBT.invoke(baublesInv, player.getEntityData());
        } catch (Throwable ignored) {
            // Baubles not present or API changed; nothing we can do, non-fatal.
        }
    }

    /**
     * Resolves the terminal ItemStack living in {@code slot}, using the same main-inventory / Baubles encoding as
     * {@link #findDualInterfaceTerminal(EntityPlayer)} and {@link #writeBackTerminal(EntityPlayer, int, ItemStack)}.
     */
    public static ItemStack getTerminalInSlot(EntityPlayer player, int slot) {
        if (slot == -1) {
            return player.getCurrentEquippedItem();
        }
        if (slot >= com.xyp.gtnotgood.ae2thing.api.Constants.BAUBLE_SLOT_OFFSET) {
            net.minecraft.inventory.IInventory baublesInv = baubles.api.BaublesApi.getBaubles(player);
            int bSlot = slot - com.xyp.gtnotgood.ae2thing.api.Constants.BAUBLE_SLOT_OFFSET;
            if (baublesInv != null && bSlot >= 0 && bSlot < baublesInv.getSizeInventory()) {
                return baublesInv.getStackInSlot(bSlot);
            }
            return null;
        }
        if (slot >= 0 && slot < player.inventory.getSizeInventory()) {
            return player.inventory.getStackInSlot(slot);
        }
        return null;
    }

    /**
     * Persists the GuiType the player last switched to onto the terminal ItemStack's NBT so reopening the terminal
     * restores that view. See {@link com.xyp.gtnotgood.ae2thing.api.Constants#LAST_GUI_MODE}.
     */
    public static void setLastGuiMode(EntityPlayer player, int slot,
        com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType type) {
        ItemStack stack = getTerminalInSlot(player, slot);
        if (stack == null || type == null) {
            return;
        }
        if (!stack.hasTagCompound()) {
            stack.setTagCompound(new NBTTagCompound());
        }
        stack.getTagCompound()
            .setByte(com.xyp.gtnotgood.ae2thing.api.Constants.LAST_GUI_MODE, (byte) type.ordinal());
    }

    /**
     * Reads back the GuiType stored by {@link #setLastGuiMode}, falling back to {@code fallback} when the stack has no
     * stored mode or the stored ordinal is not one of the two allowed terminal views.
     */
    public static com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType getLastGuiMode(ItemStack stack,
        com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType fallback) {
        if (stack == null || !stack.hasTagCompound()
            || !stack.getTagCompound()
                .hasKey(com.xyp.gtnotgood.ae2thing.api.Constants.LAST_GUI_MODE)) {
            return fallback;
        }
        com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType type = com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType
            .getByOrdinal(
                stack.getTagCompound()
                    .getByte(com.xyp.gtnotgood.ae2thing.api.Constants.LAST_GUI_MODE));
        if (type == com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType.WIRELESS_DUAL_INTERFACE_TERMINAL
            || type == com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType.WIRELESS_CRAFTING_TERMINAL) {
            return type;
        }
        return fallback;
    }

    public static int findDualInterfaceTerminal(EntityPlayer player) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null || item.getItem() == null) continue;
            if (item.getItem() instanceof ItemWirelessDualInterfaceTerminal) return x;
        }
        // Also look in the player's Baubles slots; encode the bauble slot with an offset so the GUI factory can
        // resolve it from the baubles inventory instead of the main inventory.
        net.minecraft.inventory.IInventory baublesInv = baubles.api.BaublesApi.getBaubles(player);
        if (baublesInv != null) {
            for (int x = 0; x < baublesInv.getSizeInventory(); x++) {
                ItemStack item = baublesInv.getStackInSlot(x);
                if (item == null || item.getItem() == null) continue;
                if (item.getItem() instanceof ItemWirelessDualInterfaceTerminal) {
                    return com.xyp.gtnotgood.ae2thing.api.Constants.BAUBLE_SLOT_OFFSET + x;
                }
            }
        }
        return -1;
    }

    public static IGrid getWirelessGrid(EntityPlayer player) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null || item.getItem() == null) continue;
            IGridNode node = getWirelessGridNode(item);
            if (node == null) continue;
            return node.getGrid();
        }
        return null;
    }

    public static String getModId(IAEItemStack item) {
        // [液滴分类] 可迁原生：仅为取 modId 读取流体，属查询/显示，未参与合成计算
        if (FluidDropCompat.isFluidDrop(item.getItem())) {
            FluidStack fs = FluidDropCompat.getFluidStack(item.getItemStack());
            if (fs == null) return GameRegistry.findUniqueIdentifierFor(item.getItem()).modId;
            return getFluidModID(fs.getFluid());
        }
        return Platform.getModId(item);
    }

    public static String getFluidModID(Fluid fluid) {
        String name = FluidRegistry.getDefaultFluidName(fluid);
        try {
            return name.split(":")[0];
        } catch (Exception e) {
            return "";
        }
    }

    @Nonnull
    public static String getDisplayName(IAEItemStack item) {
        FluidStack fs = StackInfo.getFluid(item.getItemStack());
        if (fs != null) {
            return fs.getLocalizedName();
        }
        return Platform.getItemDisplayName(item);
    }

    public static IGridHost getWirelessGridHost(ItemStack is) {
        if (is.getItem() instanceof ToolWirelessTerminal) {
            String key = ((ToolWirelessTerminal) is.getItem()).getEncryptionKey(is);
            return (IGridHost) AEApi.instance()
                .registries()
                .locatable()
                .getLocatableBy(Long.parseLong(key));
        }
        return null;
    }

    public static IGridNode getWirelessGridNode(ItemStack is) {
        IGridHost host = getWirelessGridHost(is);
        if (host == null) return null;
        return host.getGridNode(ForgeDirection.UNKNOWN);
    }

    public static int findItemStack(EntityPlayer player, ItemStack itemStack) {
        for (int x = 0; x < player.inventory.mainInventory.length; x++) {
            ItemStack item = player.inventory.mainInventory[x];
            if (item == null) continue;
            if (Platform.isSameItemPrecise(item, itemStack)) {
                return x;
            }
        }
        return -1;
    }

    public static IDisplayRepo getDisplayRepo(AEBaseGui gui) {
        return getDisplayRepo(gui, gui.getClass());
    }

    public static void setSearchFieldText(AEBaseGui gui, String text) {
        String displayName = NEI.searchField.getEscapedSearchText(text);
        IDisplayRepo repo = getDisplayRepo(gui);
        if (repo != null) {
            setSearchFieldText(gui, gui.getClass(), displayName);
            repo.setSearchString(displayName);
            repo.updateView();
        }
    }

    @SuppressWarnings("unchecked")
    private static void setSearchFieldText(AEBaseGui gui, Class<? extends AEBaseGui> clazz, String text) {
        try {
            if (clazz == AEBaseGui.class) {
                return;
            }
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getType() == MEGuiTextField.class) {
                    f.setAccessible(true);
                    ((MEGuiTextField) f.get(gui)).setText(text);
                    return;
                } else if (f.getType() == FCGuiTextField.class) {
                    f.setAccessible(true);
                    ((FCGuiTextField) f.get(gui)).setText(text);
                    return;
                }
            }
            setSearchFieldText(gui, (Class<? extends AEBaseGui>) clazz.getSuperclass(), text);
        } catch (Exception ignored) {}
    }

    private static IDisplayRepo getDisplayRepo(AEBaseGui gui, Class<? extends AEBaseGui> clazz) {
        Field field = DISPLAY_REPO_FIELD_CACHE.computeIfAbsent(clazz, Util::resolveDisplayRepoField);
        if (field == NO_REPO_FIELD) return null;
        try {
            return (IDisplayRepo) field.get(gui);
        } catch (Exception ignored) {}
        return null;
    }

    /**
     * Walks the superclass chain once to find the repo field for {@code clazz}; returns {@link #NO_REPO_FIELD} if none.
     */
    private static Field resolveDisplayRepoField(Class<?> clazz) {
        for (Class<?> c = clazz; c != null && c != AEBaseGui.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (f.getType() == IDisplayRepo.class || f.getType() == ItemRepo.class) {
                    f.setAccessible(true);
                    return f;
                }
            }
        }
        return NO_REPO_FIELD;
    }

    public static class DimensionalCoordSide extends DimensionalCoord {

        private ForgeDirection side = ForgeDirection.UNKNOWN;
        private final String name;

        public DimensionalCoordSide(final int _x, final int _y, final int _z, final int _dim, ForgeDirection side,
            String name) {
            super(_x, _y, _z, _dim);
            this.side = side;
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public ForgeDirection getSide() {
            return this.side;
        }

        @Override
        public void writeToNBT(NBTTagCompound data) {
            data.setInteger(Constants.SIDE, this.side.ordinal());
            data.setString(Constants.NAME, this.name);
            super.writeToNBT(data);
        }

        public static DimensionalCoordSide readFromNBT(final NBTTagCompound data) {
            return new DimensionalCoordSide(
                data.getInteger("x"),
                data.getInteger("y"),
                data.getInteger("z"),
                data.getInteger("dim"),
                ForgeDirection.getOrientation(data.getInteger(Constants.SIDE)),
                data.getString(Constants.NAME));
        }

    }

    public static class MousePos {

        public final int x;
        public final int y;

        public MousePos() {
            Minecraft mc = Minecraft.getMinecraft();
            final ScaledResolution scaledresolution = new ScaledResolution(mc, mc.displayWidth, mc.displayHeight);
            int i = scaledresolution.getScaledWidth();
            int j = scaledresolution.getScaledHeight();
            x = Mouse.getX() * i / mc.displayWidth;
            y = j - Mouse.getY() * j / mc.displayHeight - 1;
        }
    }
}
