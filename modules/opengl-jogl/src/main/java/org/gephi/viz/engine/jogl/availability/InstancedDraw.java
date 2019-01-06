package org.gephi.viz.engine.jogl.availability;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.util.gl.capabilities.GLCapabilitiesSummary;
import org.gephi.viz.engine.util.gl.DebugConstants;

/**
 *
 * @author Eduardo Ramos
 */
public class InstancedDraw {

    public static int getPreferenceInCategory() {
        return 50;
    }

    public static boolean isAvailable(VizEngine engine, GLAutoDrawable drawable) {
        if (DebugConstants.DEBUG_DISABLE_INSTANCED_DRAWING) {
            return false;
        }

        final GLCapabilitiesSummary caps = engine.getLookup().lookup(GLCapabilitiesSummary.class);

        return drawable.getGLProfile().isGL2ES3()
                && caps.isInstancingSupported()
                && caps.getExtensions().ARB_base_instance//Maybe we can avoid this extension with separated gl buffers...
                ;
    }
}
