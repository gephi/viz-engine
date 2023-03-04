package org.gephi.viz.engine.lwjgl.pipeline.common;

import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.gl.Constants;

import java.util.EnumSet;

public abstract class AbstractEdgeRenderer implements Renderer<PanamaGLRenderingTarget> {
    private static final EnumSet<RenderingLayer> LAYERS = EnumSet.of(
            RenderingLayer.BACK1,
            RenderingLayer.MIDDLE1
    );

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return LAYERS;
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_EDGES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.EDGE;
    }

}
