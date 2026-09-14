package com.xyp.gtnotgood.common.compat;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraftforge.fluids.FluidStack;

import com.xyp.gtnotgood.ae2thing.nei.QuickTerminalRecipeTransferHandler;
import com.xyp.gtnotgood.ae2thing.quickterminal.ContainerQuickEncodingTerminal;
import com.xyp.gtnotgood.ae2thing.quickterminal.DualTerminalGuiObject;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeIngredientReplacement;
import com.xyp.gtnotgood.ae2thing.quickterminal.RecipeTransferPayload;
import com.xyp.gtnotgood.utils.enums.GTNGItemList;

import appeng.api.AEApi;
import appeng.api.implementations.ICraftingPatternItem;
import appeng.api.networking.IGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEStack;
import appeng.helpers.WirelessTerminalGuiObject;
import appeng.tile.networking.TileWireless;
import appeng.util.item.AEFluidStack;
import appeng.util.item.AEItemStack;
import codechicken.nei.PositionedStack;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import gregtech.api.enums.Materials;
import gregtech.api.util.GTModHandler;
import gregtech.api.util.GTUtility;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/** Opt-in Forge check of quick-terminal NEI choices, packet serialization, slot updates and encoded pattern NBT. */
@Mod(
    modid = "ingredientqa",
    name = "Recipe Ingredient QA",
    version = "1",
    dependencies = "after:" + com.xyp.gtnotgood.utils.enums.ModList.ModIds.GT_NOT_GOOD)
public class RecipeIngredientClientChecks {

    private boolean started;
    private volatile boolean finished;
    private int exitTicks;

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        if (Boolean.getBoolean("gtng.ingredient.qa")) FMLCommonHandler.instance()
            .bus()
            .register(this);
    }

    @SubscribeEvent
    public void client(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (finished && ++exitTicks == 60) mc.shutdown();
        if (!started && mc.theWorld == null && mc.currentScreen != null) {
            started = true;
            mc.launchIntegratedServer(
                "ingredient-qa-" + System.currentTimeMillis(),
                "Ingredient QA",
                new WorldSettings(24L, WorldSettings.GameType.CREATIVE, false, false, WorldType.FLAT));
        }
    }

    @SubscribeEvent
    public void server(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || finished) return;
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance()
            .getMinecraftServerInstance();
        if (server.getConfigurationManager().playerEntityList.isEmpty()) return;
        try {
            check((EntityPlayerMP) server.getConfigurationManager().playerEntityList.get(0));
            System.out.println(
                "INGREDIENT_QA: PASS (NEI fluid quantities, packet roundtrip, quick terminal, encoded pattern)");
        } catch (Throwable error) {
            error.printStackTrace();
            System.out.println("INGREDIENT_QA: FAILED");
        } finally {
            finished = true;
        }
    }

    private static void check(EntityPlayerMP player) throws Exception {
        ItemStack terminal = GTNGItemList.WirelessDualInterfaceTerminal.get(1);
        player.inventory.setInventorySlotContents(0, terminal);
        DualTerminalGuiObject host = new DualTerminalGuiObject(
            AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(terminal),
            terminal,
            player,
            player.worldObj,
            0);
        // Supply a real AE grid; wireless binding and range are outside this encoding check.
        player.worldObj.setBlock(
            0,
            6,
            0,
            AEApi.instance()
                .definitions()
                .blocks()
                .wireless()
                .maybeBlock()
                .get());
        TileWireless accessPoint = (TileWireless) player.worldObj.getTileEntity(0, 6, 0);
        accessPoint.onReady();
        IGrid grid = accessPoint.getActionableNode()
            .getGrid();
        for (String name : new String[] { "targetGrid", "sg", "myWap" }) {
            Field field = WirelessTerminalGuiObject.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(
                host,
                name.equals("targetGrid") ? grid : name.equals("sg") ? grid.getCache(IStorageGrid.class) : accessPoint);
        }
        ContainerQuickEncodingTerminal container = new ContainerQuickEncodingTerminal(player.inventory, host);
        FluidStack[] fluids = { Materials.Water.getFluid(37), GTModHandler.getDistilledWater(28),
            Materials.Lubricant.getFluid(9) };
        ItemStack[] displays = new ItemStack[fluids.length];
        for (int i = 0; i < displays.length; i++) displays[i] = GTUtility.getFluidDisplayStack(fluids[i], true);
        PositionedStack alternatives = new PositionedStack(displays, 0, 0);
        IAEStack<?> current = AEFluidStack.create(fluids[0]);
        invoke(
            container,
            "applyRecipeTransfer",
            RecipeTransferPayload.class,
            new RecipeTransferPayload(
                false,
                false,
                4,
                false,
                new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.apple)), current },
                new IAEStack<?>[] { AEItemStack.create(new ItemStack(Items.feather, 16)) }));
        for (int choice : new int[] { 1, 2, 0 }) {
            Method adjacent = QuickTerminalRecipeTransferHandler.class.getDeclaredMethod(
                "findAdjacentAlternative",
                PositionedStack.class,
                IAEStack.class,
                int.class,
                boolean.class);
            adjacent.setAccessible(true);
            IAEStack<?> target = (IAEStack<?>) adjacent.invoke(null, alternatives, current, 1, false);
            require(target.getStackSize() == fluids[choice].amount, "NEI alternative owns its quantity");
            RecipeIngredientReplacement message = new RecipeIngredientReplacement(current, target);
            ByteBuf buffer = Unpooled.buffer();
            try {
                invokeStatic(
                    RecipeIngredientReplacement.class,
                    "write",
                    new Class<?>[] { ByteBuf.class, RecipeIngredientReplacement.class },
                    buffer,
                    message);
                message = (RecipeIngredientReplacement) invokeStatic(
                    RecipeIngredientReplacement.class,
                    "read",
                    new Class<?>[] { ByteBuf.class },
                    buffer);
            } finally {
                buffer.release();
            }
            require(
                message.getTo()
                    .getStackSize() == fluids[choice].amount,
                "packet preserves selected quantity");
            invoke(container, "applyRecipeIngredient", RecipeIngredientReplacement.class, message);
            Slot blank = (Slot) field(container, "blankPatternSlot");
            Slot encoded = (Slot) field(container, "encodedPatternSlot");
            encoded.putStack(null);
            blank.putStack(
                AEApi.instance()
                    .definitions()
                    .materials()
                    .blankPattern()
                    .maybeStack(1)
                    .get());
            Method encode = ContainerQuickEncodingTerminal.class.getDeclaredMethod("quickEncode");
            encode.setAccessible(true);
            encode.invoke(container);
            ItemStack pattern = encoded.getStack();
            require(pattern != null && pattern.getItem() instanceof ICraftingPatternItem, "pattern encoded");
            IAEStack<?>[] inputs = ((ICraftingPatternItem) pattern.getItem())
                .getPatternForItem(pattern, player.worldObj)
                .getAEInputs();
            boolean found = false;
            for (IAEStack<?> stack : inputs) if (stack instanceof IAEFluidStack fluid) {
                require(
                    fluid.getFluidStack()
                        .isFluidEqual(fluids[choice]),
                    "encoded fluid identity");
                require(stack.getStackSize() == fluids[choice].amount, "encoded fluid quantity");
                found = true;
            }
            require(found, "encoded fluid present");
            System.out.println(
                "INGREDIENT_QA: encoded " + fluids[choice].getFluid()
                    .getName() + "=" + fluids[choice].amount);
            current = target;
        }
        container.onContainerClosed(player);
    }

    private static Object field(Object target, String name) throws Exception {
        Field field = target.getClass()
            .getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void invoke(Object target, String name, Class<?> type, Object value) throws Exception {
        Method method = target.getClass()
            .getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static Object invokeStatic(Class<?> type, String name, Class<?>[] args, Object... values) throws Exception {
        Method method = type.getDeclaredMethod(name, args);
        method.setAccessible(true);
        return method.invoke(null, values);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
