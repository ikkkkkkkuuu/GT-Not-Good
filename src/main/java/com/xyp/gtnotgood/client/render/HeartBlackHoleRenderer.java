// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.CustomFramebuffer;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingManager;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class HeartBlackHoleRenderer implements IResourceManagerReloadListener {

    private static final HeartBlackHoleRenderer INSTANCE = new HeartBlackHoleRenderer();
    private static final Map<Object, DrawCommand> PENDING = new IdentityHashMap<>();
    private static final List<DrawCommand> COMMANDS = new ArrayList<>();
    private static final float[] SPHERE = HeartBlackHoleMath.sphereVertices(24, 48);
    private static final Comparator<DrawCommand> DISTANCE_ORDER = (first, second) -> Double
        .compare(second.distanceSquared(), first.distanceSquared());
    private static ShaderProgram volumeShader;
    private static ShaderProgram blurShader;
    private static ShaderProgram compositeShader;
    private static HeartBlackHoleTargets targets;
    private static int sphereBuffer;
    private static boolean initialized;
    private static boolean queued;
    private static boolean shaderUnavailable;
    private static long renderedFrames;
    private static int lastPassCount;

    private HeartBlackHoleRenderer() {}

    public static void render(Object machineKey, double centerX, double centerY, double centerZ, boolean working,
        long worldTime, float partialTicks) {
        render(machineKey, centerX, centerY, centerZ, ForgeDirection.SOUTH, working, worldTime, partialTicks);
    }

    /** Coordinates use the same camera-relative frame as the multiblock renderer callback. */
    public static void render(Object machineKey, double centerX, double centerY, double centerZ, ForgeDirection facing,
        boolean working, long worldTime, float partialTicks) {
        if (!working) {
            stop(machineKey);
            return;
        }
        initialize();
        World world = Minecraft.getMinecraft().theWorld;
        if (world == null) return;
        PENDING.put(machineKey, new DrawCommand(world, centerX, centerY, centerZ, facing, worldTime + partialTicks));
        if (!queued) {
            queued = true;
            PostProcessingManager.getInstance().addDelayedRenderer(ignored -> draw(), 0, 0, 0);
        }
    }

    public static void stop(Object machineKey) {
        PENDING.remove(machineKey);
    }

    public static boolean isAvailable() {
        return !shaderUnavailable && volumeShader != null && volumeShader.getProgram() != 0;
    }

    public static long getRenderedFrames() {
        return renderedFrames;
    }

    public static int getLastPassCount() {
        return lastPassCount;
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        PostProcessingManager.getInstance();
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager manager)
            manager.registerReloadListener(INSTANCE);
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world.isRemote) PENDING.clear();
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        GlState state = new GlState();
        try {
            releaseResources();
            shaderUnavailable = false;
            PENDING.clear();
        } finally {
            state.restore();
        }
    }

    private static void draw() {
        queued = false;
        lastPassCount = 0;
        COMMANDS.clear();
        World world = Minecraft.getMinecraft().theWorld;
        for (DrawCommand command : PENDING.values()) {
            if (command.world == world) COMMANDS.add(command);
        }
        PENDING.clear();
        if (COMMANDS.isEmpty()) return;
        COMMANDS.sort(DISTANCE_ORDER);
        GlState state = new GlState();
        try {
            if (!prepareShaders()) return;
            HeartBlackHoleTargets.DepthAttachments depth = HeartBlackHoleTargets.captureDepth(state.framebuffer);
            ensureTargets(state.viewport[2], state.viewport[3]);
            float[] cameraOffset = cameraOffset();
            prepareGeometry();
            renderVolume(targets.effect, depth, cameraOffset, 0);
            renderVolume(targets.mask, depth, cameraOffset, 1);
            renderBlur(targets.effect, targets.effectBlurA, 1F / targets.width, 0);
            renderBlur(targets.effectBlurA, targets.effectBlurB, 0, 1F / targets.blurHeight);
            renderBlur(targets.mask, targets.maskBlurA, 1F / targets.width, 0);
            renderBlur(targets.maskBlurA, targets.maskBlurB, 0, 1F / targets.blurHeight);
            state.restoreFramebuffer();
            composite(state.viewport);
            renderedFrames++;
        } catch (RuntimeException | LinkageError failure) {
            if (!shaderUnavailable) GTNotGood.LOG.error("Heart of the Universe black hole rendering failed", failure);
            shaderUnavailable = true;
        } finally {
            COMMANDS.clear();
            state.restore();
        }
    }

    private static boolean prepareShaders() {
        if (shaderUnavailable) return false;
        if (
            !OpenGlHelper.isFramebufferEnabled() || !OpenGlHelper.shadersSupported
                || !GLContext.getCapabilities().OpenGL20
                || !GLContext.getCapabilities().OpenGL15
        ) {
            throw new IllegalStateException("Heart rendering requires framebuffer and shader support");
        }
        if (volumeShader == null) {
            String domain = ModList.GTNotGood.getResourceLocation();
            volumeShader = new ShaderProgram(domain, "shaders/heart/black_hole.vsh", "shaders/heart/black_hole.fsh");
            blurShader = new ShaderProgram(domain, "shaders/heart/post.vsh", "shaders/heart/blur.fsh");
            compositeShader = new ShaderProgram(domain, "shaders/heart/post.vsh", "shaders/heart/composite.fsh");
            if (volumeShader.getProgram() == 0 || blurShader.getProgram() == 0 || compositeShader.getProgram() == 0)
                throw new IllegalStateException("Heart black hole shaders did not compile");
        }
        return true;
    }

    private static void ensureTargets(int width, int height) {
        if (targets == null || targets.width != width || targets.height != height) {
            if (targets != null) targets.close();
            targets = new HeartBlackHoleTargets(width, height);
        }
    }

    private static void prepareGeometry() {
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
        GL11.glColorMask(true, true, true, true);
        GL11.glDepthMask(false);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
    }

    private static float[] cameraOffset() {
        FloatBuffer view = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, view);
        float[] matrix = new float[16];
        view.get(matrix);
        return HeartBlackHoleMath.cameraOffset(matrix);
    }

    private static void renderVolume(CustomFramebuffer target, HeartBlackHoleTargets.DepthAttachments depth,
        float[] cameraOffset, float pass) {
        HeartBlackHoleTargets.bindVolumeTarget(target, depth);
        try {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
            GL11.glDepthFunc(GL11.GL_LEQUAL);
            GL11.glDepthMask(false);
            GL11.glDisable(GL11.GL_BLEND);
            volumeShader.use();
            uniform(volumeShader, "PassMode", pass);
            uniform(volumeShader, "BlackHoleAndDiskRadius", HeartBlackHoleMath.BLACK_HOLE_AND_DISK_RADIUS);
            uniform(volumeShader, "SpaceSolidRadius", HeartBlackHoleMath.SPACE_SOLID_RADIUS);
            uniform(volumeShader, "SpaceFadeRadius", HeartBlackHoleMath.SPACE_FADE_RADIUS);
            uniform(volumeShader, "RotationSpeed", HeartBlackHoleMath.ROTATION_SPEED);
            vector(volumeShader, "Center", 0, 0, 0);
            for (DrawCommand command : COMMANDS) {
                uniform(volumeShader, "Time", command.tick);
                vector(volumeShader, "CameraPosition", cameraOffset[0] - command.centerX,
                    cameraOffset[1] - command.centerY, cameraOffset[2] - command.centerZ);
                vector(volumeShader, "FacingAxis", command.facing.offsetX, 0, command.facing.offsetZ);
                vector(volumeShader, "BlackHoleRotation", HeartBlackHoleMath.rotation(command.seed, command.tick, 0),
                    HeartBlackHoleMath.rotation(command.seed, command.tick, 1),
                    HeartBlackHoleMath.rotation(command.seed, command.tick, 2));
                GL11.glMatrixMode(GL11.GL_MODELVIEW);
                GL11.glPushMatrix();
                try {
                    GL11.glTranslated(command.centerX, command.centerY, command.centerZ);
                    drawSphere();
                } finally {
                    GL11.glPopMatrix();
                }
            }
            lastPassCount++;
        } finally {
            HeartBlackHoleTargets.releaseDepth(target);
        }
    }

    private static void renderBlur(CustomFramebuffer input, CustomFramebuffer output, float x, float y) {
        output.bindFramebuffer(true);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_BLEND);
        blurShader.use();
        bindTexture(blurShader, "Sampler0", 0, input.framebufferTexture);
        GL20.glUniform2f(blurShader.getUniformLocation("TexelStep"), x, y);
        drawScreenQuad();
        lastPassCount++;
    }

    private static void composite(int[] viewport) {
        GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(false);
        GL11.glColorMask(true, true, true, false);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        compositeShader.use();
        bindTexture(compositeShader, "EffectSampler", 0, targets.effect.framebufferTexture);
        bindTexture(compositeShader, "BlurredEffectSampler", 1, targets.effectBlurB.framebufferTexture);
        bindTexture(compositeShader, "MaskSampler", 2, targets.mask.framebufferTexture);
        bindTexture(compositeShader, "BlurredMaskSampler", 3, targets.maskBlurB.framebufferTexture);
        GL20.glUniform2f(compositeShader.getUniformLocation("BlurInputScale"),
            (float) targets.width / targets.blurWidth, (float) targets.height / targets.blurHeight);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        drawScreenQuad();
        lastPassCount++;
    }

    private static void bindTexture(ShaderProgram shader, String name, int unit, int texture) {
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit + unit);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        shader.bindTextureSlot(name, unit);
    }

    private static void uniform(ShaderProgram shader, String name, float value) {
        GL20.glUniform1f(shader.getUniformLocation(name), value);
    }

    private static void vector(ShaderProgram shader, String name, double x, double y, double z) {
        GL20.glUniform3f(shader.getUniformLocation(name), (float) x, (float) y, (float) z);
    }

    private static void drawSphere() {
        if (sphereBuffer == 0) {
            sphereBuffer = GL15.glGenBuffers();
            FloatBuffer vertices = BufferUtils.createFloatBuffer(SPHERE.length);
            vertices.put(SPHERE).flip();
            GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sphereBuffer);
            GL15.glBufferData(GL15.GL_ARRAY_BUFFER, vertices, GL15.GL_STATIC_DRAW);
        }
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, sphereBuffer);
        GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
        GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0L);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, SPHERE.length / 3);
    }

    private static void drawScreenQuad() {
        GL11.glBegin(GL11.GL_TRIANGLES);
        screenVertex(-1, -1, 0, 0);
        screenVertex(1, -1, 1, 0);
        screenVertex(1, 1, 1, 1);
        screenVertex(-1, -1, 0, 0);
        screenVertex(1, 1, 1, 1);
        screenVertex(-1, 1, 0, 1);
        GL11.glEnd();
    }

    private static void screenVertex(float x, float y, float u, float v) {
        GL11.glTexCoord2f(u, v);
        GL11.glVertex2f(x, y);
    }

    private static void releaseResources() {
        if (volumeShader != null) volumeShader.close();
        if (blurShader != null) blurShader.close();
        if (compositeShader != null) compositeShader.close();
        volumeShader = null;
        blurShader = null;
        compositeShader = null;
        if (targets != null) targets.close();
        targets = null;
        if (sphereBuffer != 0) GL15.glDeleteBuffers(sphereBuffer);
        sphereBuffer = 0;
    }

    private static final class DrawCommand {

        private final World world;
        private final double centerX;
        private final double centerY;
        private final double centerZ;
        private final ForgeDirection facing;
        private final float tick;
        private final long seed;

        private DrawCommand(World world, double centerX, double centerY, double centerZ, ForgeDirection facing,
            float tick) {
            this.world = world;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
            this.facing = facing == null || facing.offsetY != 0 ? ForgeDirection.SOUTH : facing;
            this.tick = tick;
            seed = HeartBlackHoleMath.positionSeed((int) Math.floor(centerX + RenderManager.renderPosX),
                (int) Math.floor(centerY + RenderManager.renderPosY),
                (int) Math.floor(centerZ + RenderManager.renderPosZ));
        }

        private double distanceSquared() {
            return centerX * centerX + centerY * centerY + centerZ * centerZ;
        }
    }

    private static final class GlState {

        private final boolean shaders = GLContext.getCapabilities().OpenGL20;
        private final boolean vbos = GLContext.getCapabilities().OpenGL15;
        private final boolean framebuffers = OpenGlHelper.isFramebufferEnabled();
        private final int program = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        private final int arrayBuffer = vbos ? GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) : 0;
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
        private final boolean separateFramebuffers = framebuffers
            && (GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_ARB_framebuffer_object);
        private final int framebuffer = framebuffers ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) : 0;
        private final int readFramebuffer = separateFramebuffers ? GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
            : 0;
        private final int[] viewport = new int[4];

        private GlState() {
            IntBuffer buffer = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, buffer);
            buffer.get(viewport);
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            GL11.glPushClientAttrib(GL11.GL_CLIENT_VERTEX_ARRAY_BIT);
        }

        private void restoreFramebuffer() {
            if (!framebuffers) return;
            FBO.glBindFramebuffer(GL30.GL_FRAMEBUFFER, framebuffer);
            if (separateFramebuffers) FBO.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
        }

        private void restore() {
            restoreFramebuffer();
            GL11.glPopClientAttrib();
            GL11.glPopAttrib();
            if (vbos) GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, arrayBuffer);
            OpenGlHelper.setActiveTexture(activeTexture);
            if (shaders) GL20.glUseProgram(program);
            GL11.glMatrixMode(matrixMode);
        }
    }
}
