package org.gephi.viz.engine.spi;

import java.util.EnumSet;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 *
 * @author Eduardo Ramos
 * @param <R>
 */
public interface Renderer<R extends RenderingTarget> extends PipelinedExecutor<R> {

    void worldUpdated(R target);

    void render(R target, RenderingLayer layer);

    default EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.MIDDLE);
    }
}
