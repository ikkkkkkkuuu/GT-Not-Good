package com.xyp.gtnotgood.ae2thing.client.gui;

import java.text.NumberFormat;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.xyp.gtnotgood.ae2thing.api.adapter.terminal.IGuiCraftAmount;
import com.xyp.gtnotgood.ae2thing.inventory.InventoryHandler;
import com.xyp.gtnotgood.ae2thing.inventory.gui.GuiType;
import com.xyp.gtnotgood.common.compat.FluidDropCompat;

import appeng.api.storage.StorageName;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.client.gui.AEBaseGui;
import appeng.client.gui.slots.VirtualMESlotSingle;
import appeng.client.gui.widgets.GuiTabButton;
import appeng.client.gui.widgets.MEGuiTextField;
import appeng.container.AEBaseContainer;
import appeng.container.interfaces.IVirtualSlotHolder;
import appeng.core.AEConfig;
import appeng.core.localization.GuiText;
import appeng.util.calculators.ArithHelper;
import appeng.util.calculators.Calculator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;

public abstract class GuiAmount extends AEBaseGui implements IGuiDrawSlot, IGuiCraftAmount, IVirtualSlotHolder {

    protected MEGuiTextField amountBox;
    protected GuiTabButton originalGuiBtn;
    protected GuiButton submit;
    protected GuiButton plus1;
    protected GuiButton plus10;
    protected GuiButton plus100;
    protected GuiButton plus1000;
    protected GuiButton minus1;
    protected GuiButton minus10;
    protected GuiButton minus100;
    protected GuiButton minus1000;
    protected GuiType originalGui;
    protected ItemStack myIcon;
    private VirtualMESlotSingle craftingSlot;

    public GuiAmount(Container container) {
        super(container);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        super.initGui();
        this.craftingSlot = new VirtualMESlotSingle(34, 53, 0, null);
        this.registerVirtualSlots(this.craftingSlot);
        final int a = AEConfig.instance.craftItemsByStackAmounts(0);
        final int b = AEConfig.instance.craftItemsByStackAmounts(1);
        final int c = AEConfig.instance.craftItemsByStackAmounts(2);
        final int d = AEConfig.instance.craftItemsByStackAmounts(3);

        this.buttonList.add(this.plus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 26, 22, 20, "+" + a));
        this.buttonList.add(this.plus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 26, 28, 20, "+" + b));
        this.buttonList.add(this.plus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 26, 32, 20, "+" + c));
        this.buttonList.add(this.plus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 26, 38, 20, "+" + d));

        this.buttonList.add(this.minus1 = new GuiButton(0, this.guiLeft + 20, this.guiTop + 75, 22, 20, "-" + a));
        this.buttonList.add(this.minus10 = new GuiButton(0, this.guiLeft + 48, this.guiTop + 75, 28, 20, "-" + b));
        this.buttonList.add(this.minus100 = new GuiButton(0, this.guiLeft + 82, this.guiTop + 75, 32, 20, "-" + c));
        this.buttonList.add(this.minus1000 = new GuiButton(0, this.guiLeft + 120, this.guiTop + 75, 38, 20, "-" + d));

        this.buttonList
            .add(this.submit = new GuiButton(0, this.guiLeft + 128, this.guiTop + 51, 38, 20, GuiText.Next.getLocal()));

        setOriginGUI(((AEBaseContainer) this.inventorySlots).getTarget());
        if (this.originalGui != null && this.myIcon != null) {
            this.buttonList.add(
                this.originalGuiBtn = new GuiTabButton(
                    this.guiLeft + 151,
                    this.guiTop - 4,
                    this.myIcon,
                    this.myIcon.getDisplayName(),
                    itemRender));
            this.originalGuiBtn.setHideEdge(13);
        }
        this.amountBox = new MEGuiTextField(61, 12) {

            @Override
            public void onTextChange(String oldText) {
                this.setMessage(
                    "= " + NumberFormat.getInstance()
                        .format(getAmount()));
            }

        };
        this.amountBox.x = this.guiLeft + 60;
        this.amountBox.y = this.guiTop + 55;
        this.amountBox.setMaxStringLength(16);
        this.amountBox.setFocused(true);
    }

    @Override
    public void drawBG(final int offsetX, final int offsetY, final int mouseX, final int mouseY) {
        this.bindTexture(getBackground());
        this.drawTexturedModalRect(offsetX, offsetY, 0, 0, this.xSize, this.ySize);
    }

    @Override
    protected void keyTyped(final char character, final int key) {
        if (!this.checkHotbarKeys(key)) {
            if (key == Keyboard.KEY_TAB) {
                this.amountBox.setFocused(true);
            } else if (key == Keyboard.KEY_RETURN || key == Keyboard.KEY_NUMPADENTER) {
                this.actionPerformed(this.submit);
            }
            this.amountBox.textboxKeyTyped(character, key);
            super.keyTyped(character, key);
        }
    }

    public enum Operator {

        PLUS("+"),
        MINUS("-"),
        MULTIPLY("*"),
        DIVIDE("/"),
        MOD("%"),
        POWER("^"),
        SCIENTIFIC("e");

        public final String sign;

        Operator(String sign) {
            this.sign = sign;
        }

        @Override
        public String toString() {
            return sign;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float btn) {
        super.drawScreen(mouseX, mouseY, btn);
        if (this.amountBox.isVisible() && this.amountBox.isMouseIn(mouseX, mouseY)) {
            for (var i : Operator.values()) {
                if (this.amountBox.getText()
                    .contains(i.sign)) {
                    this.drawTooltip(mouseX, mouseY, this.amountBox.getMessage());
                    break;
                }
            }
        }
    }

    @Override
    protected void mouseClicked(int xCoord, int yCoord, int btn) {
        super.mouseClicked(xCoord, yCoord, btn);
        this.amountBox.mouseClicked(xCoord, yCoord, btn);
    }

    @Override
    protected void actionPerformed(final GuiButton btn) {
        super.actionPerformed(btn);
        if (btn == this.originalGuiBtn) {
            switchToOriginalGui();
        }
        final boolean isPlus = btn == this.plus1 || btn == this.plus10 || btn == this.plus100 || btn == this.plus1000;
        final boolean isMinus = btn == this.minus1 || btn == this.minus10
            || btn == this.minus100
            || btn == this.minus1000;

        if (isPlus || isMinus) {
            this.addQty(this.getQty(btn));
            this.amountBox.setCursorPositionEnd();
        }
    }

    protected void addQty(final int i) {
        try {
            int resultI = getAmount();
            if (resultI == 1 && i > 1) {
                resultI = 0;
            }
            resultI += i;
            if (resultI < 1) {
                resultI = 1;
            }
            String out = Long.toString(resultI);
            this.amountBox.setText(out);
        } catch (final NumberFormatException ignore) {}
    }

    protected abstract void setOriginGUI(Object target);

    /**
     * Navigate back from the craft-amount screen to the terminal the craft was launched from. Default returns to the
     * resolved {@link #originalGui}; subclasses whose origin depends on runtime state (e.g. the wireless dual interface
     * terminal, which can be viewed as either its own GUI or the AE2 wireless crafting terminal) override this.
     */
    protected void switchToOriginalGui() {
        InventoryHandler.switchGui(originalGui);
    }

    protected abstract String getBackground();

    public int getAmount() {
        try {
            String out = this.amountBox.getText();
            double result = Calculator.conversion(out);
            if (result <= 0 || Double.isNaN(result)) {
                return 0;
            } else {
                return (int) ArithHelper.round(result, 0);
            }
        } catch (final NumberFormatException e) {
            return 0;
        }
    }

    @Override
    public void func_146977_a(final Slot s) {
        if (drawSlot(s, () -> super.func_146977_a(s))) super.func_146977_a(s);
    }

    @Override
    public float getzLevel() {
        return this.zLevel;
    }

    @Override
    public AEBaseGui getAEBaseGui() {
        return this;
    }

    @Override
    public void receiveSlotStacks(StorageName invName, Int2ObjectMap<IAEStack<?>> slotStacks) {
        if (this.craftingSlot != null) {
            IAEStack<?> display = slotStacks.get(0);
            // The server keeps the craft target as an ItemFluidDrop IAEItemStack (needed for the actual
            // crafting job). Rendering that item stack draws a blank item icon, so for DISPLAY ONLY convert
            // it back to its IAEFluidStack, which draws the proper fluid texture + amount.
            // [液滴分类] 可迁原生：仅为显示把液滴还原成流体渲染,不参与合成计算(合成目标仍由服务端保留液滴)
            if (display instanceof IAEItemStack ais && FluidDropCompat.isFluidDrop(ais.getItem())) {
                IAEFluidStack fluid = FluidDropCompat.getAeFluidStack(ais);
                if (fluid != null) {
                    display = fluid;
                }
            }
            this.craftingSlot.setAEStack(display);
        }
    }
}
