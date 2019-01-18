package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.ArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.gl.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgeRendererArrayDraw implements Renderer<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final ArrayDrawEdgeData edgeData;

    public EdgeRendererArrayDraw(VizEngine engine, ArrayDrawEdgeData edgeData) {
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

        edgeData.drawArrays(layer, engine, mvpFloats);
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
        return ArrayDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Edges (Vertex Array)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return ArrayDraw.isAvailable(engine);
    }
}
