package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
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
        if (engine.getLookup().lookup(OpenGLOptions.class).isDisableIndirectDrawing()) {
            return false;
        }

        final GLCapabilities capabilities = engine.getLookup().lookup(GLCapabilities.class);

        return Checks.checkFunctions(
                capabilities.glMultiDrawArraysIndirect,
                capabilities.glBufferStorage
        );
    }

}
