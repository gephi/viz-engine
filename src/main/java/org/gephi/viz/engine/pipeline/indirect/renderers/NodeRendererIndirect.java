package org.gephi.viz.engine.pipeline.indirect.renderers;

import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.IndirectDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererIndirect implements Renderer {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;

    public NodeRendererIndirect(VizEngine engine, IndirectNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
    }

    @Override
    public void worldUpdated(GLAutoDrawable drawable) {
        final GL4 gl = drawable.getGL().getGL4();
        nodeData.updateBuffers(gl);
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(GLAutoDrawable drawable, RenderingLayer layer) {
        GL4 gl = drawable.getGL().getGL4();

        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        nodeData.drawIndirect(gl, layer, engine, mvpFloats);
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return IndirectDraw.isAvailable(engine, drawable);
    }
}
