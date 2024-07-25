package com.particle_life.app;

import com.particle_life.app.toml_util.TomlFile;
import com.particle_life.app.toml_util.TomlKey;

/**
 * Data class for app settings.
 * App settings are settings that should be saved between application restarts.
 * To clarify: There are OTHER settings which are NOT saved by this class, e.g.:
 * <ul>
 *     <li>temporary GUI configuration, such as the current camera position</li>
 *     <li>physics settings, as they are handled by a different mechanism</li>
 * </ul>
 * Supports loading and saving settings from/to a TOML file.
 * The settings file is created if it doesn't exist yet,
 * i.e. if the application is started for the first time,
 * or if the user deleted the settings file.
 */
public class AppSettings extends TomlFile {

    @TomlKey("fullscreen")
    public boolean startInFullscreen = true;
    @TomlKey("zoom_step_factor")
    public double zoomStepFactor = 1.2;
    @TomlKey("particle_size")
    public float particleSize = 4.0f;   // particle size on screen (in pixels)
    @TomlKey("particle_size_independent")
    public boolean keepParticleSizeIndependentOfZoom = false;
    @TomlKey("shift_smoothness")
    public double shiftSmoothness = 0.3;
    @TomlKey("zoom_smoothness")
    public double zoomSmoothness = 0.3;
    @TomlKey("cam_movement_speed")
    public double camMovementSpeed = 1.0;
    @TomlKey("brush_power")
    public int brushPower = 100;
    @TomlKey("gui_background_alpha")
    public float guiBackgroundAlpha = 1.0f;
    @TomlKey("matrix_step_size")
    public double matrixGuiStepSize = 0.2;
    @TomlKey("show_advanced_gui")
    public boolean showAdvancedGui = true;
    @TomlKey("palette")
    public String palette = "RainbowSmooth12.map";
    @TomlKey("shader")
    public String shader = "default";
}
