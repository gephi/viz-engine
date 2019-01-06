package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.ArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererArrayDraw implements Renderer<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final ArrayDrawNodeData nodeData;

    public NodeRendererArrayDraw(VizEngine engine, ArrayDrawNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
        nodeData.init();
    }

    @Override
    public void worldUpdated(LWJGLRenderingTarget target) {
        nodeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(LWJGLRenderingTarget target, RenderingLayer layer) {
        engine.getModelViewProjectionMatrixFloats(mvpFloats);

        nodeData.drawArrays(layer, engine, mvpFloats);
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.BACK, RenderingLayer.MIDDLE);
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_NODES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.NODE;
    }

    @Override
    public int getPreferenceInCategory() {
        return ArrayDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Vertex Array)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return ArrayDraw.isAvailable(engine);
    }
}
