package org.gephi.viz.engine.jogl.availability;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.DebugConstants;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDraw {

    public static int getPreferenceInCategory() {
        return 0;
    }

    public static boolean isAvailable(VizEngine engine, GLAutoDrawable drawable) {
        if (DebugConstants.DEBUG_DISABLE_VERTEX_ARRAY_DRAWING) {
            return false;
        }

        return drawable.getGLProfile().isGL2ES2();
    }

}
