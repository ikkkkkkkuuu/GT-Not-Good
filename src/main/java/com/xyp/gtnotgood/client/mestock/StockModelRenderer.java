package com.xyp.gtnotgood.client.mestock;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.util.ForgeDirection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnotgood.common.blocks.mestock.BlockMERequester;
import com.xyp.gtnotgood.common.blocks.mestock.TileMERequester;
import com.xyp.gtnotgood.utils.enums.ModList;

import appeng.api.parts.IPartRenderHelper;
import appeng.api.util.AEColor;
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Adapts the pinned upstream cuboids, face UVs and tint layers to the 1.7.10 tessellator.
 * Models are baked once per texture stitch; world rendering adds vertices to the existing chunk batch.
 * No model parsing, sprite lookup or gameplay polling occurs while drawing a cached model.
 */
@SideOnly(Side.CLIENT)
public final class StockModelRenderer implements ISimpleBlockRenderingHandler {

    private static volatile Map<String, IIcon> textures = Collections.emptyMap();
    private static final Map<String, JsonObject> definitions = new HashMap<>();
    private static volatile Map<String, Quad[]> models = Collections.emptyMap();

    public static void registerIcons(IIconRegister register) {
        Map<String, IIcon> registered = new HashMap<>();
        JsonObject bindings = readJson("texture_bindings");
        for (Map.Entry<String, JsonElement> entry : bindings.entrySet()) {
            registered.put(entry.getKey(),
                register.registerIcon(ModList.GTNotGood.getResourcePath(entry.getValue().getAsString())));
        }
        textures = Collections.unmodifiableMap(registered);
    }

    /** Sprite UVs are final only after stitching. Publish one immutable batch for Angelica's chunk workers. */
    @SubscribeEvent
    public void stitched(TextureStitchEvent.Post event) {
        if (event.map.getTextureType() != 0) return;
        Map<String, Quad[]> baked = new HashMap<>();
        for (JsonElement root : readJson("model_roots").getAsJsonArray("models")) {
            String identifier = root.getAsString();
            baked.put(identifier, bake(identifier));
        }
        models = Collections.unmodifiableMap(baked);
    }

    public static IIcon icon(String texture) {
        IIcon icon = textures.get(texture);
        if (icon == null) throw new IllegalStateException("Unregistered stock texture: " + texture);
        return icon;
    }

    public static String indicator(String part, int flags) {
        return "ae2:part/" + part + ((flags & 3) == 3 ? "_has_channel" : (flags & 1) != 0 ? "_on" : "_off");
    }

    public static void inventory(String model) {
        Tessellator tess = Tessellator.instance;
        tess.startDrawingQuads();
        draw(model, 0, 0, 0, ForgeDirection.EAST, ForgeDirection.UP, ForgeDirection.SOUTH, 0xf000f0,
            AEColor.Transparent, true);
        tess.draw();
    }

    public static void part(String model, int x, int y, int z, IPartRenderHelper helper, RenderBlocks renderer,
        AEColor color) {
        draw(model, x + 0.5, y + 0.5, z + 0.5, helper.getWorldX(), helper.getWorldY(), helper.getWorldZ(),
            helper.getBlock().getMixedBrightnessForBlock(renderer.blockAccess, x, y, z), color, false);
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        inventory("merequester:block/requester");
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId,
        RenderBlocks renderer) {
        ForgeDirection forward = ForgeDirection.getOrientation(BlockMERequester.front(world.getBlockMetadata(x, y, z)));
        ForgeDirection right = forward.offsetZ == 1 ? ForgeDirection.EAST
            : forward.offsetZ == -1 ? ForgeDirection.WEST
                : forward.offsetX == 1 ? ForgeDirection.NORTH : ForgeDirection.SOUTH;
        boolean active = world.getTileEntity(x, y, z) instanceof TileMERequester requester && requester.renderActive();
        draw(active ? "merequester:block/requester_active" : "merequester:block/requester", x + 0.5, y + 0.5, z + 0.5,
            right, ForgeDirection.UP, forward, block.getMixedBrightnessForBlock(world, x, y, z), AEColor.Transparent,
            false);
        return true;
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return true;
    }

    @Override
    public int getRenderId() {
        return BlockMERequester.renderId;
    }

    private static void draw(String model, double x, double y, double z, ForgeDirection right, ForgeDirection up,
        ForgeDirection forward, int brightness, AEColor color, boolean inventory) {
        Tessellator tess = Tessellator.instance;
        Quad[] mesh = models.get(model);
        if (mesh == null) throw new IllegalStateException("Unbaked stock model: " + model);
        for (Quad quad : mesh) {
            int nx = quad.normal.offsetX * right.offsetX + quad.normal.offsetY * up.offsetX
                + quad.normal.offsetZ * forward.offsetX;
            int ny = quad.normal.offsetX * right.offsetY + quad.normal.offsetY * up.offsetY
                + quad.normal.offsetZ * forward.offsetY;
            int nz = quad.normal.offsetX * right.offsetZ + quad.normal.offsetY * up.offsetZ
                + quad.normal.offsetZ * forward.offsetZ;
            int tint = switch (quad.tint) {
                case 1 -> color.blackVariant;
                case 2 -> color.mediumVariant;
                case 3, 4 -> color.whiteVariant;
                default -> 0xffffff;
            };
            float shade = inventory || !quad.shade || quad.fullBright ? 1
                : ny > 0 ? 1 : ny < 0 ? 0.5f : nz != 0 ? 0.8f : 0.6f;
            tess.setBrightness(quad.fullBright ? 0xf000f0 : brightness);
            tess.setColorOpaque_F((tint >> 16 & 255) / 255f * shade, (tint >> 8 & 255) / 255f * shade,
                (tint & 255) / 255f * shade);
            tess.setNormal(nx, ny, nz);
            for (int vertex = 0; vertex < 4; vertex++) {
                double vx = quad.vertices[vertex * 3], vy = quad.vertices[vertex * 3 + 1],
                    vz = quad.vertices[vertex * 3 + 2];
                tess.addVertexWithUV(x + vx * right.offsetX + vy * up.offsetX + vz * forward.offsetX,
                    y + vx * right.offsetY + vy * up.offsetY + vz * forward.offsetY,
                    z + vx * right.offsetZ + vy * up.offsetZ + vz * forward.offsetZ, quad.uv[vertex * 2],
                    quad.uv[vertex * 2 + 1]);
            }
        }
    }

    private static Quad[] bake(String identifier) {
        JsonObject definition = definition(identifier);
        JsonObject bindings = definition.getAsJsonObject("textures");
        ArrayList<Quad> quads = new ArrayList<>();
        for (JsonElement value : definition.getAsJsonArray("elements")) {
            JsonObject element = value.getAsJsonObject();
            double[] from = vector(element.getAsJsonArray("from")), to = vector(element.getAsJsonArray("to"));
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject("faces").entrySet()) {
                ForgeDirection face = ForgeDirection.valueOf(entry.getKey().toUpperCase(Locale.ROOT));
                JsonObject details = entry.getValue().getAsJsonObject();
                String texture = details.get("texture").getAsString();
                for (int depth = 0; texture.startsWith("#") && depth < 16; depth++)
                    texture = bindings.get(texture.substring(1)).getAsString();
                IIcon icon = icon(texture);
                double[] uv = details.has("uv") ? vector(details.getAsJsonArray("uv")) : defaultUV(face, from, to);
                double[] vertices = vertices(face, from, to);
                double[] spriteUV = new double[8];
                int rotation = details.has("rotation") ? details.get("rotation").getAsInt() / 90 : 0;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int corner = (vertex + rotation) & 3;
                    spriteUV[vertex * 2] = icon.getInterpolatedU(corner < 2 ? uv[0] : uv[2]);
                    spriteUV[vertex * 2 + 1] = icon.getInterpolatedV(corner == 0 || corner == 3 ? uv[1] : uv[3]);
                    // Modern models face north; the AE helper faces south. Rotate both X and Z, preserving UV
                    // handedness.
                    vertices[vertex * 3] = 0.5 - vertices[vertex * 3] / 16;
                    vertices[vertex * 3 + 1] = vertices[vertex * 3 + 1] / 16 - 0.5;
                    vertices[vertex * 3 + 2] = 0.5 - vertices[vertex * 3 + 2] / 16;
                }
                ForgeDirection normal = face == ForgeDirection.UP || face == ForgeDirection.DOWN ? face
                    : face.getOpposite();
                JsonObject light = details.getAsJsonObject("neoforge_data");
                quads.add(new Quad(vertices, spriteUV, normal,
                    details.has("tintindex") ? details.get("tintindex").getAsInt() : -1,
                    !element.has("shade") || element.get("shade").getAsBoolean(),
                    light != null && light.has("block_light") && light.get("block_light").getAsInt() > 0));
            }
        }
        Quad[] result = quads.toArray(new Quad[0]);
        return result;
    }

    private static JsonObject definition(String identifier) {
        JsonObject cached = definitions.get(identifier);
        if (cached != null) return cached;
        JsonObject own = readJson(identifier.replace(':', '/'));
        JsonObject result = new JsonObject(), textures = new JsonObject();
        if (
            own.has("parent") && !own.get("parent").getAsString().startsWith("minecraft:")
                && own.get("parent").getAsString().contains(":")
        ) {
            JsonObject parent = definition(own.get("parent").getAsString());
            for (Map.Entry<String, JsonElement> entry : parent.entrySet()) result.add(entry.getKey(), entry.getValue());
            if (parent.has("textures"))
                for (Map.Entry<String, JsonElement> entry : parent.getAsJsonObject("textures").entrySet())
                    textures.add(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, JsonElement> entry : own.entrySet()) result.add(entry.getKey(), entry.getValue());
        if (own.has("textures")) for (Map.Entry<String, JsonElement> entry : own.getAsJsonObject("textures").entrySet())
            textures.add(entry.getKey(), entry.getValue());
        result.add("textures", textures);
        definitions.put(identifier, result);
        return result;
    }

    private static JsonObject readJson(String path) {
        try (InputStream stream = StockModelRenderer.class
            .getResourceAsStream("/META-INF/me-stock-port/models/" + path + ".json")) {
            if (stream == null) throw new IllegalStateException("Missing stock model: " + path);
            return new JsonParser().parse(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception error) {
            throw new IllegalStateException("Cannot load stock model: " + path, error);
        }
    }

    private static double[] vector(JsonArray values) {
        double[] result = new double[values.size()];
        for (int i = 0; i < result.length; i++) result[i] = values.get(i).getAsDouble();
        return result;
    }

    private static double[] defaultUV(ForgeDirection face, double[] from, double[] to) {
        return switch (face) {
            case DOWN -> new double[] { from[0], 16 - to[2], to[0], 16 - from[2] };
            case UP -> new double[] { from[0], from[2], to[0], to[2] };
            case NORTH -> new double[] { 16 - to[0], 16 - to[1], 16 - from[0], 16 - from[1] };
            case SOUTH -> new double[] { from[0], 16 - to[1], to[0], 16 - from[1] };
            case WEST -> new double[] { from[2], 16 - to[1], to[2], 16 - from[1] };
            case EAST -> new double[] { 16 - to[2], 16 - to[1], 16 - from[2], 16 - from[1] };
            default -> throw new IllegalArgumentException("Unknown model face");
        };
    }

    private static double[] vertices(ForgeDirection face, double[] from, double[] to) {
        double x0 = from[0], y0 = from[1], z0 = from[2], x1 = to[0], y1 = to[1], z1 = to[2];
        return switch (face) {
            case NORTH -> new double[] { x1, y1, z0, x1, y0, z0, x0, y0, z0, x0, y1, z0 };
            case SOUTH -> new double[] { x0, y1, z1, x0, y0, z1, x1, y0, z1, x1, y1, z1 };
            case WEST -> new double[] { x0, y1, z0, x0, y0, z0, x0, y0, z1, x0, y1, z1 };
            case EAST -> new double[] { x1, y1, z1, x1, y0, z1, x1, y0, z0, x1, y1, z0 };
            case UP -> new double[] { x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0 };
            case DOWN -> new double[] { x0, y0, z1, x0, y0, z0, x1, y0, z0, x1, y0, z1 };
            default -> throw new IllegalArgumentException("Unknown model face");
        };
    }

    private static final class Quad {

        final double[] vertices, uv;
        final ForgeDirection normal;
        final int tint;
        final boolean shade, fullBright;

        Quad(double[] vertices, double[] uv, ForgeDirection normal, int tint, boolean shade, boolean fullBright) {
            this.vertices = vertices;
            this.uv = uv;
            this.normal = normal;
            this.tint = tint;
            this.shade = shade;
            this.fullBright = fullBright;
        }
    }
}
