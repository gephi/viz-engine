package org.gephi.viz.engine.lwjgl.pipeline.events;

/**
 *
 * @author Eduardo Ramos
 */
public class KeyEvent implements LWJGLInputEvent {

    public enum Action {
        PRESS,
        RELEASE
    }

    private final int keyCode;
    private final Action action;

    public KeyEvent(int keyCode, Action action) {
        this.keyCode = keyCode;
        this.action = action;
    }

    public int getKeyCode() {
        return keyCode;
    }

    public Action getAction() {
        return action;
    }

}
