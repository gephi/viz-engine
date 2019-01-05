package org.gephi.viz.engine.jogl.pipeline.indirect.renderers;

import com.jogamp.opengl.GL4;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.availability.IndirectDraw;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 *
 * @author Eduardo Ramos
 */
public class NodeRendererIndirect implements Renderer<JOGLRenderingTarget> {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;

    public NodeRendererIndirect(VizEngine engine, IndirectNodeData nodeData) {
        this.engine = engine;
        this.nodeData = nodeData;
    }

    @Override
    public void init(JOGLRenderingTarget target) {
    }

    @Override
    public void worldUpdated(JOGLRenderingTarget target) {
        final GL4 gl = target.getDrawable().getGL().getGL4();
        nodeData.updateBuffers(gl);
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(JOGLRenderingTarget target, RenderingLayer layer) {
        final GL4 gl = target.getDrawable().getGL().getGL4();

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
    public boolean isAvailable(JOGLRenderingTarget target) {
        return IndirectDraw.isAvailable(engine, target.getDrawable());
    }
}
