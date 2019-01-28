package org.gephi.viz.engine.lwjgl;

import java.awt.Frame;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.TimeUtils;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
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
import org.lwjgl.opengl.GLUtil;

/**
 *
 * @author Eduardo Ramos
 */
public class LWJGLRenderingTargetAWT implements LWJGLRenderingTarget {

    private final Frame frame;
    private VizEngine engine;

    private String windowTitleFormat = null;

    public LWJGLRenderingTargetAWT(Frame frame) {
        this.frame = frame;
    }

    @Override
    public void setup(VizEngine engine) {
        this.engine = engine;
    }

    private volatile boolean running = false;

    @Override
    public void start() {
        //NOOP
    }

    @Override
    public void stop() {
        running = false;
    }

    public void reshape(final int width, final int height) {
        System.out.println("Reshape: " + width + "x" + height);
        engine.reshape(width, height);
    }

    public void initializeContext() {
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
            if (frame != null && windowTitleFormat != null && windowTitleFormat.contains("$FPS")) {
                this.frame.setTitle(windowTitleFormat.replace("$FPS", String.valueOf(fps)));
            } else {
                System.out.println("FPS: " + fps);
            }
            fps = 0;
            lastFpsTime += 1000;
        }
        fps++;
    }
}
