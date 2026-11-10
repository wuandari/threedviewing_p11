package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

public class StereoAnaglyphRenderer {
    private float ipd = 0.2f;

    public void renderAnaglyph(Runnable renderSceneCallback) {
        // Left Eye (Red Channel)
        glColorMask(true, false, false, true);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderSceneCallback.run();

        // Right Eye (Cyan Channel)
        glColorMask(false, true, true, true);
        glClear(GL_DEPTH_BUFFER_BIT);
        renderSceneCallback.run();

        glColorMask(true, true, true, true);
    }
}
