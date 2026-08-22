package com.particle_life.app;

import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;

class PaletteUtils {

    static Color[] getColors(int n, Palette palette) {
        Color[] colors = new Color[n];
        for (int i = 0; i < n; i++) {
            colors[i] = palette.getColor(i, n);
        }
        return colors;
    }
}
