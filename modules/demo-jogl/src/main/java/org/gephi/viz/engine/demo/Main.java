package org.gephi.viz.engine.demo;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import java.util.Collections;
import javax.swing.JFrame;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.jogl.VizEngineJOGLConfigurator;
import org.gephi.viz.engine.util.gl.OpenGLOptions;

public class Main implements KeyListener {

    private static final boolean DISABLE_INDIRECT_RENDERING = false;
    private static final boolean DISABLE_INSTANCED_RENDERING = false;
    private static final boolean DISABLE_VAOS = false;

    private static final boolean DEBUG = true;

    private VizEngine<JOGLRenderingTarget, NEWTEvent> engine;
    private JFrame frame;
    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvas;

    public void start() {
        final GLCapabilities caps = VizEngineJOGLConfigurator.createCapabilities();

        final Display display = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(display, 0);

        glWindow = GLWindow.create(screen, caps);
        glWindow.setSize(1024, 768);
        if (DEBUG) {
            glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        }

        glWindow.addKeyListener(this);

        final JOGLRenderingTarget renderingTarget = new JOGLRenderingTarget(glWindow);

        //final String graphFile = "samples/Java.gexf";
        //final String graphFile = "samples/mixed-sample.gexf";
        //final String graphFile = "samples/Les Miserables.gexf";
        final String graphFile = "samples/comic-hero-network.gexf";
        //final String graphFile = "samples/Power Grid.gml";
        //final String graphFile = "samples/twitter_combined.csv";
        engine = VizEngineFactory.<JOGLRenderingTarget, NEWTEvent>newEngine(
                renderingTarget,
                GraphLoader.load(graphFile),
                Collections.singletonList(
                        new VizEngineJOGLConfigurator()
                )
        );

        final OpenGLOptions glOptions = engine.getLookup().lookup(OpenGLOptions.class);
        glOptions.setDisableIndirectDrawing(DISABLE_INDIRECT_RENDERING);
        glOptions.setDisableInstancedDrawing(DISABLE_INSTANCED_RENDERING);
        glOptions.setDisableVAOS(DISABLE_VAOS);
        glOptions.setDebug(DEBUG);

        engine.start();

        newtCanvas = new NewtCanvasAWT(glWindow);

        frame = new JFrame("VizEngine demo (JOGL NEWT)");
        frame.add(newtCanvas);
        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        Main main = new Main();
        main.start();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                engine.stop();
                glWindow.destroy();
                frame.dispose();
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }
}
