package com.xyp.gtnotgood.common.gui.modularui.widget;

import static com.xyp.gtnotgood.common.gui.modularui.widget.GhostMoldItemStackHandler.NO_MOLD;

import net.minecraft.item.ItemStack;

import com.cleanroommc.modularui.utils.MouseData;
import com.cleanroommc.modularui.value.sync.IntSyncValue;
import com.cleanroommc.modularui.value.sync.PanelSyncManager;
import com.cleanroommc.modularui.value.sync.PhantomItemSlotSH;
import com.cleanroommc.modularui.widgets.slot.ModularSlot;
import com.xyp.gtnotgood.common.machines.hatch.SuperMTEHatchCraftingInputME;
import com.xyp.gtnotgood.common.utils.MoldDataManager;

/**
 * 幽灵模具槽位的同步处理器
 * 处理客户端-服务器之间的模具选择同步
 */
public class GhostMoldSyncHandler extends PhantomItemSlotSH {

    private final SuperMTEHatchCraftingInputME hatch;
    private IntSyncValue indexSync;

    @SuppressWarnings("UnstableApiUsage")
    public GhostMoldSyncHandler(ModularSlot slot, SuperMTEHatchCraftingInputME hatch) {
        super(slot);
        this.hatch = hatch;
        indexSync = new IntSyncValue(() -> {
            ItemStack current = hatch.inventoryHandler.getStackInSlot(hatch.getMoldSlot());
            return current != null ? hatch.findMatchingMoldIndex(current) : NO_MOLD;
        }, index -> {
            if (index >= 0 && index < MoldDataManager.getMoldCount()) {
                hatch.setMold(MoldDataManager.getMolds()[index]);
            } else {
                hatch.setMold(null);
            }
        }).allowC2S();
    }

    /**
     * 注册索引同步值到同步管理器
     * 必须在 PanelSyncManager.initialize 运行之前调用
     */
    public void registerIndexSync(PanelSyncManager syncManager, String key) {
        if (indexSync != null) {
            syncManager.syncValue(key, 0, indexSync);
        }
    }

    @Override
    protected void phantomClick(MouseData mouseData, ItemStack cursorStack) {
        if (indexSync == null) return;

        int itemIndex = hatch.findMatchingMoldIndex(cursorStack);
        if (cursorStack != null && itemIndex != -1) {
            setSelectedIndex(itemIndex);
        } else {
            if (mouseData.mouseButton == 0) {
                // 左键点击：递增
                setSelectedIndex(getNextMoldConfig(1));
            } else if (mouseData.mouseButton == 1 && mouseData.shift) {
                // Shift+右键：清除
                setSelectedIndex(NO_MOLD);
            } else if (mouseData.mouseButton == 1) {
                // 右键点击：递减
                setSelectedIndex(getNextMoldConfig(-1));
            }
        }
    }

    @Override
    protected void phantomScroll(MouseData mouseData) {
        setSelectedIndex(getNextMoldConfig(mouseData.mouseButton));
    }

    private int getNextMoldConfig(int delta) {
        int newIndex = getSelectedIndex() + delta;
        if (newIndex < NO_MOLD) newIndex = MoldDataManager.getMoldCount() - 1;
        if (newIndex >= MoldDataManager.getMoldCount()) newIndex = NO_MOLD;
        return newIndex;
    }

    public int getSelectedIndex() {
        return indexSync != null ? indexSync.getIntValue() : NO_MOLD;
    }

    public void setSelectedIndex(int index) {
        if (indexSync != null) indexSync.setIntValue(index);
    }

    public IntSyncValue getIndexSync() {
        return indexSync;
    }
}
