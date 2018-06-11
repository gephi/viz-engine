package org.gephi.viz.engine.pipeline.instanced.updaters;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.InstancedDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgesUpdaterInstancedRendering implements WorldUpdater {

    private final VizEngine engine;
    private final InstancedEdgeData edgeData;
    private final GraphIndexImpl spatialIndex;

    public EdgesUpdaterInstancedRendering(VizEngine engine, InstancedEdgeData edgeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        edgeData.init(drawable.getGL().getGL2ES3());
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        edgeData.dispose(drawable.getGL());
    }

    @Override
    public void updateWorld() {
        //final long start = System.currentTimeMillis();
        edgeData.update(engine, spatialIndex);
        //System.out.println("Edges update ms: " + (System.currentTimeMillis() - start));
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return InstancedDraw.isAvailable(engine, drawable);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
