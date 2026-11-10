package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class ThreeDViewing {

    private long window;
    private int width = 1024;
    private int height = 768;

    private Camera3D camera;
    private StereoAnaglyphRenderer stereoRenderer;

    // Shader & FBO
    private ShaderProgram phongShader;
    private ShaderProgram shadowDepthShader;
    private ShadowMapFBO shadowMapFBO;
    private Texture2D woodTexture;

    // Meshes
    private Mesh3D cubeMesh;
    private Mesh3D planeMesh;
    private Mesh3D sphereMesh;

    // State Toggles
    private ProjectionTaxonomyManager.ProjectionType currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT;
    private boolean isStereoMode = false;
    private boolean useBlinnPhong = true;     // Key B
    private boolean useFlatShading = false;   // Key F
    private boolean enableDepthTest = true;   // Key Z
    private boolean enableCulling = true;     // Key C
    private boolean useTexture = true;        // Key T (Toggle texture filter / mode)
    private boolean enableShadows = true;     // Key H

    // Orbital Camera
    private float cameraRadius = 12.0f;
    private float orthoBounds = 6.0f;
    private float azimuth = 45.0f;
    private float elevation = 30.0f;

    // Lighting & Shadows
    private Vector3f lightPos = new Vector3f(4.0f, 8.0f, 4.0f);
    private Vector3f lightColor = new Vector3f(1.0f, 1.0f, 0.9f);

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void updateWindowTitle() {
        String title = String.format(
            "3D Visual Realism Suite | Proyeksi: %s | Shading: %s | Mode: %s | HSR(Z-Buf:%s, Cull:%s) | TexFilter: %s | Shadow: %s",
            currentProjection.name(),
            useBlinnPhong ? "Blinn-Phong" : "Phong",
            useFlatShading ? "FLAT" : "SMOOTH",
            enableDepthTest ? "ON" : "OFF",
            enableCulling ? "ON" : "OFF",
            woodTexture != null && woodTexture.isBilinear() ? "Bilinear" : "Nearest",
            enableShadows ? "ON" : "OFF"
        );
        glfwSetWindowTitle(window, title);
    }

    private void init() {
        if (!glfwInit()) throw new IllegalStateException("Gagal menginisialisasi GLFW!");

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "3D Visual Realism Suite - P11", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat jendela GLFW!");

        // Key Press Callback for Feature Toggles
        glfwSetKeyCallback(window, (win, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                switch (key) {
                    case GLFW_KEY_1 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.ORTHOGRAPHIC;
                    case GLFW_KEY_2 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.ISOMETRIC;
                    case GLFW_KEY_3 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.OBLIQUE_CAVALIER;
                    case GLFW_KEY_4 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_1POINT;
                    case GLFW_KEY_5 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_2POINT;
                    case GLFW_KEY_6 -> currentProjection = ProjectionTaxonomyManager.ProjectionType.PERSPECTIVE_3POINT;
                    case GLFW_KEY_S -> isStereoMode = !isStereoMode;
                    case GLFW_KEY_B -> useBlinnPhong = !useBlinnPhong;   // Indikator 1: Phong vs Blinn-Phong
                    case GLFW_KEY_F -> useFlatShading = !useFlatShading; // Indikator 2: Flat vs Smooth Shading
                    case GLFW_KEY_Z -> enableDepthTest = !enableDepthTest; // Indikator 3: Z-Buffer HSR
                    case GLFW_KEY_C -> enableCulling = !enableCulling;     // Indikator 3: Back-Face Culling
                    case GLFW_KEY_T -> {                                  // Indikator 4: Texture Filtering
                        if (woodTexture != null) {
                            woodTexture.setFilterMode(!woodTexture.isBilinear());
                        }
                    }
                    case GLFW_KEY_H -> enableShadows = !enableShadows;   // Indikator 5: Shadows Toggle
                }
                updateWindowTitle();
            }
        });

        // Mouse Scroll Zoom
        glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            cameraRadius = Math.max(3.0f, Math.min(40.0f, cameraRadius - (float)yoffset * 0.8f));
            orthoBounds = Math.max(2.0f, Math.min(20.0f, orthoBounds - (float)yoffset * 0.4f));
        });

        glfwSetFramebufferSizeCallback(window, (win, w, h) -> {
            this.width = w;
            this.height = h;
            glViewport(0, 0, w, h);
        });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);

        GL.createCapabilities();

        // Load Shaders & FBO
        phongShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        shadowDepthShader = new ShaderProgram("/shaders/shadow_depth.vert", "/shaders/shadow_depth.frag");
        shadowMapFBO = new ShadowMapFBO();

        // Load Texture
        String texPath = getClass().getResource("/textures/wood_checker.png").getPath();
        woodTexture = new Texture2D(texPath);

        // Meshes
        cubeMesh = Mesh3D.createCube(2.0f);
        planeMesh = Mesh3D.createPlane(16.0f);
        sphereMesh = Mesh3D.createSphere(1.2f, 32, 32);

        camera = new Camera3D(new Vector3f(0, 3, cameraRadius), new Vector3f(0, 1, 0), new Vector3f(0, 1, 0));
        stereoRenderer = new StereoAnaglyphRenderer();

        updateWindowTitle();
    }

    private void handleInput() {
        float speed = 1.5f;
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS) azimuth -= speed;
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) azimuth += speed;
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS) elevation = Math.min(89.0f, elevation + speed);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS) elevation = Math.max(-89.0f, elevation - speed);

        if (glfwGetKey(window, GLFW_KEY_EQUAL) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_ADD) == GLFW_PRESS) {
            cameraRadius = Math.max(3.0f, cameraRadius - 0.2f);
            orthoBounds = Math.max(2.0f, orthoBounds - 0.1f);
        }
        if (glfwGetKey(window, GLFW_KEY_MINUS) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_SUBTRACT) == GLFW_PRESS) {
            cameraRadius = Math.min(40.0f, cameraRadius + 0.2f);
            orthoBounds = Math.min(20.0f, orthoBounds + 0.1f);
        }
    }

    private Matrix4f calculateLightSpaceMatrix() {
        float near_plane = 1.0f, far_plane = 25.0f;
        Matrix4f lightProjection = new Matrix4f().ortho(-10.0f, 10.0f, -10.0f, 10.0f, near_plane, far_plane);
        Matrix4f lightView = new Matrix4f().lookAt(lightPos, new Vector3f(0.0f, 0.0f, 0.0f), new Vector3f(0.0f, 1.0f, 0.0f));
        return lightProjection.mul(lightView);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            handleInput();

            camera.updateOrbitalPosition(cameraRadius, azimuth, elevation);

            // Configure HSR States
            HSRManager.setupDepthTest(enableDepthTest);
            HSRManager.setupBackFaceCulling(enableCulling);

            Matrix4f lightSpaceMatrix = calculateLightSpaceMatrix();

            // PASS 1: Render Depth to Shadow Map
            if (enableShadows) {
                shadowMapFBO.bindForWriting();
                shadowDepthShader.use();
                shadowDepthShader.setUniform("lightSpaceMatrix", lightSpaceMatrix);
                renderSceneObjects(shadowDepthShader, true);
                shadowDepthShader.unbind();
                shadowMapFBO.unbind(width, height);
            }

            // PASS 2: Render Normal Lighting & Scene
            glClearColor(0.12f, 0.12f, 0.15f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            float aspect = (float) width / (float) height;
            Matrix4f projMatrix = ProjectionTaxonomyManager.getProjectionMatrix(currentProjection, aspect, orthoBounds);
            Matrix4f viewMatrix = camera.calculateLookAtMatrix();

            phongShader.use();
            phongShader.setUniform("projection", projMatrix);
            phongShader.setUniform("view", viewMatrix);
            phongShader.setUniform("lightSpaceMatrix", lightSpaceMatrix);

            phongShader.setUniform("lightPos", lightPos);
            phongShader.setUniform("viewPos", camera.getEyePosition());
            phongShader.setUniform("lightColor", lightColor);

            phongShader.setUniform("useBlinnPhong", useBlinnPhong);
            phongShader.setUniform("useFlatShading", useFlatShading);
            phongShader.setUniform("enableShadows", enableShadows);

            if (woodTexture != null) {
                woodTexture.bind(0);
                phongShader.setUniform("textureSampler", 0);
            }

            if (enableShadows) {
                shadowMapFBO.bindTexture(1);
                phongShader.setUniform("shadowMap", 1);
            }

            if (isStereoMode) {
                stereoRenderer.renderAnaglyph(() -> renderSceneObjects(phongShader, false));
            } else {
                renderSceneObjects(phongShader, false);
            }

            phongShader.unbind();

            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }

    private void renderSceneObjects(ShaderProgram shader, boolean isShadowPass) {
        // 1. Render Ground Plane
        Matrix4f planeModel = new Matrix4f().translate(0.0f, 0.0f, 0.0f);
        shader.setUniform("model", planeModel);
        if (!isShadowPass) {
            shader.setUniform("objectColor", new Vector3f(0.6f, 0.6f, 0.6f));
            shader.setUniform("useTexture", true);
        }
        planeMesh.render();

        // 2. Render Cube (Left Object)
        Matrix4f cubeModel = new Matrix4f().translate(-2.0f, 1.0f, 0.0f).rotateY((float) Math.toRadians(25));
        shader.setUniform("model", cubeModel);
        if (!isShadowPass) {
            shader.setUniform("objectColor", new Vector3f(0.8f, 0.2f, 0.2f));
            shader.setUniform("useTexture", true);
        }
        cubeMesh.render();

        // 3. Render Sphere (Right Object - smooth surface showcase)
        Matrix4f sphereModel = new Matrix4f().translate(2.0f, 1.2f, 0.0f);
        shader.setUniform("model", sphereModel);
        if (!isShadowPass) {
            shader.setUniform("objectColor", new Vector3f(0.2f, 0.7f, 0.9f));
            shader.setUniform("useTexture", false);
        }
        sphereMesh.render();
    }

    private void cleanup() {
        if (cubeMesh != null) cubeMesh.cleanup();
        if (planeMesh != null) planeMesh.cleanup();
        if (sphereMesh != null) sphereMesh.cleanup();

        if (woodTexture != null) woodTexture.cleanup();
        if (shadowMapFBO != null) shadowMapFBO.cleanup();

        if (phongShader != null) phongShader.cleanup();
        if (shadowDepthShader != null) shadowDepthShader.cleanup();

        glfwDestroyWindow(window);
        glfwTerminate();
    }

    public static void main(String[] args) {
        new ThreeDViewing().run();
    }
}
