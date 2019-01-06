package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.DebugConstants;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.Checks;

/**
 *
 * @author Eduardo Ramos
 */
public class InstancedDraw {

    public static int getPreferenceInCategory() {
        return 50;
    }

    public static boolean isAvailable(VizEngine engine) {
        if (DebugConstants.DEBUG_DISABLE_INSTANCED_DRAWING) {
            return false;
        }

        final GLCapabilities capabilities = engine.getLookup().lookup(GLCapabilities.class);

        return Checks.checkFunctions(
                capabilities.glDrawArraysInstanced,
                capabilities.glDrawArraysInstancedBaseInstance
        );
    }
}
