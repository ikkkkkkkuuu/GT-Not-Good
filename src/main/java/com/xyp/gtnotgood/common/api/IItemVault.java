package com.xyp.gtnotgood.common.api;

import java.math.BigInteger;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;

import org.jetbrains.annotations.Nullable;

import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IItemList;

/**
 * 物品保险库接口
 * 提供无限存储物品和流体的能力
 */
public interface IItemVault {

    /**
     * 注入物品到仓库
     *
     * @param aItem   要注入的物品
     * @param doInput 是否实际执行注入（false为模拟）
     * @return 实际注入的物品数量
     */
    int injectItems(ItemStack aItem, boolean doInput);

    /**
     * 注入AE物品到仓库
     *
     * @param aItem   要注入的AE物品
     * @param doInput 是否实际执行注入（false为模拟）
     * @return 实际注入的物品数量
     */
    long injectItems(IAEItemStack aItem, boolean doInput);

    /**
     * 注入流体到仓库
     *
     * @param aFluid  要注入的流体
     * @param doInput 是否实际执行注入（false为模拟）
     * @return 实际注入的流体数量
     */
    int injectFluids(FluidStack aFluid, boolean doInput);

    /**
     * 注入AE流体到仓库
     *
     * @param aFluid  要注入的AE流体
     * @param doInput 是否实际执行注入（false为模拟）
     * @return 实际注入的流体数量
     */
    long injectFluids(IAEFluidStack aFluid, boolean doInput);

    /**
     * 从仓库提取物品
     *
     * @param aItem    要提取的物品
     * @param doOutput 是否实际执行提取（false为模拟）
     * @return 实际提取的物品数量
     */
    long extractItems(IAEItemStack aItem, boolean doOutput);

    /**
     * 从仓库提取流体
     *
     * @param aFluid   要提取的流体
     * @param doOutput 是否实际执行提取（false为模拟）
     * @return 实际提取的流体数量
     */
    long extractFluids(IAEFluidStack aFluid, boolean doOutput);

    /**
     * 获取当前存储的物品类型数量
     *
     * @return 物品类型数量
     */
    long itemsCount();

    /**
     * 获取当前存储的流体类型数量
     *
     * @return 流体类型数量
     */
    long fluidsCount();

    /**
     * 获取最大物品类型数量
     *
     * @return 最大物品类型数量
     */
    long maxItemCount();

    /**
     * 获取最大流体类型数量
     *
     * @return 最大流体类型数量
     */
    long maxFluidCount();

    /**
     * 检查是否有物品存储能力
     *
     * @return 是否有物品存储能力
     */
    boolean hasItem();

    /**
     * 检查是否有流体存储能力
     *
     * @return 是否有流体存储能力
     */
    boolean hasFluid();

    /**
     * 获取存储的物品
     *
     * @param aItem 要查询的物品
     * @return 存储的AE物品，如果不存在返回null
     */
    IAEItemStack getStoredItem(@Nullable ItemStack aItem);

    /**
     * 获取存储的流体
     *
     * @param aFluid 要查询的流体
     * @return 存储的AE流体，如果不存在返回null
     */
    IAEFluidStack getStoredFluid(@Nullable FluidStack aFluid);

    /**
     * 检查是否包含指定物品
     *
     * @param aItem 要检查的物品
     * @return 是否包含
     */
    boolean containsItems(ItemStack aItem);

    /**
     * 检查是否包含指定流体
     *
     * @param aFluid 要检查的流体
     * @return 是否包含
     */
    boolean containsFluids(FluidStack aFluid);

    /**
     * 获取已存储的物品总量
     *
     * @return 物品总量
     */
    BigInteger getItemStoredAmount();

    /**
     * 获取已存储的流体总量
     *
     * @return 流体总量
     */
    BigInteger getFluidStoredAmount();

    /**
     * 获取所有存储的物品
     *
     * @return 物品列表
     */
    IItemList<IAEItemStack> getStoreItems();

    /**
     * 获取所有存储的流体
     *
     * @return 流体列表
     */
    IItemList<IAEFluidStack> getStoreFluids();

    /**
     * 设置是否销毁过量物品
     *
     * @param doVoidExcess 是否销毁过量
     */
    void setDoVoidExcess(boolean doVoidExcess);

    /**
     * 获取是否销毁过量物品
     *
     * @return 是否销毁过量
     */
    boolean getDoVoidExcess();

    /**
     * 检查仓库是否有效
     *
     * @return 是否有效
     */
    boolean isValid();
}
