package org.gephi.viz.engine.jogl.pipeline.arrays.renderers;

import com.jogamp.opengl.GL2ES2;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.availability.ArrayDraw;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererArrayDraw implements Renderer<JOGLRenderingTarget> {

    private final VizEngine engine;
    private final ArrayDrawNodeData nodeData;

    public NodeRendererArrayDraw(VizEngine engine, ArrayDrawNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(JOGLRenderingTarget target) {
        nodeData.init(target.getDrawable().getGL().getGL2ES2());
    }

    @Override
    public void worldUpdated(JOGLRenderingTarget target) {
        nodeData.updateBuffers();
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(JOGLRenderingTarget target, RenderingLayer layer) {
        final GL2ES2 gl = target.getDrawable().getGL().getGL2ES2();

        engine.getModelViewProjectionMatrixFloats(mvpFloats);

        nodeData.drawArrays(gl, layer, engine, mvpFloats);
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
    public boolean isAvailable(JOGLRenderingTarget target) {
        return ArrayDraw.isAvailable(engine, target.getDrawable());
    }
}
