package org.gephi.viz.engine.spi;

import com.jogamp.opengl.GLAutoDrawable;
import java.util.EnumSet;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 *
 * @author Eduardo Ramos
 */
public interface Renderer extends PipelinedExecutor {

    void worldUpdated(GLAutoDrawable drawable);

    void render(GLAutoDrawable drawable, RenderingLayer layer);

    default EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.MIDDLE);
    }
}
