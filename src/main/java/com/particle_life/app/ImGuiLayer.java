package com.particle_life.app;

import imgui.*;
import imgui.callback.ImStrConsumer;
import imgui.callback.ImStrSupplier;
import imgui.flag.ImGuiBackendFlags;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiKey;
import imgui.flag.ImGuiMouseCursor;
import org.lwjgl.glfw.*;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

public class ImGuiLayer {

    private long glfwWindow;

    // Mouse cursors provided by GLFW
    private final long[] mouseCursors = new long[ImGuiMouseCursor.COUNT];

    public List<GLFWMouseButtonCallbackI> mouseButtonCallbacks = new ArrayList<>();
    public List<GLFWCharCallbackI> charCallbacks = new ArrayList<>();
    public List<GLFWScrollCallbackI> scrollCallbacks = new ArrayList<>();
    public List<GLFWCursorPosCallbackI> cursorPosCallbacks = new ArrayList<>();
    public List<GLFWKeyCallbackI> keyCallbacks = new ArrayList<>();
    private ImGuiIO io;


    private final boolean[] mouseDown = new boolean[5];
    private final boolean[] pmouseDown = new boolean[5];// previous state

    public ImGuiLayer(long glfwWindow) {
        this.glfwWindow = glfwWindow;
    }

    // Initialize Dear ImGui.
    public void initImGui() {
        // IMPORTANT!!
        // This line is critical for Dear ImGui to work.
        ImGui.createContext();

        // ------------------------------------------------------------
        // Initialize ImGuiIO config
        io = ImGui.getIO();

        io.setIniFilename(null); // We don't want to save .ini file
        io.setConfigFlags(ImGuiConfigFlags.NavEnableKeyboard); // Navigation with keyboard
        io.setBackendFlags(ImGuiBackendFlags.HasMouseCursors); // Mouse cursors to display while resizing windows etc.
        io.setBackendPlatformName("imgui_java_impl_glfw");

        // ------------------------------------------------------------
        // Keyboard mapping. ImGui will use those indices to peek into the io.KeysDown[] array.
        final int[] keyMap = new int[ImGuiKey.COUNT];
        keyMap[ImGuiKey.Tab] = GLFW_KEY_TAB;
        keyMap[ImGuiKey.LeftArrow] = GLFW_KEY_LEFT;
        keyMap[ImGuiKey.RightArrow] = GLFW_KEY_RIGHT;
        keyMap[ImGuiKey.UpArrow] = GLFW_KEY_UP;
        keyMap[ImGuiKey.DownArrow] = GLFW_KEY_DOWN;
        keyMap[ImGuiKey.PageUp] = GLFW_KEY_PAGE_UP;
        keyMap[ImGuiKey.PageDown] = GLFW_KEY_PAGE_DOWN;
        keyMap[ImGuiKey.Home] = GLFW_KEY_HOME;
        keyMap[ImGuiKey.End] = GLFW_KEY_END;
        keyMap[ImGuiKey.Insert] = GLFW_KEY_INSERT;
        keyMap[ImGuiKey.Delete] = GLFW_KEY_DELETE;
        keyMap[ImGuiKey.Backspace] = GLFW_KEY_BACKSPACE;
        keyMap[ImGuiKey.Space] = GLFW_KEY_SPACE;
        keyMap[ImGuiKey.Enter] = GLFW_KEY_ENTER;
        keyMap[ImGuiKey.Escape] = GLFW_KEY_ESCAPE;
        keyMap[ImGuiKey.KeyPadEnter] = GLFW_KEY_KP_ENTER;
        keyMap[ImGuiKey.A] = GLFW_KEY_A;
        keyMap[ImGuiKey.C] = GLFW_KEY_C;
        keyMap[ImGuiKey.V] = GLFW_KEY_V;
        keyMap[ImGuiKey.X] = GLFW_KEY_X;
        keyMap[ImGuiKey.Y] = GLFW_KEY_Y;
        keyMap[ImGuiKey.Z] = GLFW_KEY_Z;
        io.setKeyMap(keyMap);

        // ------------------------------------------------------------
        // Mouse cursors mapping
        mouseCursors[ImGuiMouseCursor.Arrow] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.TextInput] = glfwCreateStandardCursor(GLFW_IBEAM_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeAll] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNS] = glfwCreateStandardCursor(GLFW_VRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeEW] = glfwCreateStandardCursor(GLFW_HRESIZE_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNESW] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.ResizeNWSE] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);
        mouseCursors[ImGuiMouseCursor.Hand] = glfwCreateStandardCursor(GLFW_HAND_CURSOR);
        mouseCursors[ImGuiMouseCursor.NotAllowed] = glfwCreateStandardCursor(GLFW_ARROW_CURSOR);

        // ------------------------------------------------------------
        // GLFW callbacks to handle user input

        glfwSetInputMode(glfwWindow, GLFW_STICKY_MOUSE_BUTTONS, GLFW_TRUE);
        glfwSetInputMode(glfwWindow, GLFW_STICKY_KEYS, GLFW_TRUE);

        glfwSetKeyCallback(glfwWindow, (w, key, scancode, action, mods) -> {
            if (action == GLFW_PRESS) {
                io.setKeysDown(key, true);
            } else if (action == GLFW_RELEASE) {
                io.setKeysDown(key, false);
            }

            io.setKeyCtrl(io.getKeysDown(GLFW_KEY_LEFT_CONTROL) || io.getKeysDown(GLFW_KEY_RIGHT_CONTROL));
            io.setKeyShift(io.getKeysDown(GLFW_KEY_LEFT_SHIFT) || io.getKeysDown(GLFW_KEY_RIGHT_SHIFT));
            io.setKeyAlt(io.getKeysDown(GLFW_KEY_LEFT_ALT) || io.getKeysDown(GLFW_KEY_RIGHT_ALT));
            io.setKeySuper(io.getKeysDown(GLFW_KEY_LEFT_SUPER) || io.getKeysDown(GLFW_KEY_RIGHT_SUPER));

            // dispatch to application
            if (!io.getWantTextInput()) {
                keyCallbacks.forEach(callback -> callback.invoke(w, key, scancode, action, mods));
            }
        });

        glfwSetCharCallback(glfwWindow, (w, c) -> {
            if (c != GLFW_KEY_DELETE) {
                io.addInputCharacter(c);
            }

            // dispatch to application
            if (!io.getWantTextInput()) {
                charCallbacks.forEach(callback -> callback.invoke(w, c));
            }
        });

//        glfwSetMouseButtonCallback(glfwWindow, (w, button, action, mods) -> {
//            final boolean[] mouseDown = new boolean[5];
//
//            mouseDown[0] = button == GLFW_MOUSE_BUTTON_1 && action != GLFW_RELEASE;
//            mouseDown[1] = button == GLFW_MOUSE_BUTTON_2 && action != GLFW_RELEASE;
//            mouseDown[2] = button == GLFW_MOUSE_BUTTON_3 && action != GLFW_RELEASE;
//            mouseDown[3] = button == GLFW_MOUSE_BUTTON_4 && action != GLFW_RELEASE;
//            mouseDown[4] = button == GLFW_MOUSE_BUTTON_5 && action != GLFW_RELEASE;
//
//            io.setMouseDown(mouseDown);
//
//            if (!io.getWantCaptureMouse() && mouseDown[1]) {
//                ImGui.setWindowFocus(null);
//            }
//
//            // dispatch to application
//            if (!io.getWantCaptureMouse()) {
//                mouseButtonCallbacks.forEach(callback -> callback.invoke(w, button, action, mods));
//            }
//        });

        glfwSetScrollCallback(glfwWindow, (w, xOffset, yOffset) -> {
            io.setMouseWheelH(io.getMouseWheelH() + (float) xOffset);
            io.setMouseWheel(io.getMouseWheel() + (float) yOffset);

            // dispatch to application
            if (!io.getWantCaptureMouse()) {
                scrollCallbacks.forEach(callback -> callback.invoke(w, xOffset, yOffset));
            }
        });

        glfwSetCursorPosCallback(glfwWindow, (window, xpos, ypos) -> {
            // dispatch to application
            if (!io.getWantCaptureMouse()) {
                cursorPosCallbacks.forEach(callback -> callback.invoke(window, xpos, ypos));
            }
        });

        io.setSetClipboardTextFn(new ImStrConsumer() {
            @Override
            public void accept(final String s) {
                glfwSetClipboardString(glfwWindow, s);
            }
        });

        io.setGetClipboardTextFn(new ImStrSupplier() {
            @Override
            public String get() {
                final String clipboardString = glfwGetClipboardString(glfwWindow);
                if (clipboardString != null) {
                    return clipboardString;
                } else {
                    return "";
                }
            }
        });

        // ------------------------------------------------------------
        // Fonts configuration
        // Read: https://raw.githubusercontent.com/ocornut/imgui/master/docs/FONTS.txt

//        final ImFontAtlas fontAtlas = io.getFonts();
//        final ImFontConfig fontConfig = new ImFontConfig(); // Natively allocated object, should be explicitly destroyed
//
//        // Glyphs could be added per-font as well as per config used globally like here
//        fontConfig.setGlyphRanges(fontAtlas.getGlyphRangesCyrillic());
//
//        // Add a default font, which is 'ProggyClean.ttf, 13px'
//        fontAtlas.addFontDefault();
//
//        // Fonts merge example
//        fontConfig.setMergeMode(true); // When enabled, all fonts added with this config would be merged with the previously added font
//        fontConfig.setPixelSnapH(true);
//
//        fontAtlas.addFontFromMemoryTTF(loadFromResources("basis33.ttf"), 16, fontConfig);
//
//        fontConfig.setMergeMode(false);
//        fontConfig.setPixelSnapH(false);
//
//        // Fonts from file/memory example
//        // We can add new fonts from the file system
//        fontAtlas.addFontFromFileTTF("src/test/resources/Righteous-Regular.ttf", 14, fontConfig);
//        fontAtlas.addFontFromFileTTF("src/test/resources/Righteous-Regular.ttf", 16, fontConfig);
//
//        // Or directly from the memory
//        fontConfig.setName("Roboto-Regular.ttf, 14px"); // This name will be displayed in Style Editor
//        fontAtlas.addFontFromMemoryTTF(loadFromResources("Roboto-Regular.ttf"), 14, fontConfig);
//        fontConfig.setName("Roboto-Regular.ttf, 16px"); // We can apply a new config value every time we add a new font
//        fontAtlas.addFontFromMemoryTTF(loadFromResources("Roboto-Regular.ttf"), 16, fontConfig);
//
//        fontConfig.destroy(); // After all fonts were added we don't need this config more
//
//        // ------------------------------------------------------------
//        // Use freetype instead of stb_truetype to build a fonts texture
//        ImGuiFreeType.buildFontAtlas(fontAtlas, ImGuiFreeType.RasterizerFlags.LightHinting);

        // set style
        ImGuiStyle style = ImGui.getStyle();
        style.setWindowRounding(0);
        style.setScrollbarRounding(0);
        style.setFrameBorderSize(1);
        ImGui.styleColorsClassic();
    }

    /**
     * scale everything to a readable size
     * @param scaleFactor 1.0 for original size
     */
    public void scaleGui(float scaleFactor) {

        ImGuiStyle style = ImGui.getStyle();
        style.scaleAllSizes(scaleFactor);  // scale buttons and other gui elements

        // scale font size
        ImFontAtlas fontAtlas = io.getFonts();
        ImFontConfig fontConfig = new ImFontConfig();
        fontConfig.setSizePixels(13 * scaleFactor); // default font size is 13px
        fontAtlas.addFontDefault(fontConfig);
        fontConfig.destroy();
    }

    public void processEvents() {
        processMouseButtonEvents();
    }

    private void processMouseButtonEvents() {

        // copy mouseDown to pmouseDown (save previous state)
        System.arraycopy(mouseDown, 0, pmouseDown, 0, mouseDown.length);

        int[] mouseButtons = new int[]{
                GLFW_MOUSE_BUTTON_1,
                GLFW_MOUSE_BUTTON_2,
                GLFW_MOUSE_BUTTON_3,
                GLFW_MOUSE_BUTTON_4,
                GLFW_MOUSE_BUTTON_5,
        };

        for (int i = 0; i < 5; i++) {
            mouseDown[i] = glfwGetMouseButton(glfwWindow, mouseButtons[i]) != GLFW_RELEASE;
        }

        io.setMouseDown(mouseDown);

        if (!io.getWantCaptureMouse() && mouseDown[1]) {
            ImGui.setWindowFocus(null);
        }

        // dispatch to application
        if (!io.getWantCaptureMouse()) {
            for (int i = 0; i < 5; i++) {
                if (mouseDown[i] != pmouseDown[i]) {
                    for (GLFWMouseButtonCallbackI callback : mouseButtonCallbacks) {
                        callback.invoke(glfwWindow, mouseButtons[i], mouseDown[i] ? GLFW_PRESS : GLFW_RELEASE, 0);
                    }
                }
            }
        }
    }

    public void setIO(final float dt, int width, int height) {

        float[] winWidth = new float[]{width};
        float[] winHeight = new float[]{height};
        double[] mousePosX = new double[]{0};
        double[] mousePosY = new double[]{0};
        glfwGetCursorPos(glfwWindow, mousePosX, mousePosY);

        // We SHOULD call those methods to update Dear ImGui state for the current frame
        final ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(winWidth[0], winHeight[0]);
        io.setDisplayFramebufferScale(1f, 1f);
        io.setMousePos((float) mousePosX[0], (float) mousePosY[0]);
        io.setDeltaTime(dt);

        // Update the mouse cursor
        final int imguiCursor = ImGui.getMouseCursor();
        glfwSetCursor(glfwWindow, mouseCursors[imguiCursor]);
        glfwSetInputMode(glfwWindow, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
    }

    // If you want to clean a room after yourself - do it by yourself
    public void destroyImGui() {
        ImGui.destroyContext();
    }
}
