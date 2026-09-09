package com.xyp.gtnotgood.common.network;

import java.lang.reflect.Proxy;

import net.minecraftforge.common.util.ForgeDirection;

import gregtech.api.interfaces.tileentity.IBasicEnergyContainer;

/** Exercises the GT energy contract with a recipient that rejects unsafe voltage and tracks accepted amperes. */
final class NetworkEnergyChecks {

    private NetworkEnergyChecks() {}

    static void run() {
        long[] state = { 0, 10000, 32, 2, 0 };
        boolean[] input = { true };
        IBasicEnergyContainer target = (IBasicEnergyContainer) Proxy.newProxyInstance(
            IBasicEnergyContainer.class.getClassLoader(),
            new Class<?>[] { IBasicEnergyContainer.class },
            (proxy, method, args) -> {
                switch (method.getName()) {
                    case "inputEnergyFrom":
                        return input[0] && args[0] == ForgeDirection.NORTH;
                    case "getStoredEU":
                        return state[0];
                    case "getEUCapacity":
                        return state[1];
                    case "getInputVoltage":
                        return state[2];
                    case "getInputAmperage":
                        return state[3];
                    case "injectEnergyUnits": {
                        long voltage = (Long) args[1];
                        long amperes = (Long) args[2];
                        require(voltage > 0 && voltage <= state[2], "no overvoltage injection");
                        long accepted = Math.min(amperes, state[3] - state[4]);
                        require(voltage * accepted <= state[1] - state[0], "no capacity overflow");
                        state[0] += voltage * accepted;
                        state[4] += accepted;
                        return accepted;
                    }
                    default:
                        throw new AssertionError("Unexpected GT method " + method.getName());
                }
            });
        long buffer = 8192;
        long moved = NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, buffer, 8192);
        require(moved == 64 && state[0] == 64, "high tier energy supplies LV at 32 V and 2 A");
        buffer -= moved;
        require(
            NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, buffer, 8192) == 0,
            "GT accepted amperes prevent a second transfer in the same tick");
        for (int i = 0; i < 127; i++) {
            state[4] = 0;
            buffer -= NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, buffer, 8192);
        }
        require(buffer == 0 && state[0] == 8192, "all EU conserved across voltage conversion");
        state[0] = 9995;
        state[4] = 0;
        require(
            NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, 1000, 1000) == 5,
            "last partial packet fits remaining capacity");
        require(NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, 1000, 1000) == 0, "full target rejected");
        state[0] = 0;
        state[4] = 0;
        require(NetworkEnergyTransfer.deliver(target, ForgeDirection.SOUTH, 1000, 1000) == 0, "input face respected");
        input[0] = false;
        require(NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, 1000, 1000) == 0, "closed input respected");
        input[0] = true;
        state[1] = Long.MAX_VALUE;
        state[2] = Long.MAX_VALUE;
        state[3] = Long.MAX_VALUE;
        require(
            NetworkEnergyTransfer.deliver(target, ForgeDirection.NORTH, Long.MAX_VALUE, Long.MAX_VALUE)
                == Long.MAX_VALUE,
            "large voltage and amperage cannot overflow");
        TileNetworkController.Channel channel = new TileNetworkController.Channel();
        channel.type = 2;
        channel.energy = 8192;
        channel.priority = 42;
        TileNetworkController.Channel restored = new TileNetworkController.Channel();
        restored.read(channel.write());
        require(
            restored.energy == 8192 && restored.priority == 42 && restored.type == 2 && restored.hasCargo(),
            "EU buffer and channel priority survive save and block drops");
        System.out
            .println("Network EU checks passed: conversion, conservation, voltage, amperage, capacity, persistence");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
