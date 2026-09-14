package com.xyp.gtnotgood.client.flux;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderBlocks;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.IIcon;
import net.minecraft.world.IBlockAccess;
import net.minecraftforge.common.util.ForgeDirection;

import org.lwjgl.opengl.GL11;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xyp.gtnotgood.common.flux.BlockFluxConnector;
import com.xyp.gtnotgood.common.flux.TileFluxConnector;
import com.xyp.gtnotgood.utils.enums.ModList;

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;

/**
 * Renders the unmodified Flux Networks JSON cuboids and UVs through Minecraft 1.7.10's tessellator.
 * Original center and six rotated connection models are shared by world and inventory rendering.
 * Only the API adapter is new; model geometry and texture pixels remain upstream assets.
 */
public final class FluxConnectorRenderer implements ISimpleBlockRenderingHandler {

    private final Map<String, JsonArray> models = new HashMap<>();

    private JsonArray model(String name) {
        return models.computeIfAbsent(name, key -> {
            try (InputStreamReader reader = new InputStreamReader(
                Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(ModList.GTNotGood.getResourceLocation("models/flux/" + key + ".json"))
                    .getInputStream(),
                StandardCharsets.UTF_8)) {
                return new JsonParser().parse(reader)
                    .getAsJsonObject()
                    .getAsJsonArray("elements");
            } catch (java.io.IOException e) {
                throw new IllegalStateException("Missing Flux model: " + key, e);
            }
        });
    }

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
        GL11.glPushMatrix();
        GL11.glTranslatef(-0.5F, -0.5F, -0.5F);
        Tessellator.instance.startDrawingQuads();
        render((BlockFluxConnector) block, null, 0, 0, 0);
        Tessellator.instance.draw();
        GL11.glPopMatrix();
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int id,
        RenderBlocks renderer) {
        Tessellator.instance.setBrightness(block.getMixedBrightnessForBlock(world, x, y, z));
        render((BlockFluxConnector) block, world, x, y, z);
        return true;
    }

    private void render(BlockFluxConnector block, IBlockAccess world, int x, int y, int z) {
        boolean active = world == null || block instanceof com.xyp.gtnotgood.common.flux.BlockFluxLogistics
            || world.getTileEntity(x, y, z) instanceof TileFluxConnector tile && tile.enabled();
        drawModel(block, model(block.plug ? "fluxplug" : "fluxpoint"), active, -1, x, y, z, world == null);
        for (ForgeDirection side : ForgeDirection.VALID_DIRECTIONS) {
            if (world == null || block.connects(world, x, y, z, side)) {
                drawModel(block, model("fluxconnection"), active, side.ordinal(), x, y, z, world == null);
            }
        }
    }

    private void drawModel(BlockFluxConnector block, JsonArray elements, boolean active, int rotation, int x, int y,
        int z, boolean inventory) {
        Tessellator t = Tessellator.instance;
        for (JsonElement entry : elements) {
            JsonObject element = entry.getAsJsonObject();
            double[] a = vector(element.getAsJsonArray("from")), b = vector(element.getAsJsonArray("to"));
            for (Map.Entry<String, JsonElement> faceEntry : element.getAsJsonObject("faces")
                .entrySet()) {
                JsonObject face = faceEntry.getValue()
                    .getAsJsonObject();
                boolean tint = face.get("texture")
                    .getAsString()
                    .equals("#colour");
                IIcon icon = block.texture(active, tint);
                ForgeDirection normal = ForgeDirection.valueOf(
                    faceEntry.getKey()
                        .toUpperCase(java.util.Locale.ROOT));
                double[][] vertices = vertices(normal, a, b);
                double[] n = rotate(normal.offsetX, normal.offsetY, normal.offsetZ, rotation);
                t.setNormal((float) n[0], (float) n[1], (float) n[2]);
                float shade = inventory ? 1 : n[1] > 0 ? 1 : n[1] < 0 ? 0.5F : n[0] != 0 ? 0.6F : 0.8F;
                t.setColorOpaque_F((tint ? 0.25F : 1) * shade, (tint ? 0.65F : 1) * shade, shade);
                JsonArray uv = face.getAsJsonArray("uv");
                double u0 = icon.getInterpolatedU(
                    uv.get(0)
                        .getAsDouble());
                double v0 = icon.getInterpolatedV(
                    uv.get(1)
                        .getAsDouble());
                double u1 = icon.getInterpolatedU(
                    uv.get(2)
                        .getAsDouble());
                double v1 = icon.getInterpolatedV(
                    uv.get(3)
                        .getAsDouble());
                // Reverse winding without changing the UV assigned to each vertex.
                for (int i = 3; i >= 0; i--) {
                    double[] p = rotate(vertices[i][0] - 0.5, vertices[i][1] - 0.5, vertices[i][2] - 0.5, rotation);
                    double epsilon = tint ? 0.0001 : 0;
                    t.addVertexWithUV(
                        x + p[0] + 0.5 + n[0] * epsilon,
                        y + p[1] + 0.5 + n[1] * epsilon,
                        z + p[2] + 0.5 + n[2] * epsilon,
                        i < 2 ? u0 : u1,
                        i == 0 || i == 3 ? v1 : v0);
                }
            }
        }
    }

    private static double[] vector(JsonArray array) {
        return new double[] { array.get(0)
            .getAsDouble() / 16,
            array.get(1)
                .getAsDouble() / 16,
            array.get(2)
                .getAsDouble() / 16 };
    }

    /** Rotates the upstream downward connector around the block center to its blockstate orientation. */
    private static double[] rotate(double x, double y, double z, int side) {
        return switch (side) {
            case 1 -> new double[] { x, -y, -z };
            case 2 -> new double[] { -x, z, y };
            case 3 -> new double[] { x, z, -y };
            case 4 -> new double[] { y, z, x };
            case 5 -> new double[] { -y, z, -x };
            default -> new double[] { x, y, z };
        };
    }

    private static double[][] vertices(ForgeDirection side, double[] a, double[] b) {
        double x = a[0], y = a[1], z = a[2], X = b[0], Y = b[1], Z = b[2];
        return switch (side) {
            case NORTH -> new double[][] { { X, y, z }, { X, Y, z }, { x, Y, z }, { x, y, z } };
            case SOUTH -> new double[][] { { x, y, Z }, { x, Y, Z }, { X, Y, Z }, { X, y, Z } };
            case WEST -> new double[][] { { x, y, z }, { x, Y, z }, { x, Y, Z }, { x, y, Z } };
            case EAST -> new double[][] { { X, y, Z }, { X, Y, Z }, { X, Y, z }, { X, y, z } };
            case UP -> new double[][] { { x, Y, Z }, { x, Y, z }, { X, Y, z }, { X, Y, Z } };
            default -> new double[][] { { x, y, z }, { x, y, Z }, { X, y, Z }, { X, y, z } };
        };
    }

    @Override
    public boolean shouldRender3DInInventory(int id) {
        return true;
    }

    @Override
    public int getRenderId() {
        return BlockFluxConnector.renderId;
    }
}
