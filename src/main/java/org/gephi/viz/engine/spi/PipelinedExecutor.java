package org.gephi.viz.engine.spi;

import com.jogamp.opengl.GLAutoDrawable;

/**
 *
 * @author Eduardo Ramos
 */
public interface PipelinedExecutor {

    String getCategory();

    int getPreferenceInCategory();

    String getName();

    default boolean isAvailable(GLAutoDrawable drawable) {
        return true;
    }

    void init(GLAutoDrawable drawable);

    default void dispose(GLAutoDrawable drawable) {
        //NOOP
    }

    int getOrder();

    class Comparator implements java.util.Comparator<PipelinedExecutor> {

        @Override
        public int compare(PipelinedExecutor o1, PipelinedExecutor o2) {
            return o1.getOrder() - o2.getOrder();
        }
    }
}
