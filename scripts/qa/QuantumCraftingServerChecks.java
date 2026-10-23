package com.xyp.gtnotgood.qa;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.server.MinecraftServer;

import com.xyp.gtnotgood.common.crafting.quantum.core.AEConfig;
import com.xyp.gtnotgood.loader.LateMixinsLoader;
import com.xyp.gtnotgood.utils.crafting.VariantBudgets;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.container.ContainerNull;
import appeng.util.inv.MEInventoryCrafting;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;

@Mod(
    modid = "quantumserverqa",
    name = "Quantum Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class QuantumCraftingServerChecks {

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.quantumCrafting.serverQA")) return;
        try {
            require(MinecraftServer.getServer().isDedicatedServer(), "dedicated server");
            for (String name : new String[] { "appeng.me.cache.CraftingGridCache",
                "appeng.me.cluster.implementations.CraftingCPUCluster",
                "appeng.container.implementations.CraftingCPUStatus",
                "appeng.container.implementations.ContainerCraftConfirm",
                "appeng.container.implementations.ContainerOptimizePatterns",
                "appeng.container.implementations.ContainerOptimizePatterns$Pattern",
                "appeng.core.sync.packets.PacketCraftingTreeData" }) {
                Class.forName(name);
            }
            require(AEConfig.instance().getCalculationLimitNanos() > 0, "server timeout enabled");
            verifyVariantBudgets();
            var mods = new HashSet<String>();
            require(new LateMixinsLoader().getMixins(mods).stream().noneMatch(s -> s.contains("quantum")),
                "quantum mixins omitted when AE2 is absent");
            mods.add(ModList.AE2.getID());
            var mixins = new LateMixinsLoader().getMixins(mods);
            require(mixins.contains("appliedenergistics.quantum.MixinContainerOptimizePatterns"),
                "optimizer mixin registered on server");
            require(mixins.stream().noneMatch(s -> s.startsWith("appliedenergistics.quantum.MixinGui")),
                "client GUI mixins excluded on server");
            Files.write(Paths.get("quantum-server-result.txt"), "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("QUANTUM_SERVER_QA: PASS");
        } finally {
            MinecraftServer.getServer().initiateShutdown();
        }
    }

    private static void verifyVariantBudgets() {
        ItemStack encoded = AEApi.instance().definitions().items().encodedPattern().maybeStack(1).get();
        NBTTagCompound tag = new NBTTagCompound();
        NBTTagList in = new NBTTagList();
        in.appendTag(new ItemStack(Blocks.planks).writeToNBT(new NBTTagCompound()));
        NBTTagList out = new NBTTagList();
        out.appendTag(new ItemStack(Items.stick).writeToNBT(new NBTTagCompound()));
        tag.setTag("in", in);
        tag.setTag("out", out);
        tag.setBoolean("crafting", false);
        tag.setBoolean("substitute", true);
        encoded.setTagCompound(tag);
        var details = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded,
            MinecraftServer.getServer().worldServerForDimension(0));
        var oak = AEItemStack.create(new ItemStack(Blocks.planks, 4, 0));
        var spruce = AEItemStack.create(new ItemStack(Blocks.planks, 6, 1));
        var input = oak.copy().setStackSize(1);
        VariantBudgets budget = new VariantBudgets();
        budget.add(details, input, Arrays.asList(oak, spruce));
        VariantBudgets restored = new VariantBudgets();
        restored.readFromNBT(budget.writeToNBT());
        require(VariantBudgets.total(restored.get(details, input)) == 10, "variant budgets survive NBT");
        var table = new MEInventoryCrafting(new ContainerNull(), 1, 1);
        table.setInventorySlotContents(0, spruce.copy().setStackSize(2));
        restored.consume(details, table, Collections.singletonList(input));
        require(
            VariantBudgets.left(restored.get(details, input), oak) == 4
                && VariantBudgets.left(restored.get(details, input), spruce) == 4,
            "accepted craft consumes only its actual variant");
        restored.clear();
        require(restored.isEmpty(), "cancelled variant budget clears");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        System.out.println("QUANTUM_SERVER_QA: " + message);
    }
}
