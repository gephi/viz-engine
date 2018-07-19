package org.gephi.viz.engine.demo;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.awt.GLCanvas;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.JFrame;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;

public class MainGLCanvas implements KeyListener {

    private VizEngine engine;
    private JFrame frame;

    public void start() {
        final GLCapabilities caps = VizEngineFactory.createCapabilities();
        final GLCanvas glCanvas = new GLCanvas(caps);

        glCanvas.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glCanvas.setSize(1024, 768);


        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("samples/Les Miserables.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("samples/Power Grid.gml"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("samples/Java.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("samples/mixed-sample.gexf"));
        engine = VizEngineFactory.newEngine(glCanvas, GraphLoader.load("samples/comic-hero-network.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("samples/twitter_combined.csv"));
        engine.start();

        frame = new JFrame("VizEngine test");
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
