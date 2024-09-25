package com.particle_life.app;

import com.particle_life.*;
import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;
import com.particle_life.app.color.PalettesProvider;
import com.particle_life.app.color.SimpleRainbowPalette;
import com.particle_life.app.cursors.*;
import com.particle_life.app.io.MatrixIO;
import com.particle_life.app.io.ParticlesIO;
import com.particle_life.app.io.ResourceAccess;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.shaders.ShaderProvider;
import com.particle_life.app.utils.ImGuiUtils;
import com.particle_life.app.utils.MathUtils;
import imgui.ImGui;
import imgui.flag.*;
import imgui.gl3.ImGuiImplGl3;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Matrix4d;
import org.joml.Vector2d;

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

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;
import static org.lwjgl.opengl.GL15.GL_CLAMP_TO_BORDER;

public class Main extends App {

    public static void main(String[] args) {
        Main main = new Main();
        try {
            main.appSettings.load(SETTINGS_FILE_NAME);
        } catch (IOException e) {
            main.error = new AppSettingsLoadException("Failed to load settings", e);
        }
        main.launch("Particle Life Simulator", main.appSettings.startInFullscreen, ".internal/favicon.png");
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
    private SelectionManager<Accelerator> accelerators;
    private SelectionManager<MatrixGenerator> matrixGenerators;
    private SelectionManager<PositionSetter> positionSetters;
    private SelectionManager<TypeSetter> typeSetters;
    private Cursor cursor;
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
    private final LoadDistributor physicsSnapshotLoadDistributor = new LoadDistributor();  // speed up taking snapshots with parallelization
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
    private final double MAX_CAM_SIZE = 10;
    boolean draggingShift = false;
    boolean leftDraggingParticles = false;  // dragging with left mouse button
    boolean rightDraggingParticles = false;  // dragging with right mouse button
    boolean leftPressed = false;
    boolean rightPressed = false;
    boolean upPressed = false;
    boolean downPressed = false;
    boolean leftShiftPressed = false;
    boolean rightShiftPressed = false;
    boolean leftControlPressed = false;
    boolean rightControlPressed = false;

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

    // offscreen particle rendering buffer
    private int screenTexture;

    @Override
    protected void setup() {
        glEnable(GL_MULTISAMPLE);

        // Method initializes LWJGL3 renderer.
        // This method SHOULD be called after you've initialized your ImGui configuration (fonts and so on).
        // ImGui context should be created as well.
        imGuiGl3.init("#version 410 core");

        particleRenderer.init();

        // cursor object must be created after renderer.init()
        try {
            cursor = new Cursor();
        } catch (IOException e) {
            this.error = e;
            return;
        }
        cursor.size = appSettings.cursorSize;

        try {
            shaders = new SelectionManager<>(new ShaderProvider());
            palettes = new SelectionManager<>(new PalettesProvider());
            accelerators = new SelectionManager<>(new AcceleratorProvider());
            matrixGenerators = new SelectionManager<>(new MatrixGeneratorProvider());
            positionSetters = new SelectionManager<>(new PositionSetterProvider());
            typeSetters = new SelectionManager<>(new TypeSetterProvider());
            cursorShapes = new SelectionManager<>(new CursorProvider());
            cursorActions1 = new SelectionManager<>(new CursorActionProvider());
            cursorActions2 = new SelectionManager<>(new CursorActionProvider());

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

        // set default selection for palette
        if (palettes.hasName(appSettings.palette)) {
            palettes.setActive(palettes.getIndexByName(appSettings.palette));
        }

        screenTexture = glGenTextures();
    }

    private void createPhysics() {
        physics = new ExtendedPhysics(
                accelerators.getActive(),
                positionSetters.getActive(),
                matrixGenerators.getActive(),
                typeSetters.getActive());
        physicsSnapshot = new PhysicsSnapshot();
        physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
        newSnapshotAvailable.set(true);

        loop = new Loop();
        loop.start(realDt -> {
            physics.settings.dt = appSettings.autoDt ? realDt : appSettings.dt;
            physics.update();
        });
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

        try {
            loop.stop(1000);
            physics.shutdown(1000);
            physicsSnapshotLoadDistributor.shutdown(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        imGuiGl3.dispose();
    }

    private void ensureTextureDimensions(int textureId, int width, int height) {
        // bind
        glBindTexture(GL_TEXTURE_2D, textureId);

        // query dimensions of texture
        int[] intBuffer = new int[1];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_WIDTH, intBuffer);
        int textureWidth = intBuffer[0];
        glGetTexLevelParameteriv(GL_TEXTURE_2D, 0, GL_TEXTURE_HEIGHT, intBuffer);
        int textureHeight = intBuffer[0];

        // set texture dimensions if necessary
        if (width != textureWidth || height != textureHeight) {
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, 0);
        }

        // unbind
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    @Override
    protected void draw(double dt) {
        if (this.error == null) {
            renderClock.tick();
            updateCanvas();

            // set shader variables
            particleRenderer.particleShader.use();

            int desiredTexSize = (int) Math.round(Math.min(width, height) / camSize);
            if (camSize > 1) {
                setTransform(transform, desiredTexSize, desiredTexSize, new Vector2d(0, 0), 1);
                ensureTextureDimensions(screenTexture, desiredTexSize, desiredTexSize);
            } else {
                int texWidth, texHeight;
                if (settings.wrap) {
                    texWidth = Math.min(desiredTexSize, width);
                    texHeight = Math.min(desiredTexSize, height);
                } else {
                    texWidth = width;
                    texHeight = height;
                }
                ensureTextureDimensions(screenTexture, texWidth, texHeight);
                setTransform(transform, texWidth, texHeight, new Vector2d(0, 0), camSize);
            }
            // set texture parameters
            glBindTexture(GL_TEXTURE_2D, screenTexture);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, settings.wrap ? GL_REPEAT : GL_CLAMP_TO_BORDER);
            glBindTexture(GL_TEXTURE_2D, 0); // unbind texture

            particleRenderer.particleShader.setTime(System.nanoTime() / 1000_000_000.0f);
            particleRenderer.particleShader.setPalette(getColorsFromPalette(settings.matrix.size(), palettes.getActive()));
            particleRenderer.particleShader.setTransform(transform);

            Vector2d extendedCamSize = new Vector2d(camSize, camSize);
            if (width > height) extendedCamSize.x *= (double) width / height;
            else if (height > width) extendedCamSize.y *= (double) height / width;
            double camLeft = camPos.x - extendedCamSize.x / 2;
            double camTop = camPos.y - extendedCamSize.y / 2;
            double camRight = camPos.x + extendedCamSize.x / 2;
            double camBottom = camPos.y + extendedCamSize.y / 2;
            if (camSize > 1) {
                particleRenderer.particleShader.setCamTopLeft(0, 0);
            } else {
                particleRenderer.particleShader.setCamTopLeft((float) camLeft, (float) camTop);
            }
            particleRenderer.particleShader.setWrap(settings.wrap);
            particleRenderer.particleShader.setSize(appSettings.particleSize
                    * (appSettings.keepParticleSizeIndependentOfZoom ? (float) camSize : 1)
                    / Math.min(width, height));
            double particleSizeOnScreen = appSettings.keepParticleSizeIndependentOfZoom ?
                    appSettings.particleSize : appSettings.particleSize / camSize;
            particleRenderer.particleShader.setDetail((int) Math.floor(3 + particleSizeOnScreen));

            particleRenderer.renderParticlesToTexture(screenTexture, !traces); // particles

            clearScreen();
            // todo: this renders the cursor onto the texture...
//            if (appSettings.showCursor) cursor.draw(transform); // cursor

            // render GUI
            // Note: Any Dear ImGui code must go between ImGui.newFrame() and ImGui.render().
            ImGui.newFrame();
            if (camSize > 1) {
                ImGui.getBackgroundDrawList().addImage(screenTexture, 0, 0, width, height,
                        (float) camLeft, (float) camTop,
                        (float) camRight, (float) camBottom);
            } else {
                int texWidth, texHeight;
                if (settings.wrap) {
                    texWidth = Math.min(desiredTexSize, width);
                    texHeight = Math.min(desiredTexSize, height);
                } else {
                    texWidth = width;
                    texHeight = height;
                }
                ImGui.getBackgroundDrawList().addImage(screenTexture, 0, 0, width, height,
                        0, 0, (float) width / texWidth, (float) height / texHeight);
            }
            if (showGui.get()) buildGui();
            ImGui.render();
            imGuiGl3.render(ImGui.getDrawData());
        } else {
            ImGui.newFrame();
            buildErrorGui();
            ImGui.render();
            clearScreen();
            imGuiGl3.render(ImGui.getDrawData());  // gui
        }
    }

    /**
     * Clears the default frame buffer.
     */
    private void clearScreen() {
        // The OpenGL Specification states that glClear() only clears the scissor rectangle when the scissor test is enabled.
        glDisable(GL_SCISSOR_TEST);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer
    }

    /**
     * Render particles, cursor etc., i.e. everything except the GUI elements.
     */
    private void updateCanvas() {
        // util object for later use
        Coordinates coordinates = new Coordinates(width, height, camPos, camSize);

        // set cursor position and size
        Vector2d cursorWorldCoordinates = coordinates.world(mouseX, mouseY);
        cursor.position.set(cursorWorldCoordinates, 0);

        if (draggingShift) {

            camPos.set(coordinates
                    .mouseShift(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY))
                    .camPos);
            camPosGoal.set(camPos);  // don't use smoothing while dragging
        }

        double camMovementStepSize = appSettings.camMovementSpeed * camSize;
        camMovementStepSize *= renderClock.getDtMillis() / 1000.0;  // keep constant speed regardless of framerate
        if (leftPressed) camPosGoal.add(-camMovementStepSize, 0.0);
        if (rightPressed) camPosGoal.add(camMovementStepSize, 0.0);
        if (upPressed) camPosGoal.add(0.0, -camMovementStepSize);
        if (downPressed) camPosGoal.add(0.0, camMovementStepSize);

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
                    final Vector2d wPrev = coordinates.world(pmouseX, pmouseY);  // where the dragging started
                    final Vector2d wNew = coordinates.world(mouseX, mouseY);  // where the dragging ended
                    final Vector2d delta = wNew.sub(wPrev);  // dragged distance
                    cursorCopy.position.set(wPrev.x, wPrev.y, 0.0);  // set cursor to start of dragging
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

        particleRenderer.particleShader = shaders.getActive();  // need to assign a shader before buffering particle data

        if (newSnapshotAvailable.get()) {

            // get local copy of snapshot

            particleRenderer.bufferParticleData(physicsSnapshot.positions, physicsSnapshot.velocities, physicsSnapshot.types);
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
                ImGuiUtils.renderCombo("##types", typeSetters);
                ImGui.sameLine();
                if (ImGui.button("Types")) {
                    loop.enqueue(() -> {
                        TypeSetter previousTypeSetter = physics.typeSetter;
                        physics.typeSetter = typeSetters.getActive();
                        physics.setTypes();
                        physics.typeSetter = previousTypeSetter;
                    });
                }
                ImGuiUtils.helpMarker("[t] Use this to set types of particles without changing their position.");

                // NTYPES
                ImInt matrixSizeInput = new ImInt(settings.matrix.size());
                if (ImGui.inputInt("Types##input", matrixSizeInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
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
                if (ImGui.treeNode("Settings##typebars")) {
                    {
                        ImInt inputValue = new ImInt(typeCountDiagramStepSize);
                        if (ImGui.inputInt("Step Size##TypeCount", inputValue, 10)) {
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

                // ACCELERATORS
                if (ImGuiUtils.renderCombo("Accelerator##accelerator", accelerators)) {
                    final Accelerator nextAccelerator = accelerators.getActive();
                    loop.enqueue(() -> physics.accelerator = nextAccelerator);
                }
                String acceleratorDescription = accelerators.getActiveDescription();
                if (!acceleratorDescription.isEmpty()) {
                    ImGuiUtils.helpMarker(acceleratorDescription);
                }
                ImGuiUtils.helpMarker("[v] Use this to set how the particles interact with one another");

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
                ImGuiUtils.helpMarker("[w] Determines if the space wraps around at the borders or not.");

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
                ImGui.popItemWidth();

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
            }
            ImGui.end();

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
                    ImGuiUtils.helpMarker("[s] Use this to set how the particles are displayed");

                    // PALETTES
                    ImGuiUtils.renderCombo("Palette", palettes);
                    ImGuiUtils.helpMarker("[l] Color of particles");

                    ImGui.text("Particle Size:");
                    float[] particleSizeSliderValue = new float[]{appSettings.particleSize};
                    if (ImGui.sliderFloat("##particle size", particleSizeSliderValue, 0.1f, 10f)) {
                        appSettings.particleSize = particleSizeSliderValue[0];
                    }
                    ImGui.sameLine();
                    if (ImGui.checkbox("Fixed", appSettings.keepParticleSizeIndependentOfZoom)) {
                        appSettings.keepParticleSizeIndependentOfZoom ^= true;
                    }
                    ImGuiUtils.helpMarker("[shift+scroll] How large the particles are displayed." +
                            "\nIf fixed is checked, the size is fixed regardless of zoom.");

                    if (ImGui.checkbox("Traces [c]", traces)) {
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
        }

        // PHYSICS NOT REACTING
        long physicsNotReactingSince = System.currentTimeMillis() - physicsSnapshot.snapshotTime;
        boolean physicsNotReacting = physicsNotReactingSince > physicsNotReactingThreshold;
        if (physicsNotReacting) ImGui.openPopup("Not reacting");
        if (ImGui.beginPopupModal("Not reacting")) {
            if (!physicsNotReacting) {
                ImGui.closeCurrentPopup();
            }

            ImGui.text("Physics didn't react since %4.1f seconds.".formatted(physicsNotReactingSince / 1000.0));

            if (ImGui.button("Try Reset")) {
                try {
                    if (loop.stop(1000)) {
                        physics.shutdown(1000);
                        createPhysics();
                    } else {
                        ImGui.openPopup("Taking too long");
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            if (ImGui.button("Particle Count = 0?")) {
                loop.enqueue(() -> physics.setParticleCount(0));
            }

            if (ImGui.beginPopupModal("Taking too long")) {

                ImGui.text("Physics couldn't be stopped.");

                if (ImGui.button("Continue Waiting")) {
                    ImGui.closeCurrentPopup();
                }

                if (ImGui.button("Close App")) {
                    close();// kill whole app
                }

                ImGui.endPopup();
            }

            ImGui.endPopup();
        }

        if (showControlsWindow.get()) {
            ImGui.setNextWindowPos(width / 2f, height / 2f, ImGuiCond.FirstUseEver, 0.5f, 0.5f);
            if (ImGui.begin("Controls", showControlsWindow, ImGuiWindowFlags.NoCollapse | ImGuiWindowFlags.NoResize)) {
                ImGui.text("""
                        [l]/[L]: change palette
                        [s]/[S]: change shader
                        [a]/[A]: change accelerator
                        [x]/[X]: change position setter
                        [r]/[R]: change matrix generator
                        
                        [p]: set positions
                        [t]: set types
                        [m]: set matrix
                        
                        [w]: toggle space wrapping
                        
                        [SPACE]: pause physics
                        
                        [F11]: toggle full screen
                        [ALT]+[F4]: quit
                        
                        [+]/[=]: zoom in
                        [-]: zoom out
                        [z]: reset zoom
                        [Z]: reset zoom (fit window)
                        
                        [c]: toggle traces (clear screen)
                        [ESCAPE]: hide GUI / show GUI
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

            // set shader variables
            String defaultShaderName = "default";
            if (shaders.hasName(defaultShaderName)) {
                particleRenderer.particleShader = shaders.get(shaders.getIndexByName(defaultShaderName)).object;
            }
            particleRenderer.particleShader.use();
            particleRenderer.particleShader.setTime(0);
            particleRenderer.particleShader.setPalette(getColorsFromPalette(
                    settings.matrix.size(),
                    new SimpleRainbowPalette()));
            Matrix4d transform = new Matrix4d();
            setTransform(transform, SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE, new Vector2d(0.5, 0.5), 1.0);
            particleRenderer.particleShader.setTransform(transform);
            particleRenderer.particleShader.setSize(4f / SAVE_IMAGE_SIZE);
            particleRenderer.particleShader.setDetail(ParticleShader.MIN_DETAIL);

            saveImage = particleRenderer.renderParticlesToImage(SAVE_IMAGE_SIZE, SAVE_IMAGE_SIZE);
            requestedSaveImage = false;

            final File selectedFile = selectedSaveFile;
            loop.enqueue(() -> {
                selectedFile.getParentFile().mkdirs();
                saveState(selectedFile);
            });
        }

        if (requestedSaveCardsLoading.getAndSet(false)) {
            loadSaveCards();
        }
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

            if (ImGui.menuItem("Quit", "Alt+F4")) {
                close();
            }

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("View")) {

            if (isFullscreen()) {
                if (ImGui.menuItem("Exit Fullscreen", "F11")) {
                    setFullscreen(false);
                }
            } else {
                if (ImGui.menuItem("Fullscreen", "F11")) {
                    setFullscreen(true);
                }
            }

            if (ImGui.menuItem("Hide GUI", "Esc")) {
                showGui.set(false);
            }

            if (ImGui.beginMenu("Zoom")) {
                if (ImGui.menuItem("100%", "z")) {
                    resetCamera(true, false);
                }
                if (ImGui.menuItem("Fit", "Z")) {
                    resetCamera(true, true);
                }
                ImGui.endMenu();
            }

            if (ImGui.menuItem("Graphics..")) {
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

    private void resetCamera(boolean smooth, boolean fit) {
        camPosGoal.set(0.5, 0.5);  // world center
        camSizeGoal = 1;
        if (!smooth) camPos.set(0);

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

    private void setTransform(Matrix4d targetMatrix,
                              float width, float height, Vector2d camPos, double camSize) {
        targetMatrix.identity();
        new Coordinates(width, height, camPos, camSize).apply(targetMatrix);
    }

    @Override
    protected void onKeyPressed(String keyName) {
        // update key states
        switch (keyName) {
            case "LEFT" -> leftPressed = true;
            case "RIGHT" -> rightPressed = true;
            case "UP" -> upPressed = true;
            case "DOWN" -> downPressed = true;
            case "LEFT_SHIFT" -> leftShiftPressed = true;
            case "RIGHT_SHIFT" -> rightShiftPressed = true;
            case "LEFT_CONTROL" -> leftControlPressed = true;
            case "RIGHT_CONTROL" -> rightControlPressed = true;
        }

        // ctrl + key shortcuts
        if (leftControlPressed | rightControlPressed) {
            switch (keyName) {
                case "s" -> {
                    showSavesPopup.set(true);
                    requestedSaveCardsLoading.set(true);

                    // Clear mods manually, because releasing the control key
                    // won't be captured once the popup is open.
                    leftControlPressed = false;
                    rightControlPressed = false;
                }
            }
            return;
        }

        // simple key shortcuts
        switch (keyName) {
            case "ESCAPE" -> showGui.set(!showGui.get());
            case "f" -> setFullscreen(!isFullscreen());
            case "c" -> traces ^= true;
            case "l" -> palettes.stepForward();
            case "L" -> palettes.stepBackward();
            case "s" -> shaders.stepForward();
            case "S" -> shaders.stepBackward();
            case "+", "=" -> camSizeGoal /= Math.pow(appSettings.zoomStepFactor, 2);// more steps than when scrolling
            case "-" -> {
                camSizeGoal *= Math.pow(appSettings.zoomStepFactor, 2);
                camSizeGoal = Math.min(camSizeGoal, MAX_CAM_SIZE);
            }
            case "z" -> resetCamera(true, false);
            case "Z" -> resetCamera(true, true);
            case "p" -> loop.enqueue(physics::setPositions);
            case "t" -> loop.enqueue(() -> {
                TypeSetter previousTypeSetter = physics.typeSetter;
                physics.typeSetter = typeSetters.getActive();
                physics.setTypes();
                physics.typeSetter = previousTypeSetter;
            });
            case "m" -> loop.enqueue(physics::generateMatrix);
            case "w" -> loop.enqueue(() -> physics.settings.wrap ^= true);
            case " " -> loop.pause ^= true;
            case "a" -> {
                accelerators.stepForward();
                final Accelerator nextAccelerator = accelerators.getActive();
                loop.enqueue(() -> physics.accelerator = nextAccelerator);
            }
            case "A" -> {
                accelerators.stepBackward();
                final Accelerator nextAccelerator = accelerators.getActive();
                loop.enqueue(() -> physics.accelerator = nextAccelerator);
            }
            case "x" -> {
                positionSetters.stepForward();
                final PositionSetter nextPositionSetter = positionSetters.getActive();
                loop.enqueue(() -> physics.positionSetter = nextPositionSetter);
            }
            case "X" -> {
                positionSetters.stepBackward();
                final PositionSetter nextPositionSetter = positionSetters.getActive();
                loop.enqueue(() -> physics.positionSetter = nextPositionSetter);
            }
            case "r" -> {
                matrixGenerators.stepForward();
                final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive();
                loop.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
            }
            case "R" -> {
                matrixGenerators.stepBackward();
                final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive();
                loop.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
            }
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
            case "LEFT_SHIFT" -> leftShiftPressed = false;
            case "RIGHT_SHIFT" -> rightShiftPressed = false;
            case "LEFT_CONTROL" -> leftControlPressed = false;
            case "RIGHT_CONTROL" -> rightControlPressed = false;
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
        boolean bothPressed = controlPressed && shiftPressed;

        if (bothPressed) {
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
        } else {
            // change camera zoom

            double factor = Math.pow(appSettings.zoomStepFactor, -y);

            Coordinates coordinates = new Coordinates(width, height, camPosGoal, camSizeGoal);
            double newCamSize = camSizeGoal * factor;
            newCamSize = Math.min(newCamSize, MAX_CAM_SIZE);
            coordinates.changeCamSizeFixed(coordinates.world(mouseX, mouseY), newCamSize);

            camSizeGoal = coordinates.camSize;
            camPosGoal.set(coordinates.camPos);
        }
    }

    @Override
    protected void setFullscreen(boolean fullscreen) {
        super.setFullscreen(fullscreen);

        // remember fullscreen state for next startup
        appSettings.startInFullscreen = fullscreen;
    }
}
