package org.gephi.viz.engine.demo;

import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.awt.GLCanvas;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Collections;
import javax.swing.JFrame;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.jogl.VizEngineJOGLConfigurator;

public class MainGLCanvas implements KeyListener {

    private VizEngine engine;
    private JFrame frame;

    public void start() {
        final GLCapabilities caps = VizEngineJOGLConfigurator.createCapabilities();
        final GLCanvas glCanvas = new GLCanvas(caps);

        glCanvas.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glCanvas.setSize(1024, 768);

        final JOGLRenderingTarget renderingTarget = new JOGLRenderingTarget(glCanvas);

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
        engine.setup();
        engine.start();

        frame = new JFrame("VizEngine demo (JOGL GLCanvas)");
        frame.add(glCanvas);
        frame.addKeyListener(this);
        frame.pack();

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        MainGLCanvas main = new MainGLCanvas();
        main.start();
    }

    @Override
    public void keyReleased(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
                engine.stop();
                frame.dispose();
                break;
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }
}
