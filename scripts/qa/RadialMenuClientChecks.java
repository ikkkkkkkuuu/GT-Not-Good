package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.WorldServer;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;

import org.lwjgl.input.Keyboard;

import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.client.radialmenu.client.gui.GuiRadialWheel;
import com.xyp.gtnotgood.client.radialmenu.client.gui.WheelRing;
import com.xyp.gtnotgood.client.radialmenu.client.gui.editor.GuiSlotEditor;
import com.xyp.gtnotgood.client.radialmenu.client.icon.IconRenderer;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileManager;
import com.xyp.gtnotgood.client.radialmenu.client.profile.ProfileStorage;
import com.xyp.gtnotgood.client.radialmenu.client.toolbelt.ToolBeltMenu;
import com.xyp.gtnotgood.client.radialmenu.config.RadialMenuConfig;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionField;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionSpec;
import com.xyp.gtnotgood.client.radialmenu.core.action.ActionTypes;
import com.xyp.gtnotgood.client.radialmenu.core.model.IconSpec;
import com.xyp.gtnotgood.client.radialmenu.core.model.MenuNode;
import com.xyp.gtnotgood.client.radialmenu.core.model.SlotLayout;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;
import com.xyp.gtnotgood.common.network.SwapItems;
import com.xyp.gtnotgood.common.network.SyncToolBeltData;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

import cpw.mods.fml.client.config.GuiSlider;
import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.network.ByteBufUtils;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "radialmenuqa",
    name = "Radial Menu QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class RadialMenuClientChecks {

    private static final String PROBE_BINDING = "key.radialmenuqa.probe";
    private static final String SAVED_TITLE = "已保存：单次按键";
    private static final String BELT_TITLE = "已保存：随身物品";
    private static final int EXPANDED_SLOTS = 16;
    private static final int LAST_SLOT = EXPANDED_SLOTS - 1;

    private final KeyBinding probe = new KeyBinding(PROBE_BINDING, 0, "Radial Menu QA");
    private boolean started;
    private volatile boolean finished;
    private volatile boolean serverReady;
    private volatile boolean packetRequested;
    private volatile boolean packetVerified;
    private volatile boolean insertRequested;
    private volatile boolean insertVerified;
    private volatile boolean resizeRequested;
    private volatile boolean resizeVerified;
    private volatile boolean stackPreparationRequested;
    private volatile boolean stackPrepared;
    private volatile boolean stackStored;
    private volatile boolean stackRetrieved;
    private volatile boolean tailStored;
    private volatile boolean shrinkRejected;
    private volatile boolean tailCleared;
    private volatile boolean shrinkVerified;
    private volatile boolean respawnRequested;
    private volatile boolean respawnVerified;
    private volatile int stage;
    private int serverTicks;
    private int startupTicks;
    private int frames;
    private int keyWaitTicks;
    private int probePresses;
    private int shrinkWaitTicks;
    private int respawnWaitTicks;
    private EntityPlayer preRespawnPlayer;
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
            } else if (stage == 7 && insertVerified && mc.thePlayer.getHeldItem() == null) {
                stackPreparationRequested = true;
                stage = 9;
            } else if (stage == 8 && resizeVerified && clientSlotCount() == EXPANDED_SLOTS) {
                ProfileManager.load();
                openBelt();
                verifyBeltPresentation(beltMenu);
                require(beltMenu.slotCount() == EXPANDED_SLOTS,
                    "saving the editor slot count resizes the real server belt and synchronizes sixteen slots");
                stage = 2;
                frames = 0;
            } else if (
                stage == 9 && stackPrepared
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), mc.thePlayer.getHeldItem())
            ) {
                openBelt();
                select(beltMenu, LAST_SLOT);
                stage = 10;
            } else if (
                stage == 10 && stackStored
                    && clientStackMatches(LAST_SLOT, stackFixture(64, "first"))
                    && mc.thePlayer.getHeldItem() == null
            ) {
                openBelt();
                stage = 17;
                frames = 0;
            } else if (
                stage == 11 && stackRetrieved
                    && clientStackMatches(LAST_SLOT, null)
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), mc.thePlayer.getHeldItem())
            ) {
                openBelt();
                select(beltMenu, LAST_SLOT);
                stage = 12;
            } else if (
                stage == 12 && tailStored
                    && clientStackMatches(LAST_SLOT, stackFixture(64, "first"))
                    && mc.thePlayer.getHeldItem() == null
            ) {
                ToolBeltMenu.requestResize(beltMenu, ToolBeltData.DEFAULT_SLOT_COUNT);
                stage = 13;
            } else if (stage == 13 && shrinkRejected) {
                require(clientSlotCount() == EXPANDED_SLOTS && clientStackMatches(LAST_SLOT, stackFixture(64, "first")),
                    "a rejected shrink retains the client slot count and occupied tail");
                openBelt();
                select(beltMenu, LAST_SLOT);
                stage = 14;
            } else if (
                stage == 14 && tailCleared
                    && clientStackMatches(LAST_SLOT, null)
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), mc.thePlayer.getHeldItem())
            ) {
                ToolBeltMenu.requestResize(beltMenu, ToolBeltData.DEFAULT_SLOT_COUNT);
                stage = 15;
            } else if (stage == 15 && shrinkVerified && clientSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT) {
                beltMenu.layoutOrDefault().slots = EXPANDED_SLOTS;
                ProfileManager.saveActive();
                ProfileManager.load();
                openBelt();
                verifyBeltPresentation(beltMenu);
                require(beltMenu.slotCount() == ToolBeltData.DEFAULT_SLOT_COUNT,
                    "reloading a profile with sixteen saved slots cannot undo the server's accepted shrink");
                prepareNavigation();
                stage = 4;
                frames = 0;
            } else if (stage == 6 && ++keyWaitTicks >= 8) {
                require(probePresses == 1, "a wheel TAP fires exactly one press through the input-event path");
                require(!probe.getIsKeyPressed() && probe.getKeyCode() == 0,
                    "TAP releases the key and restores an unbound binding");
                require(!probe.isPressed(), "no queued duplicate press remains");
                preRespawnPlayer = mc.thePlayer;
                stage = 18;
                respawnRequested = true;
            } else if (
                stage == 18 && respawnVerified
                    && mc.thePlayer != null
                    && mc.thePlayer != preRespawnPlayer
                    && ++respawnWaitTicks >= 20
            ) {
                ToolBeltData data = ToolBeltData.get(mc.thePlayer);
                require(
                    data != null && data.getOwner() == mc.thePlayer
                        && data.getSlotCount() == EXPANDED_SLOTS
                        && ItemStack.areItemStacksEqual(stackFixture(64, "respawn"), data.getStackInSlot(LAST_SLOT)),
                    "after a real S07 respawn the new client player receives sixteen slots and complete tail-stack NBT");
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
            ToolBeltData data = ToolBeltData.get(player);
            if (resizeRequested && !resizeVerified && data.getSlotCount() == EXPANDED_SLOTS) {
                require(data.getStackInSlot(LAST_SLOT) == null,
                    "the editor resize packet expands the server belt without inventing items");
                resizeVerified = true;
            }
            if (stackPreparationRequested && !stackPrepared) {
                player.inventory.setInventorySlotContents(0, stackFixture(64, "first"));
                syncInventory(player);
                stackPrepared = true;
            }
            if (
                stage == 10 && !stackStored
                    && player.getHeldItem() == null
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), data.getStackInSlot(LAST_SLOT))
            ) {
                require(true, "the real wheel packet stores an entire stack in the sixteenth server slot");
                NBTTagCompound saved = new NBTTagCompound();
                data.saveNBTData(saved);
                ToolBeltData loaded = new ToolBeltData(player);
                loaded.loadNBTData(saved);
                require(
                    loaded.getSlotCount() == EXPANDED_SLOTS
                        && ItemStack.areItemStacksEqual(stackFixture(64, "first"), loaded.getStackInSlot(LAST_SLOT)),
                    "NBT reload preserves sixteen slots and the complete sixteenth stack");
                stackStored = true;
            }
            if (
                stage == 11 && !stackRetrieved
                    && data.getStackInSlot(LAST_SLOT) == null
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), player.getHeldItem())
            ) {
                require(true, "the real wheel packet retrieves the entire sixteenth stack without changing NBT");
                stackRetrieved = true;
            }
            if (
                stage == 12 && player.getHeldItem() == null
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), data.getStackInSlot(LAST_SLOT))
            ) {
                tailStored = true;
            }
            if (stage == 13 && !shrinkRejected && ++shrinkWaitTicks >= 8) {
                require(
                    data.getSlotCount() == EXPANDED_SLOTS
                        && ItemStack.areItemStacksEqual(stackFixture(64, "first"), data.getStackInSlot(LAST_SLOT)),
                    "the resize packet refuses to discard an occupied trailing slot");
                data.syncToSelf();
                shrinkRejected = true;
            }
            if (
                stage == 14 && data.getStackInSlot(LAST_SLOT) == null
                    && ItemStack.areItemStacksEqual(stackFixture(64, "first"), player.getHeldItem())
            ) {
                tailCleared = true;
            }
            if (stage == 15 && !shrinkVerified && data.getSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT) {
                require(ItemStack.areItemStacksEqual(stackFixture(64, "first"), player.getHeldItem()),
                    "shrinking after retrieval preserves the held stack");
                shrinkVerified = true;
            }
            if (respawnRequested && !respawnVerified) {
                require(data.resize(EXPANDED_SLOTS), "the connected player has sixteen slots before respawning");
                // A pre-respawn item sync must not mask the respawn event's synchronization.
                data.setStackInSlotSilent(LAST_SLOT, stackFixture(64, "respawn"));
                EntityPlayerMP replacement = server.getConfigurationManager().respawnPlayer(player, 0, true);
                replacement.playerNetServerHandler.playerEntity = replacement;
                ToolBeltData restored = ToolBeltData.get(replacement);
                require(
                    replacement != player && restored != null
                        && restored.getOwner() == replacement
                        && restored.getSlotCount() == EXPANDED_SLOTS
                        && ItemStack.areItemStacksEqual(stackFixture(64, "respawn"),
                            restored.getStackInSlot(LAST_SLOT)),
                    "real server respawning rebinds the owner and retains all sixteen slots and tail-stack NBT");
                respawnVerified = true;
            }
        } catch (Throwable failure) {
            fail(failure);
        }
    }

    private static void checkServer(EntityPlayerMP player) throws Exception {
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
        ItemStack apples = new ItemStack(Items.apple, 64);
        player.inventory.setInventorySlotContents(0, apples.copy());
        SwapItems.swapItem(0, player);
        require(
            ItemStack.areItemStacksEqual(fixture(0), player.getHeldItem())
                && ItemStack.areItemStacksEqual(apples, data.getStackInSlot(0)),
            "ordinary stackable items can exchange with tools");
        SwapItems.swapItem(0, player);
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
        NBTTagCompound legacy = (NBTTagCompound) saved.copy();
        legacy.getCompoundTag(ToolBeltData.PROP_NAME).removeTag("SlotCount");
        ToolBeltData migrated = new ToolBeltData(player);
        migrated.loadNBTData(legacy);
        require(migrated.getSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT,
            "a legacy save without SlotCount migrates to ten slots");
        for (int i = 0; i < ToolBeltData.DEFAULT_SLOT_COUNT; i++) {
            require(ItemStack.areItemStacksEqual(fixture(i), migrated.getStackInSlot(i)),
                "legacy migration preserves tool and NBT in slot " + i);
        }
        checkDynamicStorage(player, data);
        checkSyncPackets(player);
        checkPlayerClone(player);
    }

    private static void checkPlayerClone(EntityPlayerMP loggedInPlayer) {
        WorldServer world = (WorldServer) loggedInPlayer.worldObj;
        GameProfile profile = new GameProfile(UUID.randomUUID(), "RadialCloneQA");
        EntityPlayerMP original = new EntityPlayerMP(loggedInPlayer.mcServer, world, profile,
            new ItemInWorldManager(world));
        EntityPlayerMP replacement = new EntityPlayerMP(loggedInPlayer.mcServer, world, profile,
            new ItemInWorldManager(world));
        original.setPosition(loggedInPlayer.posX + 1024, loggedInPlayer.posY, loggedInPlayer.posZ);
        replacement.setPosition(loggedInPlayer.posX + 2048, loggedInPlayer.posY, loggedInPlayer.posZ);
        ToolBeltData originalData = ToolBeltData.get(original);
        require(originalData != null && originalData.getOwner() == original,
            "an independent player fixture owns its registered tool belt before cloning");
        ItemStack[] snapshot = new ItemStack[EXPANDED_SLOTS];
        snapshot[LAST_SLOT] = stackFixture(64, "clone");
        originalData.applySnapshot(snapshot);

        replacement.clonePlayer(original, true);

        ToolBeltData cloned = ToolBeltData.get(replacement);
        require(cloned == originalData && cloned.getOwner() == replacement,
            "Forge full player cloning reuses extended properties and rebinds the belt owner to the new player");
        require(
            cloned.getSlotCount() == EXPANDED_SLOTS
                && ItemStack.areItemStacksEqual(snapshot[LAST_SLOT], cloned.getStackInSlot(LAST_SLOT)),
            "Forge full player cloning preserves sixteen slots and the complete trailing stack NBT");
        ToolBeltData active = ToolBeltData.get(loggedInPlayer);
        require(
            active != cloned && active.getOwner() == loggedInPlayer
                && active.getSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT,
            "independent clone fixtures leave the logged-in QA player's belt and owner unchanged");
    }

    private static void checkSyncPackets(EntityPlayerMP player) throws Exception {
        ItemStack[] expanded = new ItemStack[EXPANDED_SLOTS];
        expanded[0] = fixture(0);
        expanded[LAST_SLOT] = stackFixture(64, "first");
        ToolBeltData source = new ToolBeltData(player);
        source.applySnapshot(expanded);
        ByteBuf buffer = Unpooled.buffer();
        try {
            new SyncToolBeltData(player, source).toBytes(buffer);
            require(buffer.readInt() == player.getEntityId() && buffer.readInt() == 2,
                "the sync packet retains the legacy entity ID and nonempty-item count prefix");
            require(
                buffer.readInt() == 0 && ItemStack.areItemStacksEqual(expanded[0], ByteBufUtils.readItemStack(buffer))
                    && buffer.readInt() == LAST_SLOT
                    && ItemStack.areItemStacksEqual(expanded[LAST_SLOT], ByteBufUtils.readItemStack(buffer)),
                "legacy slot-item entries precede capacity and preserve the sixteenth stack's quantity and NBT");
            require(buffer.readableBytes() == 4 && buffer.readInt() == EXPANDED_SLOTS && !buffer.isReadable(),
                "the only new sync field is the trailing capacity");
            buffer.readerIndex(0);
            SyncToolBeltData decoded = new SyncToolBeltData();
            decoded.fromBytes(buffer);
            require(snapshotMatches(decoded, player, expanded) && !buffer.isReadable(),
                "a sixteen-slot sync packet round-trips all items, empty slots and NBT");

            ItemStack[] legacy = new ItemStack[ToolBeltData.DEFAULT_SLOT_COUNT];
            legacy[9] = fixture(9);
            buffer.clear();
            buffer.writeInt(player.getEntityId());
            buffer.writeInt(1);
            buffer.writeInt(9);
            ByteBufUtils.writeItemStack(buffer, legacy[9]);
            decoded.fromBytes(buffer);
            require(snapshotMatches(decoded, player, legacy) && !buffer.isReadable(),
                "a legacy sync packet without trailing capacity restores ten slots and clears old tail items");

            buffer.clear();
            buffer.writeInt(player.getEntityId());
            buffer.writeInt(0);
            decoded.fromBytes(buffer);
            require(snapshotMatches(decoded, player, new ItemStack[ToolBeltData.DEFAULT_SLOT_COUNT])
                && !buffer.isReadable(), "an empty legacy sync packet defaults to ten empty slots");
        } finally {
            buffer.release();
        }
    }

    private static boolean snapshotMatches(SyncToolBeltData packet, EntityPlayerMP player, ItemStack[] expected)
        throws Exception {
        if (field(packet, "entityId").getInt(packet) != player.getEntityId()) return false;
        ItemStack[] actual = (ItemStack[]) field(packet, "items").get(packet);
        if (actual.length != expected.length) return false;
        for (int slot = 0; slot < expected.length; slot++) {
            if (!ItemStack.areItemStacksEqual(expected[slot], actual[slot])) return false;
        }
        return true;
    }

    private static void checkDynamicStorage(EntityPlayerMP player, ToolBeltData data) {
        NBTTagCompound legacy = new NBTTagCompound();
        data.saveNBTData(legacy);
        legacy.getCompoundTag(ToolBeltData.PROP_NAME).removeTag("SlotCount");
        require(
            !data.resize(ToolBeltData.MIN_SLOT_COUNT - 1) && !data.resize(ToolBeltData.MAX_SLOT_COUNT + 1)
                && data.getSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT,
            "server resize rejects counts outside two through twenty-four");
        int firstTail = ToolBeltData.DEFAULT_SLOT_COUNT;
        int secondTail = firstTail + 1;
        require(data.resize(secondTail + 1), "two temporary tail slots are available for automatic insertion");
        data.setStackInSlot(secondTail, stackFixture(61, "first"));
        player.inventory.setInventorySlotContents(0, stackFixture(10, "first"));
        SwapItems.swapItem(-1, player);
        require(
            player.getHeldItem() == null
                && ItemStack.areItemStacksEqual(stackFixture(64, "first"), data.getStackInSlot(secondTail))
                && ItemStack.areItemStacksEqual(stackFixture(7, "first"), data.getStackInSlot(firstTail)),
            "automatic insertion fills a matching later stack before using an earlier empty slot");
        data.setStackInSlot(firstTail, null);
        data.setStackInSlot(secondTail, null);
        require(data.resize(ToolBeltData.MAX_SLOT_COUNT) && data.resize(EXPANDED_SLOTS),
            "server storage expands to its maximum and shrinks empty trailing slots");
        data.setStackInSlot(LAST_SLOT, stackFixture(61, "first"));
        player.inventory.setInventorySlotContents(0, stackFixture(10, "first"));
        SwapItems.swapItem(LAST_SLOT, player);
        require(
            ItemStack.areItemStacksEqual(stackFixture(64, "first"), data.getStackInSlot(LAST_SLOT))
                && ItemStack.areItemStacksEqual(stackFixture(7, "first"), player.getHeldItem()),
            "selecting a matching stack merges to sixty-four and leaves seven items in hand");

        player.inventory.setInventorySlotContents(0, stackFixture(11, "second"));
        SwapItems.swapItem(LAST_SLOT, player);
        require(
            ItemStack.areItemStacksEqual(stackFixture(11, "second"), data.getStackInSlot(LAST_SLOT))
                && ItemStack.areItemStacksEqual(stackFixture(64, "first"), player.getHeldItem()),
            "equal item IDs with different NBT exchange without merging");

        ItemStack sticks = new ItemStack(Items.stick, 32);
        player.inventory.setInventorySlotContents(0, sticks.copy());
        SwapItems.swapItem(LAST_SLOT, player);
        require(
            ItemStack.areItemStacksEqual(sticks, data.getStackInSlot(LAST_SLOT))
                && ItemStack.areItemStacksEqual(stackFixture(11, "second"), player.getHeldItem()),
            "different item types exchange their complete stacks");
        NBTTagCompound understated = new NBTTagCompound();
        data.saveNBTData(understated);
        understated.getCompoundTag(ToolBeltData.PROP_NAME).setInteger("SlotCount", ToolBeltData.DEFAULT_SLOT_COUNT);
        ToolBeltData loaded = new ToolBeltData(player);
        loaded.loadNBTData(understated);
        require(
            loaded.getSlotCount() == EXPANDED_SLOTS
                && ItemStack.areItemStacksEqual(sticks, loaded.getStackInSlot(LAST_SLOT)),
            "NBT loading restores the occupied sixteenth slot despite an understated SlotCount");
        loaded.loadNBTData(legacy);
        require(loaded.getSlotCount() == ToolBeltData.DEFAULT_SLOT_COUNT && loaded.getStackInSlot(LAST_SLOT) == null,
            "loading a legacy ten-slot save into the same expanded data object clears its old tail");
        for (int slot = 0; slot < ToolBeltData.DEFAULT_SLOT_COUNT; slot++) {
            require(ItemStack.areItemStacksEqual(fixture(slot), loaded.getStackInSlot(slot)),
                "reusing the data object preserves legacy fixture " + slot);
        }
        require(
            !data.resize(ToolBeltData.DEFAULT_SLOT_COUNT) && data.getSlotCount() == EXPANDED_SLOTS
                && ItemStack.areItemStacksEqual(sticks, data.getStackInSlot(LAST_SLOT)),
            "server storage refuses a shrink that would discard the occupied sixteenth slot");
        data.setStackInSlot(LAST_SLOT, null);
        require(data.resize(ToolBeltData.DEFAULT_SLOT_COUNT), "clearing the trailing slot permits shrinking to ten");
        for (int slot = 0; slot < ToolBeltData.DEFAULT_SLOT_COUNT; slot++) {
            require(ItemStack.areItemStacksEqual(fixture(slot), data.getStackInSlot(slot)),
                "dynamic storage checks leave original fixture " + slot + " unchanged");
        }
        player.inventory.setInventorySlotContents(0, null);
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

    private static ItemStack stackFixture(int count, String variant) {
        ItemStack stack = new ItemStack(Items.apple, count);
        stack.setStackDisplayName("验证物品堆叠 " + variant);
        NBTTagCompound marker = new NBTTagCompound();
        marker.setString("payload", "整组物品 NBT " + variant);
        stack.getTagCompound().setTag("radialMenuQa", marker);
        return stack;
    }

    private static boolean clientInventoryReady() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.thePlayer == null || mc.thePlayer.getHeldItem() != null) return false;
        ToolBeltData data = ToolBeltData.get(mc.thePlayer);
        if (data == null || data.getSlotCount() != ToolBeltData.DEFAULT_SLOT_COUNT) return false;
        for (int i = 0; i < 10; i++) {
            if (!ItemStack.areItemStacksEqual(fixture(i), data.getStackInSlot(i))) return false;
        }
        return true;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (
            event.phase != TickEvent.Phase.END || finished
                || stage != 1 && stage != 2 && stage != 4 && stage != 5 && stage != 16 && stage != 17
        ) return;
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
                require(mc.currentScreen instanceof GuiSlotEditor,
                    "the built-in tool-belt entry opens the real slot editor");
                editor = (GuiSlotEditor) mc.currentScreen;
                require(field(editor, "titleField").get(editor) != null,
                    "the belt entry exposes editable presentation controls");
                editor.onIconPicked(IconSpec.item("minecraft:chest", 0));
                ((GuiTextField) field(editor, "titleField").get(editor)).setText(BELT_TITLE);
                ((ActionSpec) field(editor, "spec").get(editor)).set(ActionTypes.PARAM_OPENING, "replace");
                setSlotCount(editor, EXPANDED_SLOTS);
                call(editor, "captureInputs");
                stage = 16;
            } else if (stage == 16) {
                screenshot("belt-entry-editor.png");
                resizeRequested = true;
                call(editor, "save");
                stage = 8;
            } else if (stage == 17) {
                require(
                    ItemStack.areItemStacksEqual(stackFixture(64, "first"),
                        IconRenderer.resolveItem(beltMenu.childAt(LAST_SLOT).icon)),
                    "the sixteenth rendered icon retains the full stack and custom NBT from server synchronization");
                screenshot("tool-belt-sixteen.png");
                select(beltMenu, LAST_SLOT);
                stage = 11;
            } else if (stage == 2) {
                require(beltMenu.childrenOrEmpty().size() == EXPANDED_SLOTS,
                    "built-in submenu exposes all sixteen server slots");
                screenshot("tool-belt.png");
                int first = verifyBeltIcons(beltMenu);
                field(wheel, "pointerRing").setInt(wheel, 0);
                field(wheel, "pointerSlot").setInt(wheel, first);
                call(wheel, "openEditor");
                require(mc.currentScreen == wheel, "live inventory children cannot be rewritten as custom actions");
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

    private void openBelt() throws Exception {
        wheel = new GuiRadialWheel();
        Minecraft.getMinecraft().displayGuiScreen(wheel);
        int slot = findBelt(currentMenu());
        require(slot >= 0, "the configured belt entry remains available");
        beltMenu = currentMenu().childAt(slot);
        ToolBeltMenu.refresh(beltMenu);
        select(slot);
    }

    private static void verifyBeltPresentation(MenuNode belt) {
        require(
            BELT_TITLE.equals(belt.title) && belt.icon != null
                && "minecraft:chest".equals(belt.icon.id)
                && belt.layoutOrDefault().opening == SlotLayout.Opening.Replace,
            "the belt entry title, icon and opening mode survive editor save and profile reload");
    }

    private static void setSlotCount(GuiSlotEditor target, int count) throws Exception {
        List<?> fields = (List<?>) field(target, "editableFields").get(target);
        List<?> sliders = (List<?>) field(target, "fieldSliders").get(target);
        for (int i = 0; i < fields.size(); i++) {
            if (ActionTypes.PARAM_SLOT_COUNT.equals(((ActionField) fields.get(i)).key)) {
                GuiSlider slider = (GuiSlider) sliders.get(i);
                require(slider != null, "the real belt editor exposes a slot-count slider");
                slider.setValue(count);
                slider.updateSlider();
                return;
            }
        }
        throw new AssertionError("Missing slot-count editor control");
    }

    private static int clientSlotCount() {
        ToolBeltData data = ToolBeltData.get(Minecraft.getMinecraft().thePlayer);
        return data == null ? 0 : data.getSlotCount();
    }

    private static boolean clientStackMatches(int slot, ItemStack expected) {
        ToolBeltData data = ToolBeltData.get(Minecraft.getMinecraft().thePlayer);
        return data != null && ItemStack.areItemStacksEqual(expected, data.getStackInSlot(slot));
    }

    private static void syncInventory(EntityPlayerMP player) {
        player.inventory.markDirty();
        player.inventoryContainer.detectAndSendChanges();
        ToolBeltData.get(player).syncToSelf();
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
        for (Class<?> type = target.getClass(); type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {}
        }
        throw new NoSuchFieldException(name);
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
