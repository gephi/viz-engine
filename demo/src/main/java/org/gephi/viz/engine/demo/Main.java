package org.gephi.viz.engine.demo;

import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import javax.swing.JFrame;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.VizEngineFactory;

public class Main implements KeyListener {

    private VizEngine engine;
    private JFrame frame;
    private GLWindow glWindow;
    private NewtCanvasAWT newtCanvas;

    public void start() {
        final GLCapabilities caps = VizEngineFactory.createCapabilities();

        final Display display = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(display, 0);
        
        glWindow = GLWindow.create(screen, caps);
        glWindow.setContextCreationFlags(GLContext.CTX_OPTION_DEBUG);
        glWindow.setSize(1024, 768);

        glWindow.addKeyListener(this);
        
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/Les Miserables.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/Power Grid.gml"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/Java.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/mixed-sample.gexf"));
        engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/comic-hero-network.gexf"));
        //engine = VizEngineFactory.newEngine(glWindow, GraphLoader.load("src/main/resources/samples/twitter_combined.csv"));
        engine.start();
        
        newtCanvas = new NewtCanvasAWT(glWindow);

        frame = new JFrame("VizEngine test");
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
