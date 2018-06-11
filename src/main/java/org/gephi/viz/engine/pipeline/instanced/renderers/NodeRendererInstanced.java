package org.gephi.viz.engine.pipeline.instanced.renderers;

import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.InstancedDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererInstanced implements Renderer {

    private final VizEngine engine;
    private final InstancedNodeData nodeData;

    public NodeRendererInstanced(VizEngine engine, InstancedNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        nodeData.init(drawable.getGL().getGL2ES3());
    }

    @Override
    public void worldUpdated(GLAutoDrawable drawable) {
        final GL2ES3 gl = drawable.getGL().getGL2ES3();
        nodeData.updateBuffers(gl);
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(GLAutoDrawable drawable, RenderingLayer layer) {
        final GL2ES3 gl = drawable.getGL().getGL2ES3();
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return InstancedDraw.isAvailable(engine, drawable);
    }
}
