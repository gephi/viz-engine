package org.gephi.viz.engine.jogl.pipeline.instanced.renderers;

import com.jogamp.opengl.GL2ES3;
import java.util.EnumSet;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.availability.InstancedDraw;
import org.gephi.viz.engine.jogl.JOGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.util.Constants;

/**
 * TODO: self loops
 *
 * @author Eduardo Ramos
 */
public class EdgeRendererInstanced implements Renderer<JOGLRenderingTarget> {

    private final VizEngine engine;
    private final InstancedEdgeData edgeData;

    public EdgeRendererInstanced(VizEngine engine, InstancedEdgeData edgeData) {
        this.engine = engine;
        this.edgeData = edgeData;
    }

    @Override
    public void init(JOGLRenderingTarget target) {
    }

    @Override
    public void worldUpdated(JOGLRenderingTarget target) {
        final GL2ES3 gl = target.getDrawable().getGL().getGL2ES3();
        edgeData.updateBuffers(gl);
    }

    private final float[] mvpFloats = new float[16];

    @Override
    public void render(JOGLRenderingTarget target, RenderingLayer layer) {
        final GL2ES3 gl = target.getDrawable().getGL().getGL2ES3();

        engine.getModelViewProjectionMatrixFloats(mvpFloats);
        edgeData.drawInstanced(
                gl, layer,
                engine, mvpFloats
        );
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.BACK, RenderingLayer.MIDDLE);
    }

    @Override
    public int getOrder() {
        return Constants.RENDERING_ORDER_EDGES;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.EDGE;
    }

    @Override
    public int getPreferenceInCategory() {
        return InstancedDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Edges (Instanced)";
    }

    @Override
    public boolean isAvailable(JOGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine, target.getDrawable());
    }
}
