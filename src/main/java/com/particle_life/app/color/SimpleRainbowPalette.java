package com.particle_life.app.color;

public class SimpleRainbowPalette implements Palette {
    @Override
    public Color getColor(int type, int nTypes) {
        double x = 2 * Math.PI * ((double) type / (double) nTypes);
        return new Color((float) Math.cos(x), (float) Math.cos(x - 2), (float) Math.cos(x - 4), 1);
    }
}
