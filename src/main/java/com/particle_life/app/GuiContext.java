package com.particle_life.app;

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

class GuiContext {

    Loop loop;
    ExtendedPhysics physics;
    PhysicsSnapshot physicsSnapshot;
    AppSettings appSettings;
    PhysicsSettings settings;
    Cursor cursor;
    Clock renderClock;

    SelectionManager<ParticleShader> shaders;
    SelectionManager<Palette> palettes;
    SelectionManager<MatrixGenerator> matrixGenerators;
    SelectionManager<PositionSetter> positionSetters;
    SelectionManager<TypeSetter> typeSetters;
    SelectionManager<CursorShape> cursorShapes;
    SelectionManager<CursorAction> cursorActions1;
    SelectionManager<CursorAction> cursorActions2;

    int particleCount;
    int preferredNumberOfThreads;
    int cursorParticleCount;
    boolean traces;

    float scale;
    int width;
    int height;

    int typeCountDiagramStepSize;
    boolean typeCountDisplayPercentage;
    long physicsNotReactingThreshold;

    ImBoolean showGui;
    ImBoolean showGraphicsWindow;
    ImBoolean showControlsWindow;
    ImBoolean showAboutWindow;
    ImBoolean showSavesPopup;

    ImString saveName;
    ImGuiCardView.Card[] saveCards;
    AtomicBoolean requestedSaveCardsLoading;
    boolean requestedSaveImage;
    File selectedSaveFile;

    Consumer<Exception> reportError;
    Runnable restartPhysics;
    Consumer<Boolean> resetCamera;
    Runnable onSaveRequested;
    Consumer<File> onLoadSave;
    Runnable onLoadSaveCards;
    Runnable closeApp;
    Consumer<Boolean> setFullscreen;
    java.util.function.Supplier<Boolean> isFullscreen;
    Runnable zoomIn;
    Runnable zoomOut;

    String appVersion;
    String javaHome;
    String jvmVersion;
    String lwjglVersion;
    String openGlVendor;
    String openGlRenderer;
    String openGlVersion;
    String openGlProfile;
    String glslVersion;
}
