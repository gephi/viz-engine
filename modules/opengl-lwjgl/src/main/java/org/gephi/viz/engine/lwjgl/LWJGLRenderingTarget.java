package org.gephi.viz.engine.lwjgl;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.spi.RenderingTarget;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetWindowCloseCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_RENDERER;
import static org.lwjgl.opengl.GL11.GL_VENDOR;
import static org.lwjgl.opengl.GL11.GL_VERSION;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glGetString;
import static org.lwjgl.opengl.GL11.glViewport;
import org.lwjgl.opengl.GLCapabilities;

/**
 *
 * @author Eduardo Ramos
 */
public class LWJGLRenderingTarget implements RenderingTarget {

    private final long windowHandle;
    private VizEngine engine;

    public LWJGLRenderingTarget(long windowHandle) {
        this.windowHandle = windowHandle;
    }

    private boolean sizeChanged = false;

    @Override
    public void setup(VizEngine engine) {
        this.engine = engine;

        glfwSetWindowSizeCallback(windowHandle, (window, width, height) -> {
            engine.reshape(width, height);
            sizeChanged = true;
        });
    }

    private volatile boolean isRunning = false;

    @Override
    public void start() {
        initializeContext();
        isRunning = true;

        while (isRunning) {
            loop();
        }
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    private void initializeContext() {
        //Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        //Disable v-sync
        glfwSwapInterval(0);

        glfwSetWindowCloseCallback(windowHandle, window -> {
            stop();
        });

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        final GLCapabilities capabilities = GL.createCapabilities();
        engine.addToLookup(capabilities);

        System.err.println("GL_VENDOR: " + glGetString(GL_VENDOR));
        System.err.println("GL_RENDERER: " + glGetString(GL_RENDERER));
        System.err.println("GL_VERSION: " + glGetString(GL_VERSION));

        glDisable(GL11.GL_DEPTH_TEST);//Z-order is set by the order of drawing

        //Disable blending for better performance
        glDisable(GL11.GL_BLEND);

        System.out.println("OpenGL options: " + engine.getLookup().lookup(OpenGLOptions.class));
        engine.initPipeline();
    }

    private final float[] backgroundColor = new float[4];

    public void loop() {
        if (!isRunning) {
            return;
        }
        
        // Run the rendering loop until the user has attempted to close
        // the window or has pressed the ESCAPE key.
        if (glfwWindowShouldClose(windowHandle)) {
            stop();
            return;
        }

        if (sizeChanged) {
            glViewport(0, 0, engine.getWidth(), engine.getHeight());
            sizeChanged = false;
        }

        // Set the clear color
        engine.getBackgroundColor(backgroundColor);

        glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

        engine.display();

        glfwSwapBuffers(windowHandle); // swap the color buffers

        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents();
    }
}
