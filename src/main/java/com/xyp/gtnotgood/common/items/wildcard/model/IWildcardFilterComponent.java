package com.xyp.gtnotgood.common.items.wildcard.model;

import gregtech.api.enums.Materials;

/**
 * 材料过滤组件：判定某材料是否通过。多个过滤组件按 AND 组合（对齐 Wildcard-Pattern）。
 * 每个组件带白/黑名单开关：白名单 = 命中即通过，黑名单 = 命中即排除。
 */
public interface IWildcardFilterComponent extends IWildcardComponent {

    /**
     * 该材料是否通过本过滤组件。
     * 实现应写成 {@code return isWhitelist() == matches(material);}
     */
    boolean test(Materials material);

    boolean isWhitelist();

    void setWhitelist(boolean whitelist);

    /** 供 GUI 显示的简短描述。 */
    String describe();
}
