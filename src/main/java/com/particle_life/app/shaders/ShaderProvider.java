package com.particle_life.app.shaders;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;

import java.util.ArrayList;
import java.util.List;

public class ShaderProvider implements InfoWrapperProvider<ParticleShader> {
    @Override
    public List<InfoWrapper<ParticleShader>> create() {
        List<InfoWrapper<ParticleShader>> shaders = new ArrayList<>();

        shaders.add(new InfoWrapper<>("default",
                new ParticleShader("shaders/default.vert", "shaders/default.geom", "shaders/default.frag")));
        shaders.add(new InfoWrapper<>("repeat", "Draws copies of particles (without colors) beyond the borders. This is helpful if \"wrap\" is enabled.",
                new ParticleShader("shaders/default.vert", "shaders/repeat.geom", "shaders/default.frag")));
        shaders.add(new InfoWrapper<>("velocity brightness", "Brightness based on velocity.",
                new ParticleShader("shaders/speed_fade.vert", "shaders/default.geom", "shaders/default.frag")));
        shaders.add(new InfoWrapper<>("velocity color", "Color based on velocity. Ignores the current color palette.",
                new ParticleShader("shaders/speed.vert", "shaders/default.geom", "shaders/default.frag")));

        return shaders;
    }
}
