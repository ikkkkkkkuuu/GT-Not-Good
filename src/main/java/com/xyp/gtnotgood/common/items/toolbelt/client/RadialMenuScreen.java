package com.xyp.gtnotgood.common.items.toolbelt.client;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.StatCollector;

import org.lwjgl.BufferUtils;
import org.lwjgl.input.Cursor;
import org.lwjgl.input.Mouse;

import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.common.items.toolbelt.ConfigData;
import com.xyp.gtnotgood.common.items.toolbelt.ToolBeltData;
import com.xyp.gtnotgood.common.items.toolbelt.client.radial.GenericRadialMenu;
import com.xyp.gtnotgood.common.items.toolbelt.client.radial.IRadialMenuHost;
import com.xyp.gtnotgood.common.items.toolbelt.client.radial.ItemStackRadialMenuItem;
import com.xyp.gtnotgood.common.items.toolbelt.client.radial.TextRadialMenuItem;
import com.xyp.gtnotgood.common.packet.SwapItems;
import com.xyp.gtnotgood.utils.keybind.KeyBindManager;

public class RadialMenuScreen extends GuiScreen {

    private final EntityPlayer player;
    private int inventorySize;

    private boolean keyCycleBeforeL = false;
    private boolean keyCycleBeforeR = false;

    private boolean needsRecheckStacks = true;
    private final List<ItemStackRadialMenuItem> cachedMenuItems = new ArrayList<>();
    private final TextRadialMenuItem insertMenuItem;
    private final GenericRadialMenu menu;

    public RadialMenuScreen(EntityPlayer player) {
        this.player = player;
        this.inventorySize = ToolBeltData.SLOT_COUNT;

        this.menu = new GenericRadialMenu(Minecraft.getMinecraft(), new IRadialMenuHost() {

            @Override
            public void renderTooltip(ItemStack stack, int mouseX, int mouseY) {
                RadialMenuScreen.this.renderToolTip(stack, mouseX, mouseY);
            }

            @Override
            public GuiScreen getScreen() {
                return RadialMenuScreen.this;
            }

            @Override
            public net.minecraft.client.gui.FontRenderer getFontRenderer() {
                return fontRendererObj;
            }
        }) {

            @Override
            public void onClickOutside() {
                if (ConfigData.allowClickOutsideBounds) {
                    close();
                }
            }
        };

        // #tr text.toolbelt.insert
        // # Insert
        // # zh_CN 放入
        this.insertMenuItem = new TextRadialMenuItem(menu, StatCollector.translateToLocal("text.toolbelt.insert")) {

            @Override
            public boolean onClick() {
                return RadialMenuScreen.this.trySwap(-1, null);
            }
        };
    }

    public void handleKeyInput() {
        if (KeyBindManager.cycleToolMenuLeft != null && KeyBindManager.cycleToolMenuLeft.isPressed()) {
            if (!keyCycleBeforeL) {
                // TODO: cycle
            }
            keyCycleBeforeL = true;
        } else {
            keyCycleBeforeL = false;
        }

        if (KeyBindManager.cycleToolMenuRight != null && KeyBindManager.cycleToolMenuRight.isPressed()) {
            if (!keyCycleBeforeR) {
                // TODO: cycle
            }
            keyCycleBeforeR = true;
        } else {
            keyCycleBeforeR = false;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();

        menu.tick();

        // When animation is fully closed, remove the GUI screen
        if (menu.isClosed()) {
            Minecraft.getMinecraft()
                .displayGuiScreen(null);
            return;
        }

        // Check if key is still held, close if released
        if (!KeyBindManager.isKeyDown(KeyBindManager.openToolMenuKeybind)) {
            if (ConfigData.releaseToSwap) {
                processClick(false);
            }
            close();
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            processClick(true);
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int button) {
        super.mouseMovedOrUp(mouseX, mouseY, button);
    }

    protected void processClick(boolean triggeredByMouse) {
        menu.clickItem();
    }

    @Override
    public void initGui() {
        super.initGui();
        // Centre cursor once on open so initial mouse position is the menu centre.
        Mouse.setCursorPosition(mc.displayWidth / 2, mc.displayHeight / 2);
        // Hide the OS cursor — selection is shown via wedge highlight, not a pointer.
        try {
            IntBuffer buf = BufferUtils.createIntBuffer(1);
            buf.put(0)
                .rewind();
            Mouse.setNativeCursor(new Cursor(1, 1, 0, 0, 1, buf, null));
        } catch (Exception ignored) {}
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // Pass MC's mouse coords straight to the menu. GenericRadialMenu uses the
        // angle from the menu centre for selection (no upper-radius limit), so the
        // cursor never needs to be within the ring — any direction past the deadzone
        // selects the corresponding wedge.
        super.drawScreen(mouseX, mouseY, partialTicks);

        ItemStack inHand = mc.thePlayer.getHeldItem();
        if (inHand != null && !ConfigData.isItemStackAllowed(inHand)) return;

        if (needsRecheckStacks) {
            cachedMenuItems.clear();

            ToolBeltData data = ToolBeltData.get(player);
            if (data != null) {
                for (int i = 0; i < inventorySize; i++) {
                    ItemStack inSlot = data.getStackInSlot(i);
                    if (inSlot != null) {
                        final int slot = i;
                        ItemStackRadialMenuItem item = new ItemStackRadialMenuItem(menu, inSlot) {

                            @Override
                            public boolean onClick() {
                                return RadialMenuScreen.this.trySwap(slot, null);
                            }
                        };
                        item.setVisible(true);
                        cachedMenuItems.add(item);
                    }
                }
            }

            menu.clear();
            if (inHand != null && hasSpaceForItem(inHand)) {
                menu.add(insertMenuItem);
            }
            for (ItemStackRadialMenuItem item : cachedMenuItems) {
                menu.add(item);
            }

            needsRecheckStacks = false;
        }

        menu.draw(mouseX, mouseY, partialTicks);
    }

    /**
     * Check if the belt has space to insert the given item stack.
     */
    private boolean hasSpaceForItem(ItemStack stack) {
        if (stack == null) return false;
        ToolBeltData data = ToolBeltData.get(player);
        if (data == null) return false;
        for (int i = 0; i < inventorySize; i++) {
            ItemStack inSlot = data.getStackInSlot(i);
            if (inSlot == null) {
                return true;
            }
            if (inSlot.isItemEqual(stack) && ItemStack.areItemStackTagsEqual(inSlot, stack)) {
                int max = inSlot.getMaxStackSize();
                if (inSlot.stackSize < max) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        KeyBindManager.consumeKey(KeyBindManager.openToolMenuKeybind);
        // Restore the default OS cursor
        try {
            Mouse.setNativeCursor(null);
        } catch (Exception ignored) {}
    }

    public boolean trySwap(int slotNumber, ItemStack stackSwapped) {
        GTNotGood.channel.sendToServer(new SwapItems(slotNumber));
        menu.close();
        return true;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    void close() {
        Minecraft.getMinecraft()
            .displayGuiScreen(null);
    }
}
