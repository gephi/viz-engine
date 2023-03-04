package org.gephi.viz.engine.lwjgl.pipeline.instanced.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.InstancedDraw;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeRenderer;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 * TODO: self loops
 *
 * @author Eduardo Ramos
 */
public class EdgeRendererInstanced extends AbstractEdgeRenderer {

    private final VizEngine engine;
    private final InstancedEdgeData edgeData;

    public EdgeRendererInstanced(VizEngine engine, InstancedEdgeData edgeData) {
        this.engine = engine;
        this.edgeData = edgeData;
    }

    @Override
    public void init(PanamaGLRenderingTarget target) {
        //NOOP
    }

    @Override
    public void worldUpdated(PanamaGLRenderingTarget target) {
        edgeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(PanamaGLRenderingTarget target, RenderingLayer layer) {
        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        edgeData.drawInstanced(
                layer,
                engine, mvpFloats
        );
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
    public boolean isAvailable(PanamaGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine);
    }
}
