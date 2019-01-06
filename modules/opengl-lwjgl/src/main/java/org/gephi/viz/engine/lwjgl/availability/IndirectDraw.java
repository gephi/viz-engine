package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.DebugConstants;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

/**
 *
 * @author Eduardo Ramos
 */
public class IndirectDraw {

    public static int getPreferenceInCategory() {
        return 100;
    }

    public static boolean isAvailable(VizEngine engine) {
        if (DebugConstants.DEBUG_DISABLE_INDIRECT_DRAWING) {
            return false;
        }

        final GLCapabilities capabilities = engine.getLookup().lookup(GLCapabilities.class);

        return Checks.checkFunctions(
                capabilities.glDrawArraysIndirect
        );
    }

}
