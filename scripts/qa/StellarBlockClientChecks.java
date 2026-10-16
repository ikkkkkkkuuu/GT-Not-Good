package com.xyp.gtnotgood.qa;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.CraftingManager;
import net.minecraft.item.crafting.FurnaceRecipes;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.ScreenShotHelper;
import net.minecraft.util.StringUtils;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.xyp.gtnotgood.client.GTNGCreativeTabs;
import com.xyp.gtnotgood.common.blocks.stellar.BlockStellarPanel;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.common.registry.GameRegistry;
import gregtech.api.GregTechAPI;
import gregtech.api.interfaces.ITexture;
import gregtech.api.recipe.RecipeMap;
import gregtech.api.util.GTRecipe;
import gregtech.common.render.IIconTexture;

@Mod(
    modid = "stellarblockqa",
    name = "Stellar Block QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class StellarBlockClientChecks {

    private static final String[] IDS = { "stellar_casing", "stellar_casing_2", "stellar_casing_3", "stellar_casing_4",
        "stellar_glass", "stellar_fuel_column", "stellar_rod_loading_port", "stellar_turbine_housing",
        "stellar_controller_panel", "stellar_controller_panel_2" };
    private static final int[][] METADATA = { { 1, 2, 3, 4, 5, 6, 7, 8, 10, 11, 12, 13, 14, 15 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 9 }, { 0, 1 }, { 0, 1 }, { 0 }, { 0, 1 },
        { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 }, { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 } };
    private final List<ItemStack> variants = new ArrayList<>();
    private final List<String> names = new ArrayList<>();
    private final Set<Item> blockItems = new HashSet<>();
    private volatile boolean serverReady;
    private volatile boolean finished;
    private volatile Throwable serverFailure;
    private boolean started;
    private int stage;
    private int ticks;
    private int frames;
    private int checks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.stellarBlock.qa")) FMLCommonHandler.instance().bus().register(this);
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
            if (serverFailure != null) throw new AssertionError("Server checks failed", serverFailure);
            if (++ticks > 3600) throw new AssertionError("Client timed out at stage " + stage);
            mc.gameSettings.pauseOnLostFocus = false;
            if (!started && mc.theWorld == null && mc.currentScreen instanceof GuiMainMenu) {
                started = true;
                checkRegistryAndTextures();
                checkRecipes();
                WorldSettings settings = new WorldSettings(91L, WorldSettings.GameType.CREATIVE, false, false,
                    WorldType.FLAT).enableCommands();
                mc.launchIntegratedServer("stellar-block-qa-" + System.currentTimeMillis(), "Stellar Blocks QA",
                    settings);
            }
            if (serverReady && stage == 0 && mc.thePlayer != null && mc.theWorld != null) {
                for (int index = 0; index < variants.size(); index++) {
                    ItemStack stack = variants.get(index);
                    if (mc.theWorld.getBlock(x(index), y(index), 0) != Block.getBlockFromItem(stack.getItem())) return;
                    if (mc.theWorld.getBlockMetadata(x(index), y(index), 0) != stack.getItemDamage()) return;
                }
                require(true, "all 82 placed variants and metadata synchronize to the real client world");
                mc.displayGuiScreen(null);
                mc.gameSettings.hideGUI = true;
                mc.gameSettings.thirdPersonView = 0;
                mc.thePlayer.rotationYaw = mc.thePlayer.prevRotationYaw = 0;
                mc.thePlayer.rotationPitch = mc.thePlayer.prevRotationPitch = 0;
                advance(1);
            }
        } catch (Throwable error) {
            fail(error);
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
            world.setWorldTime(6000L);
            world.getWorldInfo().setRaining(false);
            for (int x = -3; x <= 24; x++) {
                for (int z = -3; z <= 3; z++) {
                    world.setBlock(x, 68, z, Blocks.stone, 0, 2);
                    for (int y = 69; y <= 87; y++) world.setBlockToAir(x, y, z);
                }
            }
            for (int index = 0; index < variants.size(); index++) {
                ItemStack stack = variants.get(index);
                Block block = Block.getBlockFromItem(stack.getItem());
                int meta = stack.getItemDamage();
                require(((ItemBlock) stack.getItem()).getMetadata(meta) == meta,
                    label(index) + " ItemBlock preserves its placement metadata");
                require(((ItemBlock) stack.getItem()).placeBlockAt(stack.copy(), player, world, x(index), y(index), 0,
                    1, 0.5F, 0.5F, 0.5F, meta), label(index) + " places through the real ItemBlock");
                require(!block.hasTileEntity(meta) && world.getTileEntity(x(index), y(index), 0) == null,
                    label(index) + " has no tile entity");
                require(GregTechAPI.isMachineBlock(block, meta) == (block == find("stellar_casing_2") && meta == 7),
                    label(index) + " registers GT structure updates only for the Eddington wall");
                List<ItemStack> drops = block.getDrops(world, x(index), y(index), 0, meta, 0);
                require(drops.size() == 1 && drops.get(0).getItem() == stack.getItem()
                    && drops.get(0).getItemDamage() == meta
                    && drops.get(0).stackSize == 1, label(index) + " drops exactly its own appearance variant");
                int expectedLight = block == find("stellar_casing_3") && meta == 5 ? 9
                    : block == find("stellar_casing_4") && meta >= 1 && meta <= 4 ? 7 : 0;
                require(block.getLightValue(world, x(index), y(index), 0) == expectedLight,
                    label(index) + " emits the upstream light level " + expectedLight);
            }
            Block glass = find("stellar_glass");
            world.setBlock(20, 70, 0, glass, 0, 2);
            world.setBlock(21, 70, 0, glass, 0, 2);
            world.setBlock(22, 70, 0, glass, 1, 2);
            require(!glass.isOpaqueCube() && !glass.renderAsNormalBlock() && glass.getRenderBlockPass() == 1,
                "both glasses use the translucent render pass");
            require(!glass.func_149730_j(), "glass keeps the cached opaque flag false after superclass construction");
            require(glass.getLightOpacity(world, 20, 70, 0) == 0, "glass transmits skylight with zero light opacity");
            require(
                !glass.shouldSideBeRendered(world, 21, 70, 0, 5) && !glass.shouldSideBeRendered(world, 22, 70, 0, 5),
                "glass hides internal faces between equal and different glass metadata");
            require(glass.shouldSideBeRendered(world, 20, 70, -1, 2), "glass renders its exterior face toward air");
            checkHole(world, find("stellar_casing"), 0);
            checkHole(world, find("stellar_casing"), 9);
            checkHole(world, find("stellar_casing_4"), 8);
            player.capabilities.allowFlying = true;
            player.capabilities.isFlying = true;
            player.sendPlayerAbilities();
            player.setPositionAndRotation(10.5, 76.4, -23, 0, 0);
            player.setPositionAndUpdate(10.5, 76.4, -23);
            serverReady = true;
        } catch (Throwable error) {
            serverFailure = error;
        }
    }

    @SubscribeEvent
    public void render(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished || stage == 0) return;
        try {
            if (serverFailure != null) throw new AssertionError("Server checks failed", serverFailure);
            if (++frames < 80) return;
            Minecraft mc = Minecraft.getMinecraft();
            if (stage == 1) {
                screenshot("stellar-blocks-world.png");
                mc.gameSettings.hideGUI = false;
                mc.gameSettings.guiScale = 1;
                mc.displayGuiScreen(new VariantMatrixGui(0, 54, "群星之外 · 54 种方块外观"));
                advance(2);
            } else if (stage == 2) {
                screenshot("stellar-blocks-matrix.png");
                mc.displayGuiScreen(new VariantMatrixGui(54, 82, "群星之外 · 28 种控制器装饰外观"));
                advance(3);
            } else if (stage == 3) {
                screenshot("stellar-controller-panels-matrix.png");
                finish("PASS; " + checks + " checks; 82 variants");
            }
        } catch (Throwable error) {
            fail(error);
        }
    }

    private void checkRegistryAndTextures() throws Exception {
        Minecraft mc = Minecraft.getMinecraft();
        List<String> manifest = new ArrayList<>();
        manifest.add("Registry\tMetadata\tChinese name\tIcon");
        Set<String> uniqueNames = new HashSet<>();
        for (int group = 0; group < IDS.length; group++) {
            Block block = find(IDS[group]);
            require(block != null && block != Blocks.air, IDS[group] + " is registered");
            Item item = Item.getItemFromBlock(block);
            require(item instanceof ItemBlock, IDS[group] + " has a registered ItemBlock");
            require(block.getCreativeTabToDisplayOn() == GTNGCreativeTabs.GTNGItemBlock,
                IDS[group] + " uses the project's block creative tab");
            List<ItemStack> subBlocks = new ArrayList<>();
            block.getSubBlocks(item, GTNGCreativeTabs.GTNGItemBlock, subBlocks);
            Set<Integer> actual = new HashSet<>();
            for (ItemStack stack : subBlocks) actual.add(stack.getItemDamage());
            require(subBlocks.size() == METADATA[group].length && actual.size() == METADATA[group].length,
                IDS[group] + " lists exactly its expected appearance variants");
            blockItems.add(item);
            for (int meta : METADATA[group]) {
                require(actual.contains(meta), IDS[group] + ":" + meta + " retains the upstream metadata");
                ItemStack stack = new ItemStack(item, 1, meta);
                String name = StringUtils.stripControlCodes(stack.getDisplayName());
                require(name.matches(".*[\\u4e00-\\u9fff].*") && !name.contains(".name"),
                    IDS[group] + ":" + meta + " resolves to Chinese: " + name);
                require(uniqueNames.add(name), IDS[group] + ":" + meta + " has a distinct localized name");
                for (int side = 0; side < 6; side++) {
                    IIcon icon = block.getIcon(side, meta);
                    require(
                        icon instanceof TextureAtlasSprite && !icon.getIconName().contains("missing")
                            && mc.getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon
                            && icon.getIconWidth() > 0
                            && icon.getIconHeight() > 0,
                        IDS[group] + ":" + meta + " face " + side + " has a stitched texture");
                    String path = icon.getIconName();
                    int colon = path.indexOf(':');
                    ResourceLocation resource = new ResourceLocation(path.substring(0, colon),
                        "textures/blocks/" + path.substring(colon + 1) + ".png");
                    try (var stream = mc.getResourceManager().getResource(resource).getInputStream()) {
                        require(stream.read() == 137, IDS[group] + ":" + meta + " face " + side + " loads its PNG");
                    }
                }
                if (block instanceof BlockStellarPanel) checkPanel((BlockStellarPanel) block, meta);
                variants.add(stack);
                names.add(name);
                manifest.add(ModList.ModIds.GT_NOT_GOOD + ":"
                    + IDS[group]
                    + "\t"
                    + meta
                    + "\t"
                    + name
                    + "\t"
                    + block.getIcon(2, meta).getIconName());
            }
        }
        require(variants.size() == 82, "all ten block registrations expose 82 creative appearance variants");
        Files.write(new File(output(), "variants.tsv").toPath(), manifest, StandardCharsets.UTF_8);
    }

    private void checkRecipes() {
        for (Object value : CraftingManager.getInstance().getRecipeList()) {
            requireUnrelated(((IRecipe) value).getRecipeOutput(), "crafting output");
        }
        for (Object value : FurnaceRecipes.smelting().getSmeltingList().values()) {
            requireUnrelated((ItemStack) value, "smelting output");
        }
        for (RecipeMap<?> map : RecipeMap.ALL_RECIPE_MAPS.values()) {
            for (GTRecipe recipe : map.getAllRecipes()) {
                for (ItemStack input : recipe.mInputs) requireUnrelated(input, map.unlocalizedName + " input");
                for (ItemStack output : recipe.mOutputs) requireUnrelated(output, map.unlocalizedName + " output");
            }
        }
        require(true, "no crafting, smelting or GregTech processing recipe uses the imported blocks");
    }

    private void checkPanel(BlockStellarPanel block, int meta) throws Exception {
        ITexture[][] textures = block.getTextures(meta);
        String expected = block.getVariant(meta).getOverlayTexture();
        ITexture[][] inventoryTextures = block.getInventoryTextures(meta);
        int north = ForgeDirection.NORTH.ordinal();
        int west = ForgeDirection.WEST.ordinal();
        require(inventoryTextures[west] == textures[north] && inventoryTextures[north] == textures[west],
            expected + " presents its front on the visible inventory face while keeping the world front north");
        for (int side = 0; side < 6; side++) {
            require(textures[side].length == (side == 2 ? 3 : 1),
                expected + " keeps the complete casing with front-only overlay and glow");
            require(textures[side][0] == textures[2][0], expected + " shares its casing across all faces");
        }
        for (int layer = 1; layer <= 2; layer++) {
            IIcon icon = ((IIconTexture) textures[2][layer]).getIcon(2, null);
            String name = expected + (layer == 2 ? "_GLOW" : "");
            require(
                icon.getIconName().endsWith("/" + name)
                    && Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(icon.getIconName()) == icon,
                name + " is uploaded to the real texture atlas");
            if (expected.endsWith("_ACTIVE")) {
                require(((TextureAtlasSprite) icon).hasAnimationMetadata(), name + " retains its upstream animation");
            }
        }
        Field glow = textures[2][2].getClass().getDeclaredField("glow");
        glow.setAccessible(true);
        require(glow.getBoolean(textures[2][2]), expected + " uses full-bright rendering for its glow layer");
    }

    private void requireUnrelated(ItemStack stack, String source) {
        if (stack != null && blockItems.contains(stack.getItem())) {
            throw new AssertionError("Imported block participates in " + source + ": " + stack);
        }
    }

    private void checkHole(World world, Block block, int meta) {
        world.setBlock(24, 70, 0, block, meta, 2);
        require(block.getDrops(world, 24, 70, 0, meta, 0).isEmpty(),
            block.getUnlocalizedName() + ":" + meta + " reserved metadata drops no invented variant");
        require(block.getIcon(2, meta) != null && !block.getIcon(2, meta).getIconName().contains("missing"),
            block.getUnlocalizedName() + ":" + meta + " reserved metadata has a safe texture fallback");
        world.setBlockToAir(24, 70, 0);
    }

    private static Block find(String id) {
        return GameRegistry.findBlock(ModList.ModIds.GT_NOT_GOOD, id);
    }

    private String label(int index) {
        return names.get(index) + ":" + variants.get(index).getItemDamage();
    }

    private static int x(int index) {
        return index % 11 * 2;
    }

    private static int y(int index) {
        return 70 + (7 - index / 11) * 2;
    }

    private final class VariantMatrixGui extends GuiScreen {

        private final RenderItem renderer = new RenderItem();
        private final int start;
        private final int end;
        private final String title;

        private VariantMatrixGui(int start, int end, String title) {
            this.start = start;
            this.end = end;
            this.title = title;
        }

        @Override
        public boolean doesGuiPauseGame() {
            return false;
        }

        @Override
        public void drawScreen(int mouseX, int mouseY, float partialTick) {
            drawRect(0, 0, width, height, 0xFF171E28);
            drawCenteredString(fontRendererObj, title, width / 2, 22, 0xFFFFFFFF);
            drawCenteredString(fontRendererObj, "原生物品模型与中文名称", width / 2, 40, 0xFFA7B9CA);
            int columns = end - start == 28 ? 7 : 9;
            int rows = (end - start + columns - 1) / columns;
            int cellWidth = (width - 56) / columns;
            int cellHeight = (height - 94) / rows;
            for (int index = start; index < end; index++) {
                int left = 28 + (index - start) % columns * cellWidth;
                int top = 64 + (index - start) / columns * cellHeight;
                drawRect(left + 3, top + 3, left + cellWidth - 3, top + cellHeight - 3, 0xFF293646);
                GL11.glPushMatrix();
                GL11.glTranslatef(left + cellWidth / 2F - 24, top + 12, 0);
                GL11.glScalef(3, 3, 3);
                RenderHelper.enableGUIStandardItemLighting();
                renderer.renderItemAndEffectIntoGUI(fontRendererObj, mc.getTextureManager(), variants.get(index), 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
                fontRendererObj.drawSplitString(names.get(index), left + 10, top + 73, cellWidth - 20, 0xFFE8EFF7);
                drawString(fontRendererObj, "#" + (index + 1) + "  meta " + variants.get(index).getItemDamage(),
                    left + 10, top + cellHeight - 22, 0xFF9DB0C4);
            }
        }
    }

    private void advance(int next) {
        stage = next;
        frames = 0;
    }

    private static void screenshot(String name) {
        Minecraft mc = Minecraft.getMinecraft();
        ScreenShotHelper.saveScreenshot(output(), name, mc.displayWidth, mc.displayHeight, mc.getFramebuffer());
    }

    private static File output() {
        return new File(System.getProperty("gtng.stellarBlock.qa.output", "."));
    }

    private synchronized void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
        System.out.println("STELLAR_BLOCK_QA: " + message);
    }

    private void fail(Throwable error) {
        error.printStackTrace();
        finish("FAIL; stage " + stage + "; " + error);
    }

    private void finish(String result) {
        try {
            Files.write(new File(output(), "result.txt").toPath(), result.getBytes(StandardCharsets.UTF_8));
        } catch (Exception error) {
            error.printStackTrace();
        }
        System.out.println("STELLAR_BLOCK_QA: " + result);
        finished = true;
    }
}
