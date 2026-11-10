package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera3D {
    private Vector3f eye;
    private Vector3f target;
    private Vector3f up;

    public Camera3D(Vector3f eye, Vector3f target, Vector3f up) {
        this.eye = new Vector3f(eye);
        this.target = new Vector3f(target);
        this.up = new Vector3f(up);
    }

    public Matrix4f calculateLookAtMatrix() {
        return new Matrix4f().lookAt(eye, target, up);
    }

    public Vector3f getEyePosition() {
        return eye;
    }

    public void updateOrbitalPosition(float radius, float azimuthDeg, float elevationDeg) {
        float azRad = (float) Math.toRadians(azimuthDeg);
        float elRad = (float) Math.toRadians(elevationDeg);

        this.eye.x = target.x + radius * (float)(Math.cos(elRad) * Math.sin(azRad));
        this.eye.y = target.y + radius * (float)(Math.sin(elRad));
        this.eye.z = target.z + radius * (float)(Math.cos(elRad) * Math.cos(azRad));
    }
}
