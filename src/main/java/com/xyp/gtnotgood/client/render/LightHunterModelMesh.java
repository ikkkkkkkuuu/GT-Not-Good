// SPDX-License-Identifier: GPL-3.0-only
package com.xyp.gtnotgood.client.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
final class LightHunterModelMesh {

    private final float[] vertices;
    private int displayList;

    private LightHunterModelMesh(float[] vertices) {
        this.vertices = vertices;
    }

    static LightHunterModelMesh load(ResourceLocation location) throws IOException {
        List<float[]> positions = new ArrayList<>(), textures = new ArrayList<>(), normals = new ArrayList<>();
        List<Float> vertices = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
            new InputStreamReader(Minecraft.getMinecraft().getResourceManager().getResource(location).getInputStream(),
                StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.trim().split("\\s+");
                switch (values[0]) {
                    case "v" -> positions.add(vector(values, 3));
                    case "vt" -> textures.add(vector(values, 2));
                    case "vn" -> normals.add(vector(values, 3));
                    case "f" -> {
                        for (int index = 2; index < values.length - 1; index++) {
                            vertex(vertices, values[1], positions, textures, normals);
                            vertex(vertices, values[index], positions, textures, normals);
                            vertex(vertices, values[index + 1], positions, textures, normals);
                        }
                    }
                    default -> {}
                }
            }
        }
        if (vertices.isEmpty()) throw new IOException("Empty Light Hunter OBJ: " + location);
        float[] data = new float[vertices.size()];
        for (int index = 0; index < data.length; index++) data[index] = vertices.get(index);
        return new LightHunterModelMesh(data);
    }

    private static float[] vector(String[] values, int count) {
        float[] result = new float[count];
        for (int index = 0; index < count; index++) result[index] = Float.parseFloat(values[index + 1]);
        return result;
    }

    private static void vertex(List<Float> result, String reference, List<float[]> positions, List<float[]> textures,
        List<float[]> normals) {
        String[] indices = reference.split("/", -1);
        float[] position = entry(positions, indices[0]);
        float[] texture = indices.length > 1 && !indices[1].isEmpty() ? entry(textures, indices[1])
            : new float[] { 0, 0 };
        float[] normal = indices.length > 2 && !indices[2].isEmpty() ? entry(normals, indices[2])
            : new float[] { 0, 1, 0 };
        for (float value : position) result.add(value);
        result.add(texture[0]);
        result.add(1 - texture[1]);
        for (float value : normal) result.add(value);
    }

    private static float[] entry(List<float[]> entries, String reference) {
        int index = Integer.parseInt(reference);
        return entries.get(index < 0 ? entries.size() + index : index - 1);
    }

    void draw() {
        if (displayList == 0) {
            displayList = GL11.glGenLists(1);
            if (displayList == 0) throw new IllegalStateException("Unable to allocate Light Hunter model list");
            GL11.glNewList(displayList, GL11.GL_COMPILE);
            GL11.glBegin(GL11.GL_TRIANGLES);
            for (int index = 0; index < vertices.length; index += 8) {
                GL11.glNormal3f(vertices[index + 5], vertices[index + 6], vertices[index + 7]);
                GL11.glTexCoord2f(vertices[index + 3], vertices[index + 4]);
                GL11.glVertex3f(vertices[index], vertices[index + 1], vertices[index + 2]);
            }
            GL11.glEnd();
            GL11.glEndList();
        }
        GL11.glCallList(displayList);
    }

    void release() {
        if (displayList != 0) GL11.glDeleteLists(displayList, 1);
        displayList = 0;
    }
}
