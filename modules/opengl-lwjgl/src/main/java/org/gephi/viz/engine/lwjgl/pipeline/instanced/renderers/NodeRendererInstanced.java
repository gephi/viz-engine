package org.gephi.viz.engine.lwjgl.pipeline.instanced.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.InstancedDraw;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeRenderer;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererInstanced extends AbstractNodeRenderer {

    private final VizEngine engine;
    private final InstancedNodeData nodeData;

    public NodeRendererInstanced(VizEngine engine, InstancedNodeData nodeData) {
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
        nodeData.drawInstanced(layer, engine, mvpFloats);
    }

    @Override
    public int getPreferenceInCategory() {
        return InstancedDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Instanced)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine);
    }
}
