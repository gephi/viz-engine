package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.ArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeRenderer;
import org.gephi.viz.engine.pipeline.RenderingLayer;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererArrayDraw extends AbstractNodeRenderer {

    private final VizEngine engine;
    private final ArrayDrawNodeData nodeData;

    public NodeRendererArrayDraw(VizEngine engine, ArrayDrawNodeData nodeData) {
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

        nodeData.drawArrays(layer, engine, mvpFloats);
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
