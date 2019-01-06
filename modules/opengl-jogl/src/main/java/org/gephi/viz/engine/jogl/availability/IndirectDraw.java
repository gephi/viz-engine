package org.gephi.viz.engine.jogl.availability;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.util.gl.capabilities.GLCapabilitiesSummary;
import org.gephi.viz.engine.util.gl.DebugConstants;

/**
 *
 * @author Eduardo Ramos
 */
public class IndirectDraw {

    public static int getPreferenceInCategory() {
        return 100;
    }

    public static boolean isAvailable(VizEngine engine, GLAutoDrawable drawable) {
        if (DebugConstants.DEBUG_DISABLE_INDIRECT_DRAWING) {
            return false;
        }
        
        return drawable.getGLProfile().isGL4()
                && engine.getLookup().lookup(GLCapabilitiesSummary.class).isIndirectDrawSupported();
    }

}
