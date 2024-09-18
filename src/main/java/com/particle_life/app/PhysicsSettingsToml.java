package com.particle_life.app;

import com.particle_life.PhysicsSettings;
import com.particle_life.app.toml_util.TomlFile;
import com.particle_life.app.toml_util.TomlKey;

public class PhysicsSettingsToml extends TomlFile {

    @TomlKey("boundaries")
    public String boundaries = "periodic";
    @TomlKey("radius")
    public double rmax = 0.02;
    @TomlKey("friction")
    public double friction = 0.85;
    @TomlKey("force")
    public double force = 1.0;

    public static PhysicsSettingsToml fromPhysicsSettings(PhysicsSettings s) {
        PhysicsSettingsToml toml = new PhysicsSettingsToml();
        toml.boundaries = s.wrap ? "periodic" : "clamped";
        toml.rmax = s.rmax;
        toml.friction = s.friction;
        toml.force = s.force;
        return toml;
    }

    public void toPhysicsSettings(PhysicsSettings s) {
        s.wrap = boundaries.equals("periodic");
        s.rmax = rmax;
        s.friction = friction;
        s.force = force;
        // Note: PhysicsSettings.dt is not saved in the toml file.
        // Instead, it is read from AppSettings.dt and re-set each iteration.
    }
}
