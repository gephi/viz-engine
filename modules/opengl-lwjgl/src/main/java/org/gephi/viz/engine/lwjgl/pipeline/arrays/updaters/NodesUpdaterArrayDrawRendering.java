package org.gephi.viz.engine.lwjgl.pipeline.arrays.updaters;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.lwjgl.availability.ArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterArrayDrawRendering implements WorldUpdater<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final ArrayDrawNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterArrayDrawRendering(VizEngine engine, ArrayDrawNodeData nodeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.nodeData = nodeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
        nodeData.init();
    }

    @Override
    public void dispose(LWJGLRenderingTarget target) {
        nodeData.dispose();
    }

    @Override
    public void updateWorld() {
        nodeData.update(engine, spatialIndex);
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
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return ArrayDraw.isAvailable(engine);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
