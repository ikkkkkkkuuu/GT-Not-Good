package com.xyp.gtnotgood.qa;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.IIcon;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.gtnewhorizon.structurelib.structure.IStructureElement;
import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.common.blocks.NativeStructureCasings;
import com.xyp.gtnotgood.common.blocks.StructureCasing;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.FieldExtruderFactory;
import com.xyp.gtnotgood.common.machines.multiblock.structure.FieldExtruderStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.HeartOfTheUniverseStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.LightHunterSpaceStationStructure;
import com.xyp.gtnotgood.common.machines.multiblock.structure.NexusSatelliteFactoryStructure;
import com.xyp.gtnotgood.utils.enums.GTNGMachineID;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.GregTechAPI;
import gregtech.api.enums.ItemList;
import gregtech.api.enums.Textures;
import gregtech.api.metatileentity.BaseMetaTileEntity;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.recipe.RecipeMaps;
import gregtech.api.util.GTRecipe;

@Mod(
    modid = "nativecasingqa",
    name = "Native Casing QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class NativeCasingClientChecks {

    private final List<StructureCasing> casings = new ArrayList<>(NativeStructureCasings.variants());
    private final List<String> evidence = new ArrayList<>();
    private volatile boolean serverReady;
    private volatile Throwable serverFailure;
    private volatile boolean finished;
    private boolean started;
    private int ticks;
    private int stage;
    private int frames;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.nativeCasing.qa")) FMLCommonHandler.instance().bus().register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished) {
            mc.shutdown();
            return;
        }
        try {
            if (serverFailure != null) throw new AssertionError("Server validation failed", serverFailure);
            if (++ticks > 2400) throw new AssertionError("QA timed out");
            if (!started && mc.theWorld == null && mc.currentScreen != null) {
                started = true;
                registry();
                recipes();
                mc.launchIntegratedServer("native-casings-" + System.currentTimeMillis(), "Native Casing QA",
                    new WorldSettings(91, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT)
                        .enableCommands());
            }
            if (serverReady && stage == 0 && mc.thePlayer != null && mc.theWorld != null) {
                for (int index = 0; index < casings.size(); index++) {
                    StructureCasing casing = casings.get(index);
                    if (
                        mc.theWorld.getBlock(x(index), y(index), 0) != casing.getStructureBlock()
                            || mc.theWorld.getBlockMetadata(x(index), y(index), 0) != casing.getStructureMetadata()
                    ) return;
                }
                require(true, "all native construction blocks synchronize to the client");
                mc.gameSettings.guiScale = 1;
                mc.displayGuiScreen(new MatrixGui());
                stage = 1;
                frames = 0;
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void registry() {
        require(casings.size() == 38, "38 confirmed native replacements");
        for (StructureCasing casing : casings) {
            ItemStack stack = casing.getStack(1);
            require(!same(stack, casing.getLegacyStack(1)), casing + " resolves to a different registered item");
            require(same(stack, casing.getItemContainer().get(1)), casing + " item container uses native identity");
            require(
                same(casing.getLegacyStack(1),
                    ItemStack.loadItemStackFromNBT(casing.getLegacyStack(1).writeToNBT(new NBTTagCompound()))),
                casing + " old item NBT remains readable");
            List<ItemStack> creative = new ArrayList<>();
            casing.getBlock().getSubBlocks(Item.getItemFromBlock(casing.getBlock()), GTNGCreativeTabs.GTNGItemBlock,
                creative);
            require(creative.stream().noneMatch(item -> same(item, casing.getLegacyStack(1))),
                casing + " old copy is hidden");
            require(Textures.BlockIcons.getCasingTextureForId(casing.getTextureId())
                == NativeStructureCasings.texture(casing), casing + " hatch texture matches native casing");
            for (int side = 0; side < 6; side++) {
                IIcon icon = casing.getStructureBlock().getIcon(side, casing.getStructureMetadata());
                require(icon != null && !icon.getIconName().contains("missing"), casing + " native face " + side);
            }
            evidence.add(casing + "\t"
                + Item.itemRegistry.getNameForObject(stack.getItem())
                + "\t"
                + stack.getItemDamage()
                + "\t"
                + stack.getDisplayName());
        }
    }

    private void recipes() {
        for (Object recipe : CraftingManager.getInstance().getRecipeList())
            checkOutput(((IRecipe) recipe).getRecipeOutput());
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                for (ItemStack output : recipe.mOutputs) checkOutput(output);
            }
        }
        for (GTRecipe.RecipeAssemblyLine recipe : GTRecipe.RecipeAssemblyLine.sAssemblylineRecipes)
            checkOutput(recipe.mOutput);
        for (StructureCasing casing : casings) {
            boolean found = false;
            for (GTRecipe recipe : RecipeMaps.assemblerRecipes.getAllRecipes()) {
                if (recipe.mInputs.length != 2 || recipe.mOutputs.length != 1) continue;
                if (same(recipe.mInputs[0], casing.getLegacyStack(1)) && same(recipe.mOutputs[0], casing.getStack(1))) {
                    require(recipe.mInputs[0].stackSize == 1 && recipe.mOutputs[0].stackSize == 1,
                        casing + " migration is exactly one to one");
                    found = true;
                }
            }
            require(found, casing + " old copy has a native conversion recipe");
        }
        require(true, "no crafting, processing or assembly line recipe produces a retired copy");
    }

    private void checkOutput(ItemStack output) {
        for (StructureCasing casing : casings) {
            if (same(output, casing.getLegacyStack(1)))
                throw new AssertionError("Old casing is still produced: " + casing);
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || serverReady) return;
        var server = FMLCommonHandler.instance().getMinecraftServerInstance();
        if (server == null || server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            EntityPlayerMP player = (EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0);
            World world = player.getServerForPlayer();
            world.getGameRules().setOrCreateGameRule("doDaylightCycle", "false");
            world.getGameRules().setOrCreateGameRule("doMobSpawning", "false");
            world.setWorldTime(6000);
            for (int index = 0; index < casings.size(); index++) {
                StructureCasing casing = casings.get(index);
                int x = x(index), y = y(index);
                IStructureElement<Object> element = casing.asElement();
                world.setBlock(x, y, 0, casing.getBlock(), casing.getMetadata(), 3);
                require(element.check(null, world, x, y, 0), casing + " existing structure accepts legacy block");
                world.setBlock(x, y, 0, Blocks.stone, 0, 3);
                require(!element.check(null, world, x, y, 0), casing + " wrong block invalidates structure");
                world.setBlockToAir(x, y, 0);
                require(element.placeBlock(null, world, x, y, 0, new ItemStack(Blocks.stone)), casing + " constructs");
                require(
                    world.getBlock(x, y, 0) == casing.getStructureBlock()
                        && world.getBlockMetadata(x, y, 0) == casing.getStructureMetadata(),
                    casing + " construction prefers native");
                require(element.check(null, world, x, y, 0), casing + " native block passes structure check");
            }
            for (char symbol : LightHunterSpaceStationStructure.SYMBOLS.toCharArray()) {
                world.setBlock(20, 70, 0, LightHunterSpaceStationStructure.block(symbol),
                    LightHunterSpaceStationStructure.metadata(symbol), 3);
                require(LightHunterSpaceStationStructure.element(symbol).check(null, world, 20, 70, 0),
                    "Light Hunter native symbol " + symbol);
            }
            for (char symbol : NexusSatelliteFactoryStructure.SYMBOLS.toCharArray()) {
                world.setBlock(20, 70, 0, NexusSatelliteFactoryStructure.block(symbol),
                    NexusSatelliteFactoryStructure.metadata(symbol), 3);
                require(NexusSatelliteFactoryStructure.element(symbol).check(null, world, 20, 70, 0),
                    "Nexus native symbol " + symbol);
            }
            for (char symbol : "ABCDEFGHPRST".toCharArray()) {
                world.setBlock(20, 70, 0, HeartOfTheUniverseStructure.block(symbol),
                    HeartOfTheUniverseStructure.metadata(symbol), 3);
                require(HeartOfTheUniverseStructure.element(symbol).check(null, world, 20, 70, 0),
                    "Heart native symbol " + symbol);
            }
            nativeFactory(world);
            player.setPositionAndUpdate(7, 72, -12);
            serverReady = true;
        } catch (Throwable error) {
            serverFailure = error;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || stage == 0 || finished || ++frames < 100) return;
        try {
            Minecraft mc = Minecraft.getMinecraft();
            ScreenShotHelper.saveScreenshot(output(), "native-casings-" + stage + ".png", mc.displayWidth,
                mc.displayHeight, mc.getFramebuffer());
            if (stage == 1) {
                mc.refreshResources();
                registry();
                stage = 2;
                frames = 0;
            } else {
                Files.write(new File(output(), "mappings.tsv").toPath(), evidence, StandardCharsets.UTF_8);
                finish("PASS; " + checks
                    + " checks; 38 replacements; structure, recipes, NBT, textures and resource reload");
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private final class MatrixGui extends GuiScreen {

        private final RenderItem renderer = new RenderItem();

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF171E28);
            drawCenteredString(fontRendererObj, "GTNH 原生结构件 · 38 项替换", width / 2, 12, 0xFFFFFFFF);
            int cellWidth = (width - 32) / 8, cellHeight = (height - 50) / 5;
            for (int index = 0; index < casings.size(); index++) {
                ItemStack stack = casings.get(index).getStack(1);
                int left = 16 + index % 8 * cellWidth, top = 36 + index / 8 * cellHeight;
                drawRect(left + 2, top + 2, left + cellWidth - 2, top + cellHeight - 2, 0xFF293646);
                GL11.glPushMatrix();
                GL11.glTranslatef(left + cellWidth / 2F - 24, top + 12, 0);
                GL11.glScalef(3, 3, 3);
                RenderHelper.enableGUIStandardItemLighting();
                renderer.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), stack, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
                fontRendererObj.drawSplitString(stack.getDisplayName(), left + 8, top + 65, cellWidth - 16, 0xFFE8EFF7);
            }
        }
    }

    private void nativeFactory(World world) {
        int rig = 0;
        for (ForgeDirection facing : new ForgeDirection[] { ForgeDirection.NORTH, ForgeDirection.EAST,
            ForgeDirection.SOUTH, ForgeDirection.WEST }) {
            int origin = 128 + rig++ * 144;
            for (int cx = (origin - 70) >> 4; cx <= (origin + 70) >> 4; cx++)
                for (int cz = 4; cz <= 12; cz++) world.getChunkFromChunkCoords(cx, cz);
            BaseMetaTileEntity base = placeMachine(world, origin, 40, 128, GTNGMachineID.FieldExtruderFactory.id);
            base.setFrontFacing(facing);
            FieldExtruderFactory machine = (FieldExtruderFactory) base.getMetaTileEntity();
            machine.construct(new ItemStack(Items.diamond), false);
            int[][] locations = { { 6, 0, 0 }, { 7, 0, 0 }, { 8, 0, 0 }, { 6, 1, 0 } };
            ItemList[] hatches = { ItemList.Hatch_Input_Bus_EV, ItemList.Hatch_Output_Bus_EV,
                ItemList.Hatch_Maintenance, ItemList.Hatch_Energy_EV };
            for (int index = 0; index < locations.length; index++) {
                int[] offset = new int[3];
                machine.getExtendedFacing()
                    .getWorldOffset(new int[] { locations[index][0] - FieldExtruderStructure.OFFSET_X,
                        locations[index][1] - FieldExtruderStructure.OFFSET_Y,
                        locations[index][2] - FieldExtruderStructure.OFFSET_Z }, offset);
                placeMachine(world, origin + offset[0], 40 + offset[1], 128 + offset[2],
                    hatches[index].get(1).getItemDamage());
            }
            require(machine.checkStructure(true, base), "full native Field Extruder forms facing " + facing);
            require(
                machine.mInputBusses.size() == 1 && machine.mOutputBusses.size() == 1
                    && machine.mEnergyHatches.size() == 1
                    && machine.mMaintenanceHatches.size() == 1,
                "native shell detects all four hatches facing " + facing);
            int[] offset = new int[3];
            machine.getExtendedFacing().getWorldOffset(new int[] { -1, 1, 0 }, offset);
            int bx = origin + offset[0], by = 40 + offset[1], bz = 128 + offset[2];
            Block block = world.getBlock(bx, by, bz);
            int meta = world.getBlockMetadata(bx, by, bz);
            require(block != Blocks.air, "chosen shell position exists");
            world.setBlockToAir(bx, by, bz);
            require(!machine.checkStructure(true, base),
                "full native structure fails when a casing is missing " + facing);
            world.setBlock(bx, by, bz, block, meta, 3);
            require(machine.checkStructure(true, base), "full native structure reforms after repair " + facing);
        }
    }

    private static BaseMetaTileEntity placeMachine(World world, int x, int y, int z, int id) {
        world.setBlock(x, y, z, GregTechAPI.sBlockMachines, 0, 3);
        BaseMetaTileEntity tile = (BaseMetaTileEntity) world.getTileEntity(x, y, z);
        tile.setInitialValuesAsNBT(null, (short) id);
        tile.setFrontFacing(ForgeDirection.NORTH);
        tile.disableWorking();
        return tile;
    }

    private static int x(int index) {
        return index % 8 * 2;
    }

    private static int y(int index) {
        return 70 + index / 8 * 2;
    }

    private static boolean same(ItemStack first, ItemStack second) {
        return first != null && second != null && first.isItemEqual(second);
    }

    private static File output() {
        return new File(System.getProperty("gtng.nativeCasing.output"));
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("NATIVE_CASING_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        finish("FAIL; " + error);
    }

    private void finish(String result) {
        try {
            Files.write(new File(output(), "run/native-casing-qa-result.txt").toPath(),
                result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("NATIVE_CASING_QA: " + result);
        finished = true;
    }
}
