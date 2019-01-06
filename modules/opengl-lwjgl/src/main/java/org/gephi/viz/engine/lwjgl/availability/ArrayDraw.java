package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.DebugConstants;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDraw {

    public static int getPreferenceInCategory() {
        return 0;
    }

    public static boolean isAvailable(VizEngine engine) {
        if (DebugConstants.DEBUG_DISABLE_VERTEX_ARRAY_DRAWING) {
            return false;
        }

        final GLCapabilities capabilities = engine.getLookup().lookup(GLCapabilities.class);

        return Checks.checkFunctions(capabilities.glDrawArrays);
    }

}
