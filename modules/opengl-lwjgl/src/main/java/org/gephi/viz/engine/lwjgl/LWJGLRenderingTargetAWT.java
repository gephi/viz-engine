package org.gephi.viz.engine.lwjgl;

import java.util.function.Consumer;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.TimeUtils;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.opengl.GLUtil;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.system.Platform;

/**
 * @author Eduardo Ramos
 */
public class LWJGLRenderingTargetAWT implements LWJGLRenderingTarget {

    private final Consumer<Integer> fpsCallback;
    private final boolean isWindows;
    private VizEngine engine;

    public LWJGLRenderingTargetAWT(Consumer<Integer> fpsCallback) {
        this.fpsCallback = fpsCallback;
        this.isWindows = Platform.get() == Platform.WINDOWS;
    }

    public LWJGLRenderingTargetAWT() {
        this(null);
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

    public void reshape(final AWTGLCanvas canvas) {
        if (isWindows) {
            reshape(canvas.getFramebufferWidth(), canvas.getFramebufferHeight());
        } else {
            reshape(canvas.getWidth(), canvas.getHeight());
        }
    }

    public void initializeContext() {
        final GLCapabilities capabilities = GL.createCapabilities();
        engine.addToLookup(capabilities);

        if (engine.getLookup().lookup(OpenGLOptions.class).isDebug()) {
            GLUtil.setupDebugMessageCallback();
        }

        //TODO: Use logger instead
        System.err.println("GL_VENDOR: " + GL11.glGetString(GL11.GL_VENDOR));
        System.err.println("GL_RENDERER: " + GL11.glGetString(GL11.GL_RENDERER));
        System.err.println("GL_VERSION: " + GL11.glGetString(GL11.GL_VERSION));

        GL11.glDisable(GL11.GL_DEPTH_TEST);//Z-order is set by the order of drawing

        //Disable blending for better performance
        GL11.glDisable(GL11.GL_BLEND);

        System.out.println("OpenGL options: " + engine.getLookup().lookup(OpenGLOptions.class));
        engine.initPipeline();

        lastFpsTime = TimeUtils.getTimeMillis();
    }

    private final float[] backgroundColor = new float[4];

    public void display() {
        if (!running) {
            return;
        }

        GL11.glViewport(0, 0, engine.getWidth(), engine.getHeight());

        // Set the clear color
        engine.getBackgroundColor(backgroundColor);

        GL11.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT); // clear the framebuffer

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
