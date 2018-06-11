package org.gephi.viz.engine.availability;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.DebugConstants;

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

        return drawable.getGLProfile().isGL2ES3()
                && engine.getCapabilities().isInstancingSupported()
                && engine.getCapabilities().getExtensions().ARB_base_instance//Maybe we can avoid this extension with separated gl buffers...
                ;
    }
}
