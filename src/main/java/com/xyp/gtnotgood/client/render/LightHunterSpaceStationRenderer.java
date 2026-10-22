// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.world.WorldEvent;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLContext;

import com.gtnewhorizon.gtnhlib.client.renderer.postprocessing.PostProcessingManager;
import com.gtnewhorizon.gtnhlib.client.renderer.shader.ShaderProgram;
import com.xyp.gtnotgood.GTNotGood;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationMath.Orbit;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationMath.Profile;
import com.xyp.gtnotgood.client.render.LightHunterSpaceStationMath.Rotation;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public final class LightHunterSpaceStationRenderer implements IResourceManagerReloadListener {

    private static final LightHunterSpaceStationRenderer INSTANCE = new LightHunterSpaceStationRenderer();
    private static final String DOMAIN = ModList.GTNotGood.getResourceLocation();
    private static final ResourceLocation STAR_TEXTURE = texture("star_layer_1");
    private static final ResourceLocation SPACE_TEXTURE = texture("heartofuniverse");
    private static final ResourceLocation HALO_TEXTURE = texture("halo_tex1");
    private static final ResourceLocation BEAM_TEXTURE = texture("space_layer");
    private static final String[] PLANET_NAMES = { "the_nether", "overworld", "the_end", "ceres", "enceladus",
        "ganymede", "io", "mars", "mercury", "moon", "pluto", "titan", "venus" };
    private static final ResourceLocation[] PLANET_TEXTURES = new ResourceLocation[PLANET_NAMES.length];
    private static final float[][] CUBE_FACES = { { 1, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0 },
        { 0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1 }, { 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0 },
        { 1, 0, 1, 1, 0, 0, 1, 1, 0, 1, 1, 1 }, { 0, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, 0 },
        { 0, 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 1 } };
    private static final Map<Long, Profile> PROFILES = new LinkedHashMap<>();
    private static final Map<Object, BeamCommand> PENDING = new IdentityHashMap<>();
    private static final FloatBuffer MATRIX_BUFFER = BufferUtils.createFloatBuffer(16);
    private static final float[] MATRIX = new float[16];
    private static LightHunterModelMesh starModel, spaceModel;
    private static ShaderProgram beamShader;
    private static boolean initialized, queued, failed;
    private static long renderedFrames, beamFrames;
    private static int lastPlanetCount, lastBeamPassCount;
    private static boolean lastParadoxical;

    static {
        for (int index = 0; index < PLANET_TEXTURES.length; index++)
            PLANET_TEXTURES[index] = texture(PLANET_NAMES[index]);
    }

    private LightHunterSpaceStationRenderer() {}

    /** The origin uses the camera-relative block coordinates supplied by the multiblock renderer. */
    public static void render(Object machineKey, double x, double y, double z, ForgeDirection facing, boolean working,
        int astralArrayCount, long worldTick, float partialTicks, long seed) {
        if (!working) {
            stop(machineKey);
            return;
        }
        initialize();
        if (failed || Minecraft.getMinecraft().theWorld == null) return;
        ForgeDirection horizontal = facing == null || facing.offsetY != 0 ? ForgeDirection.EAST : facing;
        float tick = worldTick + partialTicks;
        GlState state = new GlState();
        try {
            ensureModels();
            Profile profile = profile(seed);
            GL11.glTranslated(x, y, z);
            prepareSolid();
            double[] center = LightHunterSpaceStationMath.rotatedOffset(-LightHunterSpaceStationMath.STAR_DISTANCE, 0,
                0, horizontal.ordinal());
            boolean paradoxical = astralArrayCount == 64;
            renderStar(center, profile.starRotation, tick, paradoxical);
            renderOrbits(center, profile, tick);
            lastPlanetCount = profile.orbits.length;
            lastParadoxical = paradoxical;
            renderedFrames++;
            PENDING.put(machineKey, new BeamCommand(Minecraft.getMinecraft().theWorld, x, y, z, horizontal, tick));
            if (!queued) {
                queued = true;
                PostProcessingManager.getInstance().addDelayedRenderer(ignored -> drawBeams(), 0, 0, 0);
            }
        } catch (Exception exception) {
            fail(exception);
        } finally {
            state.restore();
        }
    }

    public static void stop(Object machineKey) {
        PENDING.remove(machineKey);
    }

    public static boolean isAvailable() {
        return !failed && starModel != null && spaceModel != null && beamShader != null && beamShader.getProgram() != 0;
    }

    public static long getRenderedFrames() {
        return renderedFrames;
    }

    public static long getBeamFrames() {
        return beamFrames;
    }

    public static int getLastPlanetCount() {
        return lastPlanetCount;
    }

    public static int getLastBeamPassCount() {
        return lastBeamPassCount;
    }

    public static boolean wasLastParadoxical() {
        return lastParadoxical;
    }

    private static ResourceLocation texture(String name) {
        return new ResourceLocation(DOMAIN, "textures/render/lighthunter/" + name + ".png");
    }

    private static Profile profile(long seed) {
        Profile value = PROFILES.get(seed);
        if (value == null) {
            if (PROFILES.size() >= 128) PROFILES.remove(PROFILES.keySet().iterator().next());
            value = LightHunterSpaceStationMath.profile(seed);
            PROFILES.put(seed, value);
        }
        return value;
    }

    private static void initialize() {
        if (initialized) return;
        initialized = true;
        PostProcessingManager.getInstance();
        MinecraftForge.EVENT_BUS.register(INSTANCE);
        if (Minecraft.getMinecraft().getResourceManager() instanceof IReloadableResourceManager manager)
            manager.registerReloadListener(INSTANCE);
    }

    private static void ensureModels() throws Exception {
        if (starModel == null)
            starModel = LightHunterModelMesh.load(new ResourceLocation(DOMAIN, "models/lighthunter/star.obj"));
        if (spaceModel == null)
            spaceModel = LightHunterModelMesh.load(new ResourceLocation(DOMAIN, "models/lighthunter/space.obj"));
    }

    private static void prepareSolid() {
        if (GLContext.getCapabilities().OpenGL20) GL20.glUseProgram(0);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_ALPHA_TEST);
        GL11.glAlphaFunc(GL11.GL_GREATER, 0.1F);
        GL11.glColor4f(1, 1, 1, 1);
    }

    private static void bind(ResourceLocation texture) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
    }

    private static void rotate(Rotation rotation, float tick) {
        GL11.glRotatef(rotation.angle(tick), rotation.x, rotation.y, rotation.z);
    }

    private static void renderStar(double[] center, Rotation rotation, float tick, boolean paradoxical) {
        GL11.glPushMatrix();
        GL11.glTranslated(center[0], center[1], center[2]);
        GL11.glScalef(LightHunterSpaceStationMath.STAR_SCALE, LightHunterSpaceStationMath.STAR_SCALE,
            LightHunterSpaceStationMath.STAR_SCALE);
        rotate(rotation, tick);
        if (paradoxical) {
            GL11.glPushMatrix();
            GL11.glScalef(1.02F, 1.02F, 1.02F);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE);
            GL11.glDepthMask(false);
            bind(HALO_TEXTURE);
            spaceModel.draw();
            GL11.glDepthMask(true);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glPopMatrix();
        }
        bind(paradoxical ? SPACE_TEXTURE : STAR_TEXTURE);
        (paradoxical ? spaceModel : starModel).draw();
        GL11.glPopMatrix();
    }

    private static void renderOrbits(double[] center, Profile profile, float tick) {
        GL11.glPushMatrix();
        GL11.glTranslated(center[0], center[1], center[2]);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glColor4ub((byte) 255, (byte) 255, (byte) 255, (byte) 200);
        for (Orbit orbit : profile.orbits) renderRing(orbit);
        GL11.glColor4f(1, 1, 1, 1);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        for (int index = 0; index < profile.orbits.length; index++) {
            Orbit orbit = profile.orbits[index];
            double[] position = orbit.position(tick);
            GL11.glPushMatrix();
            GL11.glTranslated(position[0], position[1], position[2]);
            GL11.glRotatef(orbit.angle(tick) + 90, 0, 1, 0);
            rotate(profile.planetRotations[index], tick);
            float size = profile.planetSizes[index];
            GL11.glScalef(size, size, size);
            GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
            bind(PLANET_TEXTURES[index]);
            cube();
            GL11.glPopMatrix();
        }
        GL11.glPopMatrix();
    }

    private static void renderRing(Orbit orbit) {
        GL11.glPushMatrix();
        GL11.glRotatef(orbit.tilt, orbit.axisX, 0, orbit.axisZ);
        GL11.glBegin(GL11.GL_QUADS);
        for (int index = 0; index < LightHunterSpaceStationMath.ORBIT_SEGMENTS; index++) {
            float first = (float) (2 * Math.PI * index / LightHunterSpaceStationMath.ORBIT_SEGMENTS);
            float second = (float) (2 * Math.PI * (index + 1) / LightHunterSpaceStationMath.ORBIT_SEGMENTS);
            float cos1 = LightHunterSpaceStationMath.fastCos(first), sin1 = LightHunterSpaceStationMath.fastSin(first);
            float cos2 = LightHunterSpaceStationMath.fastCos(second),
                sin2 = LightHunterSpaceStationMath.fastSin(second);
            ringVertex(orbit.radius, cos1, sin1, -1);
            ringVertex(orbit.radius, cos1, sin1, 1);
            ringVertex(orbit.radius, cos2, sin2, 1);
            ringVertex(orbit.radius, cos2, sin2, -1);
        }
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private static void ringVertex(float radius, float cos, float sin, int side) {
        GL11.glVertex3f(radius * cos - side * sin * 1.5F, 0, radius * sin + side * cos * 1.5F);
    }

    private static void cube() {
        GL11.glBegin(GL11.GL_QUADS);
        face(0, 0, -1, CUBE_FACES[0]);
        face(0, 0, 1, CUBE_FACES[1]);
        face(-1, 0, 0, CUBE_FACES[2]);
        face(1, 0, 0, CUBE_FACES[3]);
        face(0, 1, 0, CUBE_FACES[4]);
        face(0, -1, 0, CUBE_FACES[5]);
        GL11.glEnd();
    }

    private static void face(float nx, float ny, float nz, float[] vertices) {
        GL11.glNormal3f(nx, ny, nz);
        for (int index = 0; index < 4; index++) {
            GL11.glTexCoord2f(index == 1 || index == 2 ? 1 : 0, index < 2 ? 1 : 0);
            GL11.glVertex3f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2]);
        }
    }

    private static void drawBeams() {
        queued = false;
        lastBeamPassCount = 0;
        List<BeamCommand> commands = new ArrayList<>(PENDING.values());
        PENDING.clear();
        if (failed || commands.isEmpty()) return;
        GlState state = new GlState();
        try {
            if (!GLContext.getCapabilities().OpenGL20) throw new IllegalStateException("OpenGL 2.0 is required");
            if (beamShader == null) {
                beamShader = new ShaderProgram(DOMAIN, "shaders/lighthunter/beam.vsh", "shaders/lighthunter/beam.fsh");
                if (beamShader.getProgram() == 0) throw new IllegalStateException("Light Hunter beam shader failed");
            }
            prepareSolid();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDisable(GL11.GL_ALPHA_TEST);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDepthMask(false);
            bind(BEAM_TEXTURE);
            GL20.glUseProgram(beamShader.getProgram());
            GL20.glUniform1i(beamShader.getUniformLocation("Sampler0"), 0);
            for (BeamCommand command : commands) if (command.world == Minecraft.getMinecraft().theWorld) {
                renderBeam(command);
                beamFrames++;
            }
        } catch (Exception exception) {
            fail(exception);
        } finally {
            state.restore();
        }
    }

    private static void renderBeam(BeamCommand command) {
        GL11.glPushMatrix();
        GL11.glTranslated(command.x + 0.5, command.y + 0.5, command.z + 0.5);
        double[] end = LightHunterSpaceStationMath.rotatedOffset(-LightHunterSpaceStationMath.BEAM_DISTANCE, 0, 0,
            command.facing.ordinal());
        float directionX = (float) ((end[0] - 0.5) / LightHunterSpaceStationMath.BEAM_DISTANCE);
        float directionZ = (float) ((end[2] - 0.5) / LightHunterSpaceStationMath.BEAM_DISTANCE);
        GL11.glRotatef((float) Math.toDegrees(Math.atan2(directionX, directionZ)), 0, 1, 0);
        MATRIX_BUFFER.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MATRIX_BUFFER);
        MATRIX_BUFFER.get(MATRIX);
        float[] camera = HeartBlackHoleMath.cameraOffset(MATRIX);
        GL20.glUniform3f(beamShader.getUniformLocation("CameraPosition"), camera[0], camera[1], camera[2]);
        float cameraAngle = (float) Math.atan2(camera[1], camera[0]);
        GL20.glUniform3f(beamShader.getUniformLocation("BeamColor"), 0.50980395F, 0.65882355F, 0.7529412F);
        GL20.glUniform1f(beamShader.getUniformLocation("Intensity"), 2);
        beamSurface(LightHunterSpaceStationMath.BEAM_RADIUS, cameraAngle, command.tick);
        GL20.glUniform3f(beamShader.getUniformLocation("BeamColor"), 1, 1, 1);
        GL20.glUniform1f(beamShader.getUniformLocation("Intensity"), 4);
        beamSurface(LightHunterSpaceStationMath.BEAM_RADIUS * 0.45F, cameraAngle, command.tick);
        lastBeamPassCount = 2;
        GL11.glPopMatrix();
    }

    private static void beamSurface(float radius, float cameraAngle, float tick) {
        GL11.glBegin(GL11.GL_TRIANGLES);
        for (int quad = 0; quad < 16; quad++) {
            beamVertex(radius, quad, 0, true, cameraAngle, tick);
            beamVertex(radius, quad, 1, false, cameraAngle, tick);
            beamVertex(radius, quad, 2, false, cameraAngle, tick);
            beamVertex(radius, quad, 3, false, cameraAngle, tick);
            beamVertex(radius, quad, 4, true, cameraAngle, tick);
            beamVertex(radius, quad, 5, true, cameraAngle, tick);
        }
        GL11.glEnd();
    }

    private static void beamVertex(float radius, int quad, int vertex, boolean end, float cameraAngle, float tick) {
        float pi = 3.1415926535897F;
        int offset = vertex > 1 && vertex < 5 ? 0 : 1;
        float angle = pi * (quad + offset) / 16 + cameraAngle - pi / 2;
        float distance = end ? (float) LightHunterSpaceStationMath.BEAM_DISTANCE : 0;
        float timer = tick / 240;
        float height = distance / 256 + timer;
        GL11.glTexCoord2f(height, angle / (2 * pi) + height / 3 + timer);
        GL13.glMultiTexCoord2f(GL13.GL_TEXTURE1, 0.5490F, 0);
        GL11.glVertex3f((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius, distance);
    }

    private static void fail(Exception exception) {
        if (!failed) GTNotGood.LOG.error("Light Hunter Space Station rendering is unavailable", exception);
        failed = true;
        PENDING.clear();
    }

    @SubscribeEvent
    public void worldUnloaded(WorldEvent.Unload event) {
        if (event.world.isRemote) {
            PENDING.clear();
            PROFILES.clear();
        }
    }

    @Override
    public void onResourceManagerReload(IResourceManager manager) {
        if (starModel != null) starModel.release();
        if (spaceModel != null) spaceModel.release();
        if (beamShader != null) beamShader.close();
        starModel = null;
        spaceModel = null;
        beamShader = null;
        PENDING.clear();
        failed = false;
    }

    private static final class BeamCommand {

        private final World world;
        private final double x, y, z;
        private final ForgeDirection facing;
        private final float tick;

        private BeamCommand(World world, double x, double y, double z, ForgeDirection facing, float tick) {
            this.world = world;
            this.x = x;
            this.y = y;
            this.z = z;
            this.facing = facing;
            this.tick = tick;
        }
    }

    private static final class GlState {

        private final boolean shaders = GLContext.getCapabilities().OpenGL20;
        private final int program = shaders ? GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) : 0;
        private final int texture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        private final int mode = GL11.glGetInteger(GL11.GL_MATRIX_MODE);

        private GlState() {
            GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPushMatrix();
            GL11.glLoadIdentity();
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPushMatrix();
        }

        private void restore() {
            GL11.glMatrixMode(GL11.GL_MODELVIEW);
            GL11.glPopMatrix();
            OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
            GL11.glMatrixMode(GL11.GL_TEXTURE);
            GL11.glPopMatrix();
            GL11.glPopAttrib();
            OpenGlHelper.setActiveTexture(texture);
            if (shaders) GL20.glUseProgram(program);
            GL11.glMatrixMode(mode);
        }
    }
}
