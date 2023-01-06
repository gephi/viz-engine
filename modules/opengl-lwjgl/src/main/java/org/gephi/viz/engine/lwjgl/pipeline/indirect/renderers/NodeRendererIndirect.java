package org.gephi.viz.engine.lwjgl.pipeline.indirect.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
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
    public void init(LWJGLRenderingTarget target) {
        //NOOP
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
