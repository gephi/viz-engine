package org.gephi.viz.engine.lwjgl.pipeline.events;

/**
 *
 * @author Eduardo Ramos
 */
public class MouseEvent implements LWJGLInputEvent {

    public enum Action {
        CLICK,
        DOUBLE_CLICK,
        PRESS,
        RELEASE,
        MOVE,
        DRAG,
        SCROLL
    }

    public enum Button {
        LEFT,
        MIDDLE,
        RIGHT
    }

    public final Action action;
    public final Button button;
    public final int x;
    public final int y;
    public final double xScroll;
    public final double yScroll;

    public MouseEvent(Action action, Button button, int x, int y) {
        this.action = action;
        this.button = button;
        this.x = x;
        this.y = y;
        this.xScroll = 0;
        this.yScroll = 0;
    }

    public MouseEvent(Action action, int x, int y) {
        this.action = action;
        this.x = x;
        this.y = y;
        this.button = null;
        this.xScroll = 0;
        this.yScroll = 0;
    }

    public MouseEvent(int x, int y, double xScroll, double yScroll) {
        this.action = Action.SCROLL;
        this.x = x;
        this.y = y;
        this.button = null;
        this.xScroll = xScroll;
        this.yScroll = yScroll;
    }

    public static MouseEvent click(Button button, int x, int y) {
        return new MouseEvent(Action.CLICK, button, x, y);
    }

    public static MouseEvent doubleClick(Button button, int x, int y) {
        return new MouseEvent(Action.DOUBLE_CLICK, button, x, y);
    }

    public static MouseEvent press(Button button, int x, int y) {
        return new MouseEvent(Action.PRESS, button, x, y);
    }

    public static MouseEvent release(Button button, int x, int y) {
        return new MouseEvent(Action.RELEASE, button, x, y);
    }

    public static MouseEvent move(int x, int y) {
        return new MouseEvent(Action.MOVE, x, y);
    }

    public static MouseEvent drag(int x, int y) {
        return new MouseEvent(Action.DRAG, x, y);
    }

    public static MouseEvent scroll(int x, int y, double xScroll, double yScroll) {
        return new MouseEvent(x, y, xScroll, yScroll);
    }

    @Override
    public String toString() {
        return "MouseEvent{" + "action=" + action + ", button=" + button + ", x=" + x + ", y=" + y + '}';
    }

}
