package org.gephi.viz.engine.lwjgl.pipeline.instanced.updaters;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.InstancedDraw;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgesUpdaterInstancedRendering implements WorldUpdater<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final InstancedEdgeData edgeData;
    private final GraphIndexImpl spatialIndex;

    public EdgesUpdaterInstancedRendering(VizEngine engine, InstancedEdgeData edgeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.edgeData = edgeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
        edgeData.init();
    }

    @Override
    public void dispose(LWJGLRenderingTarget target) {
        edgeData.dispose();
    }

    @Override
    public void updateWorld() {
        //final long start = TimeUtils.getTimeMillis();
        edgeData.update(engine, spatialIndex);
        //System.out.println("Edges update ms: " + (TimeUtils.getTimeMillis() - start));
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
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
