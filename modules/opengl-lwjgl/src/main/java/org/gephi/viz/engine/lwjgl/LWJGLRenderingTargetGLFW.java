package org.gephi.viz.engine.lwjgl;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.util.TimeUtils;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 *
 * @author Eduardo Ramos
 */
public class LWJGLRenderingTargetGLFW implements LWJGLRenderingTarget {

    private final long windowHandle;
    private VizEngine engine;

    private String windowTitleFormat = null;

    public LWJGLRenderingTargetGLFW(long windowHandle) {
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

    private volatile boolean running = false;

    @Override
    public void start() {
        initializeContext();
        running = true;

        lastFpsTime = TimeUtils.getTimeMillis();
        while (running) {
            loop();
        }
    }

    @Override
    public void stop() {
        running = false;
    }

    private void initializeContext() {
        //Make the OpenGL context current
        glfwMakeContextCurrent(windowHandle);

        //Disable v-sync
        glfwSwapInterval(0);

        glfwSetWindowCloseCallback(windowHandle, window -> {
            engine.stop();
        });

        // This line is critical for LWJGL's interoperation with GLFW's
        // OpenGL context, or any context that is managed externally.
        // LWJGL detects the context that is current in the current thread,
        // creates the GLCapabilities instance and makes the OpenGL
        // bindings available for use.
        final GLCapabilities capabilities = GL.createCapabilities();
        engine.addToLookup(capabilities);

        if (engine.getLookup().lookup(OpenGLOptions.class).isDebug()) {
            GLUtil.setupDebugMessageCallback();
        }

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
        if (!running) {
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

        updateFPS();

        // fGlobalTime for all program of the engine, using glfwGetTime retrive time since started;
        engine.setGlobalTime((float) glfwGetTime());

        // selectedTime for all program of the engine.
        GraphSelection selection = engine.getLookup().lookup(GraphSelection.class);

        // A selection is currently active
        if(selection.getSelectedNodesCount() > 0){
            engine.setSelectedTime();
        } else {
            engine.unsetSelectedTime();
        }
        engine.display();

        glfwSwapBuffers(windowHandle); // swap the color buffers

        // Poll for window events. The key callback above will only be
        // invoked during this call.
        glfwPollEvents();
    }

    public VizEngine getEngine() {
        return engine;
    }

    public boolean isRunning() {
        return running;
    }

    public String getWindowTitleFormat() {
        return windowTitleFormat;
    }

    public void setWindowTitleFormat(String windowTitleFormat) {
        this.windowTitleFormat = windowTitleFormat;
    }

    private int fps = 0;
    private long lastFpsTime = 0;

    private void updateFPS() {
        if (TimeUtils.getTimeMillis() - lastFpsTime > 1000) {
            if (windowTitleFormat != null && windowTitleFormat.contains("$FPS")) {
                GLFW.glfwSetWindowTitle(windowHandle, windowTitleFormat.replace("$FPS", String.valueOf(fps)));
            } else {
                System.out.println("FPS: " + fps);
            }
            fps = 0;
            lastFpsTime += 1000;
        }
        fps++;
    }
}
