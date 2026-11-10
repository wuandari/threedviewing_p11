package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class ProjectionTaxonomyManager {
    public enum ProjectionType {
        ORTHOGRAPHIC, ISOMETRIC, OBLIQUE_CAVALIER,
        PERSPECTIVE_1POINT, PERSPECTIVE_2POINT, PERSPECTIVE_3POINT
    }

    public static Matrix4f getProjectionMatrix(ProjectionType type, float aspect, float orthoBounds) {
        Matrix4f proj = new Matrix4f();

        switch (type) {
            case ORTHOGRAPHIC:
                proj.ortho(-orthoBounds * aspect, orthoBounds * aspect, -orthoBounds, orthoBounds, 1.0f, 50.0f);
                break;
            case ISOMETRIC:
                proj.ortho(-orthoBounds * aspect, orthoBounds * aspect, -orthoBounds, orthoBounds, 1.0f, 50.0f);
                proj.rotateX((float) Math.toRadians(35.264));
                proj.rotateY((float) Math.toRadians(45.0));
                break;
            case OBLIQUE_CAVALIER:
                proj.ortho(-orthoBounds * aspect, orthoBounds * aspect, -orthoBounds, orthoBounds, 1.0f, 50.0f);
                Matrix4f shear = new Matrix4f(
                    1.0f, 0.0f, 0.0f, 0.0f,
                    0.0f, 1.0f, 0.0f, 0.0f,
                    (float) Math.cos(Math.toRadians(45.0)), (float) Math.sin(Math.toRadians(45.0)), 1.0f, 0.0f,
                    0.0f, 0.0f, 0.0f, 1.0f
                );
                proj.mul(shear);
                break;
            case PERSPECTIVE_1POINT:
                proj.perspective((float) Math.toRadians(45.0), aspect, 0.1f, 50.0f);
                break;
            case PERSPECTIVE_2POINT:
                proj.perspective((float) Math.toRadians(45.0), aspect, 0.1f, 50.0f);
                proj.rotateY((float) Math.toRadians(30.0));
                break;
            case PERSPECTIVE_3POINT:
                proj.perspective((float) Math.toRadians(45.0), aspect, 0.1f, 50.0f);
                proj.rotateY((float) Math.toRadians(30.0));
                proj.rotateX((float) Math.toRadians(20.0));
                break;
        }
        return proj;
    }
}
