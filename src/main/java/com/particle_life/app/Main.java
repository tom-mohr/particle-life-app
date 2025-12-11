package com.particle_life.app;

import com.particle_life.*;
import com.particle_life.app.color.*;
import com.particle_life.app.cursors.*;
import com.particle_life.app.io.MatrixIO;
import com.particle_life.app.io.ParticlesIO;
import com.particle_life.app.io.ResourceAccess;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.CursorShader;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.shaders.ShaderProvider;
import com.particle_life.app.utils.*;
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
    public AtomicBoolean newSnapshotAvailable = new AtomicBoolean(false);

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
    boolean draggingShift = false;
    boolean leftDraggingParticles = false;  // dragging with left mouse button
    boolean rightDraggingParticles = false;  // dragging with right mouse button
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean upPressed = false;
    boolean downPressed = false;
    boolean wPressed = false;
    boolean aPressed = false;
    boolean sPressed = false;
    boolean dPressed = false;
    boolean leftShiftPressed = false;
    boolean rightShiftPressed = false;
    boolean leftControlPressed = false;
    boolean rightControlPressed = false;
    boolean leftAltPressed = false;
    boolean rightAltPressed = false;

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

    @Override
    protected void setup() {
        LWJGL_VERSION = Version.getVersion();
        OPENGL_VENDOR = glGetString(GL_VENDOR);
        OPENGL_RENDERER = glGetString(GL_RENDERER);
        OPENGL_VERSION = glGetString(GL_VERSION);
        int profileMask = glGetInteger(GL_CONTEXT_PROFILE_MASK);
        OPENGL_PROFILE = (profileMask & GL_CONTEXT_CORE_PROFILE_BIT) != 0 ? "Core" :
                (profileMask & GL_CONTEXT_COMPATIBILITY_PROFILE_BIT) != 0 ? "Compatibility" : "Unknown";
        GLSL_VERSION = glGetString(GL_SHADING_LANGUAGE_VERSION);

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

        createPhysics();
        loop = new Loop();
        loop.start(this::updatePhysics);

        // set default selection for palette
        if (palettes.hasName(appSettings.palette)) {
            palettes.setActive(palettes.getIndexByName(appSettings.palette));
        }

        // generate offscreen frame buffer to render particles to a multisampled texture
        // and also a simple texture for converting the multisampled texture to a single-sampled texture
        // (this is necessary because ImGui can't handle multisampled textures in the drawlist)
        worldTexture = new MultisampledFramebuffer();
        worldTexture.init();
        glBindTexture(GL_TEXTURE_2D, worldTexture.textureSingle);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);

        // create offscreen framebuffer for cursor rendering
        cursorTexture = new MultisampledFramebuffer();
        cursorTexture.init();
        glBindTexture(GL_TEXTURE_2D, cursorTexture.textureSingle);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    private void createPhysics() {
        Accelerator accelerator = (a, pos) -> {
            double beta = 0.3;
            double dist = pos.length();
            double force = dist < beta ? (dist / beta - 1) : a * (1 - Math.abs(1 + beta - 2 * dist) / (1 - beta));
            return pos.mul(force / dist);
        };
        physics = new ExtendedPhysics(
                accelerator,
                positionSetters.getActive(),
                matrixGenerators.getActive(),
                typeSetters.getActive());
        physicsSnapshot = new PhysicsSnapshot();
        physicsSnapshotLoadDistributor = new LoadDistributor();
        physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
        newSnapshotAvailable.set(true);
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

        if (!loop.stop(1000)) {
            loop.kill();
            physics.kill();
            physicsSnapshotLoadDistributor.kill();
        }
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
            particleShader.setPalette(getColorsFromPalette(settings.matrix.size(), palettes.getActive()));
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

        if (draggingShift) {
            new CamOperations(camPos, camSize, width, height)
                    .dragCam(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY));
            camPosGoal.set(camPos);  // don't use smoothing while dragging
        }

        double camMovementStepSize = appSettings.camMovementSpeed * camSize;
        camMovementStepSize *= renderClock.getDtMillis() / 1000.0;  // keep constant speed regardless of framerate
        if (leftPressed || aPressed) camPosGoal.add(-camMovementStepSize, 0.0);
        if (rightPressed || dPressed) camPosGoal.add(camMovementStepSize, 0.0);
        if (upPressed || wPressed) camPosGoal.add(0.0, -camMovementStepSize);
        if (downPressed || sPressed) camPosGoal.add(0.0, camMovementStepSize);

        camPos.lerp(camPosGoal, appSettings.shiftSmoothness);
        camSize = MathUtils.lerp(camSize, camSizeGoal, appSettings.zoomSmoothness);

        // count particles under cursor
        {
            try {
                cursorParticleCount = cursor.countSelection(physics.particles, physics.settings.wrap);
            } catch (NullPointerException e) {
                e.printStackTrace();
                /*
                 The particle array might be null if the physics thread
                 replaces the particle array while this executes
                 (e.g. if the particle count is changed).
                 I admit that this is not a clean solution, but anything else
                 would have required too many changes to the code
                 base, i.e. would have been overkill for this simple task.
                 For example, the following would have been a clean solution:
                     Do proper triple buffering of the particle array.
                     In physics thread:
                         1. copy Physics.particles -> physicsSnapshot1.particles
                     In main thread (here):
                         1. copy physicsSnapshot1.particles -> physicsSnapshot2.particles
                         2. upload physicsSnapshot1(or 2).particles -> GPU
                     Then, physicsSnapshot2.particles could be used here for counting the selection without risk.
                 Another clean solution would maybe be to declare Physics.particles as volatile?
                 Currently, another safe way would be to use the following:
                     loop.enqueue(() -> cursorParticleCount = cursor.countSelection(physics));
                 But this would make the particle count laggy if the physics simulation is slow,
                 and I find it a better user experience to have the particle count ALWAYS update in real time.
                */
            }
        }

        // cursor actions
        if (leftDraggingParticles || rightDraggingParticles) {

            // need to copy for async access in loop.enqueue()
            final Cursor cursorCopy;
            try {
                cursorCopy = cursor.copy();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // execute cursor action
            SelectionManager<CursorAction> cursorActions = leftDraggingParticles ? cursorActions1 : cursorActions2;
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
        if (showGui.get()) {
            // MAIN MENU
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
                    buildMainMenu();
                    ImGui.endMenuBar();
                }
            }
            ImGui.end();

            // PARTICLES
            ImGui.setNextWindowSize(-1, -1, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(width, 0, ImGuiCond.Always, 1.0f, 0.0f);
            ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Right);
            if (ImGui.begin("Particles",
                    ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
                ImGui.pushItemWidth(200);

                // N
                ImInt particleCountInput = new ImInt(particleCount);
                if (ImGui.inputInt("Particle count", particleCountInput, 1000, 1000, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    final int newCount = Math.max(0, particleCountInput.get());
                    loop.enqueue(() -> physics.setParticleCount(newCount));
                }

                // POSITION SETTERS
                if (ImGuiUtils.renderCombo("##positions", positionSetters)) {
                    final PositionSetter nextPositionSetter = positionSetters.getActive();
                    loop.enqueue(() -> physics.positionSetter = nextPositionSetter);
                }
                ImGui.sameLine();
                if (ImGui.button("Positions")) {
                    loop.enqueue(physics::setPositions);
                }
                ImGuiUtils.helpMarker("[p]");

                ImGuiUtils.separator();

                // MATRIX GENERATORS
                if (ImGuiUtils.renderCombo("##matrix", matrixGenerators)) {
                    final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive();
                    loop.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
                }
                ImGui.sameLine();
                if (ImGui.button("Matrix")) {
                    loop.enqueue(physics::generateMatrix);
                }
                ImGuiUtils.helpMarker("[m]");

                // MATRIX
                ImGuiMatrix.draw(200 * scale, 200 * scale,
                        palettes.getActive(),
                        appSettings.matrixGuiStepSize,
                        settings.matrix,
                        (i, j, newValue) -> loop.enqueue(() -> physics.settings.matrix.set(i, j, newValue))
                );
                if (ImGui.button("Copy")) {
                    ImGui.setClipboardText(MatrixParser.matrixToString(settings.matrix));
                }
                ImGui.sameLine();
                if (ImGui.button("Paste")) {
                    Matrix parsedMatrix = MatrixParser.parseMatrix(ImGui.getClipboardText());
                    if (parsedMatrix != null) {
                        loop.enqueue(() -> {
                            physics.setMatrixSize(parsedMatrix.size());
                            physics.settings.matrix = parsedMatrix;
                        });
                    }
                }
                ImGuiUtils.helpMarker("Save / load matrix via the clipboard.");
                if (ImGui.treeNode("Settings##matrix")) {
                    ImFloat inputValue = new ImFloat((float) appSettings.matrixGuiStepSize);
                    if (ImGui.inputFloat("Step Size##Matrix", inputValue, 0.05f, 0.05f, "%.2f")) {
                        appSettings.matrixGuiStepSize = MathUtils.clamp(inputValue.get(), 0.05f, 1.0f);
                    }
                    ImGui.treePop();
                }

                ImGuiUtils.separator();

                // TYPE SETTERS
                ImGuiUtils.renderCombo("##colors", typeSetters);
                ImGui.sameLine();
                if (ImGui.button("Colors")) {
                    loop.enqueue(() -> {
                        TypeSetter previousTypeSetter = physics.typeSetter;
                        physics.typeSetter = typeSetters.getActive();
                        physics.setTypes();
                        physics.typeSetter = previousTypeSetter;
                    });
                }
                ImGuiUtils.helpMarker("[c] Use this to set colors of particles without changing their position.");

                // NTYPES
                ImInt matrixSizeInput = new ImInt(settings.matrix.size());
                if (ImGui.inputInt("Colors##input", matrixSizeInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    final int newSize = Math.max(1, Math.min(matrixSizeInput.get(), 256));
                    loop.enqueue(() -> physics.setMatrixSize(newSize));
                }

                ImGuiBarGraph.draw(200, 100,
                        palettes.getActive(),
                        typeCountDiagramStepSize,
                        physicsSnapshot.typeCount,
                        (type, newValue) -> {
                            final int[] newTypeCount = Arrays.copyOf(physicsSnapshot.typeCount, physicsSnapshot.typeCount.length);
                            newTypeCount[type] = newValue;
                            loop.enqueue(() -> physics.setTypeCount(newTypeCount));
                        },
                        typeCountDisplayPercentage
                );
                if (ImGui.button("Equalize")) {
                    loop.enqueue(() -> physics.setTypeCountEqual());
                }
                if (ImGui.treeNode("Settings##colorbars")) {
                    {
                        ImInt inputValue = new ImInt(typeCountDiagramStepSize);
                        if (ImGui.inputInt("Step Size##ColorCount", inputValue, 10)) {
                            typeCountDiagramStepSize = Math.max(0, inputValue.get());
                        }
                    }

                    {
                        ImInt selected = new ImInt(typeCountDisplayPercentage ? 1 : 0);
                        ImGui.radioButton("Absolute", selected, 0);
                        ImGui.sameLine();
                        ImGui.radioButton("Percentage", selected, 1);
                        typeCountDisplayPercentage = selected.get() == 1;
                    }
                    ImGui.treePop();
                }

                ImGui.popItemWidth();
            }
            ImGui.end();

            // PHYSICS
            ImGui.setNextWindowSize(-1, -1, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(width, height, ImGuiCond.Always, 1.0f, 1.0f);
            ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Right);
            if (ImGui.begin("Physics",
                    ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
                ImGui.pushItemWidth(200);

                if (ImGui.button(loop.pause ? "Play" : "Pause", 80, 0)) {
                    loop.pause ^= true;
                }
                ImGuiUtils.helpMarker("[SPACE] " +
                        "The physics simulation runs independently from the graphics in the background.");

                ImGui.sameLine();
                if (loop.getAvgFramerate() < 100000) {
                    ImGui.text(String.format("FPS: %5.0f", loop.getAvgFramerate()));
                } else {
                    ImGui.text("");
                }

                // SliderFloat Block
                ImGuiUtils.numberInput("rmax",
                        0.005f, 1f,
                        (float) settings.rmax,
                        "%.3f",
                        value -> loop.enqueue(() -> physics.settings.rmax = value));
                ImGuiUtils.helpMarker("The distance at which particles interact.");

                ImGuiUtils.numberInput("Friction Coefficient",
                        0f, 1f,
                        (float) settings.friction,
                        "%.3f",
                        value -> loop.enqueue(() -> physics.settings.friction = value),
                        false);
                ImGuiUtils.helpMarker("The velocity of all particles is multiplied with this value" +
                        " in each update step to simulate friction (assuming 60 fps).");

                ImGuiUtils.numberInput("Force Scaling",
                        0f, 100f,
                        (float) settings.force,
                        "%.1f",
                        value -> loop.enqueue(() -> physics.settings.force = value));
                ImGuiUtils.helpMarker("Scales the forces between all particles with a constant factor.");

                ImGuiUtils.separator();

                if (ImGui.checkbox("Periodic Boundaries", settings.wrap)) {
                    final boolean newWrap = !settings.wrap;
                    loop.enqueue(() -> physics.settings.wrap = newWrap);
                }
                ImGuiUtils.helpMarker("[b] Determines if the space wraps around at the borders or not.");

                if (appSettings.autoDt) ImGui.beginDisabled();
                ImGuiUtils.numberInput(
                        "Time Step",
                        0, 100,
                        (float) appSettings.dt * 1000f,
                        "%.2f ms",
                        value -> appSettings.dt = Math.max(0, value / 1000));
                if (appSettings.autoDt) ImGui.endDisabled();
                ImGui.sameLine();
                if (ImGui.checkbox("Auto", appSettings.autoDt)) appSettings.autoDt ^= true;
                ImGuiUtils.helpMarker("[ctrl+shift+scroll] The time step of the physics computation." +
                        "\nIf 'Auto' is ticked, the time step will be chosen automatically based on the real passed time.");

                ImInt threadNumberInput = new ImInt(preferredNumberOfThreads);
                if (ImGui.inputInt("Threads", threadNumberInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                    final int newThreadNumber = Math.max(1, threadNumberInput.get());
                    loop.enqueue(() -> physics.preferredNumberOfThreads = newThreadNumber);
                }
                ImGuiUtils.helpMarker("The number of threads used by your processor for the physics computation." +
                        "\n(If you don't know what this means, just ignore it.)");

                ImGui.popItemWidth();
            }
            ImGui.end();

            // CURSOR
            ImGui.setNextWindowSize(290, 250, ImGuiCond.FirstUseEver);
            ImGui.setNextWindowPos(0, height, ImGuiCond.Always, 0.0f, 1.0f);
            ImGui.getStyle().setWindowMenuButtonPosition(ImGuiDir.Left);
            if (ImGui.begin("Cursor",
                    ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoMove)) {
                ImGui.pushItemWidth(200);

                ImGui.text("Hovered Particles: " + cursorParticleCount);
                if (ImGui.checkbox("Show", appSettings.showCursor)) {
                    appSettings.showCursor ^= true;
                }
                // cursor size slider
                ImGuiUtils.numberInput("Size",
                        0.001f, 1f,
                        (float) cursor.size,
                        "%.3f",
                        value -> cursor.size = value);
                ImGuiUtils.helpMarker("[ctrl+scroll]");

                ImGuiUtils.renderCombo("Shape##cursor", cursorShapes);
                cursor.shape = cursorShapes.getActive();

                ImGuiUtils.separator();

                if (ImGui.beginTable("Cursor Action Table", 2, ImGuiTableFlags.None)) {
                    // Set up column headers
                    ImGui.tableSetupColumn("", ImGuiTableColumnFlags.WidthFixed, 100);
                    ImGui.tableSetupColumn("", ImGuiTableColumnFlags.WidthFixed, 100);

                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(0);
                    ImGui.text("Left");
                    ImGui.tableSetColumnIndex(1);
                    ImGui.text("Right");

                    ImGui.tableNextRow();
                    ImGui.tableSetColumnIndex(0);
                    ImGui.pushItemWidth(100);
                    ImGuiUtils.renderCombo("##cursoraction1", cursorActions1);
                    ImGui.popItemWidth();
                    ImGui.tableSetColumnIndex(1);
                    ImGui.pushItemWidth(100);
                    ImGuiUtils.renderCombo("##cursoraction2", cursorActions2);
                    ImGui.popItemWidth();

                    ImGui.tableNextRow();
                    ImGui.endTable();
                }

                ImGui.indent();
                if (cursorActions1.getActive() == CursorAction.BRUSH || cursorActions2.getActive() == CursorAction.BRUSH) {
                    ImInt inputValue = new ImInt(appSettings.brushPower);
                    ImGui.pushItemWidth(100);
                    if (ImGui.inputInt("Brush Power", inputValue, 10, ImGuiInputTextFlags.EnterReturnsTrue)) {
                        appSettings.brushPower = Math.max(0, inputValue.get());
                    }
                    ImGui.popItemWidth();
                    ImGuiUtils.helpMarker("Number of particles added per frame.");
                }
                ImGui.unindent();

                ImGui.popItemWidth();
            }
            ImGui.end();
        }

        // GRAPHICS
        if (showGraphicsWindow.get()) {
            ImGui.setNextWindowSize(400, 300);
            ImGui.setNextWindowPos(width / 2f, height / 2f, ImGuiCond.FirstUseEver, 0.5f, 0.5f);
            if (ImGui.begin("Graphics", showGraphicsWindow,
                    ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoNavFocus | ImGuiWindowFlags.NoCollapse)) {
                ImGui.pushItemWidth(200);
                ImGui.text(String.format("Graphics FPS: %.0f", renderClock.getAvgFramerate()));

                // SHADERS
                ImGuiUtils.renderCombo("Shader", shaders);
                ImGuiUtils.helpMarker("Use this to set how the particles are displayed");

                // PALETTES
                ImGuiUtils.renderCombo("Palette", palettes);
                ImGuiUtils.helpMarker("Color of particles");

                ImGui.text("Particle Size:");
                ImGuiUtils.helpMarker("[shift+scroll]" +
                        "\nHow large the particles are displayed relative to rmax.");
                float[] particleSizeSliderValue = new float[]{appSettings.particleSize};
                if (ImGui.sliderFloat("##particle size", particleSizeSliderValue, 0.001f, 1f)) {
                    appSettings.particleSize = particleSizeSliderValue[0];
                }
                ImGui.sameLine();
                if (ImGui.checkbox("Zoom-Independent", appSettings.keepParticleSizeIndependentOfZoom)) {
                    appSettings.keepParticleSizeIndependentOfZoom ^= true;
                }

                if (ImGui.checkbox("Traces [t]", traces)) {
                    traces ^= true;
                }

                if (ImGui.treeNode("Camera Settings")) {
                    {
                        float[] inputValue = new float[]{(float) appSettings.camMovementSpeed};
                        if (ImGui.sliderFloat("Cam Speed", inputValue, 0.0f, 2.0f, "%0.2f")) {
                            appSettings.camMovementSpeed = inputValue[0];
                        }
                    }

                    {
                        float[] inputValue = new float[]{(float) (1.0 - appSettings.zoomSmoothness)};
                        if (ImGui.sliderFloat("Cam Smoothing", inputValue, 0.0f, 1.0f, "%0.2f")) {
                            appSettings.zoomSmoothness = 1.0 - inputValue[0];
                            appSettings.shiftSmoothness = 1.0 - inputValue[0];
                        }
                    }

                    {
                        float[] inputValue = new float[]{(float) (appSettings.zoomStepFactor - 1) * 100};
                        if (ImGui.sliderFloat("Zoom Step", inputValue, 0.0f, 100.0f, "%.1f%%", ImGuiSliderFlags.Logarithmic)) {
                            appSettings.zoomStepFactor = 1 + inputValue[0] * 0.01;
                        }
                    }

                    ImGui.treePop();
                }

                ImGui.popItemWidth();
            }
            ImGui.end();
        }

        // PHYSICS NOT REACTING
        long physicsNotReactingSince = System.currentTimeMillis() - physicsSnapshot.snapshotTime;
        boolean physicsNotReacting = physicsNotReactingSince > physicsNotReactingThreshold;
        if (physicsNotReacting) ImGui.openPopup("Not reacting");
        if (ImGui.beginPopupModal("Not reacting")) {
            if (!physicsNotReacting) {
                ImGui.closeCurrentPopup();
            }

            ImGui.text("Physics didn't react since %4.0f seconds.".formatted(physicsNotReactingSince / 1000.0));

            if (ImGui.button("Reset Physics")) {
                if (!loop.stop(1000)) {
                    // physics didn't finish in time
                    loop.kill();
                    physics.kill();
                    physicsSnapshotLoadDistributor.kill();
                }
                // re-start loop and re-create physics with initial settings
                createPhysics();
                loop = new Loop();
                loop.start(this::updatePhysics);
            }

            ImGui.endPopup();
        }

        if (showControlsWindow.get()) {
            ImGui.setNextWindowPos(width / 2f, height / 2f, ImGuiCond.FirstUseEver, 0.5f, 0.5f);
            if (ImGui.begin("Controls", showControlsWindow, ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {
                ImGui.text("""
                        [+], [=]: zoom in
                        [-]: zoom out
                        [z]: reset zoom
                        [Z]: reset zoom (fit window)
                        [ESCAPE]: hide / show GUI GUI
                        [g]: show / hide graphics settings
                        [SPACE]: pause physics
                        [p]: set positions
                        [c]: set colors
                        [m]: set matrix
                        [b]: toggle boundaries (clamped / periodic)
                        [t]: toggle traces
                        [F11], [f]: toggle full screen
                        [ALT]+[F4], [q]: quit
                        """);
            }
            ImGui.end();
        }

        if (showAboutWindow.get()) {
            ImGui.setNextWindowPos(width / 2f, height / 2f, ImGuiCond.FirstUseEver, 0.5f, 0.5f);
            if (ImGui.begin("About", showAboutWindow, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoCollapse)) {
                ImGui.text("By Tom Mohr.");
                ImGui.text("GPL-3.0 License.");
                ImGui.dummy(0, 10);
                if (ImGuiUtils.link("particle-life.com", "https://particle-life.com")) {
                    setFullscreen(false);
                }
                ImGui.dummy(0, 10);
                ImGui.text("Java Home: " + JAVA_HOME);
                ImGui.text("JVM Version: " + JVM_VERSION);
                ImGui.text("LWJGL Version: " + LWJGL_VERSION);
                ImGui.text("OpenGL Vendor: " + OPENGL_VENDOR);
                ImGui.text("OpenGL Renderer: " + OPENGL_RENDERER);
                ImGui.text("OpenGL Version: " + OPENGL_VERSION);
                ImGui.text("OpenGL Profile: " + OPENGL_PROFILE);
                ImGui.text("GLSL Version: " + GLSL_VERSION);
            }
            ImGui.end();
        }

        if (showSavesPopup.get()) ImGui.openPopup("Saves");
        ImGui.setNextWindowSize(480, -1, ImGuiCond.Always);
        ImGui.setNextWindowPos(width / 2f, height / 2f, ImGuiCond.Appearing, 0.5f, 0.5f);
        ImGui.setNextWindowBgAlpha(1f);
        if (ImGui.beginPopupModal("Saves", showSavesPopup, ImGuiWindowFlags.NoResize)) {

            ImGui.textDisabled("""
                    Left-click to load, middle-click to delete.
                    The most recent saves are at the top.
                    Each save corresponds to a .zip file in the 'saves' directory.
                    """
            );
            ImGuiUtils.separator();

            float cardViewWidth = ImGui.getWindowContentRegionMaxX() - 2 * ImGui.getStyle().getFramePaddingX();
            ImGui.beginChild("save cards", cardViewWidth, 250);
            ImGuiCardView.Card[] filteredCards = Arrays
                    .stream(saveCards)
                    .filter(card -> card.name.contains(saveName.get()))
                    .sorted(Comparator.comparing(card -> -card.file.lastModified()))  // sort by creation time (descending)
                    .toArray(ImGuiCardView.Card[]::new);
            ImGuiCardView.draw(
                    cardViewWidth,
                    100,
                    8,
                    filteredCards,
                    card -> {
                        loop.enqueue(() -> loadState(card.file));
                        showSavesPopup.set(false);
                    },
                    card -> {
                        try {
                            Files.deleteIfExists(card.file.toPath());
                        } catch (IOException e) {
                            this.error = e;
                        }
                        requestedSaveCardsLoading.set(true);
                    }
            );
            ImGui.endChild();

            if (!ImGui.isAnyItemActive() && !ImGui.isMouseClicked(0)) {
                // see https://github.com/ocornut/imgui/issues/455#issuecomment-167440172
                ImGui.setKeyboardFocusHere(0);
            }
            boolean shouldSave = ImGui.inputTextWithHint("##save name", "Save Name", saveName, ImGuiInputTextFlags.EnterReturnsTrue);
            ImGuiUtils.helpMarker("Enter a name and press Enter to save the current state.");
            if (shouldSave) {
                String title = saveName.get();
                saveName.clear();
                if (!title.isBlank()) {
                    selectedSaveFile = new File("saves/" + title + ".zip");
                    requestedSaveImage = true;
                }
            }
            ImGui.endPopup();
        }

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
        particleShader.setPalette(getColorsFromPalette(
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

    private void buildMainMenu() {
        if (ImGui.beginMenu("Menu")) {

            if (ImGui.menuItem("Saves##menu", "Ctrl+s")) {
                showSavesPopup.set(true);
                requestedSaveCardsLoading.set(true);
            }

            if (ImGui.menuItem("Controls..")) {
                showControlsWindow.set(true);
            }

            if (ImGui.menuItem("About..")) {
                showAboutWindow.set(true);
            }

            if (ImGui.menuItem("Quit", "Alt+F4, q")) {
                close();
            }

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("View")) {

            if (isFullscreen()) {
                if (ImGui.menuItem("Exit Fullscreen", "F11, f")) {
                    setFullscreen(false);
                }
            } else {
                if (ImGui.menuItem("Fullscreen", "F11, f")) {
                    setFullscreen(true);
                }
            }

            if (ImGui.menuItem("Hide GUI", "Esc")) {
                showGui.set(false);
            }

            if (ImGui.beginMenu("Zoom")) {
                if (ImGui.menuItem("100%", "z")) {
                    resetCamera(false);
                }
                if (ImGui.menuItem("Fit", "Z")) {
                    resetCamera(true);
                }
                ImGui.endMenu();
            }

            if (ImGui.menuItem("Graphics..", "g")) {
                showGraphicsWindow.set(true);
            }

            ImGui.endMenu();
        }
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
            e.printStackTrace();
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
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    private Color[] getColorsFromPalette(int n, Palette palette) {
        Color[] colors = new Color[n];
        for (int i = 0; i < n; i++) {
            colors[i] = palette.getColor(i, n);
        }
        return colors;
    }

    @Override
    protected void onKeyPressed(String keyName) {
        // update key states
        switch (keyName) {
            case "LEFT" -> leftPressed = true;
            case "RIGHT" -> rightPressed = true;
            case "UP" -> upPressed = true;
            case "DOWN" -> downPressed = true;
            case "w" -> wPressed = true;
            case "a" -> aPressed = true;
            case "s" -> sPressed = true;
            case "d" -> dPressed = true;
            case "LEFT_SHIFT" -> leftShiftPressed = true;
            case "RIGHT_SHIFT" -> rightShiftPressed = true;
            case "LEFT_CONTROL" -> leftControlPressed = true;
            case "RIGHT_CONTROL" -> rightControlPressed = true;
            case "LEFT_ALT" -> leftAltPressed = true;
            case "RIGHT_ALT" -> rightAltPressed = true;
        }

        // ctrl + key shortcuts
        if (leftControlPressed | rightControlPressed) {
            switch (keyName) {
                case "s" -> {
                    showSavesPopup.set(true);
                    requestedSaveCardsLoading.set(true);

                    // Clear key states manually, because releasing [ctrl]+[s]
                    // won't be captured once the popup is open.
                    leftControlPressed = false;
                    rightControlPressed = false;
                    sPressed = false;
                }
            }
            return;
        }

        // simple key shortcuts
        switch (keyName) {
            case "ESCAPE" -> showGui.set(!showGui.get());
            case "f" -> setFullscreen(!isFullscreen());
            case "t" -> traces ^= true;
            case "+", "=" -> camSizeGoal /= Math.pow(appSettings.zoomStepFactor, 2);// more steps than when scrolling
            case "-" -> {
                camSizeGoal *= Math.pow(appSettings.zoomStepFactor, 2);
                camSizeGoal = Math.min(camSizeGoal, MAX_CAM_SIZE);
            }
            case "z" -> resetCamera(false);
            case "Z" -> resetCamera(true);
            case "p" -> loop.enqueue(physics::setPositions);
            case "c" -> loop.enqueue(() -> {
                TypeSetter previousTypeSetter = physics.typeSetter;
                physics.typeSetter = typeSetters.getActive();
                physics.setTypes();
                physics.typeSetter = previousTypeSetter;
            });
            case "g" -> showGraphicsWindow.set(!showGraphicsWindow.get());
            case "m" -> loop.enqueue(physics::generateMatrix);
            case "b" -> loop.enqueue(() -> physics.settings.wrap ^= true);
            case " " -> loop.pause ^= true;
            case "q" -> close();
        }
    }

    @Override
    protected void onKeyReleased(String keyName) {
        // update key states
        switch (keyName) {
            case "LEFT" -> leftPressed = false;
            case "RIGHT" -> rightPressed = false;
            case "UP" -> upPressed = false;
            case "DOWN" -> downPressed = false;
            case "w" -> wPressed = false;
            case "a" -> aPressed = false;
            case "s" -> sPressed = false;
            case "d" -> dPressed = false;
            case "LEFT_SHIFT" -> leftShiftPressed = false;
            case "RIGHT_SHIFT" -> rightShiftPressed = false;
            case "LEFT_CONTROL" -> leftControlPressed = false;
            case "RIGHT_CONTROL" -> rightControlPressed = false;
            case "LEFT_ALT" -> leftAltPressed = false;
            case "RIGHT_ALT" -> rightAltPressed = false;
        }
    }

    @Override
    protected void onMousePressed(int button) {
        if (button == 2) {  // middle mouse button
            draggingShift = true;
        } else if (button == 0) {  // left mouse button
            leftDraggingParticles = true;
        } else if (button == 1) {  // right mouse button
            rightDraggingParticles = true;
        }
    }

    @Override
    protected void onMouseReleased(int button) {
        if (button == 2) {  // middle mouse button
            draggingShift = false;
        } else if (button == 0) {  // left mouse button
            leftDraggingParticles = false;
        } else if (button == 1) {  // right mouse button
            rightDraggingParticles = false;
        }
    }

    @Override
    protected void onScroll(double y) {

        boolean controlPressed = leftControlPressed || rightControlPressed;
        boolean shiftPressed = leftShiftPressed || rightShiftPressed;
        boolean altPressed = leftAltPressed || rightAltPressed;

        if (controlPressed && shiftPressed) {
            // change time step
            appSettings.dt *= Math.pow(1.2, -y);
            appSettings.dt = MathUtils.clamp(appSettings.dt, 0.00f, 0.1f);
            // deactivate auto dt
            appSettings.autoDt = false;
        } else if (shiftPressed) {
            // change particle size
            appSettings.particleSize *= (float) Math.pow(1.2, -y);
        } else if (controlPressed) {
            // change cursor size
            cursor.size *= Math.pow(1.2, -y);
        } else if (altPressed) {
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
