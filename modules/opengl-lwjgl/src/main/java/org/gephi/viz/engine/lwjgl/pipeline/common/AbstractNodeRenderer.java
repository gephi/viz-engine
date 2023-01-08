package org.gephi.viz.engine.lwjgl.pipeline.common;

import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.gl.Constants;

import java.util.EnumSet;

public abstract class AbstractNodeRenderer implements Renderer<LWJGLRenderingTarget> {
    public static final EnumSet<RenderingLayer> LAYERS = EnumSet.of(
        RenderingLayer.BACK2,
        RenderingLayer.MIDDLE2
    );

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return LAYERS;
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_NODES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.NODE;
    }
}
