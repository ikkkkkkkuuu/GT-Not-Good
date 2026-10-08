package com.xyp.gtnotgood.common.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xyp.gtnotgood.common.items.veinmining.VeinMiningPickaxe;
import com.xyp.gtnotgood.common.items.veinmining.VeinMiningSettings.Setting;
import com.xyp.gtnotgood.config.Config;

import cpw.mods.fml.common.registry.RegistryDelegate;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import sun.misc.Unsafe;

/** Exercises decoded requests against real inventory/NBT objects without invoking item registration. */
public class UpdateVeinMiningSettingTest {

    private static VeinMiningPickaxe pickaxe;
    private InventoryPlayer inventory;
    private int previousRange;
    private int previousAmount;

    @BeforeClass
    public static void createUnregisteredPickaxe() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        Unsafe unsafe = (Unsafe) unsafeField.get(null);
        pickaxe = (VeinMiningPickaxe) unsafe.allocateInstance(VeinMiningPickaxe.class);
        Field delegate = Item.class.getDeclaredField("delegate");
        delegate.setAccessible(true);
        delegate.set(pickaxe, new RegistryDelegate.Delegate<>(pickaxe, Item.class));
    }

    @Before
    public void setUp() {
        previousRange = Config.VeinMinerPickaxe.maxRange;
        previousAmount = Config.VeinMinerPickaxe.maxAmount;
        Config.VeinMinerPickaxe.maxRange = 32;
        Config.VeinMinerPickaxe.maxAmount = 327670;
        inventory = new InventoryPlayer(null);
        inventory.currentItem = 2;
        inventory.mainInventory[2] = new ItemStack(pickaxe);
    }

    @After
    public void restoreLimits() {
        Config.VeinMinerPickaxe.maxRange = previousRange;
        Config.VeinMinerPickaxe.maxAmount = previousAmount;
    }

    @Test
    public void decodedRequestChangesOnlyOneSettingAndKeepsServerItemData() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInteger("range", 3);
        tag.setInteger("amount", 120000);
        tag.setBoolean("preciseMode", true);
        NBTTagCompound tool = new NBTTagCompound();
        tool.setLong("Damage", 7654321);
        tool.setLong("MaxDamage", 20000000);
        tag.setTag("GT.ToolStats", tool);
        NBTTagList enchantments = new NBTTagList();
        NBTTagCompound enchantment = new NBTTagCompound();
        enchantment.setShort("id", (short) 35);
        enchantment.setShort("lvl", (short) 3);
        enchantments.appendTag(enchantment);
        tag.setTag("ench", enchantments);
        tag.setString("customExtension", "preserve");
        inventory.getCurrentItem().setTagCompound(tag);
        NBTTagCompound expected = (NBTTagCompound) tag.copy();
        expected.setInteger("range", 9);

        assertTrue(roundTrip(new UpdateVeinMiningSetting(2, Setting.Range, 9)).apply(inventory));
        assertSame(tag, inventory.getCurrentItem().getTagCompound());
        assertEquals(expected, tag);
        assertTrue(inventory.inventoryChanged);
    }

    @Test
    public void rejectsOtherItemsEmptySlotsAndChangedHeldSlot() {
        ItemStack pickaxeStack = inventory.getCurrentItem();
        assertFalse(roundTrip(new UpdateVeinMiningSetting(3, Setting.Range, 9)).apply(inventory));
        assertFalse(pickaxeStack.hasTagCompound());
        inventory.currentItem = 3;
        assertFalse(roundTrip(new UpdateVeinMiningSetting(2, Setting.Range, 9)).apply(inventory));
        assertFalse(roundTrip(new UpdateVeinMiningSetting(3, Setting.Range, 9)).apply(inventory));
        ItemStack other = new ItemStack(new Item());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("owner", "original");
        other.setTagCompound(tag);
        inventory.mainInventory[3] = other;
        assertFalse(roundTrip(new UpdateVeinMiningSetting(3, Setting.Amount, 1)).apply(inventory));
        assertEquals(1, tag.func_150296_c().size());
        assertEquals("original", tag.getString("owner"));
        assertFalse(inventory.inventoryChanged);
    }

    @Test
    public void rejectsSlotsOutsideHotbarAndConsumedStacks() {
        for (int slot : new int[] { -1, 9, 35, 36, 255 }) {
            assertFalse(roundTrip(new UpdateVeinMiningSetting(slot, Setting.Range, 9)).apply(inventory));
        }
        inventory.getCurrentItem().stackSize = 0;
        assertFalse(roundTrip(new UpdateVeinMiningSetting(2, Setting.Range, 9)).apply(inventory));
        assertFalse(inventory.getCurrentItem().hasTagCompound());
    }

    @Test
    public void clampsBothSettingsToCurrentServerLimits() {
        Config.VeinMinerPickaxe.maxRange = 5;
        Config.VeinMinerPickaxe.maxAmount = 12000;
        assertTrue(roundTrip(new UpdateVeinMiningSetting(2, Setting.Range, Integer.MAX_VALUE)).apply(inventory));
        assertTrue(roundTrip(new UpdateVeinMiningSetting(2, Setting.Amount, Integer.MAX_VALUE)).apply(inventory));
        NBTTagCompound tag = inventory.getCurrentItem().getTagCompound();
        assertEquals(5, tag.getInteger("range"));
        assertEquals(12000, tag.getInteger("amount"));
        assertTrue(roundTrip(new UpdateVeinMiningSetting(2, Setting.Range, Integer.MIN_VALUE)).apply(inventory));
        assertTrue(roundTrip(new UpdateVeinMiningSetting(2, Setting.Amount, Integer.MIN_VALUE)).apply(inventory));
        assertEquals(0, tag.getInteger("range"));
        assertEquals(0, tag.getInteger("amount"));
    }

    @Test
    public void scrollRetainsDefaultsStepsAndSaturatesWithoutIntegerWrap() {
        ItemStack stack = inventory.getCurrentItem();
        assertEquals(4, Setting.Range.scroll(stack, 120));
        assertEquals(2, Setting.Range.scroll(stack, -120));
        assertEquals(317670, Setting.Amount.scroll(stack, -120));
        Config.VeinMinerPickaxe.maxAmount = Integer.MAX_VALUE;
        assertTrue(new UpdateVeinMiningSetting(2, Setting.Amount, Integer.MAX_VALUE - 5).apply(inventory));
        assertEquals(Integer.MAX_VALUE, Setting.Amount.scroll(stack, 120));
        assertTrue(new UpdateVeinMiningSetting(2, Setting.Amount, 5).apply(inventory));
        assertEquals(0, Setting.Amount.scroll(stack, -120));
        Config.VeinMinerPickaxe.maxRange = -1;
        assertTrue(new UpdateVeinMiningSetting(2, Setting.Range, 9).apply(inventory));
        assertEquals(0, stack.getTagCompound().getInteger("range"));
    }

    @Test
    public void rejectsUnknownSettingsAndVariableLengthPayloads() {
        ByteBuf buffer = Unpooled.buffer();
        try {
            buffer.writeByte(2).writeByte(255).writeInt(9);
            assertThrows(IllegalArgumentException.class, () -> new UpdateVeinMiningSetting().fromBytes(buffer));
            buffer.clear();
            buffer.writeByte(2).writeByte(0).writeInt(9).writeByte(0);
            assertThrows(IllegalArgumentException.class, () -> new UpdateVeinMiningSetting().fromBytes(buffer));
            buffer.clear();
            buffer.writeInt(2);
            assertThrows(IllegalArgumentException.class, () -> new UpdateVeinMiningSetting().fromBytes(buffer));
        } finally {
            buffer.release();
        }
    }

    private static UpdateVeinMiningSetting roundTrip(UpdateVeinMiningSetting request) {
        ByteBuf buffer = Unpooled.buffer();
        try {
            request.toBytes(buffer);
            assertEquals(6, buffer.readableBytes());
            UpdateVeinMiningSetting decoded = new UpdateVeinMiningSetting();
            decoded.fromBytes(buffer);
            assertFalse(buffer.isReadable());
            return decoded;
        } finally {
            buffer.release();
        }
    }
}
