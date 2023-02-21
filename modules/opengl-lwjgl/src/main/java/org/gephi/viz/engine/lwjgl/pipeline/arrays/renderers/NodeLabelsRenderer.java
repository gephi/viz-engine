package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import java.awt.*;
import java.util.EnumSet;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.util.text.LWJGLTextRenderer;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.lwjgl.opengl.GLCapabilities;

public class NodeLabelsRenderer implements Renderer<LWJGLRenderingTarget> {

    private final VizEngine engine;

    public NodeLabelsRenderer(final VizEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.NODE_LABELS;
    }

    @Override
    public int getPreferenceInCategory() {
        return 0;
    }

    @Override
    public String getName() {
        return "Node labels";
    }

    @Override
    public void init(final LWJGLRenderingTarget target) {

    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void worldUpdated(final LWJGLRenderingTarget target) {

    }

    @Override
    public void render(final LWJGLRenderingTarget target, final RenderingLayer layer) {
        final Font font = new Font ("Arial", Font.BOLD , 20);
        final LWJGLTextRenderer r = new LWJGLTextRenderer(font);

        r.setTransform(engine.getModelViewProjectionMatrixFloats());
        r.setUseVertexArrays(false);
        r.beginRendering(
            engine.getLookup().lookup(GLCapabilities.class),
            engine.getWidth(),
            engine.getHeight()
        );
        r.draw("Testing...", 0, 0);
        r.endRendering();
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.FRONT1);
    }
}
