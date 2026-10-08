package com.xyp.gtnotgood.client.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;

import com.cleanroommc.modularui.ModularUIConfig;
import com.cleanroommc.modularui.api.widget.IWidget;
import com.cleanroommc.modularui.screen.GuiContainerWrapper;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.xyp.gtnotgood.common.machines.multiblock.LargeTransmutationMachine;
import com.xyp.gtnotgood.common.recipe.gtnotgood.ShimmerRecoveryRules;
import com.xyp.gtnotgood.loader.GTNGRecipeMaps;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;
import com.xyp.ldlib.integration.modularui.LDLibModularScreen;

import bartworks.common.loaders.ItemRegistry;
import codechicken.nei.recipe.GuiCraftingRecipe;
import codechicken.nei.recipe.GuiRecipe;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Materials;
import gregtech.api.enums.OrePrefixes;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.structure.error.StructureError;
import gregtech.api.util.GTOreDictUnificator;
import gregtech.api.util.GTRecipeBuilder;

/** Actual integrated-server disassembly and client rendering checks, enabled only by the QA init script. */
@Mod(
    modid = "transmutationqa",
    name = "Transmutation QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class TransmutationClientChecks {

    private boolean started;
    private int textureReloads;
    private int ticks, frames, stage, serverTicks;
    private volatile LargeTransmutationMachine machine;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.transmutation.qa")) {
            ModularUIConfig.guiDebugMode = false;
            FMLCommonHandler.instance().bus().register(this);
        }
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (++ticks > 6000) throw new AssertionError("TRANSMUTATION_QA_TIMEOUT stage=" + stage);
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer("transmutation-qa-" + System.currentTimeMillis(), "Transmutation QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) throws Exception {
        if (event.phase != TickEvent.Phase.END) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
        if (machine == null) {
            World world = server.worldServerForDimension(0);
            world.setWorldTime(6000);
            var shapeField = LargeTransmutationMachine.class.getDeclaredField("SHAPE");
            shapeField.setAccessible(true);
            String[][] shape = (String[][]) shapeField.get(null);
            for (int y = 0; y < shape.length; y++)
                for (int z = 0; z < shape[y].length; z++) for (int x = 0; x < shape[y][z].length(); x++) {
                    int px = 2 - x, py = 8 - y;
                    switch (shape[y][z].charAt(x)) {
                        case 'C':
                            world.setBlock(px, py, z, GregTechAPI.sBlockCasings2, 0, 3);
                            break;
                        case 'F':
                            world.setBlock(px, py, z, GregTechAPI.sBlockFrames, Materials.Titanium.mMetaItemSubID, 3);
                            break;
                        case 'P':
                            world.setBlock(px, py, z, GregTechAPI.sBlockCasings2, 13, 3);
                            break;
                        case 'G':
                            world.setBlock(px, py, z, ItemRegistry.bw_realglas, 0, 3);
                            break;
                        case 'L':
                            world.setBlock(px, py, z, Blocks.glowstone, 0, 3);
                            break;
                        default:
                            world.setBlockToAir(px, py, z);
                    }
                }
            BaseMetaTileEntity base = place(world, 0, 6, 0, GTNGMachineID.LargeTransmutationMachine.id);
            base.setOwnerName(player.getCommandSenderName());
            base.setOwnerUuid(player.getUniqueID());
            base.disableWorking();
            machine = (LargeTransmutationMachine) base.getMetaTileEntity();
            place(world, -2, 6, 0, ItemList.Hatch_Energy_EV.get(1).getItemDamage());
            place(world, -1, 6, 0, ItemList.Hatch_Input_Bus_EV.get(1).getItemDamage());
            place(world, 1, 6, 0, ItemList.Hatch_Output_Bus_EV.get(1).getItemDamage());
            place(world, 2, 6, 0, ItemList.Hatch_Output_EV.get(1).getItemDamage());
            place(world, -1, 8, 0, ItemList.Hatch_Maintenance.get(1).getItemDamage());
            player.playerNetServerHandler.setPlayerLocation(-9, 9, -10, -36, 15);
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
        }
        if (++serverTicks == 60) {
            ArrayList<StructureError> errors = new ArrayList<>();
            // GT clears hatch lists before rechecking; mirror that contract for this direct assertion.
            machine.mInputBusses.clear();
            machine.mOutputBusses.clear();
            machine.mOutputHatches.clear();
            machine.mEnergyHatches.clear();
            machine.mMaintenanceHatches.clear();
            machine.checkMachine(machine.getBaseMetaTileEntity(), null, errors);
            require(errors.isEmpty(), "Structure: "
                + errors.stream().map(StructureError::getDisplayString).collect(Collectors.joining("; ")));
            checkRecovery();
            checkProcessing();
            System.out.println(
                "TRANSMUTATION_QA_SERVER_PASS recipes=" + GTNGRecipeMaps.TransmutationRecipes.getAllRecipes().size());
        }
        if (serverTicks == 140) {
            player.playerNetServerHandler.setPlayerLocation(1, 7, -4, 0, 0);
            machine.onRightclick(machine.getBaseMetaTileEntity(), player);
        }
    }

    private static BaseMetaTileEntity place(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity base = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        base.setInitialValuesAsNBT(null, (short) id);
        base.setFrontFacing(ForgeDirection.NORTH);
        return base;
    }

    private static void checkRecovery() {
        require(ShimmerRecoveryRules.getUnprocessedMaterials(Materials.SteelMagnetic) == Materials.Steel,
            "Shimmer magnetic material rule");
    }

    private void checkProcessing() {
        require(!GTNGRecipeMaps.TransmutationRecipes.getAllRecipes().isEmpty(), "Missing generated recipes");

        for (var recipe : GTNGRecipeMaps.TransmutationRecipes.getAllRecipes()) {
            var inputData = GTOreDictUnificator.getItemData(recipe.mInputs[0]);
            if (inputData == null || inputData.mPrefix != OrePrefixes.nugget) continue;
            for (ItemStack output : recipe.mOutputs) {
                var outputData = GTOreDictUnificator.getItemData(output);
                require(outputData == null || outputData.mPrefix != OrePrefixes.ingot,
                    "Nuggets compressed into ingot by disassembly");
            }
        }
        ItemStack fixture = new ItemStack(Items.paper, 2);
        fixture.setStackDisplayName("Transmutation QA fixture");
        GTRecipeBuilder.builder().itemInputs(fixture).itemOutputs(new ItemStack(Items.stick, 3))
            .fluidOutputs(new FluidStack(FluidRegistry.WATER, 250)).nbtSensitive().duration(100).eut(1920)
            .addTo(GTNGRecipeMaps.TransmutationRecipes);
        ItemStack mismatch = fixture.copy();
        mismatch.setStackDisplayName("Wrong NBT");
        machine.mInputBusses.get(0).setInventorySlotContents(0, mismatch);
        require(!machine.checkProcessing().wasSuccessful(), "Wrong NBT accepted");
        require(mismatch.stackSize == 2, "Wrong NBT consumed");
        ItemStack input = fixture.copy();
        input.stackSize = 1;
        machine.mInputBusses.get(0).setInventorySlotContents(0, input);
        require(!machine.checkProcessing().wasSuccessful(), "Partial batch consumed");
        require(input.stackSize == 1, "Partial batch changed");
        input.stackSize = 3;
        var result = machine.checkProcessing();
        require(result.wasSuccessful(), "Valid processing failed: " + result.getID());
        require(input.stackSize == 1, "Batch remainder lost");
        require(machine.mOutputItems[0].stackSize == 3, "Wrong item yield");
        require(machine.mOutputFluids[0].amount == 250, "Wrong fluid yield");
        // Fill all output slots and ensure the protected transaction does not consume input.
        var bus = machine.mOutputBusses.get(0);
        for (int i = 0; i < bus.getSizeInventory(); i++)
            bus.setInventorySlotContents(i, new ItemStack(Items.diamond, 64));
        input.stackSize = 2;
        require(!machine.checkProcessing().wasSuccessful(), "Full output accepted");
        require(input.stackSize == 2, "Full output consumed input");
        machine.mInputBusses.get(0).setInventorySlotContents(0, null);
        machine.mOutputItems = null;
        machine.mOutputFluids = null;
        machine.mMaxProgresstime = 0;
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (stage == 5 && mc.currentScreen instanceof GuiRecipe) {
            if (++frames < 60) return;
            capture("nei-recipe");
            System.out.println(
                "TRANSMUTATION_QA_PASS structure; partial batches; NBT; output protection; item/fluid recovery; Shimmer rules; standard GUI without browser button; NEI");
            mc.shutdown();
            stage = 6;
            return;
        }
        if (machine != null && serverTicks > 80 && serverTicks < 130 && stage == 0) {
            capture("structure");
            stage = 1;
        }
        if (!(mc.currentScreen instanceof GuiContainerWrapper wrapper)) return;
        if (++frames < 60) return;
        frames = 0;
        require(!(wrapper.getScreen() instanceof LDLibModularScreen), "Unexpected LDLib GUI");
        require(findButton(wrapper.getScreen().getMainPanel()) == null, "Obsolete browser button");
        if (Boolean.getBoolean("gtng.transmutation.qa.reload") && textureReloads++ < 3) {
            mc.refreshResources();
            return;
        }
        capture("primary");
        require(GuiCraftingRecipe.openRecipeGui("recipe.gtnotgood.transmutation"), "NEI recipe page not found");
        stage = 5;
    }

    private static ButtonWidget<?> findButton(IWidget widget) {
        if (
            widget instanceof ButtonWidget<?>button && button.isSynced()
                && button.getSyncHandler().getKey().startsWith("transmutation.browser")
        ) return button;
        for (var child : widget.getChildren()) {
            var found = findButton(child);
            if (found != null) return found;
        }
        return null;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static void capture(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        File output = new File(System.getProperty("gtng.transmutation.qa.output"));
        output.mkdirs();
        ScreenShotHelper.saveScreenshot(output, name + ".png", mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }
}
