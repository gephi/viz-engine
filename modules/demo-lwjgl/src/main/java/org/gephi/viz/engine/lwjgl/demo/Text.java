package org.gephi.viz.engine.lwjgl.demo;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_CLIENT_API;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_DEBUG_CONTEXT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_ES_API;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_SAMPLES;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowContentScale;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowContentScaleCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glViewport;
import static org.lwjgl.system.MemoryUtil.NULL;

import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.util.text.LWJGLTextRenderer;
import org.gephi.viz.engine.util.TimeUtils;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;

public class Text {

    private static final boolean DISABLE_INDIRECT_RENDERING = false;
    private static final boolean DISABLE_INSTANCED_RENDERING = false;
    private static final boolean DISABLE_VAOS = false;

    private static final boolean DEBUG = true;
    private static final boolean USE_OPENGL_ES = false;

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 760;

    // The window handle
    private long windowHandle;

    public void run() throws InterruptedException {
        init();

        // Make the window visible
        glfwShowWindow(windowHandle);

        start();
//        glfwEventsListener.updateScale();
//
//        setupTestEventListeners(engine);
//
//        renderingTarget.setWindowTitleFormat("VizEngine demo (LWJGL GLFW) FPS: $FPS");
//        engine.start();//This starts the loop for GLFW in LWJGLRenderingTargetGLFW, which MUST be in main thread
//
//        stopTestEventListeners();
//
//        glfwEventsListener.destroy();
        destroy();
    }

    private LWJGLTextRenderer textRenderer;

    private GLCapabilities capabilities;

    private void start() {
        glfwSetWindowContentScaleCallback(windowHandle, (window, xScale, yScale) -> {
            reshape();
        });
        glfwSetWindowSizeCallback(windowHandle, (window, width, height) -> {
            reshape();
        });

        //Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        //Disable v-sync
        glfwSwapInterval(0);

        glfwSetWindowCloseCallback(windowHandle, window -> {
            destroy();
        });

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        capabilities = GL.createCapabilities();

        System.err.println("GL_VENDOR: " + glGetString(GL_VENDOR));
        System.err.println("GL_RENDERER: " + glGetString(GL_RENDERER));
        System.err.println("GL_VERSION: " + glGetString(GL_VERSION));

        glDisable(GL11.GL_DEPTH_TEST);//Z-order is set by the order of drawing

        //Disable blending for better performance
        glDisable(GL11.GL_BLEND);

        running = true;

        glViewport(0, 0, 100, 100);

        Font font = new Font ("Arial", Font.BOLD , 3);
        textRenderer = new LWJGLTextRenderer(font);

        while (running) {
            loop();
        }
    }

    private void reshape() {
        final int[] widthArr = new int[1];
        final int[] heightArr = new int[1];
        final float[] xScaleArr = new float[1];
        final float[] yScaleArr = new float[1];

        glfwGetWindowSize(windowHandle, widthArr, heightArr);
        glfwGetWindowContentScale(windowHandle, xScaleArr, yScaleArr);

        final int width = widthArr[0];
        final int height = heightArr[0];
        final float xScale = xScaleArr[0];
        final float yScale = yScaleArr[0];

//        engine.reshape((int) (width * xScale), (int) (height * yScale));
    }

    private final float[] backgroundColor = new float[] {0.4f, 0.4f, 0.4f, 1.0f};

    public void loop() {
        if (!running) {
            return;
        }

        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        if (glfwWindowShouldClose(windowHandle)) {
            return;
        }

        glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

        textRenderer.beginRendering(capabilities, 100, 100);
        textRenderer.setColor(Color.WHITE);
        textRenderer.draw("foo", 10, 10);
        textRenderer.endRendering();

        glfwSwapBuffers(windowHandle); // swap the color buffers

        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents();
    }

    boolean running;

    public boolean isRunning() {
        return running;
    }

    private void init() {
        // Setup an error callback. The default implementation
        // will print the error message in System.err.
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW. Most GLFW functions will not work before doing this.
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW
        glfwDefaultWindowHints(); // optional, the current window hints are already the default

        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_SAMPLES, 4);//4 samples anti-aliasing

        if (DEBUG) {
            glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);
        }

        if (USE_OPENGL_ES) {
            glfwWindowHint(GLFW_CLIENT_API, GLFW_OPENGL_ES_API);
        } else {
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        }

        // Create the window
        windowHandle = glfwCreateWindow(WIDTH, HEIGHT, "VizEngine demo (LWJGL GLFW)", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        final int[] widthArr = new int[1];
        final int[] heightArr = new int[1];
        glfwGetWindowSize(windowHandle, widthArr, heightArr);

        // Get the resolution of the primary monitor
        GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

        // Center the window
        glfwSetWindowPos(
            windowHandle,
            (vidmode.width() - widthArr[0]) / 2,
            (vidmode.height() - heightArr[0]) / 2
        );
    }

    private void destroy() {
        // Free the window callbacks and destroy the window
        glfwFreeCallbacks(windowHandle);
        glfwDestroyWindow(windowHandle);

        // Terminate GLFW and free the error callback
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }


    public static void main(String[] args) throws InterruptedException {
        final Text main = new Text();

        System.out.println(Arrays.toString(args));

        main.run();
    }
}
