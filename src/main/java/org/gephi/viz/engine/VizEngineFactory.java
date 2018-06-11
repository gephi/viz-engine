package org.gephi.viz.engine;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import static com.jogamp.opengl.GLProfile.GL2;
import static com.jogamp.opengl.GLProfile.GL3;
import static com.jogamp.opengl.GLProfile.GL4;
import static com.jogamp.opengl.GLProfile.GLES2;
import static com.jogamp.opengl.GLProfile.GLES3;
import java.util.Arrays;
import java.util.List;
import org.gephi.graph.api.GraphModel;
import org.gephi.viz.engine.pipeline.VizEngineDefaultConfigurator;
import org.gephi.viz.engine.spi.VizEngineConfigurator;

/**
 *
 * @author Eduardo Ramos
 */
public class VizEngineFactory {

    /**
     * Order of maximum programmable shader <i>core only</i> profiles
     *
     * <ul>
     * <li> GL4 </li>
     * <li> GL3 </li>
     * <li> GLES3 </li>
     * <li> GL2 </li>
     * <li> GLES2 </li>
     * </ul>
     *
     */
    public static final String[] GL_PROFILE_LIST_MAX_PROGSHADER_CORE_OR_GL2 = new String[]{GL4, GL3, GLES3, GL2, GLES2};

    public static GLCapabilities createCapabilities() {
        GLProfile.getDefaultDevice();

        GLProfile glProfile = GLProfile.get(GL_PROFILE_LIST_MAX_PROGSHADER_CORE_OR_GL2, true);
        GLCapabilities caps = new GLCapabilities(glProfile);

        System.out.println(GLProfile.glAvailabilityToString());
        System.out.println("GL Profile: " + glProfile);

        caps.setAlphaBits(8);
        caps.setDoubleBuffered(true);
        caps.setHardwareAccelerated(true);

        caps.setSampleBuffers(true);
        caps.setNumSamples(4);

        return caps;
    }

    public static VizEngine newEngine(GLAutoDrawable drawable, GraphModel graphModel) {
        return newEngine(drawable, graphModel, Arrays.asList(
                new VizEngineDefaultConfigurator()
        ));
    }

    public static VizEngine newEngine(GLAutoDrawable drawable, GraphModel graphModel, List<? extends VizEngineConfigurator> configurators) {
        final VizEngine engine = new VizEngine(graphModel);

        engine.setup(drawable);

        //Configure
        if (configurators != null) {
            for (VizEngineConfigurator configurator : configurators) {
                if (configurator != null) {
                    configurator.configure(engine);
                }
            }
        }

        return engine;
    }
}
