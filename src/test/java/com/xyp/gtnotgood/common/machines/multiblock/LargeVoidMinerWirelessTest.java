package com.xyp.gtnotgood.common.machines.multiblock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import gregtech.api.interfaces.tileentity.IGregTechTileEntity;
import gregtech.common.misc.GlobalEnergyWorldSavedData;
import gregtech.common.misc.GlobalVariableStorage;
import gregtech.common.misc.WirelessNetworkManager;
import gregtech.common.misc.spaceprojects.SpaceProjectManager;

public class LargeVoidMinerWirelessTest {

    private static Class<?> fixtureClass;
    private static Method drain;
    private final UUID owner = UUID.randomUUID();
    private final Map<UUID, BigInteger> savedEnergy = new HashMap<>();
    private final Map<UUID, UUID> savedTeams = new HashMap<>();
    private GlobalEnergyWorldSavedData savedData;
    private ControllerTile controller;
    private UUID currentOwner;
    private boolean serverSide;

    @BeforeClass
    public static void loadProductionDrain() throws Exception {
        String source = "com/xyp/gtnotgood/common/machines/multiblock/LargeVoidMiner";
        String fixture = source + "DrainFixture";
        ClassNode node = new ClassNode();
        try (InputStream bytes = LargeVoidMinerWirelessTest.class.getClassLoader()
            .getResourceAsStream(source + ".class")) {
            assertNotNull(bytes);
            new ClassReader(bytes).accept(node, 0);
        }
        // Keep the production method intact; the normal GT superclass bootstraps Forge's LaunchClassLoader.
        node.superName = Type.getInternalName(ControllerTile.class);
        node.signature = null;
        node.interfaces.clear();
        node.fields.removeIf(field -> !field.name.equals("wirelessOwner"));
        node.methods.removeIf(method -> !method.name.equals("drainEnergyInput") || !method.desc.equals("(J)Z"));
        assertEquals(1, node.methods.size());
        MethodNode constructor = new MethodNode(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.visitVarInsn(Opcodes.ALOAD, 0);
        constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, node.superName, "<init>", "()V", false);
        constructor.visitInsn(Opcodes.RETURN);
        constructor.visitMaxs(1, 1);
        node.methods.add(constructor);
        ClassWriter writer = new ClassWriter(0);
        node.accept(new ClassRemapper(writer, new SimpleRemapper(source, fixture)));
        byte[] bytecode = writer.toByteArray();
        fixtureClass = new ClassLoader(LargeVoidMinerWirelessTest.class.getClassLoader()) {

            Class<?> defineFixture() {
                return defineClass(fixture.replace('/', '.'), bytecode, 0, bytecode.length);
            }
        }.defineFixture();
        drain = fixtureClass.getMethod("drainEnergyInput", long.class);
    }

    @Before
    public void prepareNetwork() throws Exception {
        savedEnergy.putAll(GlobalVariableStorage.GlobalEnergy);
        savedTeams.putAll(SpaceProjectManager.spaceTeams);
        savedData = GlobalEnergyWorldSavedData.INSTANCE;
        GlobalEnergyWorldSavedData.INSTANCE = new GlobalEnergyWorldSavedData();
        currentOwner = owner;
        serverSide = true;
        controller = (ControllerTile) fixtureClass.getConstructor().newInstance();
        controller.base = (IGregTechTileEntity) Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class<?>[] { IGregTechTileEntity.class }, (proxy, method, arguments) -> switch (method.getName()) {
            case "isServerSide" -> serverSide;
            case "getOwnerUuid" -> currentOwner;
            default -> throw new AssertionError("Unexpected tile access: " + method.getName());
            });
    }

    @After
    public void restoreNetwork() {
        GlobalVariableStorage.GlobalEnergy.clear();
        GlobalVariableStorage.GlobalEnergy.putAll(savedEnergy);
        SpaceProjectManager.spaceTeams.clear();
        SpaceProjectManager.spaceTeams.putAll(savedTeams);
        GlobalEnergyWorldSavedData.INSTANCE = savedData;
    }

    @Test
    public void chargesTheExactCostWithoutAnEnergyHatch() throws Exception {
        BigInteger balance = BigInteger.TEN.pow(30);
        WirelessNetworkManager.setUserEU(owner, balance);
        for (long cost : new long[] { 30, 120, 480, 122880, (long) Integer.MAX_VALUE + 1, Long.MAX_VALUE }) {
            assertTrue(drain(cost));
            balance = balance.subtract(BigInteger.valueOf(cost));
            assertEquals(balance, WirelessNetworkManager.getUserEU(owner));
        }
        assertTrue(GlobalEnergyWorldSavedData.INSTANCE.isDirty());
    }

    @Test
    public void insufficientFundsRemainUnchangedAndCannotBecomeNegative() throws Exception {
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(29));
        assertFalse(drain(30));
        assertEquals(BigInteger.valueOf(29), WirelessNetworkManager.getUserEU(owner));
        assertTrue(drain(29));
        assertEquals(BigInteger.ZERO, WirelessNetworkManager.getUserEU(owner));
        assertFalse(drain(1));
        assertEquals(BigInteger.ZERO, WirelessNetworkManager.getUserEU(owner));
    }

    @Test
    public void newOwnersStartWithNoSpendableEnergy() throws Exception {
        assertFalse(drain(30));
        assertEquals(BigInteger.ZERO, WirelessNetworkManager.getUserEU(owner));
        assertTrue(SpaceProjectManager.isInTeam(owner));
    }

    @Test
    public void missingOwnerOrTileCannotSpendEnergy() throws Exception {
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(90));
        currentOwner = null;
        assertFalse(drain(30));
        assertFalse(SpaceProjectManager.spaceTeams.containsKey(null));
        controller.base = null;
        assertFalse(drain(30));
        assertEquals(BigInteger.valueOf(90), WirelessNetworkManager.getUserEU(owner));
    }

    @Test
    public void clientAndNegativeRequestsCannotAlterTheNetwork() throws Exception {
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(90));
        serverSide = false;
        assertFalse(drain(30));
        serverSide = true;
        assertFalse(drain(-30));
        assertFalse(drain(Long.MIN_VALUE));
        assertEquals(BigInteger.valueOf(90), WirelessNetworkManager.getUserEU(owner));
        GlobalEnergyWorldSavedData.INSTANCE.setDirty(false);
        assertTrue(drain(0));
        assertFalse(GlobalEnergyWorldSavedData.INSTANCE.isDirty());
    }

    @Test
    public void ownershipChangesChargeTheNewOwnerOnly() throws Exception {
        UUID replacement = UUID.randomUUID();
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(90));
        WirelessNetworkManager.setUserEU(replacement, BigInteger.valueOf(60));
        assertTrue(drain(30));
        currentOwner = replacement;
        assertTrue(drain(20));
        assertEquals(BigInteger.valueOf(60), WirelessNetworkManager.getUserEU(owner));
        assertEquals(BigInteger.valueOf(40), WirelessNetworkManager.getUserEU(replacement));
    }

    @Test
    public void teamChangesUseTheCurrentSharedNetwork() throws Exception {
        UUID firstLeader = UUID.randomUUID();
        UUID secondLeader = UUID.randomUUID();
        WirelessNetworkManager.setUserEU(owner, BigInteger.valueOf(90));
        WirelessNetworkManager.setUserEU(firstLeader, BigInteger.valueOf(60));
        WirelessNetworkManager.setUserEU(secondLeader, BigInteger.valueOf(40));
        SpaceProjectManager.putInTeam(owner, firstLeader);
        assertTrue(drain(30));
        SpaceProjectManager.putInTeam(owner, secondLeader);
        assertTrue(drain(20));
        assertEquals(BigInteger.valueOf(30), WirelessNetworkManager.getUserEU(firstLeader));
        assertEquals(BigInteger.valueOf(20), WirelessNetworkManager.getUserEU(secondLeader));
        SpaceProjectManager.putInTeam(owner, owner);
        assertEquals(BigInteger.valueOf(90), WirelessNetworkManager.getUserEU(owner));
    }

    private boolean drain(long eu) throws Exception {
        return (boolean) drain.invoke(controller, eu);
    }

    public static class ControllerTile {

        private IGregTechTileEntity base;

        public IGregTechTileEntity getBaseMetaTileEntity() {
            return base;
        }
    }
}
