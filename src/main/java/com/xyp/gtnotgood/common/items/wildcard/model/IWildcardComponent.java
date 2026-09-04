package com.xyp.gtnotgood.common.items.wildcard.model;

import net.minecraft.nbt.NBTTagCompound;

/**
 * 通配符组件的公共契约：可序列化为 {@code { "type": key, "data": compound }}。
 * 对齐 Wildcard-Pattern 的组件序列化范式。
 */
public interface IWildcardComponent {

    /** 序列化键（类型标识），如 "prefix" / "fluid" / "simple"。 */
    String typeKey();

    /** 把组件自身状态写入 data 复合标签。 */
    NBTTagCompound writeData();
}
