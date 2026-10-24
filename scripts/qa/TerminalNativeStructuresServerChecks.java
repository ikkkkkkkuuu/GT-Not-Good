package com.xyp.gtnotgood.qa;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.StatCollector;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.ForgeDirection;

import com.gtnewhorizon.structurelib.alignment.IAlignment;
import com.gtnewhorizon.structurelib.alignment.constructable.ChannelDataAccessor;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalBlockRecovery;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalItemSource;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalOperations;
import com.xyp.gtnotgood.common.items.multiblockterminal.TerminalSettings;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.metatileentity.implementations.MTEHatch;
import gregtech.api.metatileentity.implementations.MTEMultiBlockBase;
import gregtech.api.structure.error.PositionedStructureError;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GlassTier;
import gregtech.common.misc.GTStructureChannels;
import gregtech.common.tileentities.machines.multi.MTEAssemblyLine;
import gregtech.common.tileentities.machines.multi.MTEDistillationTower;
import io.netty.buffer.Unpooled;

public final class TerminalNativeStructuresServerChecks {

    private static final int HEIGHT = 8;
    private final EntityPlayerMP player;
    private final WorldServer world;
    private final ItemStack[] savedInventory;
    private final int savedSlot;
    private final double savedX, savedY, savedZ;
    private int checks;

    private TerminalNativeStructuresServerChecks(EntityPlayerMP player) {
        this.player = player;
        world = (WorldServer) player.worldObj;
        savedInventory = player.inventory.mainInventory.clone();
        savedSlot = player.inventory.currentItem;
        savedX = player.posX;
        savedY = player.posY;
        savedZ = player.posZ;
    }

    public static int verify(MinecraftServer server, EntityPlayerMP player) {
        TerminalNativeStructuresServerChecks check = new TerminalNativeStructuresServerChecks(player);
        try {
            check.require(!player.capabilities.isCreativeMode,
                "native structures QA uses real survival material costs");
            check.distillationTower();
            check.assemblyLine();
            return check.checks;
        } finally {
            check.clear(512, 80, 128, 3, 1, HEIGHT + 1, 4);
            check.clear(576, 80, 128, 18, 3, 4, 4);
            System.arraycopy(check.savedInventory, 0, player.inventory.mainInventory, 0, check.savedInventory.length);
            player.inventory.currentItem = check.savedSlot;
            player.setPosition(check.savedX, check.savedY, check.savedZ);
            player.inventory.markDirty();
            player.openContainer.detectAndSendChanges();
        }
    }

    private void distillationTower() {
        int x = 512, y = 80, z = 128;
        clear(x, y, z, 3, 1, HEIGHT + 1, 4);
        player.setPosition(x + 0.5, y, z - 2.5);
        BaseMetaTileEntity base = machine(x, y, z, ItemList.Distillation_Tower.get(1));
        MTEDistillationTower tower = (MTEDistillationTower) base.getMetaTileEntity();
        ItemStack casing = new ItemStack(GregTechAPI.sBlockCasings4, 64, 1);
        ItemList[] hatches = { ItemList.Hatch_Energy_LV, ItemList.Hatch_Output_Bus_LV, ItemList.Hatch_Input_LV,
            ItemList.Hatch_Input_Bus_LV, ItemList.Hatch_Maintenance, ItemList.Hatch_Output_LV };
        ItemStack builder = terminal();
        TerminalSettings.setCount(builder, HEIGHT - 2);
        supplies(builder, casing, hatches);
        require(
            TerminalSettings.count(builder) == HEIGHT - 2
                && !ChannelDataAccessor.hasSubChannel(builder, GTStructureChannels.STRUCTURE_HEIGHT.get()),
            "native height channel adds the minimum-height offset: master six builds eight layers");
        require(operate(builder, x, y, z), "terminal accepts the real native distillation tower");
        formed(tower, base, "eight-layer tower");
        require(count(casing) == 11 && countWorld(casing, x, y, z, 3, 1, HEIGHT + 1, 4) == 53,
            "eight-layer native tower consumes exactly 53 clean stainless casings");
        for (int index = 0; index < hatches.length; index++) require(count(hatches[index].get(1)) == 0,
            "native tower consumes the supplied hatch role " + hatches[index]);
        for (int layer = 1; layer < HEIGHT; layer++)
            require(countWorld(ItemList.Hatch_Output_LV.get(1), x, y + layer, z, 3, 0, 0, 4) == 1,
                "real native tower places one output hatch at layer " + layer);
        require(blocksExceptController(x, y, z, 3, 1, HEIGHT + 1, 4) == 65,
            "high-count tower has all 65 physical structure blocks besides its controller");

        ItemStack fresh = terminal();
        TerminalSettings.setDismantle(fresh, true);
        player.inventory.mainInventory[0] = fresh;
        require(
            TerminalSettings.count(fresh) == 1
                && !ChannelDataAccessor.hasSubChannel(fresh, GTStructureChannels.STRUCTURE_HEIGHT.get()),
            "fresh dismantling terminal has default master one and no copied height channel");
        require(operate(fresh, x, y, z), "fresh default-count terminal dismantles the formed tower in one operation");
        int remaining = blocksExceptController(x, y, z, 3, 1, HEIGHT + 1, 4);
        boolean retainedController = world.getTileEntity(x, y, z) == base;
        String teardownDiagnostic = remaining == 0 && retainedController ? ""
            : diagnostics(tower, base, "eight-layer tower after fresh teardown", List.of());
        require(retainedController && remaining == 0,
            "actual formed height captures every upper tower layer while retaining the controller; remaining="
                + remaining
                + " "
                + teardownDiagnostic);
        towerRefunds(casing, hatches, "complete tower");

        player.inventory.mainInventory[0] = builder;
        require(operate(builder, x, y, z), "returned materials rebuild the native eight-layer tower");
        formed(tower, base, "rebuilt tower before partial teardown");
        int[] broken = findCasing(x, y, z, casing);
        require(broken != null && TerminalBlockRecovery.harvest(player, world, broken[0], broken[1], broken[2],
            new TerminalItemSource(builder, player)), "native player harvesting removes one lower tower casing");
        require(!tower.checkStructure(true, base), "missing lower casing genuinely breaks the native tower structure");
        ItemStack partial = terminal();
        TerminalSettings.setDismantle(partial, true);
        player.inventory.mainInventory[0] = partial;
        require(TerminalSettings.count(partial) == 1,
            "partial-layout search also starts with a fresh default terminal");
        require(operate(partial, x, y, z), "default-count terminal searches the partially broken tower layout");
        require(world.getTileEntity(x, y, z) == base && blocksExceptController(x, y, z, 3, 1, HEIGHT + 1, 4) == 0,
            "partial-layout inference recovers all surviving upper layers after a lower casing was removed");
        towerRefunds(casing, hatches, "partially broken tower");

        player.inventory.mainInventory[0] = builder;
        require(operate(builder, x, y, z), "returned materials rebuild the tower for a legacy top-hatch check");
        formed(tower, base, "tower before legacy top hatch");
        int topY = y + HEIGHT - 1;
        require(casing.isItemEqual(stackAt(x, topY, z + 1)), "native top center starts as a casing");
        require(TerminalBlockRecovery.harvest(player, world, x, topY, z + 1, new TerminalItemSource(builder, player)),
            "native player harvesting recovers the original top-center casing");
        BaseMetaTileEntity extraOutput = machine(x, topY, z + 1, ItemList.Hatch_Output_LV.get(1));
        extraOutput.setFrontFacing(ForgeDirection.UP);
        formed(tower, base, "tower with legacy accepted top-center output hatch");
        require(tower.mOutputHatches.size() == 1,
            "native legacy addOutputToMachineList accepts the extra top-center hatch");
        ItemStack topDismantler = terminal();
        TerminalSettings.setDismantle(topDismantler, true);
        player.inventory.mainInventory[0] = topDismantler;
        require(operate(topDismantler, x, y, z), "fresh terminal dismantles the tower with a legacy top-center hatch");
        int topRemaining = blocksExceptController(x, y, z, 3, 1, HEIGHT + 1, 4);
        String topDiagnostic = topRemaining == 0 ? ""
            : diagnostics(tower, base, "tower after legacy top-hatch teardown", List.of());
        require(world.getTileEntity(x, y, z) == base && topRemaining == 0,
            "native successful legacy check retains evidence for the top-center hatch; remaining=" + topRemaining
                + " "
                + topDiagnostic);
        require(count(casing) == 64, "legacy top-hatch tower returns every original casing");
        for (int index = 0; index < hatches.length; index++)
            require(count(hatches[index].get(1)) == (index == hatches.length - 1 ? HEIGHT : 1),
                "legacy top-hatch tower returns each original hatch and the added top hatch " + hatches[index]);
    }

    private void towerRefunds(ItemStack casing, ItemList[] hatches, String description) {
        require(count(casing) == 64, description + " returns the exact original casing quantity");
        for (int index = 0; index < hatches.length; index++)
            require(count(hatches[index].get(1)) == (index == hatches.length - 1 ? HEIGHT - 1 : 1),
                description + " returns the exact original hatch quantity " + hatches[index]);
    }

    private void assemblyLine() {
        int x = 576, y = 80, z = 128;
        clear(x, y, z, 18, 3, 4, 4);
        player.setPosition(x + 0.5, y, z - 2.5);
        Arrays.fill(player.inventory.mainInventory, null);
        player.inventory.currentItem = 0;
        ItemStack builder = terminal();
        player.inventory.mainInventory[0] = builder;
        BaseMetaTileEntity base = machine(x, y, z, ItemList.Machine_Multi_Assemblyline.get(1));
        MTEAssemblyLine machine = (MTEAssemblyLine) base.getMetaTileEntity();
        ItemStack steel = new ItemStack(GregTechAPI.sBlockCasings2, 64, 0);
        ItemStack grate = new ItemStack(GregTechAPI.sBlockCasings3, 64, 10);
        ItemStack assembler = new ItemStack(GregTechAPI.sBlockCasings2, 64, 9);
        ItemStack assembly = new ItemStack(GregTechAPI.sBlockCasings2, 64, 5);
        var registeredGlass = GlassTier.getGlassList().get(0);
        ItemStack glass = new ItemStack(registeredGlass.getLeft(), 64, registeredGlass.getRight());
        ItemStack[] material = { steel, grate, assembler, assembly, glass, ItemList.Hatch_Input_Bus_LV.get(5),
            ItemList.Hatch_Output_Bus_LV.get(1), ItemList.Hatch_Energy_LV.get(1), ItemList.Hatch_Maintenance.get(1),
            ItemList.Hatch_Input_LV.get(4), ItemList.Hatch_DataAccess_EV.get(1) };
        for (int slot = 0; slot < material.length; slot++)
            player.inventory.mainInventory[slot + 1] = material[slot].copy();
        require(
            TerminalSettings.count(builder) == 1
                && !ChannelDataAccessor.hasSubChannel(builder, GTStructureChannels.STRUCTURE_LENGTH.get()),
            "native assembly line uses a fresh terminal and the original minimum-length default");
        require(operate(builder, x, y, z), "terminal builds the real native assembly line");
        formed(machine, base, "five-slice native assembly line");
        require(machine.mInputBusses.size() == 5 && machine.mOutputBusses.size() == 1,
            "native survival-only last piece places the output bus while retaining all five input buses");
        var output = machine.mOutputBusses.get(0).getBaseMetaTileEntity();
        require(Math.abs(output.getXCoord() - x) == 4,
            "the real output bus occupies the last slice, rather than an earlier generic casing position");
        require(count(steel) == 56 && count(grate) == 56
            && count(assembler) == 59
            && count(assembly) == 59
            && count(glass) == 54, "native assembly line consumes exact casing and registered-glass quantities");
        for (int index = 5; index < material.length; index++) require(count(material[index]) == 0,
            "native assembly line consumes the exact supplied hatch role " + index);
        require(blocksExceptController(x, y, z, 18, 3, 4, 4) == 49,
            "native assembly-line fixture has all 49 non-controller blocks");

        ItemStack dismantler = terminal();
        TerminalSettings.setDismantle(dismantler, true);
        player.inventory.mainInventory[0] = dismantler;
        require(
            TerminalSettings.count(dismantler) == 1
                && !ChannelDataAccessor.hasSubChannel(dismantler, GTStructureChannels.STRUCTURE_LENGTH.get()),
            "assembly-line dismantling uses a fresh default terminal without copied length settings");
        require(operate(dismantler, x, y, z), "fresh terminal dismantles the formed native assembly line");
        int remaining = blocksExceptController(x, y, z, 18, 3, 4, 4);
        boolean retainedController = world.getTileEntity(x, y, z) == base;
        String teardownDiagnostic = remaining == 0 && retainedController ? ""
            : diagnostics(machine, base, "five-slice native assembly line after teardown", List.of());
        require(retainedController && remaining == 0,
            "native assembly-line teardown recovers every block and legacy-accepted output bus; remaining=" + remaining
                + " "
                + teardownDiagnostic);
        for (int index = 0; index < material.length; index++)
            require(count(material[index]) == material[index].stackSize,
                "native assembly-line teardown returns the exact original material quantity " + index);
    }

    private void supplies(ItemStack terminal, ItemStack casing, ItemList[] hatches) {
        Arrays.fill(player.inventory.mainInventory, null);
        player.inventory.currentItem = 0;
        player.inventory.mainInventory[0] = terminal;
        player.inventory.mainInventory[1] = casing.copy();
        for (int index = 0; index < hatches.length; index++) player.inventory.mainInventory[index + 2] = hatches[index]
            .get(index == hatches.length - 1 ? HEIGHT - 1 : 1);
    }

    private static ItemStack terminal() {
        ItemStack terminal = GTNGItemList.MultiblockTerminal.get(1);
        TerminalSettings.setNoHatches(terminal, false);
        return terminal;
    }

    private BaseMetaTileEntity machine(int x, int y, int z, ItemStack item) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        base.setInitialValuesAsNBT(null, (short) item.getItemDamage());
        base.setOwnerUuid(player.getUniqueID());
        base.setFrontFacing(ForgeDirection.NORTH);
        return base;
    }

    private void formed(MTEMultiBlockBase machine, BaseMetaTileEntity base, String description) {
        machine.clearHatches();
        List<StructureError> errors = new ArrayList<>();
        machine.checkMachine(base, null, errors);
        String diagnostic = errors.isEmpty() ? "" : diagnostics(machine, base, description, errors);
        require(errors.isEmpty(), description + " passes real native checkMachine: " + diagnostic);
        require(machine.checkStructure(true, base), description + " is genuinely formed without forced state");
    }

    private String diagnostics(MTEMultiBlockBase machine, BaseMetaTileEntity base, String description,
        List<StructureError> errors) {
        StringBuilder report = new StringBuilder("description=").append(description).append("\ncontroller=")
            .append(machine.getClass().getName()).append(" at ").append(base.xCoord).append(',').append(base.yCoord)
            .append(',').append(base.zCoord).append("\nfacing=")
            .append(machine instanceof IAlignment alignment ? alignment.getExtendedFacing() : base.getFrontFacing())
            .append("\nheld=").append(describeStack(player.getHeldItem())).append("\nhatch counters: input=")
            .append(machine.mInputHatches.size()).append(" output=").append(machine.mOutputHatches.size())
            .append(" inputBus=").append(machine.mInputBusses.size()).append(" outputBus=")
            .append(machine.mOutputBusses.size()).append(" energy=").append(machine.mEnergyHatches.size())
            .append(" maintenance=").append(machine.mMaintenanceHatches.size()).append('\n');
        StringBuilder summary = new StringBuilder();
        for (int index = 0; index < errors.size(); index++) {
            String error = describeError(errors.get(index));
            report.append("error[").append(index).append("]=").append(error).append('\n');
            if (index > 0) summary.append("; ");
            summary.append(error);
        }
        if (machine instanceof MTEDistillationTower) {
            for (String name : new String[] { "mHeight", "mTopLayerFound", "mCasing", "mOutputHatchesByLayer" }) {
                try {
                    Field field = MTEDistillationTower.class.getDeclaredField(name);
                    field.setAccessible(true);
                    Object value = field.get(machine);
                    if (value instanceof List<?>layers) {
                        List<Integer> counts = new ArrayList<>();
                        for (Object layer : layers) counts.add(((List<?>) layer).size());
                        value = counts;
                    }
                    report.append(name).append('=').append(value).append('\n');
                } catch (ReflectiveOperationException failure) {
                    report.append(name).append("=unavailable: ").append(failure).append('\n');
                }
            }
        }
        report.append("\nremaining supplies:\n");
        for (int slot = 0; slot < player.inventory.mainInventory.length; slot++) {
            ItemStack stack = player.inventory.mainInventory[slot];
            if (stack != null)
                report.append("slot[").append(slot).append("]=").append(describeStack(stack)).append('\n');
        }
        int rx = machine instanceof MTEAssemblyLine ? 18 : 3;
        int below = machine instanceof MTEAssemblyLine ? 3 : 1;
        int above = machine instanceof MTEAssemblyLine ? 4 : HEIGHT + 1;
        report.append("\nworld layers; @ controller, H GT hatch, T other tile, C ordinary block, . air, ? unloaded\n");
        for (int dy = -below; dy <= above; dy++) {
            Map<String, Integer> blocks = new LinkedHashMap<>();
            Map<String, Integer> hatches = new LinkedHashMap<>();
            StringBuilder details = new StringBuilder();
            report.append("layer dy=").append(dy).append(" worldY=").append(base.yCoord + dy).append('\n');
            for (int dz = -4; dz <= 4; dz++) {
                report.append("  z=").append(base.zCoord + dz).append(' ');
                for (int dx = -rx; dx <= rx; dx++) {
                    int x = base.xCoord + dx, y = base.yCoord + dy, z = base.zCoord + dz;
                    if (!world.blockExists(x, y, z)) {
                        report.append('?');
                        continue;
                    }
                    Block block = world.getBlock(x, y, z);
                    if (block.isAir(world, x, y, z)) {
                        report.append('.');
                        continue;
                    }
                    TileEntity tile = world.getTileEntity(x, y, z);
                    String key = Block.blockRegistry.getNameForObject(block) + "#" + world.getBlockMetadata(x, y, z);
                    blocks.merge(key, 1, Integer::sum);
                    details.append("  [").append(x).append(',').append(y).append(',').append(z).append("] ").append(key)
                        .append(" block=").append(block.getClass().getName());
                    char symbol = tile == null ? 'C' : 'T';
                    if (tile != null) details.append(" tile=").append(tile.getClass().getName());
                    if (tile instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() != null) {
                        var meta = gt.getMetaTileEntity();
                        details.append(" MTE=").append(meta.getClass().getName()).append(" stack=")
                            .append(describeStack(meta.getStackForm(1))).append(" front=").append(gt.getFrontFacing());
                        if (meta instanceof MTEHatch) {
                            symbol = 'H';
                            hatches.merge(meta.getClass().getSimpleName(), 1, Integer::sum);
                        }
                    }
                    if (dx == 0 && dy == 0 && dz == 0) symbol = '@';
                    report.append(symbol);
                    details.append('\n');
                }
                report.append('\n');
            }
            report.append("  block counts=").append(blocks).append("\n  hatch counts=").append(hatches).append('\n')
                .append(details);
        }
        try {
            Path directory = Path.of(System.getProperty("gtng.terminal.qa.output"), "native-structures-diagnostics");
            Files.createDirectories(directory);
            Path output = directory
                .resolve(description.replaceAll("[^A-Za-z0-9]+", "-").toLowerCase(Locale.ROOT) + ".txt");
            Files.write(output, report.toString().getBytes(StandardCharsets.UTF_8));
            System.out.println("TERMINAL_NATIVE_STRUCTURES_QA: DIAGNOSTIC " + output + " " + summary);
            return summary + " [diagnostic " + output + "]";
        } catch (Exception failure) {
            System.err.println("TERMINAL_NATIVE_STRUCTURES_QA: diagnostic write failed: " + failure + "\n" + report);
            return summary + " [diagnostic write failed: " + failure + "]";
        }
    }

    private String describeError(StructureError error) {
        StringBuilder message = new StringBuilder(error.getClass().getName()).append(" id=").append(error.getId());
        PacketBuffer buffer = new PacketBuffer(Unpooled.buffer());
        try {
            error.serialize(buffer);
            if (error instanceof PositionedStructureError) {
                int x = buffer.readInt(), y = buffer.readInt(), z = buffer.readInt();
                List<String> expected = new ArrayList<>();
                int count = buffer.readInt();
                for (int index = 0; index < count; index++) {
                    String key = buffer.readStringFromBuffer(256);
                    expected.add(key + " => " + StatCollector.translateToLocal(key));
                }
                message.append(" position=").append(x).append(',').append(y).append(',').append(z).append(" expected=")
                    .append(expected).append(" actual=").append(
                        world.blockExists(x, y, z)
                            ? Block.blockRegistry.getNameForObject(world.getBlock(x, y, z)) + "#"
                                + world.getBlockMetadata(x, y, z)
                            : "unloaded");
            } else {
                byte[] payload = new byte[buffer.readableBytes()];
                buffer.readBytes(payload);
                message.append(" serializedBase64=").append(Base64.getEncoder().encodeToString(payload));
            }
        } catch (Exception failure) {
            message.append(" serializationFailure=").append(failure);
        } finally {
            buffer.release();
        }
        return message.toString();
    }

    private static String describeStack(ItemStack stack) {
        return stack == null ? "null"
            : String.valueOf(GameRegistry.findUniqueIdentifierFor(stack.getItem())) + "#"
                + stack.getItemDamage()
                + " x"
                + stack.stackSize
                + " nbt="
                + stack.getTagCompound();
    }

    private int[] findCasing(int x, int y, int z, ItemStack casing) {
        for (int dx = -2; dx <= 2; dx++) for (int dz = -3; dz <= 3; dz++) {
            if (dx == 0 && dz == 0) continue;
            ItemStack actual = stackAt(x + dx, y, z + dz);
            if (actual != null && casing.isItemEqual(actual)) return new int[] { x + dx, y, z + dz };
        }
        return null;
    }

    private int count(ItemStack expected) {
        int count = 0;
        for (ItemStack stack : player.inventory.mainInventory)
            if (stack != null && stack.isItemEqual(expected)) count += stack.stackSize;
        return count;
    }

    private int countWorld(ItemStack expected, int x, int y, int z, int rx, int below, int above, int rz) {
        int count = 0;
        for (int dx = -rx; dx <= rx; dx++) for (int dy = -below; dy <= above; dy++) for (int dz = -rz; dz <= rz; dz++) {
            ItemStack stack = stackAt(x + dx, y + dy, z + dz);
            if (stack != null && stack.isItemEqual(expected)) count++;
        }
        return count;
    }

    private ItemStack stackAt(int x, int y, int z) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof IGregTechTileEntity gt && gt.getMetaTileEntity() != null)
            return gt.getMetaTileEntity().getStackForm(1);
        Block block = world.getBlock(x, y, z);
        return block.isAir(world, x, y, z) ? null
            : new ItemStack(block, 1, block.damageDropped(world.getBlockMetadata(x, y, z)));
    }

    private int blocksExceptController(int x, int y, int z, int rx, int below, int above, int rz) {
        int count = 0;
        for (int dx = -rx; dx <= rx; dx++) for (int dy = -below; dy <= above; dy++) for (int dz = -rz; dz <= rz; dz++)
            if ((dx != 0 || dy != 0 || dz != 0) && !world.isAirBlock(x + dx, y + dy, z + dz)) count++;
        return count;
    }

    private void clear(int x, int y, int z, int rx, int below, int above, int rz) {
        for (int chunkX = (x - rx) >> 4; chunkX <= (x + rx) >> 4; chunkX++)
            for (int chunkZ = (z - rz) >> 4; chunkZ <= (z + rz) >> 4; chunkZ++)
                world.getChunkFromChunkCoords(chunkX, chunkZ);
        for (int dx = -rx; dx <= rx; dx++) for (int dy = -below; dy <= above; dy++)
            for (int dz = -rz; dz <= rz; dz++) world.setBlockToAir(x + dx, y + dy, z + dz);
    }

    @SuppressWarnings("unchecked")
    private boolean operate(ItemStack terminal, int x, int y, int z) {
        try {
            Field cooldown = TerminalOperations.class.getDeclaredField("lastUse");
            cooldown.setAccessible(true);
            ((Map<EntityPlayerMP, Long>) cooldown.get(null)).remove(player);
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("cannot reset this QA player's operation interval", exception);
        }
        return TerminalOperations.execute(terminal, player, world, x, y, z, ForgeDirection.NORTH.ordinal());
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("TERMINAL_NATIVE_STRUCTURES_QA: PASS " + message);
    }
}
