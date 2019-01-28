package org.gephi.viz.engine.lwjgl.pipeline.events;

import java.awt.event.KeyAdapter;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.lwjgl.opengl.awt.AWTGLCanvas;

/**
 *
 * @author Eduardo Ramos
 */
public class AWTEventsListener {

    private final AWTGLCanvas canvas;
    private final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine;

    public AWTEventsListener(AWTGLCanvas canvas, VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
        this.canvas = canvas;
        this.engine = engine;
    }

    public void destroy() {
        //NOOP
    }

    public void register() {
        canvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() > 1) {
                    engine.queueEvent(
                            MouseEvent.doubleClick(getButton(e), e.getX(), e.getY())
                    );
                } else {
                    engine.queueEvent(
                            MouseEvent.click(getButton(e), e.getX(), e.getY())
                    );
                }
            }

            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                engine.queueEvent(
                        MouseEvent.press(getButton(e), e.getX(), e.getY())
                );
            }

            @Override
            public void mouseReleased(java.awt.event.MouseEvent e) {
                engine.queueEvent(
                        MouseEvent.release(getButton(e), e.getX(), e.getY())
                );
            }

            private MouseEvent.Button getButton(java.awt.event.MouseEvent e) {
                switch (e.getButton()) {
                    case java.awt.event.MouseEvent.BUTTON2:
                        return MouseEvent.Button.MIDDLE;
                    case java.awt.event.MouseEvent.BUTTON3:
                        return MouseEvent.Button.RIGHT;
                    default:
                        return MouseEvent.Button.LEFT;
                }
            }
        });

        canvas.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(java.awt.event.MouseEvent e) {
                engine.queueEvent(
                        MouseEvent.drag(e.getX(), e.getY())
                );
            }

            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                System.out.println("Mouse: " + e.getX() + ":" + e.getY());
                engine.queueEvent(
                        MouseEvent.move(e.getX(), e.getY())
                );
            }
        });

        canvas.addMouseWheelListener((MouseWheelEvent e) -> {
            engine.queueEvent(
                    MouseEvent.scroll(e.getX(), e.getY(), 0, -e.getPreciseWheelRotation())
            );
        });

        canvas.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                engine.queueEvent(
                        new KeyEvent(e.getKeyCode(), KeyEvent.Action.PRESS)
                );
            }

            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                engine.queueEvent(
                        new KeyEvent(e.getKeyCode(), KeyEvent.Action.RELEASE)
                );
            }
        });
    }
}
