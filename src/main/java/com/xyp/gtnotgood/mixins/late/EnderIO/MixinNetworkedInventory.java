
package com.xyp.gtnotgood.mixins.late.EnderIO;

import net.minecraftforge.common.util.ForgeDirection;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import crazypants.enderio.conduit.item.IItemConduit;
import crazypants.enderio.conduit.item.NetworkedInventory;

/**
 * 网络物品库存 Mixin 类
 * <p>
 * 用于修改 {@link NetworkedInventory} 的物品传输逻辑，大幅提升物品管道的传输速度
 */
@Mixin(value = NetworkedInventory.class, remap = false)
public abstract class MixinNetworkedInventory {

    @Shadow
    boolean canExtract() {
        return false;
    }

    @Shadow
    IItemConduit con;

    @Shadow
    ForgeDirection conDir;

    @Shadow
    private boolean transferItems() {
        return false;
    }

    /**
     * 覆盖原有的 tick 更新方法
     * <p>
     * 原作者：Silvia<br>
     * 修改原因：提升物品传输速度
     * <p>
     * 在单个 tick 内循环执行最多 4096 次物品传输操作，而非原版的一次传输
     *
     * @author Silvia
     * @reason Speed
     */
    @Overwrite
    public void onTick() {
        int i = 0;
        while (canExtract() && con.isExtractionRedstoneConditionMet(conDir) && i <= 4096 && transferItems()) {
            i++;
        }
    }
}
