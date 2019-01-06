package org.gephi.viz.engine.jogl.pipeline.instanced.renderers;

import com.jogamp.opengl.GL2ES3;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.availability.InstancedDraw;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererInstanced implements Renderer<JOGLRenderingTarget> {

    private final VizEngine engine;
    private final InstancedNodeData nodeData;

    public NodeRendererInstanced(VizEngine engine, InstancedNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(JOGLRenderingTarget target) {
        nodeData.init(target.getDrawable().getGL().getGL2ES3());
    }

    @Override
    public void worldUpdated(JOGLRenderingTarget target) {
        final GL2ES3 gl = target.getDrawable().getGL().getGL2ES3();
        nodeData.updateBuffers(gl);
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(JOGLRenderingTarget target, RenderingLayer layer) {
        final GL2ES3 gl = target.getDrawable().getGL().getGL2ES3();
        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        nodeData.drawInstanced(gl, layer, engine, mvpFloats);
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
        return InstancedDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Instanced)";
    }

    @Override
    public boolean isAvailable(JOGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine, target.getDrawable());
    }
}
