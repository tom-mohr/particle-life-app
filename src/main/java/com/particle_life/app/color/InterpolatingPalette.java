package com.particle_life.app.color;

public class InterpolatingPalette implements Palette {

    private final Color[] colors;

    public InterpolatingPalette(Color[] colors) {
        this.colors = colors;
    }

    @Override
    public Color getColor(int type, int nTypes) {

        nTypes = Math.max(1, nTypes);

        if (type < 0) {
            type = 0;
        } else if (type >= nTypes) {
            type = nTypes - 1;
        }

        float exactIndex = colors.length * type / (float) nTypes;

        int smallerIndex = (int) Math.floor(exactIndex);
        int largerIndex = (int) Math.ceil(exactIndex);

        if (smallerIndex == largerIndex) {
            return colors[smallerIndex];
        } else {
            Color a = colors[smallerIndex];
            Color b = colors[largerIndex];
            float f = exactIndex - smallerIndex;
            return interpolate(a, b, f);
        }
    }

    private Color interpolate(Color a, Color b, float f) {
        return new Color(
                lerp(a.r, b.r, f),
                lerp(a.g, b.g, f),
                lerp(a.b, b.b, f),
                lerp(a.a, b.a, f)
        );
    }

    private float lerp(float a, float b, float f) {
        return a + (b - a) * f;
    }
}
