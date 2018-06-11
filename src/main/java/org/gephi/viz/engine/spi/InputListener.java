package org.gephi.viz.engine.spi;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;

/**
 *
 * @author Eduardo Ramos
 */
public interface InputListener extends PipelinedExecutor {

    default void frameStart() {

    }

    default void frameEnd() {

    }

    /**
     * A key has been {@link KeyEvent#EVENT_KEY_PRESSED pressed}, excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys. See {@link KeyEvent}.
     *
     * @param e
     * @return
     */
    default boolean keyPressed(KeyEvent e) {
        return false;
    }

    /**
     * A key has been {@link KeyEvent#EVENT_KEY_RELEASED released}, excluding {@link #isAutoRepeat() auto-repeat} {@link #isModifierKey() modifier} keys. See {@link KeyEvent}.
     * <p>
     * To simulated the removed <code>keyTyped(KeyEvent e)</code> semantics, simply apply the following constraints upfront and bail out if not matched, i.e.:
     * <pre>
     * if( !e.isPrintableKey() || e.isAutoRepeat() ) {
     * return;
     * }
     * </pre>
     * </p>
     *
     * @param e
     * @return
     */
    default boolean keyReleased(KeyEvent e) {
        return false;
    }

    default boolean mouseClicked(MouseEvent e) {
        return false;
    }

    /**
     * Only generated for {@link PointerType#Mouse}
     *
     * @param e
     * @return
     */
    default boolean mouseEntered(MouseEvent e) {
        return false;
    }

    /**
     * Only generated for {@link PointerType#Mouse}
     *
     * @param e
     * @return
     */
    default boolean mouseExited(MouseEvent e) {
        return false;
    }

    default boolean mousePressed(MouseEvent e) {
        return false;
    }

    default boolean mouseReleased(MouseEvent e) {
        return false;
    }

    default boolean mouseMoved(MouseEvent e) {
        return false;
    }

    default boolean mouseDragged(MouseEvent e) {
        return false;
    }

    /**
     * Traditional event name originally produced by a {@link PointerType#Mouse mouse} pointer type.
     * <p>
     * Triggered for any rotational pointer events, see {@link MouseEvent#getRotation()} and {@link MouseEvent#getRotationScale()}.
     * </p>
     *
     * @param e
     * @return
     */
    default boolean mouseWheelMoved(MouseEvent e) {
        return false;
    }
}
