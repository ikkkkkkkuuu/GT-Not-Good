package com.xyp.gtnotgood.common.user;

import java.util.UUID;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayer;

import com.mojang.authlib.GameProfile;

import cpw.mods.fml.common.FMLCommonHandler;

/**
 * Isolated survival fake player with a packet sink. Forge 1.7.10's base fake player has no connection,
 * but cancelled interactions, inventory windows and several tools still send server packets.
 */
final class MechanicalUserPlayer extends FakePlayer {

    MechanicalUserPlayer(WorldServer world) {
        super(world, new GameProfile(UUID.fromString("307c525e-3d17-4b8a-8e0a-bb73a0d35d49"), "[GTNGUser]"));
        playerNetServerHandler = new NetHandlerPlayServer(
            FMLCommonHandler.instance()
                .getMinecraftServerInstance(),
            new NetworkManager(false),
            this) {

            @Override
            public void sendPacket(Packet packet) {}
        };
        theItemInWorldManager.setGameType(net.minecraft.world.WorldSettings.GameType.SURVIVAL);
    }

    @Override
    public net.minecraft.util.ChunkCoordinates getPlayerCoordinates() {
        return new net.minecraft.util.ChunkCoordinates(
            net.minecraft.util.MathHelper.floor_double(posX),
            net.minecraft.util.MathHelper.floor_double(posY),
            net.minecraft.util.MathHelper.floor_double(posZ));
    }
}
