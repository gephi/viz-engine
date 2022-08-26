package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.GLCapabilities;

/**
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

        if (capabilities.OpenGL43) {
            return true;
        }

        return capabilities.OpenGL40
                && capabilities.GL_ARB_draw_indirect
                && capabilities.GL_ARB_multi_draw_indirect
                && capabilities.GL_ARB_buffer_storage;
    }

}
