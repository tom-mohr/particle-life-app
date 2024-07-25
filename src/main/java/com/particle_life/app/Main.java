package com.particle_life.app;

import com.particle_life.*;
import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;
import com.particle_life.app.color.PalettesProvider;
import com.particle_life.app.cursors.*;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.shaders.ShaderProvider;
import com.particle_life.app.utils.ImGuiUtils;
import com.particle_life.app.utils.MathUtils;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Main extends App {

    public static void main(String[] args) {
        Main main = new Main();
        try {
            main.appSettings.load(SETTINGS_FILE_NAME);
        } catch (IOException | IllegalAccessException e) {
            main.error = new AppSettingsLoadException("Failed to load settings", e);
        }
        main.launch("Particle Life Simulator", main.appSettings.startInFullscreen);
    }

    private AppSettings appSettings = new AppSettings();
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
    private SelectionManager<CursorAction> cursorActions;

    // helper classes
    private final ImGuiStatsFormatter statsFormatter = new ImGuiStatsFormatter();
    private final Matrix4d transform = new Matrix4d();
    private final Renderer renderer = new Renderer();

    private ExtendedPhysics physics;
    private Loop loop;
    private boolean autoDt = true;
    private double fallbackDt = 0.02;
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
    private final Vector3d shift = new Vector3d(0);
    private final Vector3d shiftGoal = new Vector3d(shift);
    private final double MIN_ZOOM = 0.1;
    private double zoom = 1;
    private double zoomGoal = zoom;
    boolean draggingShift = false;
    boolean draggingParticles = false;
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
    private final ImBoolean showStyleEditor = new ImBoolean(false);
    private final ImBoolean showSettings = new ImBoolean(false);
    private final ImBoolean showShortcutsWindow = new ImBoolean(false);
    private final ImBoolean showAboutWindow = new ImBoolean(false);

    // GUI: store data on the current state of the GUI
    private boolean tracesBefore = traces;// if "traces" was enabled in last frame

    @Override
    protected void setup() {
        glEnable(GL_MULTISAMPLE);
        renderer.init();

        // cursor object must be created after renderer.init()
        try {
            cursor = new Cursor();
        } catch (IOException e) {
            this.error = e;
            return;
        }

        try {
            shaders = new SelectionManager<>(new ShaderProvider());
            palettes = new SelectionManager<>(new PalettesProvider());
            accelerators = new SelectionManager<>(new AcceleratorProvider());
            matrixGenerators = new SelectionManager<>(new MatrixGeneratorProvider());
            positionSetters = new SelectionManager<>(new PositionSetterProvider());
            typeSetters = new SelectionManager<>(new TypeSetterProvider());
            cursorShapes = new SelectionManager<>(new CursorProvider());
            cursorActions = new SelectionManager<>(new CursorActionProvider());
        } catch (Exception e) {
            this.error = e;
            return;
        }

        cursor.shape = cursorShapes.getActive();  // set initial cursor shape (would be null otherwise)

        try {
            shaders.setActive(appSettings.shader);
        } catch (IllegalArgumentException e) {
            // todo: emit warning
            shaders.setActive(0);
        }

        createPhysics();

        // set default selection for palette
        if (palettes.hasName(appSettings.palette)) {
            palettes.setActive(palettes.getIndexByName(appSettings.palette));
        }
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
        loop.start(dt -> {
            physics.settings.dt = autoDt ? dt : fallbackDt;
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
            // Note: Why are we not storing the fullscreen state here?
            // I.e. why not appSettings.startInFullscreen = isFullscreen()?
            // Because here, the glfw window is already closed,
            // and we can't access the fullscreen state anymore.
            // (That's why we override App.setFullscreen().)

            try {
                appSettings.save(SETTINGS_FILE_NAME);
            } catch (IOException | IllegalAccessException e) {
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
        renderer.dispose();
    }

    @Override
    protected void draw(double dt) {
        if (this.error == null) {
            renderClock.tick();
            updateCanvas();
            ImGui.newFrame();
            // Any Dear ImGui code must go between ImGui.newFrame() and ImGui.render()
            if (!traces && showGui.get()) buildGui();
            ImGui.render();
            setShaderVariables();
            if (!traces || !tracesBefore) renderer.clear();
            tracesBefore = traces;
            renderer.run(transform, cursor, ImGui.getDrawData(), width, height);  // draw particles and GUI
        } else {
            ImGui.newFrame();
            buildErrorGui();
            ImGui.render();
            renderer.clear();
            renderer.run(transform, cursor, ImGui.getDrawData(), width, height);  // draw GUI
        }
    }

    /**
     * Render particles, cursor etc., i.e. everything except the GUI elements.
     */
    private void updateCanvas() {
        // util object for later use
        Coordinates coordinates = new Coordinates(width, height, shift, zoom);

        // set cursor position and size
        Vector3d cursorWorldCoordinates = coordinates.world(mouseX, mouseY);
        cursor.position.set(cursorWorldCoordinates);

        if (draggingShift) {

            shift.set(coordinates
                    .mouseShift(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY))
                    .shift);
            shiftGoal.set(shift);  // don't use smoothing while dragging
        }

        double camMovementStepSize = appSettings.camMovementSpeed / zoom;
        camMovementStepSize *= renderClock.getDtMillis() / 1000.0;  // keep constant speed regardless of framerate
        if (leftPressed) shiftGoal.add(camMovementStepSize, 0.0, 0.0);
        if (rightPressed) shiftGoal.add(-camMovementStepSize, 0.0, 0.0);
        if (upPressed) shiftGoal.add(0.0, camMovementStepSize, 0.0);
        if (downPressed) shiftGoal.add(0.0, -camMovementStepSize, 0.0);

        shift.lerp(shiftGoal, appSettings.shiftSmoothness);
        zoom = MathUtils.lerp(zoom, zoomGoal, appSettings.zoomSmoothness);

        if (draggingParticles) {

            // need to copy for async access in loop.enqueue()
            final Cursor cursorCopy;
            try {
                cursorCopy = cursor.copy();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            // execute cursor action
            switch (cursorActions.getActive()) {
                case MOVE -> {
                    final Vector3d wPrev = coordinates.world(pmouseX, pmouseY);  // where the dragging started
                    final Vector3d wNew = coordinates.world(mouseX, mouseY);  // where the dragging ended
                    final Vector3d delta = wNew.sub(wPrev);  // dragged distance
                    cursorCopy.position.set(wPrev.x, wPrev.y, 0.0);  // set cursor to start of dragging
                    loop.enqueue(() -> {
                        for (Particle p : cursorCopy.getSelection(physics)) {
                            p.position.add(delta);
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
                            if (!cursorCopy.isInside(particle, physics)) {
                                newParticles[j] = particle;
                                j++;
                            }
                        }
                        physics.particles = Arrays.copyOf(newParticles, j);  // cut to correct length
                    });
                }
            }
        }

        renderer.particleShader = shaders.getActive();  // need to assign a shader before buffering particle data

        if (newSnapshotAvailable.get()) {

            // get local copy of snapshot

            //todo: only write types if necessary?
            renderer.bufferParticleData(physicsSnapshot.positions, physicsSnapshot.velocities, physicsSnapshot.types);
            settings = physicsSnapshot.settings.deepCopy();
            particleCount = physicsSnapshot.particleCount;
            preferredNumberOfThreads = physics.preferredNumberOfThreads;
            // todo: make this in a clean async way: cursorParticleCount = cursors.getActive().object.getSelection(physics).size();

            newSnapshotAvailable.set(false);
        }

        loop.doOnce(() -> {
            physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
            newSnapshotAvailable.set(true);
        });

        if (mouseX == 0 && mouseY == 0 && (!showGui.get() || traces)) {
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
        ImGui.setNextWindowBgAlpha(appSettings.guiBackgroundAlpha);
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
            if (ImGui.button("Close App")) close();  // kill whole app
            ImGui.end();
        }
    }

    private void buildGui() {

        ImGui.setNextWindowBgAlpha(appSettings.guiBackgroundAlpha);
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(-1, height);
        if (showGui.get()) {
            if (ImGui.begin("App", showGui, ImGuiWindowFlags.None
                    | ImGuiWindowFlags.NoResize
                    | ImGuiWindowFlags.NoNavFocus
                    | ImGuiWindowFlags.NoTitleBar
                    | ImGuiWindowFlags.MenuBar)) {

                if (ImGui.beginMenuBar()) {
                    buildMainMenu();
                    ImGui.endMenuBar();
                }

                ImGui.pushItemWidth(200);

                {
                    statsFormatter.start();
                    statsFormatter.put("Graphics FPS", String.format("%.0f", renderClock.getAvgFramerate()));
                    statsFormatter.put("Physics FPS", loop.getAvgFramerate() < 100000 ? String.format("%.0f", loop.getAvgFramerate()) : "inf");
                    if (appSettings.showAdvancedGui) {
                        statsFormatter.put("Physics vs. Graphics", loop.getAvgFramerate() < 100000 ? String.format("%.2f", loop.getAvgFramerate() / renderClock.getAvgFramerate()) : "inf");
                        //todo display when functional: statsFormatter.put("Particles in Cursor", String.valueOf(cursorParticleCount));
                    }
                    statsFormatter.end();
                }

                ImGui.separator();

                ImGui.text("Particles");
                {

                    // N
                    ImInt particleCountInput = new ImInt(particleCount);
                    if (ImGui.inputInt("particle count", particleCountInput, 1000, 1000, ImGuiInputTextFlags.EnterReturnsTrue)) {
                        final int newCount = Math.max(0, particleCountInput.get());
                        loop.enqueue(() -> physics.setParticleCount(newCount));
                    }

                    ImGui.separator();

                    // TYPES
                    {
                        // NTYPES
                        ImInt matrixSizeInput = new ImInt(settings.matrix.size());
                        if (ImGui.inputInt("types", matrixSizeInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final int newSize = Math.max(1, Math.min(matrixSizeInput.get(), 256));
                            loop.enqueue(() -> physics.setMatrixSize(newSize));
                        }

                        if (appSettings.showAdvancedGui) {

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

                            if (ImGui.button("equalize type count")) {
                                loop.enqueue(() -> physics.setTypeCountEqual());
                            }
                        }

                        // MATRIX
                        ImGuiMatrix.draw(200 * scale, 200 * scale,
                                palettes.getActive(),
                                appSettings.matrixGuiStepSize,
                                settings.matrix,
                                (i, j, newValue) -> loop.enqueue(() -> physics.settings.matrix.set(i, j, newValue))
                        );

                        if (appSettings.showAdvancedGui) {
                            ImGui.text("Clipboard:");
                            ImGui.sameLine();
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
                        }

                        // MATRIX GENERATORS
                        if (renderCombo("##matrix", matrixGenerators)) {
                            final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive();
                            loop.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
                        }
                        ImGui.sameLine();
                        if (ImGui.button("matrix [m]")) {
                            loop.enqueue(physics::generateMatrix);
                        }
                    }

                    ImGui.separator();

                    // POSITION SETTERS
                    if (renderCombo("##positions", positionSetters)) {
                        final PositionSetter nextPositionSetter = positionSetters.getActive();
                        loop.enqueue(() -> physics.positionSetter = nextPositionSetter);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("positions [p]")) {
                        loop.enqueue(physics::setPositions);
                    }

                    ImGui.separator();

                    // TYPE SETTERS
                    renderCombo("##types", typeSetters);
                    ImGui.sameLine();
                    if (ImGui.button("types [t]")) {
                        loop.enqueue(() -> {
                            TypeSetter previousTypeSetter = physics.typeSetter;
                            physics.typeSetter = typeSetters.getActive();
                            physics.setTypes();
                            physics.typeSetter = previousTypeSetter;
                        });
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("Use this to set types of particles without changing their position.");
                }

                ImGui.separator();
                ImGui.text("Physics");
                {

                    ImGui.separator();

                    if (ImGui.button("%s [SPACE]".formatted(loop.pause ? "Unpause" : "Pause"))) {
                        loop.pause ^= true;
                    }

                    // ACCELERATORS
                    if (appSettings.showAdvancedGui) {
                        ImGui.text("Accelerator [v]");
                        if (renderCombo("##accelerator", accelerators)) {
                            final Accelerator nextAccelerator = accelerators.getActive();
                            loop.enqueue(() -> physics.accelerator = nextAccelerator);
                        }
                        String acceleratorDescription = accelerators.getActiveDescription();
                        if (!acceleratorDescription.isEmpty()) {
                            ImGui.sameLine();
                            ImGuiUtils.helpMarker(acceleratorDescription);
                        }
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("Use this to set how the particles interact with one another");

                    if (ImGui.checkbox("wrap [w]", settings.wrap)) {
                        final boolean newWrap = !settings.wrap;
                        loop.enqueue(() -> physics.settings.wrap = newWrap);
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("determines if the boardering space wraps around or not");

                    // SliderFloat Block
                    {
                        float displayValue = (float) settings.rmax;
                        float[] rmaxSliderValue = new float[]{displayValue};
                        if (ImGui.sliderFloat("rmax##Slider", rmaxSliderValue, 0.005f, 1.000f, String.format("%.3f", displayValue), ImGuiSliderFlags.Logarithmic)) {
                            final float newRmax = rmaxSliderValue[0];
                            loop.enqueue(() -> physics.settings.rmax = newRmax);
                        }
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("rmax is the radius for particles to interact");

                    // InputFloat Block
                    {
                        ImFloat rmaxInputValue = new ImFloat((float) settings.rmax);
                        if (ImGui.inputFloat("rmax##Input", rmaxInputValue, 0.005f, 1.000f, "%.3f", ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final float newRmax = Math.max(0.005f, Math.min(rmaxInputValue.get(), 1.000f)); // Clamping the value within a range
                            loop.enqueue(() -> physics.settings.rmax = newRmax);
                        }
                    }

                    {// FRICTION
                        float[] frictionSliderValue = new float[]{(float) settings.velocityHalfLife};
                        if (ImGui.sliderFloat("velocity half life",
                                frictionSliderValue, 0.0f, 1.0f,
                                String.format("%4.0f ms", settings.velocityHalfLife * 1000),
                                ImGuiSliderFlags.Logarithmic)) {
                            final double newVelocityHalfLife = frictionSliderValue[0];
                            loop.enqueue(() -> physics.settings.velocityHalfLife = newVelocityHalfLife);
                        }
                        ImGui.sameLine();
                        ImGuiUtils.helpMarker("The time after which half the velocity of a particle should be lost due to friction.");
                    }
                    {
                        ImFloat frictionfactorInputValue = new ImFloat((float) settings.velocityHalfLife);
                        if (ImGui.inputFloat("friction##Input", frictionfactorInputValue, 0.005f, 1.000f, "%.04f", ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final float newFrictionFactor = Math.max(0.005f, Math.min(frictionfactorInputValue.get(), 1.000f)); // Clamping the value within a range
                            loop.enqueue(() -> physics.settings.velocityHalfLife = newFrictionFactor);
                        }
                    }
                    float[] forceFactorSliderValue = new float[]{(float) settings.force};
                    if (ImGui.sliderFloat("force scaling", forceFactorSliderValue, 0.0f, 100.0f)) {
                        final float newForceFactor = forceFactorSliderValue[0];
                        loop.enqueue(() -> physics.settings.force = newForceFactor);
                    }

                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("The value of force between particles");

                    {
                        ImFloat forcefactorInputValue = new ImFloat((float) settings.force);
                        if (ImGui.inputFloat("force##Input", forcefactorInputValue, 0.005f, 1.000f, "%.04f", ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final float newForceFactor = Math.max(0.005f, Math.min(forcefactorInputValue.get(), 1000.000f)); // Clamping the value within a range
                            loop.enqueue(() -> physics.settings.force = newForceFactor);
                        }
                    }

                    if (appSettings.showAdvancedGui) {

                        ImGui.separator();

                        ImInt threadNumberInput = new ImInt(preferredNumberOfThreads);
                        if (ImGui.inputInt("threads", threadNumberInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final int newThreadNumber = Math.max(1, threadNumberInput.get());
                            loop.enqueue(() -> physics.preferredNumberOfThreads = newThreadNumber);
                        }
                        ImGui.sameLine();
                        ImGuiUtils.helpMarker("controls the number of threads used by your processor (if you don't know what this means leave it alone)");

                        if (ImGui.checkbox("auto time", autoDt)) autoDt ^= true;
                        ImGui.sameLine();
                        ImGuiUtils.helpMarker("If ticked, the time step of the physics computation will be chosen automatically based on the real passed time.");
                        if (autoDt) ImGui.beginDisabled();
                        float[] dtSliderValue = new float[]{(float) fallbackDt};
                        if (ImGui.sliderFloat("##dt", dtSliderValue, 0.0f, 0.1f, String.format("%.04f ms", fallbackDt * 1000.0))) {
                            fallbackDt = dtSliderValue[0];
                        }

                        {
                            ImFloat inputValue = new ImFloat((float) fallbackDt);
                            if (ImGui.inputFloat("Step##Time", inputValue, 0.0005f, 0.005f, "%.04f")) {
                                fallbackDt = MathUtils.constrain(inputValue.get(), 0.00f, 0.1f);
                            }
                        }


                        if (autoDt) ImGui.endDisabled();
                    }
                }

                ImGui.separator();
                ImGui.text("Graphics");
                {
                    // SHADERS
                    renderCombo("shader [s]", shaders);
                    String shaderDescription = shaders.getActiveDescription();
                    if (!shaderDescription.isBlank()) {
                        ImGui.sameLine();
                        ImGuiUtils.helpMarker("(i)", shaderDescription);
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("types of particles");

                    // PALETTES
                    renderCombo("palette [l]", palettes);
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("color of particles");

                    float[] particleSizeSliderValue = new float[]{appSettings.particleSize};
                    if (ImGui.sliderFloat("particle size [ctrl+scroll]", particleSizeSliderValue, 0.1f, 10f)) {
                        appSettings.particleSize = particleSizeSliderValue[0];
                    }
                    ImGui.sameLine();
                    ImGuiUtils.helpMarker("How large the particles are displayed.");

                    if (ImGui.checkbox("clear screen [c]", traces)) {
                        traces ^= true;
                    }

                    // CURSOR
                    if (appSettings.showAdvancedGui) {
                        ImGui.text("Cursor");
                        renderCombo("##cursoraction", cursorActions);
                        renderCombo("##cursor", cursorShapes);
                        cursor.shape = cursorShapes.getActive();
                        ImGui.sameLine();
                        if (ImGui.checkbox("show cursor", renderer.drawCursor)) {
                            renderer.drawCursor ^= true;
                        }
                        // cursor size slider
                        float[] cursorSizeSliderValue = new float[]{(float) cursor.size};
                        if (ImGui.sliderFloat("cursor size [shift+scroll]", cursorSizeSliderValue,
                                0.001f, 1.000f,
                                String.format("%.3f", cursor.size),
                                ImGuiSliderFlags.Logarithmic)) {
                            cursor.size = cursorSizeSliderValue[0];
                        }
                        if (cursorActions.getActive() == CursorAction.BRUSH) {
                            // brush power slider
                            int[] brushPowerSliderValue = new int[]{appSettings.brushPower};
                            if (ImGui.sliderInt("brush power", brushPowerSliderValue, 1, 100)) {
                                appSettings.brushPower = brushPowerSliderValue[0];
                            }
                        }

                    }
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

            if (ImGui.button("particle count = 0?")) {
                loop.enqueue(() -> physics.setParticleCount(0));
            }

            if (ImGui.beginPopupModal("Taking too long")) {

                ImGui.text("Physics couldn't be stopped.");

                if (ImGui.button("continue waiting")) {
                    ImGui.closeCurrentPopup();
                }

                if (ImGui.button("close app")) {
                    close();// kill whole app
                }

                ImGui.endPopup();
            }

            ImGui.endPopup();
        }

        ImGui.setNextWindowBgAlpha(appSettings.guiBackgroundAlpha);
        ImGui.setNextWindowSize(400 * scale, 400 * scale, ImGuiCond.Once);
        ImGui.setNextWindowPos((width - 400 * scale) / 2f, (height - 400 * scale) / 2f, ImGuiCond.Once);
        if (showSettings.get() && showGui.get()) {
            if (ImGui.begin("Settings", showSettings, ImGuiWindowFlags.None
                    | ImGuiWindowFlags.NoResize
                    | ImGuiWindowFlags.NoFocusOnAppearing)) {

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

                if (ImGui.checkbox("make particle size zoom-independent", appSettings.keepParticleSizeIndependentOfZoom)) {
                    appSettings.keepParticleSizeIndependentOfZoom ^= true;
                }

                {
                    float[] inputValue = new float[]{appSettings.guiBackgroundAlpha};
                    if (ImGui.sliderFloat("GUI Opacity", inputValue, 0.0f, 1.0f)) {
                        appSettings.guiBackgroundAlpha = inputValue[0];
                    }
                }

                {
                    ImGui.text("Matrix Diagram:");

                    ImGui.indent();

                    {
                        ImFloat inputValue = new ImFloat((float) appSettings.matrixGuiStepSize);
                        if (ImGui.inputFloat("Step Size##Matrix", inputValue, 0.05f, 0.05f, "%.2f")) {
                            appSettings.matrixGuiStepSize = MathUtils.constrain(inputValue.get(), 0.05f, 1.0f);
                        }
                    }

                    ImGui.unindent();
                }

                {
                    ImGui.text("Type Count Diagram:");

                    ImGui.indent();

                    {
                        ImInt inputValue = new ImInt(typeCountDiagramStepSize);
                        if (ImGui.inputInt("Step Size##TypeCount", inputValue, 10)) {
                            typeCountDiagramStepSize = Math.max(0, inputValue.get());
                        }
                    }

                    {
                        ImInt selected = new ImInt(typeCountDisplayPercentage ? 1 : 0);
                        ImGui.radioButton("absolute", selected, 0);
                        ImGui.sameLine();
                        ImGui.radioButton("percentage", selected, 1);
                        typeCountDisplayPercentage = selected.get() == 1;
                    }

                    ImGui.unindent();
                }

                if (ImGui.button("Style Editor")) {
                    showStyleEditor.set(!showStyleEditor.get());
                }
            }
            ImGui.end();
        }

        if (showStyleEditor.get()) {
            ImGui.showStyleEditor();
        }

        if (showShortcutsWindow.get()) {
            if (ImGui.begin("Shortcuts", showShortcutsWindow)) {
                ImGui.text("""
                        [l]/[L]: change palette
                        [s]/[S]: change shader
                        [v]/[V]: change accelerator
                                                
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
                                                
                        [a]: toggle advanced GUI
                        [c]: toggle traces (clear screen)
                        [h]: hide GUI / show GUI
                        """);
            }
            ImGui.end();
        }

        if (showAboutWindow.get()) {
            if (ImGui.begin("About", showAboutWindow)) {
                ImGui.text("By Tom Mohr.");
            }
            ImGui.end();
        }
    }

    private void buildMainMenu() {
        if (ImGui.beginMenu("Main")) {

            if (ImGui.menuItem("Settings..")) {
                showSettings.set(true);
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
                if (ImGui.menuItem("Enter Fullscreen", "F11")) {
                    setFullscreen(true);
                }
            }

            if (ImGui.beginMenu("Zoom")) {
                if (ImGui.menuItem("100%", "z")) {
                    resetCamera(true);
                }
                if (ImGui.menuItem("Fit", "Z")) {
                    resetCamera(true);
                    // zoom to fit larger dimension
                    zoomGoal = Math.max(width, height) / (double) Math.min(width, height);
                }
                ImGui.endMenu();
            }

            if (ImGui.menuItem("Hide GUI", "h")) {
                showGui.set(false);
            }

            if (ImGui.menuItem("Advanced GUI", "a", appSettings.showAdvancedGui)) {
                appSettings.showAdvancedGui ^= true;
            }

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Help")) {

            if (ImGui.menuItem("Shortcuts..")) {
                showShortcutsWindow.set(true);
            }

            if (ImGui.menuItem("About..")) {
                showAboutWindow.set(true);
            }

            ImGui.endMenu();
        }
    }

    /**
     * @return if the selection index changed
     */
    private boolean renderCombo(String label, SelectionManager<?> selectionManager) {
        int previousIndex = selectionManager.getActiveIndex();
        if (ImGui.beginCombo(label, selectionManager.getActiveName())) {
            for (int i = 0; i < selectionManager.size(); i++) {
                boolean isSelected = selectionManager.getActiveIndex() == i;
                if (ImGui.selectable(selectionManager.get(i).name, isSelected)) {
                    selectionManager.setActive(i);
                }
                if (isSelected) {
                    ImGui.setItemDefaultFocus();
                }
            }
            ImGui.endCombo();
        }
        return selectionManager.getActiveIndex() != previousIndex;
    }

    private void resetCamera(boolean smooth) {
        shiftGoal.set(0);
        zoomGoal = 1;
        if (!smooth) shift.set(0);
    }

    private void setShaderVariables() {

        transform.identity();
        new Coordinates(width, height, shift, zoom).apply(transform);

        // calculate palette
        int nTypes = settings.matrix.size();//todo: use value from buffer?
        Color[] colors = new Color[nTypes];
        Palette palette = palettes.getActive();
        for (int i = 0; i < nTypes; i++) {
            colors[i] = palette.getColor(i, nTypes);
        }

        ParticleShader particleShader = renderer.particleShader;

        particleShader.use();
        particleShader.setTime(System.nanoTime() / 1000_000_000.0f);
        particleShader.setPalette(colors);
        particleShader.setTransform(transform);
        particleShader.setSize(appSettings.particleSize / Math.min(width, height) / (appSettings.keepParticleSizeIndependentOfZoom ? (float) zoom : 1));
        particleShader.setDetail(MathUtils.constrain(getDetailFromZoom(), ParticleShader.MIN_DETAIL, ParticleShader.MAX_DETAIL));
    }

    private int getDetailFromZoom() {

        double particleSizeOnScreen = appSettings.keepParticleSizeIndependentOfZoom ? appSettings.particleSize : appSettings.particleSize * zoom;

        double minDetailSize = 4;  // at this size, the detail is 4
        double detailPerSize = 0.4;// from then on, the detail increases with this rate (per size on screen in pixels)

        if (particleSizeOnScreen < minDetailSize) {
            return 4;
        } else {
            return (int) Math.floor(minDetailSize + (particleSizeOnScreen - minDetailSize) * detailPerSize);
        }
    }

    @Override
    protected void onKeyPressed(String keyName) {
        switch (keyName) {
            case "LEFT" -> leftPressed = true;
            case "RIGHT" -> rightPressed = true;
            case "UP" -> upPressed = true;
            case "DOWN" -> downPressed = true;
            case "LEFT_SHIFT" -> leftShiftPressed = true;
            case "RIGHT_SHIFT" -> rightShiftPressed = true;
            case "LEFT_CONTROL" -> leftControlPressed = true;
            case "RIGHT_CONTROL" -> rightControlPressed = true;
            case "a" -> appSettings.showAdvancedGui ^= true;
            case "c" -> traces ^= true;
            case "h" -> {
                if (traces) {
                    traces = false;
                    showGui.set(true);
                } else {
                    showGui.set(!showGui.get());
                }
            }
            case "l" -> palettes.stepForward();
            case "L" -> palettes.stepBackward();
            case "s" -> shaders.stepForward();
            case "S" -> shaders.stepBackward();
            case "+", "=" -> zoomGoal *= Math.pow(appSettings.zoomStepFactor, 2);// more steps than when scrolling
            case "-" -> zoomGoal = Math.max(MIN_ZOOM, zoomGoal / Math.pow(appSettings.zoomStepFactor, 2));
            case "z" -> resetCamera(true);
            case "Z" -> {
                resetCamera(true);
                // zoom to fit larger dimension
                zoomGoal = Math.max(width, height) / (double) Math.min(width, height);
            }
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
            case "v" -> {
                accelerators.stepForward();
                final Accelerator nextAccelerator = accelerators.getActive();
                loop.enqueue(() -> physics.accelerator = nextAccelerator);
            }
            case "V" -> {
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
        if (button == 2) {
            draggingShift = true;
        } else if (button == 0) {
            draggingParticles = true;
        }
    }

    @Override
    protected void onMouseReleased(int button) {
        if (button == 2) {
            draggingShift = false;
        } else if (button == 0) {
            draggingParticles = false;
        }
    }

    @Override
    protected void onScroll(double y) {

        boolean controlPressed = leftControlPressed || rightControlPressed;
        boolean shiftPressed = leftShiftPressed || rightShiftPressed;
        boolean bothPressed = controlPressed && shiftPressed;

        if (bothPressed) {
            // nothing
        } else if (controlPressed) {
            // change particle size
            appSettings.particleSize *= Math.pow(1.2, -y);
        } else if (shiftPressed) {
            // change cursor size
            cursor.size *= Math.pow(1.2, -y);
        } else {
            // change camera zoom

            double zoomIncrease = Math.pow(appSettings.zoomStepFactor, y);

            Coordinates c = new Coordinates(width, height, shiftGoal, zoomGoal);  // use "goal" shift and zoom
            c.zoomInOnMouse(new Vector2d(mouseX, mouseY), Math.max(MIN_ZOOM, zoomGoal * zoomIncrease));

            zoomGoal = c.zoom;
            shiftGoal.set(c.shift);
        }
    }

    @Override
    protected void setFullscreen(boolean fullscreen) {
        super.setFullscreen(fullscreen);

        // remember fullscreen state for next startup
        appSettings.startInFullscreen = fullscreen;
    }
}
