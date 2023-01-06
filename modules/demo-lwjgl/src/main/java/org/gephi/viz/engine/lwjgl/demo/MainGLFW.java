package org.gephi.viz.engine.lwjgl.demo;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTargetGLFW;
import org.gephi.viz.engine.lwjgl.VizEngineLWJGLConfigurator;
import org.gephi.viz.engine.lwjgl.pipeline.events.GLFWEventsListener;
import org.gephi.viz.engine.lwjgl.pipeline.events.KeyEvent;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.spi.WorldUpdaterExecutionMode;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class MainGLFW {

    private static final boolean DISABLE_INDIRECT_RENDERING = false;
    private static final boolean DISABLE_INSTANCED_RENDERING = false;
    private static final boolean DISABLE_VAOS = false;

    private static final boolean DEBUG = false;
    private static final boolean USE_OPENGL_ES = false;

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 760;

    private static final WorldUpdaterExecutionMode UPDATE_DATA_MODE = WorldUpdaterExecutionMode.CONCURRENT_ASYNCHRONOUS;

    // The window handle
    private long windowHandle;

    public void run(final String graphFilePath) throws InterruptedException {
        init();

        final LWJGLRenderingTargetGLFW renderingTarget = new LWJGLRenderingTargetGLFW(windowHandle);

        final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine = VizEngineFactory.<LWJGLRenderingTarget, LWJGLInputEvent>newEngine(
                renderingTarget,
                GraphLoader.load(graphFilePath),
                Collections.singletonList(
                        new VizEngineLWJGLConfigurator()
                )
        );
        engine.setWorldUpdatersExecutionMode(UPDATE_DATA_MODE);

        final OpenGLOptions glOptions = engine.getLookup().lookup(OpenGLOptions.class);
        glOptions.setDisableIndirectDrawing(DISABLE_INDIRECT_RENDERING);
        glOptions.setDisableInstancedDrawing(DISABLE_INSTANCED_RENDERING);
        glOptions.setDisableVAOS(DISABLE_VAOS);
        glOptions.setDebug(DEBUG);

        final GLFWEventsListener glfwEventsListener = new GLFWEventsListener(windowHandle, engine);
        glfwEventsListener.register();

        reshape(engine);

        // Make the window visible
        glfwShowWindow(windowHandle);
        glfwEventsListener.updateScale();

        setupTestEventListeners(engine);

        renderingTarget.setWindowTitleFormat("VizEngine demo (LWJGL GLFW) FPS: $FPS");
        engine.start();//This starts the loop for GLFW in LWJGLRenderingTargetGLFW, which MUST be in main thread

        stopTestEventListeners();

        glfwEventsListener.destroy();
        destroy();
    }

    private void reshape(final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
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

        engine.reshape((int) (width * xScale), (int) (height * yScale));
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

    private final ExecutorService LAYOUT_THREAD_POOL = Executors.newSingleThreadExecutor();

    private void setupTestEventListeners(final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {

        engine.addInputListener(new InputListener<LWJGLRenderingTarget, LWJGLInputEvent>() {
            private volatile boolean layoutEnabled = false;

            @Override
            public boolean processEvent(LWJGLInputEvent event) {
                if (event instanceof KeyEvent) {
                    final KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getKeyCode() == GLFW_KEY_SPACE && keyEvent.getAction() == KeyEvent.Action.RELEASE) {
                        toggleLayout(engine);
                    }

                    if (keyEvent.getKeyCode() == GLFW_KEY_ESCAPE && keyEvent.getAction() == KeyEvent.Action.RELEASE) {
                        engine.destroy();
                    }
                }

                return false;
            }

            @Override
            public String getCategory() {
                return "DEMO";
            }

            @Override
            public int getPreferenceInCategory() {
                return 0;
            }

            @Override
            public String getName() {
                return "Demo event listener";
            }

            @Override
            public void init(LWJGLRenderingTarget target) {
                //NOOP
            }

            @Override
            public int getOrder() {
                return 0;
            }

            private void toggleLayout(VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
                if (layoutEnabled) {
                    System.out.println("Stopping layout");
                    layoutEnabled = false;
                } else {
                    System.out.println("Starting layout");
                    LAYOUT_THREAD_POOL.submit(() -> {
                        layoutEnabled = true;
                        final GraphModel graphModel = engine.getGraphModel();

                        final ForceAtlas2Builder forceAtlas2Builder = new ForceAtlas2Builder();
                        final ForceAtlas2 forceAtlas2 = forceAtlas2Builder.buildLayout();

                        forceAtlas2.setGraphModel(graphModel);
                        forceAtlas2.setBarnesHutOptimize(true);
                        forceAtlas2.setScalingRatio(1000.0);
                        forceAtlas2.setAdjustSizes(true);
                        forceAtlas2.initAlgo();
                        while (layoutEnabled && forceAtlas2.canAlgo()) {
                            forceAtlas2.goAlgo();
                        }
                        forceAtlas2.endAlgo();
                    });
                }
            }
        });
    }

    private void stopTestEventListeners() {
        LAYOUT_THREAD_POOL.shutdown();
    }

    public static void main(String[] args) throws InterruptedException {
        final MainGLFW main = new MainGLFW();

        System.out.println(Arrays.toString(args));

        final String graphFile = "samples/test.gexf";
        //final String graphFile = "samples/Java.gexf";
        //final String graphFile = "samples/mixed-sample.gexf";
        //final String graphFile = "samples/Les Miserables.gexf";
        // final String graphFile = "samples/comic-hero-network.gexf";
        //final String graphFile = "samples/Power Grid.gml";
        //final String graphFile = "samples/twitter_combined.csv";

        main.run(
                args.length > 0 ? args[0] : graphFile
        );
    }
}
