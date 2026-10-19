package com.xyp.gtnotgood.common.blocks.packaged;

import java.util.concurrent.Future;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidStack;

import com.glodblock.github.loader.ItemAndBlockHolder;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.config.Actionable;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.crafting.ICraftingJob;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPatternDetails;
import appeng.api.networking.security.MachineSource;
import appeng.api.parts.IPartHost;
import appeng.api.storage.IMEInventory;
import appeng.api.storage.IMEInventoryHandler;
import appeng.api.storage.StorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.storage.data.IAEStack;
import appeng.api.util.AEColor;
import appeng.api.util.CraftCompleteListener;
import appeng.helpers.UltimatePatternHelper;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.storage.CellInventory;
import appeng.tile.storage.TileDrive;
import appeng.util.Platform;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEFluidStackType;
import appeng.util.item.AEItemStack;
import gregtech.api.util.GTRecipe;

public final class CircuitAssemblyLineQaNetwork {

    public final TilePackagedProvider provider;
    public final TileDrive drive;
    public final GTRecipe recipe;
    public final int batches;
    public final int terminalX, terminalY, terminalZ;
    public final ICraftingPatternDetails pattern;
    public Future<ICraftingJob> plan;
    public ICraftingLink link;
    public CraftingCPUCluster cpu;
    public boolean observedBusy, completedByCpu;
    public long completedOutputCount, completedElapsedTime;
    private boolean completedOutputMatches;
    private CpuCompletion completion;
    private final World world;
    private final MachineSource source;

    public CircuitAssemblyLineQaNetwork(EntityPlayerMP player, GTRecipe recipe, PackagedTarget target, int x, int y,
        int z, int batches) throws Exception {
        require(batches > 0 && recipe != null && recipe.mOutputs.length == 1, "one real CAL output and positive batch");
        this.world = player.worldObj;
        this.recipe = recipe;
        this.batches = batches;
        var definitions = AEApi.instance().definitions();
        var blocks = definitions.blocks();
        place(x, y, z, GTNGItemList.WirelessPackagedPatternProvider.get(1));
        provider = (TilePackagedProvider) world.getTileEntity(x, y, z);
        provider.setOwnerName(player.getCommandSenderName());
        provider.setInventorySlotContents(TilePackagedProvider.CORE, GTNGItemList.CircuitAssemblyLineCore.get(1));
        provider.autoReturn = true;
        require(provider.bind(target), "physical CAL controller bound to the provider");
        source = new MachineSource(provider);
        place(x - 1, y, z, blocks.controller().maybeStack(1).get());
        place(x - 1, y, z + 1, blocks.energyCellCreative().maybeStack(1).get());
        place(x - 2, y, z, blocks.drive().maybeStack(1).get());
        drive = (TileDrive) world.getTileEntity(x - 2, y, z);
        drive.getInternalInventory().setInventorySlotContents(0, definitions.items().cell64k().maybeStack(1).get());
        ItemStack fluidCell = new ItemStack(ItemAndBlockHolder.CELL16384KM);
        var fluids = (IMEInventoryHandler<IAEFluidStack>) CellInventory.getCell(fluidCell, null,
            AEFluidStackType.FLUID_STACK_TYPE);
        require(fluids != null, "physical multi-fluid storage cell is supported");
        for (FluidStack fluid : recipe.mFluidInputs) {
            var stock = AEFluidStack.create(fluid).setStackSize((long) fluid.amount * batches);
            require(fluids.injectItems(stock, Actionable.MODULATE, source) == null,
                "native fluid cell accepts " + fluid.getLocalizedName());
        }
        drive.getInternalInventory().setInventorySlotContents(1, fluidCell);
        place(x - 2, y, z + 1, blocks.craftingStorage64k().maybeStack(1).get());
        place(x - 3, y, z + 1, blocks.craftingAccelerator().maybeStack(1).get());
        terminalX = x - 3;
        terminalY = y;
        terminalZ = z;
        terminal(player, terminalX, y, z, definitions.parts().craftingTerminal().maybeStack(1).get());
        terminal(player, x - 4, y, z, definitions.parts().patternTerminal().maybeStack(1).get());
        ItemStack encoded = encode(recipe);
        encoded = ItemStack.loadItemStackFromNBT(encoded.writeToNBT(new NBTTagCompound()));
        pattern = ((ICraftingPatternItem) encoded.getItem()).getPatternForItem(encoded, world);
        require(pattern instanceof UltimatePatternHelper, "persisted native Ultimate processing pattern decodes");
        int fluidInputs = 0;
        for (var input : pattern.getAEInputs()) if (input instanceof IAEFluidStack) fluidInputs++;
        require(fluidInputs == recipe.mFluidInputs.length, "Ultimate pattern retains every native fluid input");
        provider.setInventorySlotContents(0, encoded);
    }

    public boolean ready() throws Exception {
        return provider.getProxy().isActive() && !provider.getProxy().getCrafting().getCpus().isEmpty();
    }

    public void supplyAndPlan() throws Exception {
        require(plan == null && link == null && ready(), "native ME network ready before the only order");
        require(storedOutput() == 0 && storedOutputInCell() == 0, "result absent before the actual AE order");
        var storage = provider.getProxy().getStorage();
        require(drive.getActionableNode().getGrid() == provider.getProxy().getGrid(),
            "physical drive and provider share the same ME network");
        for (ItemStack item : recipe.mInputs) {
            var stock = AEItemStack.create(item).setStackSize((long) item.stackSize * batches);
            require(storage.getItemInventory().injectItems(stock, Actionable.MODULATE, source) == null,
                "ME storage accepts real recipe input " + item.getDisplayName());
        }
        for (FluidStack fluid : recipe.mFluidInputs) {
            long expected = (long) fluid.amount * batches;
            require(storedFluid(fluid) >= expected,
                "native fluid is visible to the crafting grid: " + fluid.getLocalizedName() + "=" + storedFluid(fluid));
        }
        var request = AEItemStack.create(recipe.mOutputs[0]).setStackSize(expectedOutput());
        plan = provider.getProxy().getCrafting().beginCraftingJob(world, provider.getProxy().getGrid(), source, request,
            null);
        require(plan != null, "real AE crafting calculation started");
    }

    public boolean submitIfReady() throws Exception {
        require(plan != null, "AE plan was requested before submission");
        if (link != null) return true;
        if (!plan.isDone()) return false;
        ICraftingJob job = plan.get();
        require(!job.isSimulation(),
            "AE calculated a complete native item/fluid material plan: " + job.getErrorMessage());
        require(job.getByteTotal() <= 65536, "native crafting plan fits the physical 64k CPU");
        var cpus = provider.getProxy().getCrafting().getCpus();
        require(cpus.size() == 1, "the physical network contains exactly one native crafting CPU");
        var selected = cpus.iterator().next();
        require(selected instanceof CraftingCPUCluster, "the selected CPU is a real crafting block cluster");
        cpu = (CraftingCPUCluster) selected;
        require(!cpu.isBusy(), "the selected physical CPU is idle before this order");
        link = provider.getProxy().getCrafting().submitJob(job, null, cpu, false, source);
        require(link != null && link.isStandalone(), "native CPU accepted the standalone AE order");
        require(!link.isCanceled(), "accepted AE order remains live");
        observedBusy = cpu.isBusy() && cpu.getLastCraftingLink() == link;
        require(observedBusy, "the selected physical CPU owns the accepted order and has native pending work");
        completion = new CpuCompletion(recipe.mOutputs[0]);
        cpu.addOnCompleteListener(completion);
        return true;
    }

    /** Standalone links have no nexus; the physical CPU reports completion. */
    public boolean orderDone() {
        if (completion != null) {
            completedByCpu = completion.completed;
            completedOutputMatches = completion.outputMatches;
            completedOutputCount = completion.count;
            completedElapsedTime = completion.elapsed;
        }
        return link != null && !link.isCanceled()
            && cpu != null
            && observedBusy
            && completedByCpu
            && completedOutputMatches
            && completedOutputCount == expectedOutput()
            && !cpu.isBusy()
            && cpu.getUsedStorage() == 0
            && cpu.getRemainingItemCount() == 0
            && cpu.getLastCraftingLink() == null;
    }

    public long expectedOutput() {
        return (long) recipe.mOutputs[0].stackSize * batches;
    }

    public long storedOutput() throws Exception {
        return storedItem(recipe.mOutputs[0]);
    }

    public long storedItem(ItemStack item) throws Exception {
        var found = provider.getProxy().getStorage().getItemInventory()
            .extractItems(AEItemStack.create(item).setStackSize(Long.MAX_VALUE), Actionable.SIMULATE, source);
        return found == null ? 0 : found.getStackSize();
    }

    public long storedFluid(FluidStack fluid) throws Exception {
        var found = provider.getProxy().getStorage().getFluidInventory()
            .extractItems(AEFluidStack.create(fluid).setStackSize(Long.MAX_VALUE), Actionable.SIMULATE, source);
        return found == null ? 0 : found.getStackSize();
    }

    public long storedOutputInCell() {
        IMEInventory<IAEItemStack> cell = AEApi.instance().registries().cell()
            .getCellInventory(drive.getInternalInventory().getStackInSlot(0), null, StorageChannel.ITEMS);
        require(cell != null, "physical item cell retains a native inventory");
        var found = cell.extractItems(AEItemStack.create(recipe.mOutputs[0]).setStackSize(Long.MAX_VALUE),
            Actionable.SIMULATE, source);
        return found == null ? 0 : found.getStackSize();
    }

    public boolean complete() throws Exception {
        return orderDone() && provider.queuedJobs() == 0
            && storedOutput() == expectedOutput()
            && storedOutputInCell() == expectedOutput();
    }

    public void verifyConsumed() throws Exception {
        require(complete(), "AE job completes and every real result is persisted in the physical item cell");
        for (ItemStack item : recipe.mInputs)
            require(storedItem(item) == 0, "exact seeded input consumed from ME storage: " + item.getDisplayName());
        for (FluidStack fluid : recipe.mFluidInputs) require(storedFluid(fluid) == 0,
            "exact seeded native fluid consumed from ME storage: " + fluid.getLocalizedName());
    }

    public String status() throws Exception {
        return "active=" + provider.getProxy().isActive()
            + "; cpus="
            + provider.getProxy().getCrafting().getCpus().size()
            + "; planned="
            + (plan != null && plan.isDone())
            + "; submitted="
            + (link != null)
            + "; done="
            + orderDone()
            + "; cpuBusy="
            + (cpu != null && cpu.isBusy())
            + "; cpuUsedBytes="
            + (cpu == null ? -1 : cpu.getUsedStorage())
            + "; cpuRemaining="
            + (cpu == null ? -1 : cpu.getRemainingItemCount())
            + "; cpuLink="
            + (cpu != null && cpu.getLastCraftingLink() != null)
            + "; completedCallback="
            + completedByCpu
            + "; callbackOutput="
            + completedOutputCount
            + "; canceled="
            + (link != null && link.isCanceled())
            + "; receipts="
            + provider.queuedJobs()
            + "; output="
            + storedOutput()
            + "/"
            + expectedOutput()
            + "; cell="
            + storedOutputInCell();
    }

    private static ItemStack encode(GTRecipe recipe) {
        ItemStack encoded = AEApi.instance().definitions().items().encodedUltimatePattern().maybeStack(1).get();
        var tag = new NBTTagCompound();
        var inputs = new NBTTagList();
        var outputs = new NBTTagList();
        for (ItemStack item : recipe.mInputs) {
            var entry = new NBTTagCompound();
            Platform.writeStackNBT(AEItemStack.create(item), entry);
            inputs.appendTag(entry);
        }
        for (FluidStack fluid : recipe.mFluidInputs) {
            var entry = new NBTTagCompound();
            Platform.writeStackNBT(AEFluidStack.create(fluid), entry);
            inputs.appendTag(entry);
        }
        var result = new NBTTagCompound();
        Platform.writeStackNBT(AEItemStack.create(recipe.mOutputs[0]), result);
        outputs.appendTag(result);
        tag.setTag("in", inputs);
        tag.setTag("out", outputs);
        tag.setBoolean("crafting", false);
        encoded.setTagCompound(tag);
        return encoded;
    }

    private void place(int x, int y, int z, ItemStack stack) {
        require(world.setBlock(x, y, z, Block.getBlockFromItem(stack.getItem()), stack.getItemDamage(), 3),
            "physical ME block placed at " + x + "," + y + "," + z);
    }

    private void terminal(EntityPlayerMP player, int x, int y, int z, ItemStack terminal) {
        place(x, y, z, AEApi.instance().definitions().blocks().multiPart().maybeStack(1).get());
        IPartHost host = (IPartHost) world.getTileEntity(x, y, z);
        require(host.addPart(AEApi.instance().definitions().parts().cableGlass().stack(AEColor.Transparent, 1),
            ForgeDirection.UNKNOWN, player) != null, "physical terminal ME cable added");
        require(host.addPart(terminal, ForgeDirection.NORTH, player) != null, "native ME terminal attached");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private static final class CpuCompletion implements CraftCompleteListener {

        private static final long serialVersionUID = 1L;
        private final String itemId, nbt;
        private final int damage;
        private boolean completed, outputMatches;
        private long count, elapsed;

        private CpuCompletion(ItemStack expected) {
            itemId = Item.itemRegistry.getNameForObject(expected.getItem());
            damage = expected.getItemDamage();
            nbt = expected.hasTagCompound() ? expected.getTagCompound().toString() : "";
        }

        @Override
        public void apply(IAEStack<?> output, long count, long elapsed) {
            if (output instanceof IAEItemStack item) {
                ItemStack actual = item.getItemStack();
                outputMatches = actual != null && itemId.equals(Item.itemRegistry.getNameForObject(actual.getItem()))
                    && damage == actual.getItemDamage()
                    && nbt.equals(actual.hasTagCompound() ? actual.getTagCompound().toString() : "");
            }
            this.count = count;
            this.elapsed = elapsed;
            completed = true;
        }
    }
}
