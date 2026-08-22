package com.particle_life.app;

import com.particle_life.app.color.NaturalRainbowPalette;
import com.particle_life.app.color.Palette;
import com.particle_life.app.color.PalettesProvider;
import com.particle_life.app.cursors.*;
import com.particle_life.app.io.MatrixIO;
import com.particle_life.app.io.ParticlesIO;
import com.particle_life.app.io.ResourceAccess;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.CursorShader;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.shaders.ShaderProvider;
import com.particle_life.app.utils.*;
import com.particle_life.backend.*;
import imgui.ImGui;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.Version;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL13C.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL30C.*;
import static org.lwjgl.opengl.GL32C.*;

public class Main extends App {

    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String JVM_VERSION = System.getProperty("java.vm.version");
    private static String APP_VERSION;
    private static String LWJGL_VERSION;
    private static String OPENGL_VENDOR;
    private static String OPENGL_RENDERER;
    private static String OPENGL_VERSION;
    private static String OPENGL_PROFILE;
    private static String GLSL_VERSION;

    public static void main(String[] args) {
        System.out.println("Java Home: " + JAVA_HOME);
        System.out.println("JVM Version: " + JVM_VERSION);

        Main main = new Main();
        try {
            main.appSettings.load(SETTINGS_FILE_NAME);
        } catch (IOException e) {
            main.error = new AppSettingsLoadException("Failed to load settings", e);
        }
        main.launch("Particle Life Simulator",
                main.appSettings.startInFullscreen,
                ".internal/favicon.png",
                // request OpenGL version 4.1 (corresponds to "#version 410" in shaders)
                4, 1
        );
    }

    private final AppSettings appSettings = new AppSettings();
    private static final String SETTINGS_FILE_NAME = "settings.toml";

    /* If this value is set, an error popup is displayed,
     * waiting for the user to close the app. */
    private Exception error = null;

    // data
    private final Clock renderClock = new Clock(60);
    private SelectionManager<ParticleShader> shaders;
    private SelectionManager<Palette> palettes;
    private SelectionManager<MatrixGenerator> matrixGenerators;
    private SelectionManager<PositionSetter> positionSetters;
    private SelectionManager<TypeSetter> typeSetters;
    private Cursor cursor;
    private CursorShader cursorShader;
    private SelectionManager<CursorShape> cursorShapes;
    private SelectionManager<CursorAction> cursorActions1;
    private SelectionManager<CursorAction> cursorActions2;

    // helper classes
    private final Matrix4d transform = new Matrix4d();
    private final ParticleRenderer particleRenderer = new ParticleRenderer();
    private final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();

    private final PhysicsSession physicsSession = new PhysicsSession();
    private ExtendedPhysics physics;
    private Loop loop;
    /**
     * The snapshot is used to store a deep copy of the physics state
     * (particles, physics settings, ...) just for this thread,
     * so that the physics simulation can continue modifying the data
     * in different threads in the meantime.
     * Otherwise, the renderer could get in trouble if it tries to
     * access the data while it is being modified by the physics simulation.
     */
    private PhysicsSnapshot physicsSnapshot;
    private LoadDistributor physicsSnapshotLoadDistributor;  // speed up taking snapshots with parallelization
    private final AtomicBoolean newSnapshotAvailable = physicsSession.newSnapshotAvailable;

    // local copy of snapshot:
    private PhysicsSettings settings;
    private int particleCount;
    private int preferredNumberOfThreads;
    private int cursorParticleCount = 0;

    // particle rendering: controls
    private boolean traces = false;
    private final Vector2d camPos = new Vector2d(0.5, 0.5);  // world center
    private final Vector2d camPosGoal = new Vector2d(camPos);
    private double camSize = 1.0;
    private double camSizeGoal = camSize;
    private final double MAX_CAM_SIZE = 20;
    private final InputState input = new InputState();

    // GUI: constants that control how the GUI behaves
    private long physicsNotReactingThreshold = 3000;  // time in milliseconds
    private int typeCountDiagramStepSize = 100;
    private boolean typeCountDisplayPercentage = false;

    // GUI: hide / show parts
    private final ImBoolean showGui = new ImBoolean(true);
    private final ImBoolean showGraphicsWindow = new ImBoolean(false);
    private final ImBoolean showControlsWindow = new ImBoolean(false);
    private final ImBoolean showAboutWindow = new ImBoolean(false);
    private final ImBoolean showSavesPopup = new ImBoolean(false);

    // GUI: widget state variables
    private final ImString saveName = new ImString();
    private ImGuiCardView.Card[] saveCards = new ImGuiCardView.Card[0];
    private final AtomicBoolean requestedSaveCardsLoading = new AtomicBoolean(true);
    private int[] saveImage = null;
    private static final int SAVE_IMAGE_SIZE = 256;
    private boolean requestedSaveImage = false;
    private File selectedSaveFile = null;

    // offscreen rendering buffers
    private MultisampledFramebuffer worldTexture;  // particles
    private MultisampledFramebuffer cursorTexture;  // cursor

    private final GuiContext guiContext = new GuiContext();

    @Override
    protected void setup() {
        try {
            APP_VERSION = ResourceAccess.readTextFile(".internal/version.txt").trim();
        } catch (IOException e) {
            APP_VERSION = "(version unknown)";
        }
        LWJGL_VERSION = Version.getVersion();
        OPENGL_VENDOR = glGetString(GL_VENDOR);
        OPENGL_RENDERER = glGetString(GL_RENDERER);
        OPENGL_VERSION = glGetString(GL_VERSION);
        int profileMask = glGetInteger(GL_CONTEXT_PROFILE_MASK);
        OPENGL_PROFILE = (profileMask & GL_CONTEXT_CORE_PROFILE_BIT) != 0 ? "Core" :
                (profileMask & GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0 ? "Compatibility" : "Unknown";
        GLSL_VERSION = glGetString(GL_SHADING_LANGUAGE_VERSION);

        System.out.println("Particle Life App " + APP_VERSION);
        System.out.println("LWJGL Version: " + LWJGL_VERSION);
        System.out.println("OpenGL Vendor: " + OPENGL_VENDOR);
        System.out.println("OpenGL Renderer: " + OPENGL_RENDERER);
        System.out.println("OpenGL Version: " + OPENGL_VERSION);
        System.out.println("OpenGL Profile: " + OPENGL_PROFILE);
        System.out.println("GLSL Version: " + GLSL_VERSION);

        glEnable(GL_MULTISAMPLE);

        // Method initializes LWJGL3 renderer.
        // This method SHOULD be called after you've initialized your ImGui configuration (fonts and so on).
        // ImGui context should be created as well.
        imGuiGl3.init("#version 410 core");

        particleRenderer.init();

        try {
            cursorShader = new CursorShader();
        } catch (IOException e) {
            this.error = e;
            return;
        }

        cursor = new Cursor();
        cursor.size = appSettings.cursorSize;

        try {
            shaders = new SelectionManager<>(new ShaderProvider());
            palettes = new SelectionManager<>(new PalettesProvider());
            matrixGenerators = new SelectionManager<>(new MatrixGeneratorProvider());
            positionSetters = new SelectionManager<>(new PositionSetterProvider());
            typeSetters = new SelectionManager<>(new TypeSetterProvider());
            cursorShapes = new SelectionManager<>(new CursorProvider());
            cursorActions1 = new SelectionManager<>(new CursorActionProvider());
            cursorActions2 = new SelectionManager<>(new CursorActionProvider());

            positionSetters.setActiveByName(appSettings.positionSetter);
            cursorActions1.setActiveByName(appSettings.cursorActionLeft);
            cursorActions2.setActiveByName(appSettings.cursorActionRight);
        } catch (Exception e) {
            this.error = e;
            return;
        }

        cursor.shape = cursorShapes.getActive();  // set initial cursor shape (would be null otherwise)

        try {
            shaders.setActiveByName(appSettings.shader);
        } catch (IllegalArgumentException e) {
            // todo: emit warning
            shaders.setActive(0);
        }

        physicsSession.create(
                positionSetters.getActive(),
                matrixGenerators.getActive(),
                typeSetters.getActive());
        physics = physicsSession.physics;
        physicsSnapshot = physicsSession.physicsSnapshot;
        physicsSnapshotLoadDistributor = physicsSession.physicsSnapshotLoadDistributor;
        physicsSession.start(this::updatePhysics);
        loop = physicsSession.loop;

        // set default selection for palette
        if (palettes.hasName(appSettings.palette)) {
            palettes.setActive(palettes.getIndexByName(appSettings.palette));
        }

        // generate offscreen frame buffer to render particles to a multisampled texture
        // and also a simple texture for converting the multisampled texture to a single-sampled texture
        // (this is necessary because ImGui can't handle multisampled textures in the drawlist)
        worldTexture = MultisampledFramebuffer.createLinearFiltered();

        // create offscreen framebuffer for cursor rendering
        cursorTexture = MultisampledFramebuffer.createLinearFiltered();

        initGuiContext();
    }

    private void initGuiContext() {
        guiContext.loop = loop;
        guiContext.physics = physics;
        guiContext.physicsSnapshot = physicsSnapshot;
        guiContext.appSettings = appSettings;
        guiContext.settings = settings;
        guiContext.cursor = cursor;
        guiContext.renderClock = renderClock;
        guiContext.shaders = shaders;
        guiContext.palettes = palettes;
        guiContext.matrixGenerators = matrixGenerators;
        guiContext.positionSetters = positionSetters;
        guiContext.typeSetters = typeSetters;
        guiContext.cursorShapes = cursorShapes;
        guiContext.cursorActions1 = cursorActions1;
        guiContext.cursorActions2 = cursorActions2;
        guiContext.showGui = showGui;
        guiContext.showGraphicsWindow = showGraphicsWindow;
        guiContext.showControlsWindow = showControlsWindow;
        guiContext.showAboutWindow = showAboutWindow;
        guiContext.showSavesPopup = showSavesPopup;
        guiContext.saveName = saveName;
        guiContext.saveCards = saveCards;
        guiContext.requestedSaveCardsLoading = requestedSaveCardsLoading;
        guiContext.typeCountDiagramStepSize = typeCountDiagramStepSize;
        guiContext.typeCountDisplayPercentage = typeCountDisplayPercentage;
        guiContext.physicsNotReactingThreshold = physicsNotReactingThreshold;
        guiContext.reportError = this::reportError;
        guiContext.restartPhysics = this::restartPhysics;
        guiContext.resetCamera = this::resetCamera;
        guiContext.onSaveRequested = () -> {
            selectedSaveFile = guiContext.selectedSaveFile;
            requestedSaveImage = true;
        };
        guiContext.onLoadSave = file -> loop.enqueue(() -> loadState(file));
        guiContext.onLoadSaveCards = this::loadSaveCards;
        guiContext.closeApp = this::close;
        guiContext.setFullscreen = this::setFullscreen;
        guiContext.isFullscreen = this::isFullscreen;
        guiContext.zoomIn = () -> camSizeGoal /= Math.pow(appSettings.zoomStepFactor, 2);
        guiContext.zoomOut = () -> {
            camSizeGoal *= Math.pow(appSettings.zoomStepFactor, 2);
            camSizeGoal = Math.min(camSizeGoal, MAX_CAM_SIZE);
        };
        guiContext.appVersion = APP_VERSION;
        guiContext.javaHome = JAVA_HOME;
        guiContext.jvmVersion = JVM_VERSION;
        guiContext.lwjglVersion = LWJGL_VERSION;
        guiContext.openGlVendor = OPENGL_VENDOR;
        guiContext.openGlRenderer = OPENGL_RENDERER;
        guiContext.openGlVersion = OPENGL_VERSION;
        guiContext.openGlProfile = OPENGL_PROFILE;
        guiContext.glslVersion = GLSL_VERSION;
    }

    private void syncGuiContext() {
        guiContext.loop = loop;
        guiContext.physics = physics;
        guiContext.physicsSnapshot = physicsSnapshot;
        guiContext.settings = settings;
        guiContext.particleCount = particleCount;
        guiContext.preferredNumberOfThreads = preferredNumberOfThreads;
        guiContext.cursorParticleCount = cursorParticleCount;
        guiContext.traces = traces;
        guiContext.scale = scale;
        guiContext.width = width;
        guiContext.height = height;
        guiContext.typeCountDiagramStepSize = typeCountDiagramStepSize;
        guiContext.typeCountDisplayPercentage = typeCountDisplayPercentage;
        guiContext.saveCards = saveCards;
        guiContext.requestedSaveImage = requestedSaveImage;
        guiContext.selectedSaveFile = selectedSaveFile;
    }

    private void restartPhysics() {
        physicsSession.restart(this::updatePhysics);
        physics = physicsSession.physics;
        physicsSnapshot = physicsSession.physicsSnapshot;
        physicsSnapshotLoadDistributor = physicsSession.physicsSnapshotLoadDistributor;
        loop = physicsSession.loop;
        initGuiContext();
    }

    private void updatePhysics(double realDt) {
        physics.settings.dt = appSettings.autoDt ? realDt : appSettings.dt;
        physics.update();
    }

    @Override
    protected void beforeClose() {

        // try to save app settings
        if (this.error == null || !(this.error instanceof AppSettingsLoadException)) {
            // Don't save settings if the app settings could not
            // be loaded properly (which is where an
            // AppSettingsException would be thrown).
            // Why? Because in this case, the settings would be
            // just the defaults and the user would lose their
            // actual settings, as they would be overwritten.

            // Here, we also need to save all the app settings
            // that are stored outside the app settings object
            // during runtime.
            appSettings.palette = palettes.getActiveName();
            appSettings.shader = shaders.getActiveName();
            appSettings.cursorSize = cursor.size;
            appSettings.cursorActionLeft = cursorActions1.getActiveName();
            appSettings.cursorActionRight = cursorActions2.getActiveName();
            appSettings.positionSetter = positionSetters.getActiveName();
            // Note: Why are we not storing the fullscreen state here?
            // I.e. why not appSettings.startInFullscreen = isFullscreen()?
            // Because here, the glfw window is already closed,
            // and we can't access the fullscreen state anymore.
            // (That's why we override App.setFullscreen().)

            try {
                appSettings.save(SETTINGS_FILE_NAME);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        physicsSession.stopAndKill();
        imGuiGl3.dispose();
    }

    @Override
    protected void draw(double dt) {
        if (this.error == null) {
            renderClock.tick();
            updateCanvas();

            int texWidth, texHeight;

            // todo: make this part look less like magic
            int desiredTexSize = (int) Math.round(Math.min(width, height) / camSize);
            if (camSize > 1) {
                texWidth = desiredTexSize;
                texHeight = desiredTexSize;
                new NormalizedDeviceCoordinates(
                        new Vector2d(0.5, 0.5),  // center camera
                        new Vector2d(1, 1)  // capture whole screen
                ).getMatrix(transform);
            } else {
                if (settings.wrap) {
                    texWidth = Math.min(desiredTexSize, width);
                    texHeight = Math.min(desiredTexSize, height);
                } else {
                    texWidth = width;
                    texHeight = height;
                }
                Vector2d texCamSize = new Vector2d(camSize);
                if (width > height) texCamSize.x *= (double) texWidth / texHeight;
                else if (height > width) texCamSize.y *= (double) texHeight / texWidth;
                new NormalizedDeviceCoordinates(
                        new Vector2d(texCamSize.x / 2, texCamSize.y / 2),
                        texCamSize
                ).getMatrix(transform);
            }

            worldTexture.ensureSize(texWidth, texHeight, 16);

            ParticleShader particleShader = shaders.getActive();

            // set shader variables
            particleShader.use();

            particleShader.setTime(System.nanoTime() / 1000_000_000.0f);
            particleShader.setPalette(PaletteUtils.getColors(settings.matrix.size(), palettes.getActive()));
            particleShader.setTransform(transform);

            CamOperations cam = new CamOperations(camPos, camSize, width, height);
            CamOperations.BoundingBox camBox = cam.getBoundingBox();
            if (camSize > 1) {
                particleShader.setCamTopLeft(0, 0);
            } else {
                particleShader.setCamTopLeft((float) camBox.left, (float) camBox.top);
            }
            particleShader.setWrap(settings.wrap);
            particleShader.setSize(appSettings.particleSize * 2 * (float) settings.rmax
                    * (appSettings.keepParticleSizeIndependentOfZoom ? (float) camSize : 1));

            if (!traces) worldTexture.clear(0, 0, 0, 0);

            glEnable(GL_BLEND);
            particleShader.blendMode.glBlendFunc();

            glDisable(GL_SCISSOR_TEST);
            glViewport(0, 0, texWidth, texHeight);

            glBindFramebuffer(GL_FRAMEBUFFER, worldTexture.framebufferMulti);
            particleRenderer.drawParticles();
            worldTexture.toSingleSampled();

            glBindTexture(GL_TEXTURE_2D, worldTexture.textureSingle);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
            glBindTexture(GL_TEXTURE_2D, 0);

            // render cursor onto separate framebuffer
            cursorTexture.ensureSize(width, height, 16);
            cursorTexture.clear(0, 0, 0, 0);
            if (appSettings.showCursor) {
                new NormalizedDeviceCoordinates(
                        camPos,
                        cam.getCamDimensions()
                ).getMatrix(transform);
                transform.translate(cursor.position);
                transform.scale(cursor.size);

                glViewport(0, 0, width, height);

                glBindFramebuffer(GL_FRAMEBUFFER, cursorTexture.framebufferMulti);

                cursorShader.use();
                cursorShader.setTransform(transform);
                cursor.draw();
            }
            // convert multisampled texture to single-sampled texture
            cursorTexture.toSingleSampled();

            // render GUI
            // Note: Any Dear ImGui code must go between ImGui.newFrame() and ImGui.render().
            ImGui.newFrame();
            if (camSize > 1) {
                ImGui.getBackgroundDrawList().addImage(worldTexture.textureSingle, 0, 0, width, height,
                        (float) camBox.left, (float) camBox.top,
                        (float) camBox.right, (float) camBox.bottom);
            } else {
                ImGui.getBackgroundDrawList().addImage(worldTexture.textureSingle, 0, 0, width, height,
                        0, 0, (float) width / texWidth, (float) height / texHeight);
            }
            ImGui.getBackgroundDrawList().addImage(cursorTexture.textureSingle, 0, 0, width, height,
                    0, 0, 1, 1);

            buildGui();
            ImGui.render();

            glDisable(GL_SCISSOR_TEST);
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            imGuiGl3.render(ImGui.getDrawData());
        } else {
            ImGui.newFrame();
            buildErrorGui();
            ImGui.render();

            glDisable(GL_SCISSOR_TEST);
            glClearColor(0, 0, 0, 1);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            imGuiGl3.render(ImGui.getDrawData());
        }
    }

    /**
     * Render particles, cursor etc., i.e. everything except the GUI elements.
     */
    private void updateCanvas() {
        // util object for later use
        ScreenCoordinates screen = new ScreenCoordinates(camPos, camSize, width, height);

        // set cursor position and size
        cursor.position.set(screen.screenToWorld(new Vector2d(mouseX, mouseY)));

        if (input.draggingShift) {
            new CamOperations(camPos, camSize, width, height)
                    .dragCam(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY));
            camPosGoal.set(camPos);  // don't use smoothing while dragging
        }

        double camMovementStepSize = appSettings.camMovementSpeed * camSize;
        camMovementStepSize *= renderClock.getDtMillis() / 1000.0;  // keep constant speed regardless of framerate
        if (input.leftPressed || input.aPressed) camPosGoal.add(-camMovementStepSize, 0.0);
        if (input.rightPressed || input.dPressed) camPosGoal.add(camMovementStepSize, 0.0);
        if (input.upPressed || input.wPressed) camPosGoal.add(0.0, -camMovementStepSize);
        if (input.downPressed || input.sPressed) camPosGoal.add(0.0, camMovementStepSize);

        camPos.lerp(camPosGoal, appSettings.shiftSmoothness);
        camSize = MathUtils.lerp(camSize, camSizeGoal, appSettings.zoomSmoothness);

        // count particles under cursor from snapshot data (avoids cross-thread access to physics.particles)
        if (physicsSnapshot.positions != null) {
            cursorParticleCount = cursor.countSelection(
                    physicsSnapshot.positions,
                    physicsSnapshot.particleCount,
                    settings.wrap);
        }

        // cursor actions
        if (input.leftDraggingParticles || input.rightDraggingParticles) {

            // need to copy for async access in loop.enqueue()
            final Cursor cursorCopy;
            try {
                cursorCopy = cursor.copy();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // execute cursor action
            SelectionManager<CursorAction> cursorActions = input.leftDraggingParticles ? cursorActions1 : cursorActions2;
            switch (cursorActions.getActive()) {
                case MOVE -> {
                    final Vector3d dragStartWorld = screen.screenToWorld(pmouseX, pmouseY);  // where the dragging started
                    final Vector3d dragStopWorld = screen.screenToWorld(mouseX, mouseY);  // where the dragging ended
                    final Vector3d delta = dragStopWorld.sub(dragStartWorld);  // dragged distance
                    cursorCopy.position.set(dragStartWorld.x, dragStartWorld.y, 0.0);  // set cursor copy to start of dragging
                    loop.enqueue(() -> {
                        for (Particle p : cursorCopy.getSelection(physics.particles, physics.settings.wrap)) {
                            p.position.add(delta.x, delta.y, 0);
                            physics.ensurePosition(p.position);  // wrap or clamp
                        }
                    });
                }
                case BRUSH -> {
                    final int addCount = appSettings.brushPower;
                    loop.enqueue(() -> {
                        int prevLength = physics.particles.length;
                        physics.particles = Arrays.copyOf(physics.particles, prevLength + addCount);
                        for (int i = 0; i < addCount; i++) {
                            Particle particle = new Particle();
                            particle.position.set(cursorCopy.sampleRandomPoint());
                            physics.ensurePosition(particle.position);
                            particle.type = physics.typeSetter.getType(
                                    particle.position,
                                    particle.velocity,
                                    particle.type,
                                    physics.settings.matrix.size()
                            );
                            physics.particles[prevLength + i] = particle;
                        }
                    });
                }
                case DELETE -> {
                    loop.enqueue(() -> {
                        Particle[] newParticles = new Particle[physics.particles.length];
                        int j = 0;
                        for (Particle particle : physics.particles) {
                            if (!cursorCopy.isInside(particle, physics.settings.wrap)) {
                                newParticles[j] = particle;
                                j++;
                            }
                        }
                        physics.particles = Arrays.copyOf(newParticles, j);  // cut to correct length
                    });
                }
            }
        }

        if (newSnapshotAvailable.get()) {

            // get local copy of snapshot

            particleRenderer.bufferParticleData(shaders.getActive(),
                    physicsSnapshot.positions,
                    physicsSnapshot.velocities,
                    physicsSnapshot.types);
            settings = physicsSnapshot.settings.deepCopy();
            particleCount = physicsSnapshot.particleCount;
            preferredNumberOfThreads = physics.preferredNumberOfThreads;

            newSnapshotAvailable.set(false);
        }

        loop.doOnce(() -> {
            physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
            newSnapshotAvailable.set(true);
        });

        if (mouseX == 0 && mouseY == 0 && !showGui.get()) {
            showGui.set(true);
            traces = false;

            // this is a bugfix:
            // for some reason, ImGui behaves differently if the mouse
            // is still at the same position when it's displayed again.
            mouseX += 1;
            mouseY += 1;
        }
    }

    private void buildErrorGui() {
        ImGui.setNextWindowSize(-1, -1);
        if (ImGui.begin("Error", new ImBoolean(true), ImGuiWindowFlags.None)) {
            ImGui.textColored(255, 0, 0, 255, this.error.getMessage());
            if (ImGui.treeNode("Details")) {
                ImGui.text(this.error.toString());
                for (StackTraceElement element : this.error.getStackTrace()) {
                    ImGui.text(element.toString());
                }
                if (ImGui.button("Copy")) {
                    String text = this.error.toString() + "\n" +
                            Arrays.stream(this.error.getStackTrace())
                                    .map(StackTraceElement::toString)
                                    .collect(Collectors.joining("\n"));
                    ImGui.setClipboardText(text);
                }
                ImGui.treePop();
            }
            if (ImGui.button("Exit")) close();  // kill whole app
            ImGui.end();
        }
    }

    private void buildGui() {
        syncGuiContext();

        if (showGui.get()) {
            ImGui.setNextWindowSize(-1, -1, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(0, 0, ImGuiCond.Always, 0.0f, 0.0f);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowRounding, 0);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowPadding, 4f, 0f);
            ImGui.pushStyleVar(ImGuiStyleVar.WindowMinSize, 0f, 0f);
            if (ImGui.begin("Particle Life Simulator",
                    ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove
                            | ImGuiWindowFlags.NoTitleBar | ImGuiWindowFlags.MenuBar)) {
                ImGui.popStyleVar(3);
                if (ImGui.beginMenuBar()) {
                    GuiMainMenuBar.draw(guiContext);
                    ImGui.endMenuBar();
                }
            }
            ImGui.end();

            GuiParticlesPanel.draw(guiContext);
            GuiPhysicsPanel.draw(guiContext);
            GuiCursorPanel.draw(guiContext);
        }

        GuiGraphicsPanel.draw(guiContext);
        GuiPhysicsNotReactingDialog.draw(guiContext);
        GuiControlsDialog.draw(guiContext);
        GuiAboutDialog.draw(guiContext);
        GuiSavesDialog.draw(guiContext);

        if (requestedSaveImage) {
            saveImage = renderParticlesToImage();
            final File selectedFile = selectedSaveFile;
            loop.enqueue(() -> {
                selectedFile.getParentFile().mkdirs();
                saveState(selectedFile);
            });
            requestedSaveImage = false;
        }

        if (requestedSaveCardsLoading.getAndSet(false)) {
            loadSaveCards();
        }
    }


    private int[] renderParticlesToImage() {

        // get shader
        ParticleShader particleShader;
        String defaultShaderName = "default";
        if (shaders.hasName(defaultShaderName)) {
            particleShader = shaders.get(shaders.getIndexByName(defaultShaderName)).object;
        } else {
            particleShader = shaders.getActive();
        }

        glEnable(GL_BLEND);
        particleShader.blendMode.glBlendFunc();

        // set shader variables
        particleShader.use();
        particleShader.setTime(0);
        particleShader.setPalette(PaletteUtils.getColors(
                settings.matrix.size(),
                new NaturalRainbowPalette()));
        particleShader.setTransform(new NormalizedDeviceCoordinates(
                new Vector2d(0.5, 0.5),  // center camera
                new Vector2d(1, 1)  // capture whole world
        ).getMatrix());
        particleShader.setSize(0.015f);
        particleShader.setCamTopLeft(0, 0);
        particleShader.setWrap(false);

        int[] pixels = new int[SAVE_IMAGE_SIZE * SAVE_IMAGE_SIZE];
        MultisampledFramebuffer tex = new MultisampledFramebuffer();
        tex.init();
        tex.ensureSize(SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE, 16);
        tex.clear(0, 0, 0, 0);
        glViewport(0, 0, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE);
        glBindFramebuffer(GL_FRAMEBUFFER, tex.framebufferMulti);
        particleRenderer.drawParticles();
        tex.toSingleSampled();
        glBindFramebuffer(GL_FRAMEBUFFER, tex.framebufferSingle);
        glReadPixels(0, 0, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE, GL_BGRA, GL_UNSIGNED_BYTE, pixels);

        // unbind, delete
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        tex.delete();

        return pixels;
    }


    private void loadSaveCards() {
        List<Path> saves;
        try {
            saves = ResourceAccess.listFiles("saves");
        } catch (IOException e) {
            this.error = e;
            return;
        }
        saveCards = ImGuiCardView.loadCards(saves);
    }

    private void saveState(File file) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (ZipOutputStream zip = new ZipOutputStream(fileOutputStream)) {

                // PARTICLES
                zip.putNextEntry(new ZipEntry("particles.tsv"));
                ParticlesIO.saveParticles(physics.particles, zip);
                zip.closeEntry();

                // PHYSICS SETTINGS
                zip.putNextEntry(new ZipEntry("physics.toml"));
                PhysicsSettingsToml.fromPhysicsSettings(physics.settings).save(zip);
                zip.closeEntry();

                // MATRIX
                zip.putNextEntry(new ZipEntry("matrix.tsv"));
                MatrixIO.saveMatrix(physics.settings.matrix, zip);
                zip.closeEntry();

                // IMAGE
                if (saveImage != null) {
                    zip.putNextEntry(new ZipEntry("img.png"));
                    // convert to png format
                    BufferedImage bufferedImage = new BufferedImage(
                            SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE,
                            BufferedImage.TYPE_INT_ARGB
                    );
                    bufferedImage.setRGB(
                            0, 0, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE,
                            saveImage, 0, SAVE_IMAGE_SIZE
                    );
                    ImageIO.write(bufferedImage, "png", zip);
                    zip.closeEntry();
                    saveImage = null;
                }
            }
        } catch (IOException e) {
            reportError(e);
        }
        requestedSaveCardsLoading.set(true);
    }

    /**
     * Load the state from a ZIP file.
     * The zip file can contain the following files:
     * <ul>
     *     <li>particles.tsv</li>
     *     <li>physics.toml</li>
     *     <li>matrix.tsv</li>
     * </ul>
     * If a file is missing, the existing state will be kept for that part.
     * Currently, this might lead to an error, e.g. if the matrix size
     * doesn't match the particle types.
     *
     * @param file a zip file
     */
    private void loadState(File file) {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case "particles.tsv": {
                        physics.particles = ParticlesIO.loadParticles(zip);
                        break;
                    }
                    case "physics.toml": {
                        PhysicsSettingsToml toml = new PhysicsSettingsToml();
                        toml.load(zip);
                        toml.toPhysicsSettings(physics.settings);  // copy values
                        break;
                    }
                    case "matrix.tsv": {
                        physics.settings.matrix = MatrixIO.loadMatrix(zip);
                        physics.ensureTypes();  // in case the matrix size changed
                        break;
                    }
                    case "img.png": {
                        // ignore
                        break;
                    }
                    default: {
                        System.err.println("Unknown file in ZIP: " + entry.getName());
                        break;
                    }
                }
                zip.closeEntry();
            }
            physics.ensureTypes();
            validateLoadedState();
        } catch (IOException | IllegalStateException e) {
            reportError(e);
        }
    }

    private void validateLoadedState() {
        int matrixSize = physics.settings.matrix.size();
        for (Particle particle : physics.particles) {
            if (particle.type < 0 || particle.type >= matrixSize) {
                throw new IllegalStateException(
                        "Particle type " + particle.type + " is out of range for matrix size " + matrixSize);
            }
        }
    }

    private synchronized void reportError(Exception e) {
        this.error = e;
    }

    private void resetCamera(boolean fit) {
        if (settings.wrap) camPos.sub(Math.floor(camPos.x), Math.floor(camPos.y));  // remove periodic offset
        camPosGoal.set(0.5, 0.5);  // world center
        camSizeGoal = 1;

        if (fit) {
            // zoom to fit larger dimension
            camSizeGoal = (double) Math.min(width, height) / Math.max(width, height);
        }
    }

    @Override
    protected void onKeyPressed(String keyName) {
        InputShortcuts.handleKey(keyName, guiContext, input);
    }

    @Override
    protected void onKeyReleased(String keyName) {
        input.onKeyReleased(keyName);
    }

    @Override
    protected void onMousePressed(int button) {
        if (button == 2) {  // middle mouse button
            input.draggingShift = true;
        } else if (button == 0) {  // left mouse button
            input.leftDraggingParticles = true;
        } else if (button == 1) {  // right mouse button
            input.rightDraggingParticles = true;
        }
    }

    @Override
    protected void onMouseReleased(int button) {
        if (button == 2) {  // middle mouse button
            input.draggingShift = false;
        } else if (button == 0) {  // left mouse button
            input.leftDraggingParticles = false;
        } else if (button == 1) {  // right mouse button
            input.rightDraggingParticles = false;
        }
    }

    @Override
    protected void onScroll(double y) {

        if (input.isControlPressed() && input.isShiftPressed()) {
            // change time step
            appSettings.dt *= Math.pow(1.2, -y);
            appSettings.dt = MathUtils.clamp(appSettings.dt, 0.00f, 0.1f);
            // deactivate auto dt
            appSettings.autoDt = false;
        } else if (input.isShiftPressed()) {
            // change particle size
            appSettings.particleSize *= (float) Math.pow(1.2, -y);
        } else if (input.isControlPressed()) {
            // change cursor size
            cursor.size *= Math.pow(1.2, -y);
        } else if (input.isAltPressed()) {
            // change rmax
            loop.enqueue(() -> physics.settings.rmax *= Math.pow(1.2, -y));
        } else {
            // change camera zoom

            double factor = Math.pow(appSettings.zoomStepFactor, -y);

            CamOperations cam = new CamOperations(camPosGoal, camSizeGoal, width, height);
            cam.zoom(
                    mouseX, mouseY, // zoom in on mouse
                    Math.min(camSizeGoal * factor, MAX_CAM_SIZE)
            );  // this already modifies camPosGoal
            camSizeGoal = cam.camSize;
        }
    }

    @Override
    protected void setFullscreen(boolean fullscreen) {
        super.setFullscreen(fullscreen);

        // remember fullscreen state for next startup
        appSettings.startInFullscreen = fullscreen;
    }
}
