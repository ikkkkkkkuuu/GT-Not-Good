package com.xyp.gtnotgood.client.render;

import java.nio.FloatBuffer;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import com.github.bsideup.jabel.Desugar;
import com.gtnewhorizon.structurelib.alignment.enumerable.ExtendedFacing;
import com.xyp.gtnotgood.common.machines.multiblock.modularmachine.SuperconductingElectromagneticFactory;
import com.xyp.gtnotgood.common.machines.multiblock.structure.SuperconductingElectromagneticStructure;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import gregtech.api.interfaces.tileentity.IGregTechTileEntity;

@SideOnly(Side.CLIENT)
public final class ElectromagneticRotorRenderer {

    private static final int ROTATION_TICKS = 240;
    private static final float DEGREES_PER_TICK = 360F / ROTATION_TICKS;
    private static final int[][] AXES = { { 1, 0, 0 }, { 0, -1, 0 }, { 0, 0, 1 } };
    private static final Map<SuperconductingElectromagneticFactory, Rotor> rotors = new IdentityHashMap<>();
    private static volatile Set<Long> hidden = Collections.emptySet();
    private static volatile World world;
    private static ElectromagneticRotorModel model;
    private static final FloatBuffer orientation = BufferUtils.createFloatBuffer(16);

    private ElectromagneticRotorRenderer() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ElectromagneticRotorRenderer());
    }

    public static void update(SuperconductingElectromagneticFactory machine) {
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        if (tile == null || tile.getWorld() != Minecraft.getMinecraft().theWorld) return;
        if (world != tile.getWorld()) {
            rotors.clear();
            hidden = Collections.emptySet();
            world = tile.getWorld();
        }
        Rotor previous = rotors.get(machine);
        if (!machine.isRotorRunning()) {
            if (previous != null) remove(machine);
            return;
        }
        ExtendedFacing facing = machine.getExtendedFacing();
        if (previous != null && previous.facing == facing) return;
        remove(machine);
        Set<Long> positions = new HashSet<>();
        String[][] shape = SuperconductingElectromagneticStructure.shape();
        int[] offset = new int[3];
        for (int row = 2; row <= 18; row++)
            for (int depth = 4; depth <= 10; depth++) for (int column = 4; column <= 10; column++) {
                if (shape[row][depth].charAt(column) == ' ') continue;
                facing.getWorldOffset(new int[] { column - 7, row - 20, depth }, offset);
                positions
                    .add(key(tile.getXCoord() + offset[0], tile.getYCoord() + offset[1], tile.getZCoord() + offset[2]));
            }
        rotors.put(machine, new Rotor(facing, positions));
        rebuildHidden();
        refresh(positions);
    }

    public static void remove(SuperconductingElectromagneticFactory machine) {
        Rotor rotor = rotors.remove(machine);
        if (rotor == null) return;
        rebuildHidden();
        refresh(rotor.positions);
    }

    private static void rebuildHidden() {
        Set<Long> positions = new HashSet<>();
        for (Rotor rotor : rotors.values()) positions.addAll(rotor.positions);
        // Chunk builders may read this snapshot off the client thread.
        hidden = Collections.unmodifiableSet(positions);
    }

    private static void refresh(Set<Long> positions) {
        if (world == null || positions.isEmpty()) return;
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (long position : positions) {
            int x = (int) (position >> 38), y = (int) (position & 4095), z = (int) (position << 26 >> 38);
            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }
        world.markBlockRangeForRenderUpdate(minX - 1, minY - 1, minZ - 1, maxX + 1, maxY + 1, maxZ + 1);
    }

    public static boolean hidesFace(IBlockAccess access, int x, int y, int z, int side) {
        Set<Long> snapshot = hidden;
        if (snapshot.isEmpty()) return false;
        if (
            world == null || (access instanceof World && access != world) || access instanceof ElectromagneticRotorModel
        ) return false;
        ForgeDirection direction = ForgeDirection.getOrientation(side);
        return snapshot.contains(key(x - direction.offsetX, y - direction.offsetY, z - direction.offsetZ));
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x3ffffffL) << 38 | ((long) z & 0x3ffffffL) << 12 | (y & 4095);
    }

    public static void render(SuperconductingElectromagneticFactory machine, double x, double y, double z,
        float partialTicks) {
        update(machine);
        if (!rotors.containsKey(machine)) return;
        if (model == null) model = new ElectromagneticRotorModel();
        IGregTechTileEntity tile = machine.getBaseMetaTileEntity();
        int[] offset = new int[3];
        ExtendedFacing facing = machine.getExtendedFacing();
        facing.getWorldOffset(new int[] { 0, -10, 7 }, offset);
        int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            GL20.glUseProgram(0);
            OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
            GL11.glDisable(GL11.GL_TEXTURE_2D);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            GL11.glDisable(GL11.GL_LIGHTING);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_ALPHA_TEST);
            GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
            GL11.glTranslated(x + offset[0] + 0.5, y + offset[1] + 0.5, z + offset[2] + 0.5);
            orientation.clear();
            for (int[] axis : AXES) {
                facing.getWorldOffset(axis, offset);
                orientation.put(offset[0]).put(offset[1]).put(offset[2]).put(0);
            }
            orientation.put(0).put(0).put(0).put(1).flip();
            GL11.glMultMatrix(orientation);
            GL11.glRotatef(((tile.getWorld().getTotalWorldTime() % ROTATION_TICKS) + partialTicks) * DEGREES_PER_TICK,
                0, 1, 0);
            model.render();
        } finally {
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            GL20.glUseProgram(program);
            OpenGlHelper.setActiveTexture(activeTexture);
            GL11.glMatrixMode(matrixMode);
        }
    }

    @SubscribeEvent
    public void texturesReloaded(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() == 0) model = null;
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world != world) return;
        rotors.clear();
        hidden = Collections.emptySet();
        world = null;
    }

    @Desugar
    private record Rotor(ExtendedFacing facing, Set<Long> positions) {}
}
