package org.gephi.viz.engine.lwjgl.pipeline.indirect.renderers;

import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.availability.IndirectDraw;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererIndirect implements Renderer<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;

    public NodeRendererIndirect(VizEngine engine, IndirectNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
    }

    @Override
    public void worldUpdated(LWJGLRenderingTarget target) {
        nodeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(LWJGLRenderingTarget target, RenderingLayer layer) {
        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        nodeData.drawIndirect(layer, engine, mvpFloats);
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
        return IndirectDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Indirect)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return IndirectDraw.isAvailable(engine);
    }
}
