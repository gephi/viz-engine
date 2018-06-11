package org.gephi.viz.engine.pipeline.arrays.updaters;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.ArrayDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgesUpdaterArrayDrawRendering implements WorldUpdater {

    private final VizEngine engine;
    private final ArrayDrawEdgeData edgeData;
    private final GraphIndexImpl spatialIndex;

    public EdgesUpdaterArrayDrawRendering(VizEngine engine, ArrayDrawEdgeData edgeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        edgeData.init(drawable.getGL().getGL2ES2());
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        edgeData.dispose(drawable.getGL().getGL2ES2());
    }

    @Override
    public void updateWorld() {
        edgeData.update(engine, spatialIndex);
    }

    @Override
    public String getCategory() {
        return PipelineCategory.EDGE;
    }

    @Override
    public int getPreferenceInCategory() {
        return ArrayDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Edges (Vertex Array)";
    }

    @Override
    public boolean isAvailable(GLAutoDrawable drawable) {
        return ArrayDraw.isAvailable(engine, drawable);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
