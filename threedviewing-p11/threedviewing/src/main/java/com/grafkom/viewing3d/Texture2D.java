package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;

public class Texture2D {
    private int textureID;
    private boolean isBilinear = true;

    public Texture2D(String resourcePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        setFilterMode(true);

        stbi_set_flip_vertically_on_load(true);

        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                System.err.println("Texture resource not found: " + resourcePath);
                return;
            }
            byte[] bytes = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(bytes.length);
            buffer.put(bytes).flip();

            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);

                ByteBuffer data = stbi_load_from_memory(buffer, width, height, channels, 4);
                if (data != null) {
                    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width.get(0), height.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                    glGenerateMipmap(GL_TEXTURE_2D);
                    stbi_image_free(data);
                    System.out.println("Texture loaded successfully!");
                } else {
                    System.err.println("Failed to load texture data: " + stbi_failure_reason());
                }
            } finally {
                MemoryUtil.memFree(buffer);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setFilterMode(boolean bilinearMipmapped) {
        this.isBilinear = bilinearMipmapped;
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (isBilinear) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public boolean isBilinear() {
        return isBilinear;
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() {
        glDeleteTextures(textureID);
    }
}
