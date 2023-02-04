package com.particle_life.app;

import com.particle_life.Clock;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public abstract class App {

    // The window handle
    protected long window;
    protected int width;
    protected int height;
    protected double mouseX = -1;
    protected double mouseY = -1;
    protected double pmouseX = mouseX;// previous mouse position
    protected double pmouseY = mouseY;

    // global scaling of GUI -> apply this to window sizes etc.
    protected float scale = 1.0f;

    // remember window position and size before switching to fullscreen
    private int windowPosX;
    private int windowPosY;
    private int windowWidth = -1;
    private int windowHeight = -1;

    public void launch(String title, boolean fullscreen) {
        System.out.println("Using LWJGL " + Version.getVersion());

        init(title, fullscreen);

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        GL.createCapabilities();

        // Set the clear color
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        ImGuiLayer imGuiLayer = new ImGuiLayer(window);
        imGuiLayer.initImGui();
        setCallbacks(imGuiLayer);

        scale = (float) height / 1080;
        imGuiLayer.scaleGui(scale);

        setup();

        Clock guiClock = new Clock(1);

        while (!glfwWindowShouldClose(window)) {

            guiClock.tick();

            pmouseX = mouseX;
            pmouseY = mouseY;

            glfwPollEvents();
            imGuiLayer.processEvents();

            double dt = guiClock.getDtMillis() / 1000.0;
            imGuiLayer.setIO((float) dt, width, height);
            draw(dt);

            glfwSwapBuffers(window); // swap the color buffers
        }

        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();

        beforeClose();

        imGuiLayer.destroyImGui();
    }

    private void init(String title, boolean fullscreen) {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_SAMPLES, 16);

        // request OpenGL version 4.1 (corresponds to "#version 410" in shaders)
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 1);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create the window
        long monitor = glfwGetPrimaryMonitor();
        GLFWVidMode videoMode = glfwGetVideoMode(monitor);
        if (videoMode == null) throw new RuntimeException("glfwGetVideoMode() returned null");
        int monitorWidth = videoMode.width();
        int monitorHeight = videoMode.height();

        // set reasonable defaults for window position and size
        double f = 0.2;
        windowPosX = (int) (f * monitorWidth / 2);
        windowPosY = (int) (f * monitorHeight / 2);
        windowWidth = (int) ((1 - f) * monitorWidth);
        windowHeight = (int) ((1 - f) * monitorHeight);

        if (fullscreen) {
            width = monitorWidth;
            height = monitorHeight;
            window = glfwCreateWindow(width, height, title, monitor, NULL);
        } else {
            width = windowWidth;
            height = windowHeight;
            window = glfwCreateWindow(width, height, title, NULL, NULL);
        }

        if (window == NULL) throw new RuntimeException("Failed to create the GLFW window");

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {

            // Get the resolution of the primary monitor

            // Center the window
            glfwSetWindowPos(window, windowPosX, windowPosY);
        } // the stack frame is popped automatically

        // Make the OpenGL context current
        glfwMakeContextCurrent(window);

        glfwSwapInterval(1);  // Enable v-sync

        // Make the window visible
        glfwShowWindow(window);
    }

    private void setCallbacks(ImGuiLayer imGuiLayer) {
        glfwSetWindowSizeCallback(window, (window1, newWidth, newHeight) -> {
            //todo: use fame buffer size or window size?
//            System.out.printf("window size changed: %d %d%n", newWidth, newHeight);

//            int[] frameBufferWidth = new int[1];
//            int[] frameBufferHeight = new int[1];
//            glfwGetFramebufferSize(window, frameBufferWidth, frameBufferHeight);
//            System.out.printf("frame buffer size: %d, %d%n", frameBufferWidth[0], frameBufferHeight[0]);

            width = newWidth;
            height = newHeight;
        });

        imGuiLayer.keyCallbacks.add((window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                this.setFullscreen(!isFullscreen());
            } else {
                String keyName = glfwGetKeyName(key, scancode);
                if (keyName == null) {
                    // try to recover special keys
                    keyName = switch (key) {
                        case GLFW_KEY_SPACE -> " ";
                        case GLFW_KEY_LEFT -> "LEFT";
                        case GLFW_KEY_RIGHT -> "RIGHT";
                        case GLFW_KEY_UP -> "UP";
                        case GLFW_KEY_DOWN -> "DOWN";
                        case GLFW_KEY_LEFT_SHIFT -> "LEFT_SHIFT";
                        case GLFW_KEY_RIGHT_SHIFT -> "RIGHT_SHIFT";
                        case GLFW_KEY_LEFT_CONTROL -> "LEFT_CONTROL";
                        case GLFW_KEY_RIGHT_CONTROL -> "RIGHT_CONTROL";
                        default -> null;
                    };
                }
                if (keyName != null) {
                    if (mods == GLFW_MOD_SHIFT) {
                        keyName = keyName.toUpperCase();
                    }
                    switch (action) {
                        case GLFW_PRESS -> this.onKeyPressed(keyName);
                        case GLFW_REPEAT -> this.onKeyRepeated(keyName);
                        case GLFW_RELEASE -> this.onKeyReleased(keyName);
                    }
                }
            }
        });
        imGuiLayer.cursorPosCallbacks.add((window1, xpos, ypos) -> {
            mouseX = xpos;
            mouseY = ypos;
        });
        imGuiLayer.mouseButtonCallbacks.add((window1, button, action, mods) -> {
            switch (action) {
                case GLFW_PRESS -> this.onMousePressed(button);
                case GLFW_RELEASE -> this.onMouseReleased(button);
            }
        });
        imGuiLayer.scrollCallbacks.add((window1, xoffset, yoffset) -> {
            this.onScroll(yoffset);
        });
    }

    protected boolean isFullscreen() {
        return glfwGetWindowMonitor(window) != NULL;
    }

    protected void setFullscreen(boolean fullscreen) {
        //todo: this could create problems with multi threading

        if (isFullscreen() == fullscreen) return;

        if (fullscreen) {
            // make fullscreen

            // backup window position and size
            int[] xposBuf = new int[1];
            int[] yposBuf = new int[1];
            int[] widthBuf = new int[1];
            int[] heightBuf = new int[1];
            glfwGetWindowPos(window, xposBuf, yposBuf);
            glfwGetWindowSize(window, widthBuf, heightBuf);
            windowPosX = xposBuf[0];
            windowPosY = yposBuf[0];
            windowWidth = widthBuf[0];
            windowHeight = heightBuf[0];

            // get resolution of monitor
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode videoMode = glfwGetVideoMode(monitor);

            // switch to fullscreen
            width = videoMode.width();
            height = videoMode.height();
            glfwSetWindowMonitor(window, monitor, 0, 0, width, height, GLFW_DONT_CARE);

            glfwSwapInterval(1);  // Enable v-sync

        } else {
            // restore last window size and position
            width = windowWidth;
            height = windowHeight;
            glfwSetWindowMonitor(window, NULL, windowPosX, windowPosY, width, height, GLFW_DONT_CARE);

            glfwSwapInterval(1);  // Enable v-sync
        }
    }

    protected void setup() {
    }

    protected final void close() {
        glfwSetWindowShouldClose(window, true);
    }

    /**
     * Will be called using v-sync.
     *
     * @param dt elapsed time since last call in seconds
     */
    protected void draw(double dt) {
    }

    protected void onKeyPressed(String keyName) {
    }

    protected void onKeyRepeated(String keyName) {
    }

    protected void onKeyReleased(String keyName) {
    }

    /**
     * left: 0, right: 1, middle: 2.
     *
     * @param button
     */
    protected void onMousePressed(int button) {
    }

    protected void onMouseReleased(int button) {
    }

    protected void onScroll(double y) {
    }

    protected void beforeClose() {
    }
}