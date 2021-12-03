package org.gephi.viz.engine.lwjgl;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.TimeUtils;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;

import java.util.function.Consumer;

import static org.lwjgl.opengl.GL11.*;

/**
 * @author Eduardo Ramos
 */
public class LWJGLRenderingTargetAWT implements LWJGLRenderingTarget {

    private final Consumer<Integer> fpsCallback;
    private VizEngine engine;

    public LWJGLRenderingTargetAWT(Consumer<Integer> fpsCallback) {
        this.fpsCallback = fpsCallback;
    }

    public LWJGLRenderingTargetAWT() {
        this.fpsCallback = null;
    }

    @Override
    public void setup(VizEngine engine) {
        this.engine = engine;
    }

    private volatile boolean running = false;

    @Override
    public void start() {
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    public void reshape(final int width, final int height) {
        engine.reshape(width, height);
    }

    public void initializeContext() {
        final GLCapabilities capabilities = GL.createCapabilities();
        engine.addToLookup(capabilities);

        if (engine.getLookup().lookup(OpenGLOptions.class).isDebug()) {
            GLUtil.setupDebugMessageCallback();
        }

        //TODO: Use logger instead
        System.err.println("GL_VENDOR: " + glGetString(GL_VENDOR));
        System.err.println("GL_RENDERER: " + glGetString(GL_RENDERER));
        System.err.println("GL_VERSION: " + glGetString(GL_VERSION));

        glDisable(GL11.GL_DEPTH_TEST);//Z-order is set by the order of drawing

        //Disable blending for better performance
        glDisable(GL11.GL_BLEND);

        System.out.println("OpenGL options: " + engine.getLookup().lookup(OpenGLOptions.class));
        engine.initPipeline();

        lastFpsTime = TimeUtils.getTimeMillis();
    }

    private final float[] backgroundColor = new float[4];

    public void display() {
        if (!running) {
            return;
        }

        glViewport(0, 0, engine.getWidth(), engine.getHeight());

        // Set the clear color
        engine.getBackgroundColor(backgroundColor);

        glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        glClear(GL_COLOR_BUFFER_BIT); // clear the framebuffer

        updateFPS();
        engine.display();
    }

    public VizEngine getEngine() {
        return engine;
    }

    public boolean isRunning() {
        return running;
    }

    private int fps = 0;
    private long lastFpsTime = 0;

    private void updateFPS() {
        if (fpsCallback == null) {
            return;
        }

        if (TimeUtils.getTimeMillis() - lastFpsTime > 1000) {
            fpsCallback.accept(fps);

            fps = 0;
            lastFpsTime += 1000;
        }
        fps++;
    }
}
