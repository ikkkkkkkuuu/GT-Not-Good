// SPDX-License-Identifier: LGPL-3.0-only
package com.xyp.gtnotgood.client.render;

import static com.gtnewhorizon.gtnhlib.client.opengl.GLCaps.FBO;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
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
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.I3DGeometryRenderer;
import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingManager;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

public final class KerrNewmanBlackHoleRenderer implements IResourceManagerReloadListener {

    private static final KerrNewmanBlackHoleRenderer INSTANCE = new KerrNewmanBlackHoleRenderer();
    private static final Map<Object, BlackHoleState> ANIMATIONS = new WeakHashMap<>();
    private static final List<BlackHoleState> ENDING = new ArrayList<>();
    private static final I3DGeometryRenderer DELAYED_RENDERER = KerrNewmanBlackHoleRenderer::draw;
    private static final SphereMesh[] CORE_MESHES = { new SphereMesh(16, 32), new SphereMesh(32, 64),
        new SphereMesh(64, 128) };
    private static final SphereMesh[] HORIZON_MESHES = { new SphereMesh(20, 40), new SphereMesh(40, 80),
        new SphereMesh(64, 128) };
    private static boolean initialized;
    private static boolean shaderUnavailable;
    private static boolean sceneCopied;
    private static ShaderProgram shader;
    private static CustomFramebuffer scene;

    private KerrNewmanBlackHoleRenderer() {}

    /** Submit every formed frame, including idle frames while the previous black hole shrinks. */
    public static void render(Object machineKey, double centerX, double centerY, double centerZ, boolean working,
        long worldTime, float partialTicks) {
        initialize();
        BlackHoleState state = ANIMATIONS.get(machineKey);
        if (state == null || working && !state.working) {
            state = new BlackHoleState();
            ANIMATIONS.put(machineKey, state);
        }
        if (working || state.working) {
            state.centerX = centerX + RenderManager.renderPosX;
            state.centerY = centerY + RenderManager.renderPosY;
            state.centerZ = centerZ + RenderManager.renderPosZ;
        }
        if (!working) {
            state.end(worldTime);
            return;
        }
        state.working = true;
        float scale = state.animation.update(true, worldTime, partialTicks);
        if (scale <= 0.001F) return;
        PostProcessingManager.getInstance().addDelayedRenderer(DELAYED_RENDERER, centerX, centerY, centerZ,
            new DrawSubmission(state.world, scale));
    }

    public static void stop(Object machineKey, long worldTime) {
        BlackHoleState state = ANIMATIONS.remove(machineKey);
        if (state != null) state.end(worldTime);
    }

    public static boolean isDistortionAvailable() {
        return shader != null && shader.getProgram() != 0 && !shaderUnavailable;
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        PostProcessingManager.getInstance();
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager manager)
            manager.registerReloadListener(INSTANCE);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void beginFrame(RenderWorldLastEvent event) {
        sceneCopied = false;
        if (Minecraft.getMinecraft().theWorld == null) return;
        long worldTime = Minecraft.getMinecraft().theWorld.getTotalWorldTime();
        Iterator<BlackHoleState> iterator = ENDING.iterator();
        while (iterator.hasNext()) {
            BlackHoleState state = iterator.next();
            if (state.world != Minecraft.getMinecraft().theWorld) {
                iterator.remove();
                continue;
            }
            float scale = state.animation.update(false, worldTime, event.partialTicks);
            if (scale <= 0.001F) {
                iterator.remove();
            } else {
                PostProcessingManager.getInstance().addDelayedRenderer(DELAYED_RENDERER,
                    state.centerX - RenderManager.renderPosX, state.centerY - RenderManager.renderPosY,
                    state.centerZ - RenderManager.renderPosZ, new DrawSubmission(state.world, scale));
            }
        }
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            ANIMATIONS.clear();
            ENDING.clear();
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        GlState state = new GlState();
        try {
            if (shader != null) shader.close();
            shader = null;
            shaderUnavailable = false;
            if (scene != null) scene.checkDeleteFramebuffer();
            scene = null;
            sceneCopied = false;
            for (SphereMesh mesh : CORE_MESHES) mesh.release();
            for (SphereMesh mesh : HORIZON_MESHES) mesh.release();
        } finally {
            state.restore();
        }
    }

    private static void draw(Object data) {
        DrawSubmission submission = (DrawSubmission) data;
        if (submission.world != Minecraft.getMinecraft().theWorld) return;
        float scale = submission.scale;
        GlState state = new GlState();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        try {
            float coreRadius = KerrNewmanBlackHoleMath.CORE_RADIUS * scale;
            float horizonRadius = KerrNewmanBlackHoleMath.EVENT_HORIZON_RADIUS * scale;
            KerrNewmanBlackHoleMath.Projection projection = KerrNewmanBlackHoleMath.projectSphere(
                matrix(GL11.GL_MODELVIEW_MATRIX), matrix(GL11.GL_PROJECTION_MATRIX), state.viewport[2],
                state.viewport[3], coreRadius, horizonRadius);
            if (projection == null) return;
            prepareGeometry(state);
            try {
                if (prepareShader()) {
                    captureScene(state);
                    drawHorizon(horizonRadius, projection, state.viewport);
                }
            } catch (RuntimeException | LinkageError failure) {
                disableShader(failure);
                state.restoreFramebuffer();
            }
            if (state.shaders) GL20.glUseProgram(0);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDepthMask(true);
            GL11.glColor4f(0, 0, 0, 1);
            drawSphere(CORE_MESHES[KerrNewmanBlackHoleMath.detailLevel(projection.innerRadius)], coreRadius);
        } finally {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            state.restore();
        }
    }

    private static void prepareGeometry(GlState state) {
        if (state.shaders) GL20.glUseProgram(0);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glFrontFace(GL11.GL_CCW);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthFunc(GL11.GL_LEQUAL);
        GL11.glDisableClientState(GL11.GL_COLOR_ARRAY);
        GL11.glDisableClientState(GL11.GL_NORMAL_ARRAY);
    }

    private static boolean prepareShader() {
        if (
            shaderUnavailable || !OpenGlHelper.isFramebufferEnabled()
                || !OpenGlHelper.shadersSupported
                || !GLContext.getCapabilities().OpenGL20
        ) return false;
        if (shader == null) {
            shader = new ShaderProgram(ModList.GTNotGood.getResourceLocation(),
                "shaders/kerrnewman/black_hole_event_horizon.vsh", "shaders/kerrnewman/black_hole_event_horizon.fsh");
            if (shader.getProgram() == 0) {
                disableShader(new IllegalStateException("Black hole shader did not compile"));
                return false;
            }
        }
        return true;
    }

    private static void captureScene(GlState state) {
        int width = state.viewport[2];
        int height = state.viewport[3];
        if (scene == null || scene.framebufferWidth != width || scene.framebufferHeight != height) {
            if (scene != null) scene.checkDeleteFramebuffer();
            scene = new CustomFramebuffer(width, height, CustomFramebuffer.TEXTURE_LINEAR);
            state.restoreFramebuffer();
            sceneCopied = false;
        }
        if (sceneCopied) return;
        if (state.separateFramebuffers) {
            FBO.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, state.framebuffer);
            scene.bindDrawFramebuffer();
            GL30.glBlitFramebuffer(state.viewport[0], state.viewport[1], state.viewport[0] + width,
                state.viewport[1] + height, 0, 0, width, height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        } else {
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.framebufferTexture);
            GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, state.viewport[0], state.viewport[1], width, height);
        }
        state.restoreFramebuffer();
        sceneCopied = true;
    }

    private static void drawHorizon(float radius, KerrNewmanBlackHoleMath.Projection projection, int[] viewport) {
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, scene.framebufferTexture);
        shader.use();
        shader.bindTextureSlot("DiffuseSampler", 0);
        GL20.glUniform2f(shader.getUniformLocation("ScreenSize"), viewport[2], viewport[3]);
        GL20.glUniform2f(shader.getUniformLocation("ScreenOrigin"), viewport[0], viewport[1]);
        GL20.glUniform2f(shader.getUniformLocation("BlackHoleCenterScreen"), projection.centerX, projection.centerY);
        GL20.glUniform1f(shader.getUniformLocation("BlackHoleRadiusScreen"),
            KerrNewmanBlackHoleMath.coreMaskRadius(projection.innerRadius));
        GL20.glUniform1f(shader.getUniformLocation("EventHorizonRadiusScreen"), projection.outerRadius);
        GL20.glUniform1f(shader.getUniformLocation("DistortionStrength"), KerrNewmanBlackHoleMath.DISTORTION_STRENGTH);
        GL11.glEnable(GL11.GL_BLEND);
        GL14.glBlendEquation(GL14.GL_FUNC_ADD);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glDepthMask(false);
        GL11.glColor4f(1, 1, 1, 1);
        drawSphere(HORIZON_MESHES[KerrNewmanBlackHoleMath.detailLevel(projection.outerRadius)], radius);
    }

    private static void drawSphere(SphereMesh mesh, float radius) {
        GL11.glPushMatrix();
        GL11.glScalef(radius, radius, radius);
        try {
            mesh.draw();
        } finally {
            GL11.glPopMatrix();
        }
    }

    private static float[] matrix(int name) {
        FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
        GL11.glGetFloat(name, buffer);
        float[] matrix = new float[16];
        buffer.get(matrix);
        return matrix;
    }

    private static void disableShader(Throwable failure) {
        if (!shaderUnavailable) GTNotGood.LOG.warn("Kerr-Newman event horizon distortion is unavailable", failure);
        shaderUnavailable = true;
    }

    private static final class BlackHoleState {

        private final KerrNewmanBlackHoleMath.Animation animation = new KerrNewmanBlackHoleMath.Animation();
        private final World world = Minecraft.getMinecraft().theWorld;
        private double centerX;
        private double centerY;
        private double centerZ;
        private boolean working;

        private void end(long worldTime) {
            if (!working) return;
            animation.update(false, worldTime, 0);
            working = false;
            ENDING.add(this);
        }
    }

    private static final class DrawSubmission {

        private final World world;
        private final float scale;

        private DrawSubmission(World world, float scale) {
            this.world = world;
            this.scale = scale;
        }
    }

    private static final class SphereMesh {

        private final float[] vertices;
        private int vertexBuffer;

        private SphereMesh(int latitudes, int longitudes) {
            vertices = KerrNewmanBlackHoleMath.sphereVertices(latitudes, longitudes);
        }

        private void draw() {
            if (GLContext.getCapabilities().OpenGL15) {
                if (vertexBuffer == 0) {
                    vertexBuffer = GL15.glGenBuffers();
                    FloatBuffer positions = BufferUtils.createFloatBuffer(vertices.length);
                    positions.put(vertices).flip();
                    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
                    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, positions, GL15.GL_STATIC_DRAW);
                }
                GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vertexBuffer);
                GL11.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GL11.glVertexPointer(3, GL11.GL_FLOAT, 0, 0L);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, vertices.length / 3);
            } else {
                Tessellator tessellator = Tessellator.instance;
                tessellator.startDrawing(GL11.GL_TRIANGLES);
                for (int offset = 0; offset < vertices.length; offset += 3)
                    tessellator.addVertex(vertices[offset], vertices[offset + 1], vertices[offset + 2]);
                tessellator.draw();
            }
        }

        private void release() {
            if (vertexBuffer != 0) GL15.glDeleteBuffers(vertexBuffer);
            vertexBuffer = 0;
        }
    }

    private static final class GlState {

        private final boolean shaders = GLContext.getCapabilities().OpenGL20;
        private final boolean vbos = GLContext.getCapabilities().OpenGL15;
        private final boolean framebuffers = OpenGlHelper.isFramebufferEnabled();
        private final boolean separateFramebuffers = framebuffers
            && (GLContext.getCapabilities().OpenGL30 || GLContext.getCapabilities().GL_ARB_framebuffer_object);
        private final int program = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        private final int arrayBuffer = vbos ? GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING) : 0;
        private final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int matrixMode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);
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
