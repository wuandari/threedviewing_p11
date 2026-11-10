package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class Mesh3D {
    private int vaoID;
    private int vboID;
    private int eboID;
    private int vertexCount;

    public Mesh3D(float[] vertices, int[] indices) {
        this.vertexCount = indices.length;

        vaoID = glGenVertexArrays();
        glBindVertexArray(vaoID);

        FloatBuffer vBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vBuffer.put(vertices).flip();

        vboID = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboID);
        glBufferData(GL_ARRAY_BUFFER, vBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(vBuffer);

        IntBuffer iBuffer = MemoryUtil.memAllocInt(indices.length);
        iBuffer.put(indices).flip();

        eboID = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboID);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, iBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(iBuffer);

        int stride = 8 * Float.BYTES; // 3 pos, 3 normal, 2 uv
        // Position
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        // Normal
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        // UV TexCoords
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }

    public void render() {
        glBindVertexArray(vaoID);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glDeleteBuffers(vboID);
        glDeleteBuffers(eboID);

        glBindVertexArray(0);
        glDeleteVertexArrays(vaoID);
    }

    public static Mesh3D createCube(float size) {
        float s = size / 2.0f;
        float[] vertices = {
            // Front Face (Normal +Z)
            -s, -s,  s,  0, 0, 1,  0, 0,
             s, -s,  s,  0, 0, 1,  1, 0,
             s,  s,  s,  0, 0, 1,  1, 1,
            -s,  s,  s,  0, 0, 1,  0, 1,

            // Back Face (Normal -Z)
            -s, -s, -s,  0, 0,-1,  1, 0,
            -s,  s, -s,  0, 0,-1,  1, 1,
             s,  s, -s,  0, 0,-1,  0, 1,
             s, -s, -s,  0, 0,-1,  0, 0,

            // Top Face (Normal +Y)
            -s,  s, -s,  0, 1, 0,  0, 1,
            -s,  s,  s,  0, 1, 0,  0, 0,
             s,  s,  s,  0, 1, 0,  1, 0,
             s,  s, -s,  0, 1, 0,  1, 1,

            // Bottom Face (Normal -Y)
            -s, -s, -s,  0,-1, 0,  0, 0,
             s, -s, -s,  0,-1, 0,  1, 0,
             s, -s,  s,  0,-1, 0,  1, 1,
            -s, -s,  s,  0,-1, 0,  0, 1,

            // Right Face (Normal +X)
             s, -s, -s,  1, 0, 0,  1, 0,
             s,  s, -s,  1, 0, 0,  1, 1,
             s,  s,  s,  1, 0, 0,  0, 1,
             s, -s,  s,  1, 0, 0,  0, 0,

            // Left Face (Normal -X)
            -s, -s, -s, -1, 0, 0,  0, 0,
            -s, -s,  s, -1, 0, 0,  1, 0,
            -s,  s,  s, -1, 0, 0,  1, 1,
            -s,  s, -s, -1, 0, 0,  0, 1
        };

        int[] indices = {
             0,  1,  2,  2,  3,  0,
             4,  5,  6,  6,  7,  4,
             8,  9, 10, 10, 11,  8,
            12, 13, 14, 14, 15, 12,
            16, 17, 18, 18, 19, 16,
            20, 21, 22, 22, 23, 20
        };

        return new Mesh3D(vertices, indices);
    }

    public static Mesh3D createPlane(float size) {
        float s = size / 2.0f;
        float[] vertices = {
            -s, 0, -s,  0, 1, 0,  0, 0,
             s, 0, -s,  0, 1, 0,  4, 0,
             s, 0,  s,  0, 1, 0,  4, 4,
            -s, 0,  s,  0, 1, 0,  0, 4
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        return new Mesh3D(vertices, indices);
    }

    public static Mesh3D createSphere(float radius, int sectors, int stacks) {
        List<Float> vList = new ArrayList<>();
        List<Integer> iList = new ArrayList<>();

        float x, y, z, xy;
        float nx, ny, nz, lengthInv = 1.0f / radius;
        float s, t;

        float sectorStep = (float)(2 * Math.PI / sectors);
        float stackStep = (float)(Math.PI / stacks);
        float sectorAngle, stackAngle;

        for (int i = 0; i <= stacks; ++i) {
            stackAngle = (float)(Math.PI / 2 - i * stackStep);
            xy = radius * (float)Math.cos(stackAngle);
            z = radius * (float)Math.sin(stackAngle);

            for (int j = 0; j <= sectors; ++j) {
                sectorAngle = j * sectorStep;

                x = xy * (float)Math.cos(sectorAngle);
                y = xy * (float)Math.sin(sectorAngle);

                nx = x * lengthInv;
                ny = y * lengthInv;
                nz = z * lengthInv;

                s = (float)j / sectors;
                t = (float)i / stacks;

                vList.add(x); vList.add(y); vList.add(z);
                vList.add(nx); vList.add(ny); vList.add(nz);
                vList.add(s); vList.add(t);
            }
        }

        int k1, k2;
        for (int i = 0; i < stacks; ++i) {
            k1 = i * (sectors + 1);
            k2 = k1 + sectors + 1;

            for (int j = 0; j < sectors; ++j, ++k1, ++k2) {
                if (i != 0) {
                    iList.add(k1);
                    iList.add(k2);
                    iList.add(k1 + 1);
                }
                if (i != (stacks - 1)) {
                    iList.add(k1 + 1);
                    iList.add(k2);
                    iList.add(k2 + 1);
                }
            }
        }

        float[] vArr = new float[vList.size()];
        for (int i = 0; i < vList.size(); i++) vArr[i] = vList.get(i);

        int[] iArr = new int[iList.size()];
        for (int i = 0; i < iList.size(); i++) iArr[i] = iList.get(i);

        return new Mesh3D(vArr, iArr);
    }
}
