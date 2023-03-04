package org.gephi.viz.engine.lwjgl.pipeline.indirect.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.IndirectDraw;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeRenderer;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererIndirect extends AbstractNodeRenderer {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;

    public NodeRendererIndirect(VizEngine engine, IndirectNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(PanamaGLRenderingTarget target) {
        //NOOP
    }

    @Override
    public void worldUpdated(PanamaGLRenderingTarget target) {
        nodeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(PanamaGLRenderingTarget target, RenderingLayer layer) {
        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        nodeData.drawIndirect(layer, engine, mvpFloats);
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
    public boolean isAvailable(PanamaGLRenderingTarget target) {
        return IndirectDraw.isAvailable(engine);
    }
}
