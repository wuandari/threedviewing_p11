package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL20.*;

public class ShaderProgram {
    private int programID;
    private Map<String, Integer> uniforms = new HashMap<>();

    public ShaderProgram(String vertResourcePath, String fragResourcePath) {
        String vertCode = loadShaderResource(vertResourcePath);
        String fragCode = loadShaderResource(fragResourcePath);

        int vertID = compileShader(vertCode, GL_VERTEX_SHADER);
        int fragID = compileShader(fragCode, GL_FRAGMENT_SHADER);

        programID = glCreateProgram();
        glAttachShader(programID, vertID);
        glAttachShader(programID, fragID);
        glLinkProgram(programID);

        if (glGetProgrami(programID, GL_LINK_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error linking shader program: " + glGetProgramInfoLog(programID));
        }

        glDeleteShader(vertID);
        glDeleteShader(fragID);
    }

    private String loadShaderResource(String path) {
        try (InputStream is = getClass().getResourceAsStream(path)) {
            if (is == null) {
                throw new RuntimeException("Shader resource not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to read shader resource: " + path, e);
        }
    }

    private int compileShader(String code, int type) {
        int shaderID = glCreateShader(type);
        glShaderSource(shaderID, code);
        glCompileShader(shaderID);

        if (glGetShaderi(shaderID, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new RuntimeException("Error compiling shader (" + type + "): " + glGetShaderInfoLog(shaderID));
        }
        return shaderID;
    }

    public void use() {
        glUseProgram(programID);
    }

    public void unbind() {
        glUseProgram(0);
    }

    public void createUniform(String uniformName) {
        int location = glGetUniformLocation(programID, uniformName);
        if (location < 0) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found or unused.");
        }
        uniforms.put(uniformName, location);
    }

    public void setUniform(String name, Matrix4f value) {
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            value.get(buffer);
            int loc = uniforms.getOrDefault(name, glGetUniformLocation(programID, name));
            glUniformMatrix4fv(loc, false, buffer);
        }
    }

    public void setUniform(String name, Vector3f value) {
        int loc = uniforms.getOrDefault(name, glGetUniformLocation(programID, name));
        glUniform3f(loc, value.x, value.y, value.z);
    }

    public void setUniform(String name, int value) {
        int loc = uniforms.getOrDefault(name, glGetUniformLocation(programID, name));
        glUniform1i(loc, value);
    }

    public void setUniform(String name, boolean value) {
        int loc = uniforms.getOrDefault(name, glGetUniformLocation(programID, name));
        glUniform1i(loc, value ? 1 : 0);
    }

    public void cleanup() {
        unbind();
        if (programID != 0) {
            glDeleteProgram(programID);
        }
    }
}
