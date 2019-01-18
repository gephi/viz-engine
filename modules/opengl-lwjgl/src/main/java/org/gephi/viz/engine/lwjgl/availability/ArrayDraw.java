package org.gephi.viz.engine.lwjgl.availability;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.GLCapabilities;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDraw {

    public static int getPreferenceInCategory() {
        return 0;
    }

    public static boolean isAvailable(VizEngine engine) {
        if (engine.getLookup().lookup(OpenGLOptions.class).isDisableVertexArrayDrawing()) {
            return false;
        }

        final GLCapabilities capabilities = engine.getLookup().lookup(GLCapabilities.class);

        return capabilities.GL_ARB_shader_objects && capabilities.GL_ARB_vertex_shader && capabilities.GL_ARB_fragment_shader;
    }

}
