package org.gephi.viz.engine.lwjgl.pipeline.common;

import java.util.EnumSet;

import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.gl.Constants;

public abstract class AbstractNodeRenderer implements Renderer<LWJGLRenderingTarget> {
    public static final EnumSet<RenderingLayer> LAYERS = EnumSet.of(
        RenderingLayer.BACK2, RenderingLayer.BACK3,
        RenderingLayer.MIDDLE2, RenderingLayer.MIDDLE3
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
