package com.particle_life.app.gui;

import com.particle_life.app.AppSettings;
import com.particle_life.app.ExtendedPhysics;
import com.particle_life.app.ImGuiCardView;
import com.particle_life.app.PhysicsSnapshot;
import com.particle_life.app.color.Palette;
import com.particle_life.app.cursors.Cursor;
import com.particle_life.app.cursors.CursorAction;
import com.particle_life.app.cursors.CursorShape;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.backend.*;
import imgui.type.ImBoolean;
import imgui.type.ImString;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class Context {

    public Loop loop;
    public ExtendedPhysics physics;
    public PhysicsSnapshot physicsSnapshot;
    public AppSettings appSettings;
    public PhysicsSettings settings;
    public Cursor cursor;
    public Clock renderClock;

    public SelectionManager<ParticleShader> shaders;
    public SelectionManager<Palette> palettes;
    public SelectionManager<MatrixGenerator> matrixGenerators;
    public SelectionManager<PositionSetter> positionSetters;
    public SelectionManager<TypeSetter> typeSetters;
    public SelectionManager<CursorShape> cursorShapes;
    public SelectionManager<CursorAction> cursorActions1;
    public SelectionManager<CursorAction> cursorActions2;

    public int particleCount;
    public int preferredNumberOfThreads;
    public int cursorParticleCount;
    public boolean traces;

    public float scale;
    public int width;
    public int height;

    public int typeCountDiagramStepSize;
    public boolean typeCountDisplayPercentage;
    public long physicsNotReactingThreshold;

    public ImBoolean showGui;
    public ImBoolean showGraphicsWindow;
    public ImBoolean showControlsWindow;
    public ImBoolean showAboutWindow;
    public ImBoolean showSavesPopup;

    public ImString saveName;
    public ImGuiCardView.Card[] saveCards;
    public AtomicBoolean requestedSaveCardsLoading;
    public boolean requestedSaveImage;
    public File selectedSaveFile;

    public Consumer<Exception> reportError;
    public Runnable restartPhysics;
    public Consumer<Boolean> resetCamera;
    public Runnable onSaveRequested;
    public Consumer<File> onLoadSave;
    public Runnable onLoadSaveCards;
    public Runnable closeApp;
    public Consumer<Boolean> setFullscreen;
    public java.util.function.Supplier<Boolean> isFullscreen;
    public Runnable zoomIn;
    public Runnable zoomOut;

    public String appVersion;
    public String javaHome;
    public String jvmVersion;
    public String lwjglVersion;
    public String openGlVendor;
    public String openGlRenderer;
    public String openGlVersion;
    public String openGlProfile;
    public String glslVersion;
}
