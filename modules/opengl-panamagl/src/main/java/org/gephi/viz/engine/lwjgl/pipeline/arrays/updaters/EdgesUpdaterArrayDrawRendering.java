package org.gephi.viz.engine.lwjgl.pipeline.arrays.updaters;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.ArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgesUpdaterArrayDrawRendering implements WorldUpdater<PanamaGLRenderingTarget> {

    private final VizEngine engine;
    private final ArrayDrawEdgeData edgeData;
    private final GraphIndexImpl spatialIndex;

    public EdgesUpdaterArrayDrawRendering(VizEngine engine, ArrayDrawEdgeData edgeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(PanamaGLRenderingTarget target) {
        edgeData.init();
    }

    @Override
    public void dispose(PanamaGLRenderingTarget target) {
        edgeData.dispose();
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
    public boolean isAvailable(PanamaGLRenderingTarget target) {
        return ArrayDraw.isAvailable(engine);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
