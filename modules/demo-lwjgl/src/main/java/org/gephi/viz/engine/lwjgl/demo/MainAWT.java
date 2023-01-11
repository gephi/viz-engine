package org.gephi.viz.engine.lwjgl.demo;

import org.gephi.graph.api.GraphModel;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2;
import org.gephi.layout.plugin.forceAtlas2.ForceAtlas2Builder;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTargetAWT;
import org.gephi.viz.engine.lwjgl.VizEngineLWJGLConfigurator;
import org.gephi.viz.engine.lwjgl.pipeline.events.AWTEventsListener;
import org.gephi.viz.engine.lwjgl.pipeline.events.KeyEvent;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.spi.WorldUpdaterExecutionMode;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;
import org.lwjgl.system.Platform;

import javax.swing.*;

import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Demo with lwjgl-awt
 *
 * @author Eduardo Ramos
 */
public class MainAWT {

    private static final boolean DISABLE_INDIRECT_RENDERING = false;
    private static final boolean DISABLE_INSTANCED_RENDERING = false;
    private static final boolean DISABLE_VAOS = false;

    private static final boolean DEBUG = false;
    private static final boolean USE_OPENGL_ES = false;

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 760;

    private static final WorldUpdaterExecutionMode UPDATE_DATA_MODE = WorldUpdaterExecutionMode.CONCURRENT_ASYNCHRONOUS;

    private JFrame frame;
    private LWJGLRenderingTargetAWT renderingTarget;
    private VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine;
    private AWTEventsListener eventsListener;

    public void run(final String graphFilePath) {
        frame = new JFrame("VizEngine demo (LWJGL AWT)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));

        final GLData data = new GLData();

        if (USE_OPENGL_ES) {
            data.api = GLData.API.GLES;
        }

        if (Platform.get() == Platform.MACOSX) {
            //In mac we have to set the version or it won't give the latest automatically

            data.majorVersion = 3;
            data.minorVersion = 2;
            data.forwardCompatible = true;
        }

        data.samples = 4;//4 samples anti-aliasing
        data.swapInterval = 0;

        renderingTarget = new LWJGLRenderingTargetAWT(fps -> {
            frame.setTitle("VizEngine demo (LWJGL AWT) FPS: " + fps);
        });

        engine = VizEngineFactory.<LWJGLRenderingTarget, LWJGLInputEvent>newEngine(
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

        final AWTGLCanvas canvas = new AWTGLCanvas(data) {
            @Override
            public void initGL() {
                renderingTarget.initializeContext();
            }

            @Override
            public void paintGL() {
                renderingTarget.display();
                swapBuffers();
            }
        };

        eventsListener = new AWTEventsListener(canvas, engine);
        eventsListener.register();

        setupTestEventListeners(engine);

        frame.add(canvas, BorderLayout.CENTER);

        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.transferFocus();

        engine.start();

        final Runnable renderLoop = new Runnable() {
            @Override
            public void run() {
                if (!canvas.isValid() || !renderingTarget.isRunning()) {
                    stopAll();
                    return;
                }

                canvas.render();
                SwingUtilities.invokeLater(this);
            }
        };

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                stopAll();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                renderingTarget.reshape(canvas);
            }

            @Override
            public void windowOpened(WindowEvent e) {
                renderingTarget.reshape(canvas);
            }
        });
        canvas.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                renderingTarget.reshape(canvas);
            }
        });

        SwingUtilities.invokeLater(renderLoop);
    }

    private void stopAll() {
        engine.destroy();
        eventsListener.destroy();
        stopTestEventListeners();
        System.exit(0);
    }

    private static final ExecutorService LAYOUT_THREAD_POOL = Executors.newSingleThreadExecutor();

    private void setupTestEventListeners(final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {

        engine.addInputListener(new InputListener<LWJGLRenderingTarget, LWJGLInputEvent>() {
            private volatile boolean layoutEnabled = false;

            @Override
            public boolean processEvent(LWJGLInputEvent event) {
                if (event instanceof KeyEvent) {
                    final KeyEvent keyEvent = (KeyEvent) event;
                    if (keyEvent.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE && keyEvent.getAction() == KeyEvent.Action.RELEASE) {
                        toggleLayout(engine);
                    }

                    if (keyEvent.getKeyCode() == java.awt.event.KeyEvent.VK_CONTROL && keyEvent.getAction() == KeyEvent.Action.RELEASE) {
                        toggleSelectionMode(engine);
                    }

                    if (keyEvent.getKeyCode() == java.awt.event.KeyEvent.VK_ESCAPE && keyEvent.getAction() == KeyEvent.Action.RELEASE) {
                        frame.setVisible(false);
                        frame.dispose();
                        stopAll();
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

            private void toggleSelectionMode(VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
                final GraphSelection selection = engine.getLookup().lookup(GraphSelection.class);
                final GraphSelection.GraphSelectionMode mode = selection.getMode();

                if (mode != GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION) {
                    selection.setMode(GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION);
                } else {
                    selection.setMode(GraphSelection.GraphSelectionMode.SIMPLE_MOUSE_SELECTION);
                }
            }
        });
    }

    private void stopTestEventListeners() {
        LAYOUT_THREAD_POOL.shutdown();
    }

    public static void main(String[] args) {
        final MainAWT main = new MainAWT();

        System.out.println(Arrays.toString(args));

        final String graphFile = "samples/test.gexf";
        // final String graphFile = "samples/Java.gexf";
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
