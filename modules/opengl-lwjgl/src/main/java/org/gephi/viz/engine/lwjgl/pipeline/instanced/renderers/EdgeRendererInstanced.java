package org.gephi.viz.engine.lwjgl.pipeline.instanced.renderers;

import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.lwjgl.availability.InstancedDraw;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 * TODO: self loops
 *
 * @author Eduardo Ramos
 */
public class EdgeRendererInstanced implements Renderer<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final InstancedEdgeData edgeData;

    public EdgeRendererInstanced(VizEngine engine, InstancedEdgeData edgeData) {
        this.engine = engine;
        this.edgeData = edgeData;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
        //NOOP
    }

    @Override
    public void worldUpdated(LWJGLRenderingTarget target) {
        edgeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(LWJGLRenderingTarget target, RenderingLayer layer) {
        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        edgeData.drawInstanced(
                layer,
                engine, mvpFloats
        );
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.BACK, RenderingLayer.MIDDLE);
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_EDGES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.EDGE;
    }

    @Override
    public int getPreferenceInCategory() {
        return InstancedDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Edges (Instanced)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine);
    }
}
