package com.particle_life.app;

import com.particle_life.*;
import com.particle_life.app.color.Color;
import com.particle_life.app.color.Palette;
import com.particle_life.app.color.PalettesProvider;
import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.app.shaders.ShaderProvider;
import imgui.ImGui;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiInputTextFlags;
import imgui.flag.ImGuiSliderFlags;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL13.GL_MULTISAMPLE;

public class Main extends App {

    public static void main(String[] args) {
        new Main().launch("Particle Life Simulator", true);
    }

    // data
    private final Clock renderClock = new Clock(60);
    private final Clock renderTimer = new Clock(60);
    public static final Clock debugTimer = new Clock(10);
    private final SelectionManager<ParticleShader> shaders = new SelectionManager<>();
    private final SelectionManager<Palette> palettes = new SelectionManager<>();
    private final SelectionManager<AcceleratorCodeData> accelerators = new SelectionManager<>();
    private final SelectionManager<MatrixGenerator> matrixGenerators = new SelectionManager<>();
    private final SelectionManager<PositionSetter> positionSetters = new SelectionManager<>();
    private final SelectionManager<TypeSetter> typeSetters = new SelectionManager<>();

    // helper classes
    private final ImGuiStatsFormatter statsFormatter = new ImGuiStatsFormatter();
    private final AcceleratorCompiler acceleratorCompiler = new AcceleratorCompiler();
    private final Matrix4d transform = new Matrix4d();
    private final Renderer renderer = new Renderer();

    private ExtendedPhysics physics;
    private PhysicsSnapshot physicsSnapshot;
    public AtomicBoolean newSnapshotAvailable = new AtomicBoolean(false);

    // local copy of snapshot:
    private PhysicsSettings settings;
    private double physicsActualDt;
    private double physicsAvgFramerate;
    private int preferredNumberOfThreads;

    // particle rendering: constants
    private double zoomStepFactor = 1.2;
    private float particleSize = 4.0f;   // particle size on screen (in pixels)
    private boolean keepParticleSizeIndependentOfZoom = false;
    private double shiftSmoothness = 0.3f;
    private double zoomSmoothness = 0.3f;

    // particle rendering: controls
    private boolean traces = false;
    private final Vector3d shift = new Vector3d(0);
    private final Vector3d shiftGoal = new Vector3d(shift);
    private double zoom = 1;
    private double zoomGoal = zoom;
    boolean draggingShift = false;
    boolean draggingParticles = false;

    // GUI: style
    private float guiBackgroundAlpha = 1.0f;

    // GUI: constants that control how the GUI behaves
    private long physicsNotReactingThreshold = 3000;// time in milliseconds
    private double matrixGuiStepSize = 0.2;
    private int typeCountDiagramStepSize = 500;
    private boolean typeCountDisplayPercentage = false;

    // GUI: hide / show parts
    private final ImBoolean showGui = new ImBoolean(true);
    private boolean advancedGui = false;
    private boolean showImportWindow = false;
    private boolean showExportWindow = false;
    private final ImBoolean showStyleEditor = new ImBoolean(false);
    private final ImBoolean showAcceleratorEditor = new ImBoolean(false);
    private final ImBoolean showGraphicsSettings = new ImBoolean(false);
    private final ImBoolean showHelpWindow = new ImBoolean(false);
    private final ImBoolean showAboutWindow = new ImBoolean(false);

    // GUI: store data on the current state of the GUI
    private final ExportSettings exportSettings = new ExportSettings();
    private final ImportSettings importSettings = new ImportSettings();
    private InfoWrapper<AcceleratorCodeData> editingAccelerator = null;
    private final ImString textInputAcceleratorCodeImports = new ImString();
    private final ImString textInputAcceleratorCodeMethodCode = new ImString();
    private final ImString textInputAcceleratorCodeClassName = new ImString();
    private boolean tracesBefore = traces;// if "traces" was enabled in last frame

    @Override
    protected void setup() {

        // todo: throw error if any return 0 elements
        shaders.addAll(new ShaderProvider().create());
        palettes.addAll(new PalettesProvider().create());
        accelerators.addAll(new AcceleratorProvider().create());
        positionSetters.addAll(new PositionSetterProvider().create());
        matrixGenerators.addAll(new MatrixGeneratorProvider().create());
        typeSetters.addAll(new TypeSetterProvider().create());

        createPhysics();

        //todo: what should be the default palette?
        if (palettes.size() > 1) {
            palettes.setActive(1);
        }

        glEnable(GL_MULTISAMPLE);

        renderer.init();
    }

    private void createPhysics() {
        physics = new ExtendedPhysics(
                accelerators.getActive().object.accelerator,
                positionSetters.getActive().object,
                matrixGenerators.getActive().object,
                typeSetters.getActive().object);
        physicsSnapshot = new PhysicsSnapshot();
        physicsSnapshot.take(physics);
        newSnapshotAvailable.set(true);
        physics.startLoop();
    }

    @Override
    protected void beforeClose() {
        physics.stopLoop();
        renderer.dispose();
    }

    @Override
    protected void draw(double dt) {

        renderClock.tick();

        if (draggingShift) {

            shift.set(new Coordinates(width, height, shift, zoom)
                    .mouseShift(new Vector2d(pmouseX, pmouseY), new Vector2d(mouseX, mouseY))
                    .shift);
            shiftGoal.set(shift);  // don't use smoothing while dragging
        }

        shift.lerp(shiftGoal, shiftSmoothness);
        zoom = MathUtils.lerp(zoom, zoomGoal, zoomSmoothness);

        if (draggingParticles) {
            Coordinates coordinates = new Coordinates(width, height, shift, zoom);
            final Vector3d w = coordinates.world(pmouseX, pmouseY);
            final Vector3d delta = coordinates.world(mouseX, mouseY).sub(w);
            physics.enqueue(() -> {
                for (Particle p : physics.particles) {
                    if (physics.distance(w, p.x) < 0.1) {
                        p.x.add(delta);
                        physics.ensurePosition(p.x);
                    }
                }
            });
        }

        renderer.particleShader = shaders.getActive().object;  // need to assign a shader before buffering particle data

        if (newSnapshotAvailable.get()) {

            // get local copy of snapshot

            //todo: only write types if necessary?
            renderer.bufferParticleData(physicsSnapshot.x, physicsSnapshot.v, physicsSnapshot.types);
            settings = physicsSnapshot.settings.deepCopy();
            physicsActualDt = physicsSnapshot.actualDt;
            physicsAvgFramerate = physicsSnapshot.avgFramerate;
            preferredNumberOfThreads = physics.preferredNumberOfThreads;

            newSnapshotAvailable.set(false);
        }

        physics.doOnce(() -> {
            physicsSnapshot.take(physics);
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

        ImGui.newFrame();
        // Any Dear ImGui code must go between ImGui.newFrame() and ImGui.render()
        if (!traces && showGui.get()) {
            buildGui();
        }
        ImGui.render();

        renderTimer.in();

        setParticleShaderUniforms();
        if (!traces || !tracesBefore) renderer.clear();
        tracesBefore = traces;
        renderer.run(ImGui.getDrawData(), width, height);

        renderTimer.out();
    }

    private void buildGui() {

        ImGui.setNextWindowBgAlpha(guiBackgroundAlpha);
        ImGui.setNextWindowPos(0, 0);
        ImGui.setNextWindowSize(-1, height);
        if (showGui.get()) {
            if (ImGui.begin("App", showGui, ImGuiWindowFlags.None
                    | ImGuiWindowFlags.NoResize
                    | ImGuiWindowFlags.NoFocusOnAppearing
                    | ImGuiWindowFlags.NoBringToFrontOnFocus
                    | ImGuiWindowFlags.NoTitleBar
                    | ImGuiWindowFlags.MenuBar)) {

                if (ImGui.beginMenuBar()) {
                    buildMainMenu();
                    ImGui.endMenuBar();
                }

                ImGui.pushItemWidth(200);

                ImGui.text("Info");
                {
                    statsFormatter.start();
                    statsFormatter.put("Graphics FPS", String.format("%.0f", renderClock.getAvgFramerate()));
                    statsFormatter.put("Physics FPS", physicsAvgFramerate < 100000 ? String.format("%.0f", physicsAvgFramerate) : "inf");
                    statsFormatter.put("Physics vs. Graphics", physicsAvgFramerate < 100000 ? String.format("%.2f", physicsAvgFramerate / renderClock.getAvgFramerate()) : "inf");
                    if (advancedGui) {
                        statsFormatter.put("Graphics FPS Standard Deviation", String.format("%.2f", renderClock.getStandardDeviation()));
                        statsFormatter.put("debug", String.format("%.1f ms", debugTimer.getAvgDtMillis()));
                    }
                    statsFormatter.end();
                }

                ImGui.separator();

                ImGui.text("Particles");
                {

                    // N
                    ImInt particleCountInput = new ImInt(settings.n);
                    if (ImGui.inputInt("particle count", particleCountInput, 1000, 1000, ImGuiInputTextFlags.EnterReturnsTrue)) {
                        final int newCount = Math.max(0, particleCountInput.get());
                        physics.enqueue(() -> physics.settings.n = newCount);
                    }

                    ImGui.separator();

                    // TYPES
                    {
                        // NTYPES
                        ImInt matrixSizeInput = new ImInt(settings.matrix.size());
                        if (ImGui.inputInt("types", matrixSizeInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final int newSize = Math.max(1, matrixSizeInput.get());
                            physics.enqueue(() -> physics.setMatrixSize(newSize));
                        }

                        if (advancedGui) {

                            ImGuiBarGraph.draw(200, 100,
                                    palettes.getActive().object,
                                    typeCountDiagramStepSize,
                                    physicsSnapshot.typeCount,
                                    (type, newValue) -> {
                                        final int[] newTypeCount = Arrays.copyOf(physicsSnapshot.typeCount, physicsSnapshot.typeCount.length);
                                        newTypeCount[type] = newValue;
//                                        physics.enqueue(() -> physics.settings.n = Arrays.stream(newTypeCount).sum());
                                        physics.enqueue(() -> physics.setTypeCount(newTypeCount));
                                    },
                                    typeCountDisplayPercentage
                            );
                            ImGui.sameLine();
                            ImGuiUtils.advancedGuiHint();

                            if (ImGui.button("equalize type count")) {
                                physics.enqueue(() -> physics.setTypeCountEqual());
                            }
                        }

                        // MATRIX
                        ImGuiMatrix.draw(200, 200,
                                palettes.getActive().object,
                                matrixGuiStepSize,
                                settings.matrix,
                                (i, j, newValue) -> physics.enqueue(() -> physics.settings.matrix.set(i, j, newValue))
                        );

                        if (advancedGui) {
                            ImGui.text("Clipboard:");
                            ImGui.sameLine();
                            if (ImGui.button("Copy")) {
                                ImGui.setClipboardText(MatrixParser.matrixToString(settings.matrix));
                            }
                            ImGui.sameLine();
                            if (ImGui.button("Paste")) {
                                Matrix parsedMatrix = MatrixParser.parseMatrix(ImGui.getClipboardText());
                                if (parsedMatrix != null) {
                                    physics.enqueue(() -> {
                                        physics.setMatrixSize(parsedMatrix.size());
                                        physics.settings.matrix = parsedMatrix;
                                    });
                                }
                            }
                        }

                        // MATRIX GENERATORS
                        if (renderCombo("##matrix", matrixGenerators)) {
                            final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive().object;
                            physics.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
                        }
                        ImGui.sameLine();
                        if (ImGui.button("matrix [m]")) {
                            physics.enqueue(physics::generateMatrix);
                        }
                    }

                    ImGui.separator();

                    // POSITION SETTERS
                    if (renderCombo("##positions", positionSetters)) {
                        final PositionSetter nextPositionSetter = positionSetters.getActive().object;
                        physics.enqueue(() -> physics.positionSetter = nextPositionSetter);
                    }
                    ImGui.sameLine();
                    if (ImGui.button("positions [p]")) {
                        physics.enqueue(physics::setPositions);
                    }

                    ImGui.separator();

                    // TYPE SETTERS
                    if (renderCombo("##types", typeSetters)) {
                    }
                    ImGui.sameLine();
                    if (ImGui.button("types [t]")) {
                        physics.enqueue(() -> {
                            TypeSetter previousTypeSetter = physics.typeSetter;
                            physics.typeSetter = typeSetters.getActive().object;
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

                    if (ImGui.button("%s [SPACE]".formatted(physicsSnapshot.pause ? "Unpause" : "Pause"))) {
                        final boolean newValue = !physicsSnapshot.pause;
                        physics.enqueue(() -> physics.pause = newValue);
                    }

                    // ACCELERATORS
                    if (advancedGui) {
                        ImGui.text("Accelerator [v]");
                        if (renderCombo("##accelerator", accelerators)) {
                            final Accelerator nextAccelerator = accelerators.getActive().object.accelerator;
                            physics.enqueue(() -> physics.accelerator = nextAccelerator);
                        }
                        ImGui.sameLine();
                        if (accelerators.getActive().object.mayEdit) {
                            if (ImGui.button("Edit")) {
                                showAcceleratorEditor.set(true);
                                editingAccelerator = accelerators.getActive();
                                acceleratorCompiler.clearError();
                            }
                            ImGui.sameLine();
                        }
                        if (ImGui.button("New")) {
                            showAcceleratorEditor.set(true);
                            editingAccelerator = new InfoWrapper<>("ExampleAccelerator", "", new AcceleratorCodeData(
                                    true,
                                    "",
                                    "ExampleAccelerator",
                                    "return x;",
                                    null
                            ));
                            acceleratorCompiler.clearError();
                        }
                    }

                    if (ImGui.checkbox("wrap [w]", settings.wrap)) {
                        final boolean newWrap = !settings.wrap;
                        physics.enqueue(() -> physics.settings.wrap = newWrap);
                    }

                    {
                        float displayValue = (float) settings.rmax;
                        float[] rmaxSliderValue = new float[]{displayValue};
                        if (ImGui.sliderFloat("rmax", rmaxSliderValue, 0.005f, 1.000f, String.format("%.3f", displayValue), ImGuiSliderFlags.Logarithmic)) {
                            final float newRmax = rmaxSliderValue[0];
                            physics.enqueue(() -> physics.settings.rmax = newRmax);
                        }
                    }

                    {// FRICTION
                        double secondPart = 10.0;
                        double frictionVal = Math.pow(settings.friction, 1.0 / secondPart);
                        float[] frictionSliderValue = new float[]{(float) frictionVal};
                        if (ImGui.sliderFloat("friction", frictionSliderValue, 0.0f, 1.0f, String.format("%.3f / %.1fs", frictionVal, 1.0 / secondPart))) {
                            final double newFriction = Math.pow(frictionSliderValue[0], secondPart);
                            physics.enqueue(() -> physics.settings.friction = newFriction);
                        }
                    }

                    float[] forceFactorSliderValue = new float[]{(float) settings.forceFactor};
                    if (ImGui.sliderFloat("force", forceFactorSliderValue, 0.0f, 5.0f)) {
                        final float newForceFactor = forceFactorSliderValue[0];
                        physics.enqueue(() -> physics.settings.forceFactor = newForceFactor);
                    }

                    if (advancedGui) {

                        ImGui.separator();

                        ImInt threadNumberInput = new ImInt(preferredNumberOfThreads);
                        if (ImGui.inputInt("threads", threadNumberInput, 1, 1, ImGuiInputTextFlags.EnterReturnsTrue)) {
                            final int newThreadNumber = Math.max(1, threadNumberInput.get());
                            physics.enqueue(() -> physics.preferredNumberOfThreads = newThreadNumber);
                        }

                        float[] dtSliderValue = new float[]{(float) settings.fallbackDt};
                        if (ImGui.sliderFloat("##dt", dtSliderValue, 0.0f, 0.1f, String.format("%4.1f ms", settings.fallbackDt * 1000.0), ImGuiSliderFlags.Logarithmic)) {
                            physics.enqueue(() -> physics.settings.fallbackDt = dtSliderValue[0]);
                        }
                        ImGui.sameLine();
                        if (ImGui.checkbox("fixed step", !settings.autoDt)) {
                            final boolean newAutoDt = !settings.autoDt;
                            physics.enqueue(() -> physics.settings.autoDt = newAutoDt);
                        }
                    }
                }

                ImGui.separator();
                ImGui.text("Graphics");
                {
                    // SHADERS
                    renderCombo("shader [s]", shaders);
                    String shaderDescription = shaders.getActive().description;
                    if (!shaderDescription.isBlank()) {
                        ImGui.sameLine();
                        ImGuiUtils.helpMarker("(i)", shaderDescription);
                    }

                    // PALETTES
                    renderCombo("palette [l]", palettes);

                    float[] particleSizeSliderValue = new float[]{particleSize};
                    if (ImGui.sliderFloat("particle size", particleSizeSliderValue, 0.1f, 10f)) {
                        particleSize = particleSizeSliderValue[0];
                    }

                    if (ImGui.checkbox("clear screen [c]", traces)) {
                        traces ^= true;
                    }
                }

                ImGui.popItemWidth();
            }
            ImGui.end();
        }

        // EXPORT
        if (showExportWindow && showGui.get()) ImGui.openPopup("Export");
        if (ImGui.beginPopupModal("Export")) {

            ImGui.text("Choose what you want to export:");

            if (ImGui.checkbox("Particles", exportSettings.particles)) {
                exportSettings.particles ^= true;
            }
            if (exportSettings.particles) {
                ImGui.sameLine();
                ImGui.textDisabled(settings.n + " particles");
            }

            if (ImGui.checkbox("matrix", exportSettings.matrix)) {
                exportSettings.matrix ^= true;
            }
            if (exportSettings.matrix) {
                ImGui.sameLine();
                ImGui.textDisabled("size " + settings.matrix.size());
            }

            if (ImGui.checkbox("time step", exportSettings.timeStep)) {
                exportSettings.timeStep ^= true;
            }
            if (exportSettings.timeStep) {
                ImGui.sameLine();
                ImGui.textDisabled("%.1f ms".formatted(settings.fallbackDt * 1000));
            }

            if (ImGui.button("Cancel")) {
                ImGui.closeCurrentPopup();
                showExportWindow = false;
            }
            ImGui.sameLine();
            if (ImGui.button("Export")) {
                exportData();
                ImGui.closeCurrentPopup();
                showExportWindow = false;
            }
            ImGui.endPopup();
        }

        // IMPORT
        if (showImportWindow && showGui.get()) ImGui.openPopup("Import");
        if (ImGui.beginPopupModal("Import")) {
            ImGui.text("Import this and that...");

            ImGui.textDisabled(importSettings.path);

            ImString inputString = new ImString(importSettings.path);
            if (ImGui.inputText("Path", inputString, ImGuiInputTextFlags.EnterReturnsTrue)) {
                importSettings.path = inputString.get();
            }

            if (ImGui.button("Cancel")) {
                ImGui.closeCurrentPopup();
                showImportWindow = false;
            }
            ImGui.sameLine();
            if (ImGui.button("Import")) {
                importData();
                ImGui.closeCurrentPopup();
                showImportWindow = false;
            }
            ImGui.endPopup();
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

                if (physics.stopLoop()) {
                    createPhysics();
                } else {
                    ImGui.openPopup("Taking too long");
                }
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

        ImGui.setNextWindowBgAlpha(guiBackgroundAlpha);
        ImGui.setNextWindowSize(400, 400, ImGuiCond.Once);
        ImGui.setNextWindowPos((width - 400) / 2f, (height - 400) / 2f, ImGuiCond.Once);
        if (showGraphicsSettings.get() && showGui.get()) {
            if (ImGui.begin("Graphics Settings", showGraphicsSettings, ImGuiWindowFlags.None
                    | ImGuiWindowFlags.NoResize
                    | ImGuiWindowFlags.NoFocusOnAppearing)) {
                {
                    float[] inputValue = new float[]{(float) zoomSmoothness};
                    if (ImGui.sliderFloat("Camera Speed", inputValue, 0.0f, 1.0f, "%0.2f")) {
                        zoomSmoothness = inputValue[0];
                        shiftSmoothness = inputValue[0];
                    }
                }

                {
                    float[] inputValue = new float[]{(float) (zoomStepFactor - 1) * 100};
                    if (ImGui.sliderFloat("Zoom Step", inputValue, 0.0f, 100.0f, "%.1f%%", ImGuiSliderFlags.Logarithmic)) {
                        zoomStepFactor = 1 + inputValue[0] * 0.01;
                    }
                }

                if (ImGui.checkbox("make particle size zoom-independent", keepParticleSizeIndependentOfZoom)) {
                    keepParticleSizeIndependentOfZoom ^= true;
                }

                {
                    float[] inputValue = new float[]{guiBackgroundAlpha};
                    if (ImGui.sliderFloat("GUI Background Alpha", inputValue, 0.0f, 1.0f)) {
                        guiBackgroundAlpha = inputValue[0];
                    }
                }

                {
                    ImGui.text("Matrix Diagram:");

                    ImGui.indent();

                    {
                        ImFloat inputValue = new ImFloat((float) matrixGuiStepSize);
                        if (ImGui.inputFloat("Step Size##Matrix", inputValue, 0.05f, 0.05f, "%.2f")) {
                            matrixGuiStepSize = MathUtils.constrain(inputValue.get(), 0.05f, 1.0f);
                        }
                    }

                    ImGui.unindent();
                }

                {
                    ImGui.text("Type Count Diagram:");
                    ImGui.sameLine();
                    ImGuiUtils.advancedGuiHint();

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

        if (showHelpWindow.get()) {
            if (ImGui.begin("Help", showHelpWindow)) {
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
                                                
                        [+]/[-]: zoom
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

        if (showAcceleratorEditor.get()) {

            float w = 400;
            float h = 400;
            ImGui.setNextWindowPos((width - w) / 2f, (height - h) / 2f, ImGuiCond.Once);
            ImGui.setNextWindowSize(0, 0, ImGuiCond.Once);
            ImGui.setNextWindowBgAlpha(guiBackgroundAlpha);

            if (ImGui.begin("Edit Accelerator", showAcceleratorEditor)) {

                textInputAcceleratorCodeImports.set(editingAccelerator.object.importCode);
                textInputAcceleratorCodeMethodCode.set(editingAccelerator.object.methodCode);
                textInputAcceleratorCodeClassName.set(editingAccelerator.object.className);

                ImGui.textDisabled("// imports:");
                ImGui.sameLine();
                ImGuiUtils.helpMarker("Add imports here if you need them. Example: import java.util.List;");
                if (ImGui.inputTextMultiline("##Imports", textInputAcceleratorCodeImports, -1, 50, ImGuiInputTextFlags.CallbackResize)) {
                    editingAccelerator.object.importCode = textInputAcceleratorCodeImports.get();
                }

                ImGui.text("public class ");
                ImGui.sameLine();
                ImGui.pushItemWidth(200);
                if (ImGui.inputText("##Class Name", textInputAcceleratorCodeClassName, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.CharsNoBlank | ImGuiInputTextFlags.AutoSelectAll)) {
                    editingAccelerator.object.className = textInputAcceleratorCodeClassName.get();
                    editingAccelerator.name = textInputAcceleratorCodeClassName.get();
                }
                ImGui.popItemWidth();
                ImGui.sameLine();
                ImGui.text("implements Accelerator {");
                ImGui.indent();

                ImGui.text("public Vector3d accelerate(double a, Vector3d x) {");
                ImGui.indent();
                if (ImGui.inputTextMultiline("##Code", textInputAcceleratorCodeMethodCode, -1, 100, ImGuiInputTextFlags.CallbackResize | ImGuiInputTextFlags.AllowTabInput)) {
                    editingAccelerator.object.methodCode = textInputAcceleratorCodeMethodCode.get();
                }
                ImGui.unindent();
                ImGui.text("}");
                ImGui.unindent();
                ImGui.text("}");

                if (ImGui.button("OK")) {

                    Accelerator compiledAccelerator = acceleratorCompiler.compile(editingAccelerator.object);

                    if (compiledAccelerator != null) {
                        editingAccelerator.object.accelerator = compiledAccelerator;

                        if (accelerators.contains(editingAccelerator)) {
                            // if this accelerator is currently active, also set the accelerator of physics to the new one.
                            if (accelerators.getActive() == editingAccelerator) {
                                final Accelerator newAccelerator = editingAccelerator.object.accelerator;
                                physics.enqueue(() -> physics.accelerator = newAccelerator);
                            }
                        } else {
                            // add new
                            accelerators.add(editingAccelerator);
                        }
                    }
                }

                if (acceleratorCompiler.hasError()) {
                    ImGui.textColored(1, 0, 0, 1, acceleratorCompiler.getErrorMessage());
                    if (ImGui.button("Clear")) {
                        acceleratorCompiler.clearError();
                    }
                }
            }
            ImGui.end();
        }
    }

    private void setInitialExportSettings() {
        exportSettings.timeStep = !settings.autoDt;
    }

    private void exportData() {

        //todo: this is just a demo

        System.out.println("Now exporting...");

        if (exportSettings.particles) {
            System.out.println("- particles");
        }

        if (exportSettings.matrix) {
            System.out.println("- matrix");
        }

        if (exportSettings.timeStep) {
            System.out.println("- time step");
        }
    }

    private void importData() {
        System.out.println("Now importing...");

        if (exportSettings.timeStep) {
            System.out.println("- path: " + importSettings.path);
        }
    }

    private void buildMainMenu() {
        if (ImGui.beginMenu("File")) {
            if (ImGui.menuItem("Import..", "Str+O")) {
                //todo show file dialog
                //     importSettings.path = ...
                showImportWindow = true;
            }

            if (ImGui.menuItem("Export..", "Str+S")) {
                showExportWindow = true;
                setInitialExportSettings();
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

            if (ImGui.menuItem("Graphics Settings..")) {
                showGraphicsSettings.set(true);
            }

            if (ImGui.menuItem("Advanced GUI", "a", advancedGui)) {
                advancedGui = !advancedGui;
            }

            ImGui.endMenu();
        }

        if (ImGui.beginMenu("Help")) {

            if (ImGui.menuItem("Help")) {
                showHelpWindow.set(true);
            }

            if (ImGui.menuItem("About")) {
                showAboutWindow.set(true);
            }

            ImGui.endMenu();
        }
    }

    /**
     * @return if the selection index changed
     */
    private boolean renderCombo(String label, SelectionManager<?> selectionManager) {
        InfoWrapper<?> currentShader = selectionManager.getActive();
        int previousIndex = selectionManager.getActiveIndex();
        if (ImGui.beginCombo(label, currentShader.name)) {
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

    private void setParticleShaderUniforms() {

        transform.identity();
        new Coordinates(width, height, shift, zoom).apply(transform);

        // calculate palette
        int nTypes = settings.matrix.size();//todo: use value from buffer?
        Color[] colors = new Color[nTypes];
        Palette palette = palettes.getActive().object;
        for (int i = 0; i < nTypes; i++) {
            colors[i] = palette.getColor(i, nTypes);
        }

        ParticleShader particleShader = renderer.particleShader;

        particleShader.use();
        particleShader.setTime(System.nanoTime() / 1000_000_000.0f);
        particleShader.setPalette(colors);
        particleShader.setTransform(transform);
        particleShader.setSize(particleSize / Math.min(width, height) / (keepParticleSizeIndependentOfZoom ? (float) zoom : 1));
        particleShader.setDetail(MathUtils.constrain(getDetailFromZoom(), ParticleShader.MIN_DETAIL, ParticleShader.MAX_DETAIL));
    }

    private int getDetailFromZoom() {

        double particleSizeOnScreen = keepParticleSizeIndependentOfZoom ? particleSize : particleSize * zoom;

        double minDetailSize = 4;  // at this size, the detail is 4
        double detailPerSize = 0.4;// from then on, the detail increases with this rate (per size on screen in pixels)

        if (particleSizeOnScreen < minDetailSize) {
            return 4;
        } else {
            return (int) Math.floor(minDetailSize + (particleSizeOnScreen - minDetailSize) * detailPerSize);
        }
    }

    private void textureTest() throws IOException {
//        URL url = ClassLoader.getSystemClassLoader().getResourceAsStream();
        String filename = "textures/particle.png";
        URL url = ClassLoader.getSystemClassLoader().getResource(filename);
        System.out.println(url);
        BufferedImage img = ImageIO.read(url);
        int imgWidth = img.getWidth();
        int imgHeight = img.getHeight();
        int[] pixels = ((DataBufferInt) img.getData().getDataBuffer()).getData();
        System.out.println(pixels.length);
//        int texture = glGenTextures();
//        glBindTexture(GL_TEXTURE_2D, texture);
//        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, imgWidth, imgHeight, 0, GL_RGBA, GL_INT, pixels);
    }

    @Override
    protected void onKeyPressed(char c) {
        switch (c) {
            case 'a' -> advancedGui = !advancedGui;
            case 'c' -> traces = !traces;
            case 'h' -> {
                if (traces) {
                    traces = false;
                    showGui.set(true);
                } else {
                    showGui.set(!showGui.get());
                }
            }
            case 'l' -> selectionStep(palettes, 1);
            case 'L' -> selectionStep(palettes, -1);
            case 's' -> selectionStep(shaders, 1);
            case 'S' -> selectionStep(shaders, -1);
            case '+' -> zoomGoal *= Math.pow(zoomStepFactor, 2);// more steps than when scrolling
            case '-' -> zoomGoal /= Math.pow(zoomStepFactor, 2);
            case 'z' -> resetCamera(true);
            case 'Z' -> {
                resetCamera(true);
                // zoom to fit larger dimension
                zoomGoal = Math.max(width, height) / (double) Math.min(width, height);
            }
            case 'p' -> physics.enqueue(physics::setPositions);
            case 't' -> physics.enqueue(() -> {
                TypeSetter previousTypeSetter = physics.typeSetter;
                physics.typeSetter = typeSetters.getActive().object;
                physics.setTypes();
                physics.typeSetter = previousTypeSetter;
            });
            case 'm' -> physics.enqueue(physics::generateMatrix);
            case 'w' -> physics.enqueue(() -> physics.settings.wrap = !physics.settings.wrap);
            case ' ' -> physics.enqueue(() -> physics.pause = !physics.pause);
            case 'v' -> {
                selectionStep(accelerators, 1);
                final Accelerator nextAccelerator = accelerators.getActive().object.accelerator;
                physics.enqueue(() -> physics.accelerator = nextAccelerator);
            }
            case 'V' -> {
                selectionStep(accelerators, -1);
                final Accelerator nextAccelerator = accelerators.getActive().object.accelerator;
                physics.enqueue(() -> physics.accelerator = nextAccelerator);
            }
            case 'x' -> {
                selectionStep(positionSetters, 1);
                final PositionSetter nextPositionSetter = positionSetters.getActive().object;
                physics.enqueue(() -> physics.positionSetter = nextPositionSetter);
            }
            case 'X' -> {
                selectionStep(positionSetters, -1);
                final PositionSetter nextPositionSetter = positionSetters.getActive().object;
                physics.enqueue(() -> physics.positionSetter = nextPositionSetter);
            }
            case 'r' -> {
                selectionStep(matrixGenerators, 1);
                final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive().object;
                physics.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
            }
            case 'R' -> {
                selectionStep(matrixGenerators, -1);
                final MatrixGenerator nextMatrixGenerator = matrixGenerators.getActive().object;
                physics.enqueue(() -> physics.matrixGenerator = nextMatrixGenerator);
            }
        }
    }

    private void selectionStep(SelectionManager<?> selectionManager, int step) {
        selectionManager.setActive(MathUtils.modulo(selectionManager.getActiveIndex() + step, selectionManager.size()));
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

        double zoomIncrease = Math.pow(zoomStepFactor, y);

        Coordinates c = new Coordinates(width, height, shiftGoal, zoomGoal);  // use "goal" shift and zoom
        c.zoomInOnMouse(new Vector2d(mouseX, mouseY), zoomIncrease);

        zoomGoal = c.zoom;
        shiftGoal.set(c.shift);
    }
}
