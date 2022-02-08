package com.particle_life.app.color;

public class Color {

    public float r, g, b, a;

    public Color(float r, float g, float b, float a) {
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    }

    public Color() {
        this(0, 0, 0, 1);
    }
}
