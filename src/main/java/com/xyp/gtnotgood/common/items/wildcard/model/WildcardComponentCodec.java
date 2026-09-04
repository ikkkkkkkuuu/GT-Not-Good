package com.xyp.gtnotgood.common.items.wildcard.model;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.Constants;

import com.xyp.gtnotgood.common.items.wildcard.model.filter.PrefixFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.PropertyFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.SimpleFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.StringFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.filter.SubTagFilterComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.FluidIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.PrefixIOComponent;
import com.xyp.gtnotgood.common.items.wildcard.model.io.SimpleIOComponent;

/**
 * 组件列表的 NBT 编解码。每个组件写成 {@code { "type": key, "data": compound }}，整体一个 TAG_LIST。
 * 对齐 Wildcard-Pattern 的通用序列化格式。
 */
public final class WildcardComponentCodec {

    private static final String KEY_TYPE = "type";
    private static final String KEY_DATA = "data";

    private WildcardComponentCodec() {}

    // ============================================================
    // IO 组件
    // ============================================================

    public static NBTTagList writeIO(List<IWildcardIOComponent> components) {
        NBTTagList list = new NBTTagList();
        for (IWildcardIOComponent component : components) {
            if (component == null) continue;
            list.appendTag(wrap(component.typeKey(), component.writeData()));
        }
        return list;
    }

    public static List<IWildcardIOComponent> readIO(NBTTagList list) {
        List<IWildcardIOComponent> result = new ArrayList<>();
        if (list == null) return result;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            String type = entry.getString(KEY_TYPE);
            NBTTagCompound data = entry.getCompoundTag(KEY_DATA);
            IWildcardIOComponent component = readIOComponent(type, data);
            if (component != null) result.add(component);
        }
        return result;
    }

    private static IWildcardIOComponent readIOComponent(String type, NBTTagCompound data) {
        if (type == null) return null;
        switch (type) {
            case PrefixIOComponent.TYPE:
                return PrefixIOComponent.readData(data);
            case FluidIOComponent.TYPE:
                return FluidIOComponent.readData(data);
            case SimpleIOComponent.TYPE:
                return SimpleIOComponent.readData(data);
            default:
                return null;
        }
    }

    // ============================================================
    // 过滤组件
    // ============================================================

    public static NBTTagList writeFilters(List<IWildcardFilterComponent> components) {
        NBTTagList list = new NBTTagList();
        for (IWildcardFilterComponent component : components) {
            if (component == null) continue;
            list.appendTag(wrap(component.typeKey(), component.writeData()));
        }
        return list;
    }

    public static List<IWildcardFilterComponent> readFilters(NBTTagList list) {
        List<IWildcardFilterComponent> result = new ArrayList<>();
        if (list == null) return result;
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound entry = list.getCompoundTagAt(i);
            String type = entry.getString(KEY_TYPE);
            NBTTagCompound data = entry.getCompoundTag(KEY_DATA);
            IWildcardFilterComponent component = readFilterComponent(type, data);
            if (component != null) result.add(component);
        }
        return result;
    }

    private static IWildcardFilterComponent readFilterComponent(String type, NBTTagCompound data) {
        if (type == null) return null;
        switch (type) {
            case SimpleFilterComponent.TYPE:
                return SimpleFilterComponent.readData(data);
            case PropertyFilterComponent.TYPE:
                return PropertyFilterComponent.readData(data);
            case SubTagFilterComponent.TYPE:
                return SubTagFilterComponent.readData(data);
            case PrefixFilterComponent.TYPE:
                return PrefixFilterComponent.readData(data);
            case StringFilterComponent.TYPE:
                return StringFilterComponent.readData(data);
            default:
                return null;
        }
    }

    // ============================================================

    private static NBTTagCompound wrap(String type, NBTTagCompound data) {
        NBTTagCompound entry = new NBTTagCompound();
        entry.setString(KEY_TYPE, type);
        entry.setTag(KEY_DATA, data == null ? new NBTTagCompound() : data);
        return entry;
    }

    public static List<IWildcardIOComponent> readIO(NBTTagCompound tag, String key) {
        return readIO(tag == null ? null : tag.getTagList(key, Constants.NBT.TAG_COMPOUND));
    }

    public static List<IWildcardFilterComponent> readFilters(NBTTagCompound tag, String key) {
        return readFilters(tag == null ? null : tag.getTagList(key, Constants.NBT.TAG_COMPOUND));
    }
}
