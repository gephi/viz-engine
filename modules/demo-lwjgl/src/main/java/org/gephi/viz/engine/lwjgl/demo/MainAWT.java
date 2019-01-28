package org.gephi.viz.engine.lwjgl.demo;

import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Collections;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTargetAWT;
import org.gephi.viz.engine.lwjgl.VizEngineLWJGLConfigurator;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.awt.AWTGLCanvas;
import org.lwjgl.opengl.awt.GLData;

/**
 *
 * @author Eduardo Ramos
 */
public class MainAWT {

    private static final boolean DISABLE_INDIRECT_RENDERING = false;
    private static final boolean DISABLE_INSTANCED_RENDERING = false;
    private static final boolean DISABLE_VAOS = false;

    private static final boolean DEBUG = true;
    private static final boolean USE_OPENGL_ES = false;

    private static final int WIDTH = 1024;
    private static final int HEIGHT = 760;

    public void init() {
        final JFrame frame = new JFrame("VizEngine demo (LWJGL AWT)");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(WIDTH, HEIGHT);

        final GLData data = new GLData();

        if (USE_OPENGL_ES) {
            data.api = GLData.API.GLES;
        } else {
            data.majorVersion = 3;
            data.minorVersion = 2;
            data.forwardCompatible = true;
            data.profile = GLData.Profile.CORE;
        }

        data.samples = 4;//4 samples anti-aliasing
        data.swapInterval = 0;

        final LWJGLRenderingTargetAWT renderingTarget = new LWJGLRenderingTargetAWT(frame);

        //final String graphFile = "samples/Java.gexf";
        //final String graphFile = "samples/mixed-sample.gexf";
        //final String graphFile = "samples/Les Miserables.gexf";
        final String graphFile = "samples/comic-hero-network.gexf";
        //final String graphFile = "samples/Power Grid.gml";
        //final String graphFile = "samples/twitter_combined.csv";
        final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine = VizEngineFactory.<LWJGLRenderingTarget, LWJGLInputEvent>newEngine(
                renderingTarget,
                GraphLoader.load(graphFile),
                Collections.singletonList(
                        new VizEngineLWJGLConfigurator()
                )
        );
        renderingTarget.setWindowTitleFormat("VizEngine demo (LWJGL GLFW) FPS: $FPS");

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

        frame.add(canvas);

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.transferFocus();

        engine.start();

        final Runnable renderLoop = new Runnable() {
            @Override
            public void run() {
                if (!canvas.isValid()) {
                    return;
                }
                canvas.render();
                SwingUtilities.invokeLater(this);
            }
        };

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent event) {
                renderingTarget.reshape(event.getComponent().getWidth(), event.getComponent().getHeight());
            }
        });
        renderingTarget.reshape(WIDTH, HEIGHT);

        SwingUtilities.invokeLater(renderLoop);
    }

    public static void main(String[] args) {
        final MainAWT main = new MainAWT();
        main.init();
    }
}
