package org.gephi.viz.engine.lwjgl.demo;

import org.lwjgl.glfw.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Collections;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.VizEngineLWJGLConfigurator;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 760;

    // The window handle
    private long windowHandle;

    public void run() throws InterruptedException {
        Configuration.DEBUG.set(true);
        init();

        final LWJGLRenderingTarget renderingTarget = new LWJGLRenderingTarget(windowHandle);

        final String graphFile = "samples/comic-hero-network.gexf";
        final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine = VizEngineFactory.<LWJGLRenderingTarget, LWJGLInputEvent>newEngine(
                renderingTarget,
                GraphLoader.load(graphFile),
                Collections.singletonList(
                        new VizEngineLWJGLConfigurator()
                )
        );

        glfwSetMouseButtonCallback(windowHandle, (window, button, action, modifiers) -> {
            engine.queueEvent(
                    new LWJGLInputEvent()
            );
        });
        engine.reshape(WIDTH, HEIGHT);

        engine.start();

        // Make the window visible
        glfwShowWindow(windowHandle);

        renderingTarget.join();

        destroy();
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

        glfwWindowHint(GLFW_OPENGL_DEBUG_CONTEXT, GLFW_TRUE);//DEBUG!!
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable
        glfwWindowHint(GLFW_SAMPLES, 4);

        // Create the window
        windowHandle = glfwCreateWindow(WIDTH, HEIGHT, "VizEngine demo (LWJGL GLFW)", NULL, NULL);
        if (windowHandle == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }

        // Setup a key callback. It will be called every time a key is pressed, repeated or released.
        glfwSetKeyCallback(windowHandle, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
            }
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = stackPush()) {
            final IntBuffer pWidth = stack.mallocInt(1); // int*
            final IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            glfwGetWindowSize(windowHandle, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    windowHandle,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2
            );
        } // the stack frame is popped automatically
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
        new Main().run();
    }

}
