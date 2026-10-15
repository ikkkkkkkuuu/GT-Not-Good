package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiRadialWheel;
import com.xyp.gtnotgood.client.radialmenu.client.gui.WheelRing;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiSlotEditor;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;
import com.xyp.gtnotgood.common.network.SwapItems;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

@Mod(
    modid = "radialmenuqa",
    name = "Radial Menu QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class RadialMenuClientChecks {

    private static final String PROBE_BINDING = "key.radialmenuqa.probe";
    private static final String SAVED_TITLE = "已保存：单次按键";

    private final KeyBinding probe = new KeyBinding(PROBE_BINDING, 0, "Radial Menu QA");
    private boolean started;
    private volatile boolean finished;
    private volatile boolean serverReady;
    private volatile boolean packetRequested;
    private volatile boolean packetVerified;
    private volatile boolean insertRequested;
    private volatile boolean insertVerified;
    private volatile int stage;
    private int serverTicks;
    private int startupTicks;
    private int frames;
    private int keyWaitTicks;
    private int probePresses;
    private GuiRadialWheel wheel;
    private GuiSlotEditor editor;
    private MenuNode testRoot;
    private MenuNode beltMenu;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (!Boolean.getBoolean("gtng.radialMenu.qa")) return;
        ClientRegistry.registerKeyBinding(probe);
        FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void key(InputEvent.KeyInputEvent event) {
        while (probe.isPressed()) probePresses++;
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            mc.gameSettings.pauseOnLostFocus = false;
            mc.gameSettings.guiScale = 2;
            if (++startupTicks > 3600) throw new AssertionError("Client timed out at stage " + stage);
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                mc.launchIntegratedServer("radial-menu-qa-" + System.currentTimeMillis(), "Radial Menu QA",
                    new WorldSettings(73L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
            }
            if (stage > 0 && mc.theWorld == null) throw new AssertionError("Disconnected at stage " + stage);
            if (serverReady && stage == 0 && clientInventoryReady()) {
                require(KeyBindManager.openToolMenuKeybind.getKeyCode() == Keyboard.KEY_R,
                    "R remains the default wheel key");
                require(ProfileStorage.rootDir().getCanonicalPath().startsWith(mc.mcDataDir.getCanonicalPath()),
                    "profile storage stays inside the disposable client directory");
                ProfileStorage.init(new File(mc.mcDataDir, "qa-profile-" + System.currentTimeMillis()));
                ProfileManager.load();
                RadialMenuConfig.scrollToSelect = false;
                RadialMenuConfig.inlineHoverDelayMs = 500;
                wheel = new GuiRadialWheel();
                mc.displayGuiScreen(wheel);
                require(findBelt(currentMenu()) >= 0, "default wheel contains the built-in tool belt");
                stage = 1;
                frames = 0;
            } else if (
                stage == 3 && packetVerified && ItemStack.areItemStacksEqual(fixture(0), mc.thePlayer.getHeldItem())
            ) {
                wheel = new GuiRadialWheel();
                mc.displayGuiScreen(wheel);
                int beltSlot = findBelt(currentMenu());
                beltMenu = currentMenu().childAt(beltSlot);
                select(beltSlot);
                insertRequested = true;
                select(beltMenu, 0);
                stage = 7;
            } else if (stage == 7 && insertVerified) {
                prepareNavigation();
                stage = 4;
                frames = 0;
            } else if (stage == 6 && ++keyWaitTicks >= 8) {
                require(probePresses == 1, "a wheel TAP fires exactly one press through the input-event path");
                require(!probe.getIsKeyPressed() && probe.getKeyCode() == 0,
                    "TAP releases the key and restores an unbound binding");
                require(!probe.isPressed(), "no queued duplicate press remains");
                finish("PASS");
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        try {
            if (++serverTicks > 1800) throw new AssertionError("Radial menu timed out at stage " + stage);
            if (!serverReady) {
                checkServer(player);
                player.inventory.currentItem = 0;
                player.inventory.setInventorySlotContents(0, null);
                player.inventory.markDirty();
                player.inventoryContainer.detectAndSendChanges();
                ToolBeltData.get(player).syncToSelf();
                serverReady = true;
            }
            if (packetRequested && !packetVerified && player.getHeldItem() != null) {
                require(ItemStack.areItemStacksEqual(fixture(0), player.getHeldItem()),
                    "a real wheel action sends the expected tool-belt swap to the server");
                require(ToolBeltData.get(player).getStackInSlot(0) == null,
                    "the server removes the selected belt slot exactly once");
                packetVerified = true;
            }
            if (insertRequested && !insertVerified && player.getHeldItem() == null) {
                require(ItemStack.areItemStacksEqual(fixture(0), ToolBeltData.get(player).getStackInSlot(0)),
                    "selecting an empty built-in slot puts the held tool back through the real packet path");
                insertVerified = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void checkServer(EntityPlayerMP player) {
        ToolBeltData data = ToolBeltData.get(player);
        require(data != null && data.getSlotCount() == 10, "the server owns a ten-slot player tool belt");
        player.inventory.currentItem = 0;
        for (int i = 0; i < 10; i++) data.setStackInSlot(i, null);
        for (int i = 0; i < 10; i++) {
            player.inventory.setInventorySlotContents(0, fixture(i));
            SwapItems.swapItem(i, player);
            require(player.getHeldItem() == null && ItemStack.areItemStacksEqual(fixture(i), data.getStackInSlot(i)),
                "server insert preserves slot " + i + " and its item NBT");
        }
        player.inventory.setInventorySlotContents(0, fixture(10));
        SwapItems.swapItem(-1, player);
        require(ItemStack.areItemStacksEqual(fixture(10), player.getHeldItem()), "a full belt keeps the held item");
        SwapItems.swapItem(9, player);
        require(
            ItemStack.areItemStacksEqual(fixture(9), player.getHeldItem())
                && ItemStack.areItemStacksEqual(fixture(10), data.getStackInSlot(9)),
            "server exchange preserves both stacks");
        SwapItems.swapItem(9, player);
        player.inventory.setInventorySlotContents(0, null);
        SwapItems.swapItem(0, player);
        require(ItemStack.areItemStacksEqual(fixture(0), player.getHeldItem()) && data.getStackInSlot(0) == null,
            "empty-hand retrieval clears its belt slot");
        SwapItems.swapItem(-1, player);
        player.inventory.setInventorySlotContents(0, new ItemStack(Items.apple));
        SwapItems.swapItem(0, player);
        require(
            player.getHeldItem().getItem() == Items.apple
                && ItemStack.areItemStacksEqual(fixture(0), data.getStackInSlot(0)),
            "disallowed held items cannot replace tools");
        player.inventory.setInventorySlotContents(0, null);
        SwapItems.swapItem(10, player);
        require(player.getHeldItem() == null, "an out-of-range slot leaves the inventory unchanged");
        NBTTagCompound saved = new NBTTagCompound();
        data.saveNBTData(saved);
        ToolBeltData loaded = new ToolBeltData(player);
        loaded.loadNBTData(saved);
        for (int i = 0; i < 10; i++) {
            require(ItemStack.areItemStacksEqual(fixture(i), loaded.getStackInSlot(i)),
                "NBT reload preserves tool " + i + ", damage, enchantments and custom tags");
        }
    }

    private static ItemStack fixture(int index) {
        ItemStack stack = new ItemStack(Items.diamond_pickaxe, 1, index * 7);
        stack.setStackDisplayName("验证工具 " + index);
        stack.addEnchantment(Enchantment.efficiency, 1 + index % 5);
        NBTTagCompound marker = new NBTTagCompound();
        marker.setInteger("index", index);
        marker.setString("payload", "独立物品 NBT " + index);
        stack.getTagCompound().setTag("radialMenuQa", marker);
        return stack;
    }

    private static boolean clientInventoryReady() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() != null) return false;
        ToolBeltData data = ToolBeltData.get(mc.thePlayer);
        if (data == null) return false;
        for (int i = 0; i < 10; i++) {
            if (!ItemStack.areItemStacksEqual(fixture(i), data.getStackInSlot(i))) return false;
        }
        return true;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0 || stage == 3 || stage == 6 || stage == 7)
            return;
        if (++frames < 40) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 1) {
                screenshot("main-wheel.png");
                int beltSlot = findBelt(currentMenu());
                beltMenu = currentMenu().childAt(beltSlot);
                field(wheel, "pointerRing").setInt(wheel, 0);
                field(wheel, "pointerSlot").setInt(wheel, beltSlot);
                call(wheel, "openEditor");
                require(mc.currentScreen == wheel, "the built-in tool-belt entry cannot be edited");
                select(beltSlot);
                stage = 2;
            } else if (stage == 2) {
                require(beltMenu.childrenOrEmpty().size() == 10, "built-in submenu exposes the ten real slots");
                screenshot("tool-belt.png");
                int first = verifyBeltIcons(beltMenu);
                ProfileManager.saveActive();
                String saved = new String(
                    Files.readAllBytes(ProfileStorage.profileFile(ProfileManager.activeName()).toPath()),
                    StandardCharsets.UTF_8);
                require(!saved.contains("radialMenuQa") && !saved.contains("验证工具"),
                    "live server inventory and NBT are excluded from the saved menu profile");
                packetRequested = true;
                select(beltMenu, first);
                stage = 3;
            } else if (stage == 4) {
                require(((List<?>) field(wheel, "rings").get(wheel)).size() > 1,
                    "inline submenu renders an additional outer ring");
                screenshot("outer-ring.png");
                editor = new GuiSlotEditor(testRoot, 4);
                mc.displayGuiScreen(editor);
                ((GuiTextField) field(editor, "titleField").get(editor)).setText(SAVED_TITLE);
                stage = 5;
            } else if (stage == 5) {
                screenshot("slot-editor.png");
                call(editor, "save");
                require(SAVED_TITLE.equals(testRoot.childAtIndex(4).title), "the actual editor saves its title");
                ProfileManager.load();
                testRoot = ProfileManager.active().root;
                require(SAVED_TITLE.equals(testRoot.childAtIndex(4).title), "editor changes survive profile reload");
                require(testRoot.childAtIndex(0).childAtIndex(0).isCategory(),
                    "nested menus survive editor save and reload");
                require(testRoot.childAtIndex(2).layoutOrDefault().isInline(),
                    "inline opening mode survives editor save and reload");
                wheel = new GuiRadialWheel();
                mc.displayGuiScreen(wheel);
                select(testRoot.slotForChildIndex(4));
                stage = 6;
            }
            frames = 0;
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private void prepareNavigation() throws Exception {
        MenuNode root = MenuNode.category("多级菜单验证", null, SlotLayout.fixed(8));
        MenuNode middle = MenuNode.category("常用操作", IconSpec.item("minecraft:chest", 0), SlotLayout.dynamic());
        MenuNode deep = MenuNode.category("更多操作", IconSpec.item("minecraft:book", 0), SlotLayout.dynamic());
        deep.setChildAt(0, probeNode("单次按键"));
        middle.setChildAt(0, deep);
        root.setChildAt(0, middle);
        SlotLayout inlineLayout = SlotLayout.dynamic();
        for (SlotLayout.Opening opening : SlotLayout.Opening.values()) {
            if (opening.name().equalsIgnoreCase("inline")) inlineLayout.opening = opening;
        }
        MenuNode inline = MenuNode.category("外圈操作", IconSpec.item("minecraft:compass", 0), inlineLayout);
        inline.setChildAt(0, probeNode("按键"));
        inline.setChildAt(1, MenuNode.leaf("箱子", IconSpec.item("minecraft:chest", 0), new ActionSpec("keybind")));
        inline.setChildAt(2, MenuNode.leaf("书本", IconSpec.item("minecraft:book", 0), new ActionSpec("keybind")));
        root.setChildAt(2, inline);
        root.setChildAt(4, probeNode("编辑并保存"));
        root.ensureSlotCapacity();
        ProfileManager.active().root = root;
        ProfileManager.saveActive();
        testRoot = root;
        wheel = new GuiRadialWheel();
        Minecraft.getMinecraft().displayGuiScreen(wheel);
        select(0);
        require(currentMenu() == middle && wheel.isSticky(), "selecting a submenu enters a persistent wheel");
        select(0);
        require(currentMenu() == deep, "second-level submenu navigation reaches the actual child node");
        back();
        require(currentMenu() == middle, "right-click navigation returns one level");
        back();
        require(currentMenu() == root, "right-click navigation returns to the root");
        select(2);
        require(currentMenu() == root, "inline expansion retains the parent wheel");
    }

    private static MenuNode probeNode(String title) {
        return MenuNode.leaf(title, IconSpec.item("minecraft:diamond_pickaxe", 0),
            new ActionSpec("keybind").set("binding", PROBE_BINDING).set("mode", "tap"));
    }

    private int findBelt(MenuNode root) {
        for (int slot = 0; slot < root.slotCount(); slot++) {
            MenuNode child = root.childAt(slot);
            if (child != null && child.isCategory() && ToolBeltMenu.TYPE.equals(child.builtIn)) return slot;
        }
        return -1;
    }

    private static int verifyBeltIcons(MenuNode belt) throws Exception {
        int first = -1;
        for (int index = 0; index < 10; index++) {
            boolean found = false;
            for (int slot = 0; slot < belt.slotCount(); slot++) {
                MenuNode child = belt.childAt(slot);
                if (child == null || child.icon == null) continue;
                ItemStack icon = IconRenderer.resolveItem(child.icon);
                if (ItemStack.areItemStacksEqual(fixture(index), icon)) {
                    found = true;
                    if (index == 0) first = slot;
                    break;
                }
            }
            require(found, "rendered belt icon preserves complete NBT for tool " + index);
        }
        return first;
    }

    private MenuNode currentMenu() throws Exception {
        return (MenuNode) call(wheel, "currentMenu");
    }

    private void select(int slot) throws Exception {
        select(currentMenu(), slot);
    }

    private void select(MenuNode menu, int slot) throws Exception {
        require(slot >= 0, "the target menu slot exists");
        Method build = GuiRadialWheel.class.getDeclaredMethod("buildRings", boolean.class);
        build.setAccessible(true);
        List<?> rings = (List<?>) build.invoke(wheel, false);
        field(wheel, "rings").set(wheel, rings);
        int ringIndex = -1;
        for (int index = 0; index < rings.size(); index++) {
            if (((WheelRing) rings.get(index)).menu == menu) ringIndex = index;
        }
        require(ringIndex >= 0, "the selected menu has a rendered ring");
        field(wheel, "hoveredRing").setInt(wheel, ringIndex);
        field(wheel, "hoveredSlot").setInt(wheel, slot);
        call(wheel, "activateHovered");
    }

    private void back() throws Exception {
        field(wheel, "pointerRing").setInt(wheel, 0);
        field(wheel, "pointerSlot").setInt(wheel, -1);
        call(wheel, "goBackOrClose");
    }

    private static Object call(Object target, String name) throws Exception {
        Method method = target.getClass().getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(target);
    }

    private static Field field(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(outputDirectory(), name, mc.displayWidth, mc.displayHeight,
            mc.getFramebuffer());
    }

    private static File outputDirectory() {
        return new File(System.getProperty("gtng.radialMenu.qa.output", "."));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("RADIAL_MENU_QA: " + message);
    }

    private void fail(Throwable failure) {
        failure.printStackTrace();
        finish("FAIL: " + failure);
    }

    private void finish(String result) {
        try {
            Files.write(new File(outputDirectory(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception failure) {
            failure.printStackTrace();
        }
        System.out.println("RADIAL_MENU_QA: " + result);
        finished = true;
    }
}
