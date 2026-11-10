package com.grafkom.viewing3d;

import org.joml.Matrix4f;

public class PerspectiveProjection {
    private float fovDegree;
    private float aspectRatio;
    private float zNear;
    private float zFar;

    public PerspectiveProjection(float fovDegree, float aspectRatio, float zNear, float zFar) {
        this.fovDegree = fovDegree;
        this.aspectRatio = aspectRatio;
        this.zNear = zNear;
        this.zFar = zFar;
    }

    public Matrix4f calculateProjectionMatrix() {
        return new Matrix4f().perspective((float) Math.toRadians(fovDegree), aspectRatio, zNear, zFar);
    }

    public void setAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }
}
