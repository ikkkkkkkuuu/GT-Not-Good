package com.xyp.gtnotgood.qa;

import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.util.FakePlayerFactory;

import com.brandon3055.draconicevolution.common.ModItems;
import com.brandon3055.draconicevolution.common.items.armor.DraconicArmor;
import com.brandon3055.draconicevolution.common.items.armor.WyvernArmor;
import com.brandon3055.draconicevolution.common.network.ItemConfigPacket;
import com.brandon3055.draconicevolution.common.utils.IConfigurableItem;
import com.brandon3055.draconicevolution.common.utils.ItemConfigField;
import com.brandon3055.draconicevolution.common.utils.ItemNBTHelper;
import com.mojang.authlib.GameProfile;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLServerStartedEvent;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.Side;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

@Mod(
    modid = "draconicflightserverqa",
    name = "Draconic Flight Server QA",
    version = "1",
    dependencies = "after:" + ModList.ModIds.GT_NOT_GOOD)
public final class DraconicFlightServerChecks {

    private int checks;

    @Mod.EventHandler
    public void started(FMLServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("gtng.draconicflight.serverQA")) return;
        MinecraftServer server = MinecraftServer.getServer();
        Path result = Path.of(System.getProperty("gtng.draconicflight.qa.output"), "result.txt");
        try {
            require(server.isDedicatedServer(), "dedicated server runtime");
            verify(server);
            Files.write(result, "PASS".getBytes(StandardCharsets.UTF_8));
            System.out.println("DRACONIC_FLIGHT_SERVER_QA: PASS (" + checks + " checks)");
        } catch (Throwable failure) {
            Files.write(result, ("FAIL: " + failure).getBytes(StandardCharsets.UTF_8));
            throw new AssertionError("draconic flight server QA failed", failure);
        } finally {
            server.initiateShutdown();
        }
    }

    public int verify(MinecraftServer server) throws Exception {
        EntityPlayerMP player = FakePlayerFactory.get(server.worldServerForDimension(0),
            new GameProfile(UUID.fromString("d00fc40a-e995-4ba5-baf0-97ac00f7ca91"), "FlightQA"));
        player.playerNetServerHandler = new NetHandlerPlayServer(server, new NetworkManager(false), player);
        ItemStack chest = new ItemStack(ModItems.draconicChest);
        player.inventory.setInventorySlotContents(0, chest);
        ItemConfigField speed = field(chest, 0, "ArmorFlightSpeedMult");
        equal(10, speed.castMaxToDouble(), "chest flight limit");
        equal(0, speed.castMinToDouble(), "minimum preserved");
        equal(0.1, ((Number) speed.incroment).doubleValue(), "ten percent increment preserved");
        equal(0, speed.castToDouble(), "new chest default preserved");
        equal(8, field(chest, 0, "VerticalAcceleration").castMaxToDouble(), "vertical acceleration unchanged");
        equal(8, field(new ItemStack(ModItems.draconicLeggs), 0, "ArmorSpeedMult").castMaxToDouble(),
            "walking speed unchanged");
        require(((WyvernArmor) ModItems.wyvernChest).getFields(new ItemStack(ModItems.wyvernChest), 0).stream()
            .noneMatch(f -> "ArmorFlightSpeedMult".equals(f.name)), "wyvern flight unchanged");

        send(player, speed, 6f);
        equal(6, IConfigurableItem.ProfileHelper.getFloat(chest, speed.name, -1), "existing 600 percent accepted");
        send(player, speed, 10f);
        equal(10, IConfigurableItem.ProfileHelper.getFloat(chest, speed.name, -1),
            "native packet accepts 1000 percent");
        equal(10, ((DraconicArmor) chest.getItem()).getFlightSpeedModifier(chest, player),
            "actual armor getter consumes 1000 percent");
        send(player, speed, 10.1f);
        equal(10, IConfigurableItem.ProfileHelper.getFloat(chest, speed.name, -1),
            "native packet rejects over maximum");
        send(player, speed, -0.1f);
        equal(10, IConfigurableItem.ProfileHelper.getFloat(chest, speed.name, -1), "native packet rejects below zero");
        ItemStack restored = ItemStack.loadItemStackFromNBT(chest.writeToNBT(new NBTTagCompound()));
        equal(10, field(restored, 0, speed.name).castToDouble(), "1000 percent survives item NBT round trip");
        ItemNBTHelper.setInteger(chest, "ConfigProfile", 1);
        equal(0, field(chest, 0, speed.name).castToDouble(), "other profile remains independent");
        send(player, speed, 4f);
        equal(4, field(chest, 0, speed.name).castToDouble(), "other profile remains adjustable");
        ItemNBTHelper.setInteger(chest, "ConfigProfile", 0);
        equal(10, field(chest, 0, speed.name).castToDouble(), "original profile retains 1000 percent");
        return checks;
    }

    public static ItemConfigField field(ItemStack stack, int slot, String name) {
        List<ItemConfigField> fields = ((IConfigurableItem) stack.getItem()).getFields(stack, slot);
        return fields.stream().filter(f -> name.equals(f.name)).findFirst()
            .orElseThrow(() -> new AssertionError("missing field " + name));
    }

    private static void send(EntityPlayerMP player, ItemConfigField field, float value) throws Exception {
        field.value = value;
        ByteBuf bytes = Unpooled.buffer();
        try {
            new ItemConfigPacket(field).toBytes(bytes);
            ItemConfigPacket inbound = new ItemConfigPacket();
            inbound.fromBytes(bytes);
            Constructor<?> constructor = MessageContext.class.getDeclaredConstructors()[0];
            constructor.setAccessible(true);
            MessageContext context = (MessageContext) constructor.newInstance(player.playerNetServerHandler,
                Side.SERVER);
            new ItemConfigPacket.Handler().onMessage(inbound, context);
        } finally {
            bytes.release();
        }
    }

    private void equal(double expected, double actual, String message) {
        require(Math.abs(expected - actual) < 0.000001, message + ": " + actual);
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
        checks++;
    }
}
