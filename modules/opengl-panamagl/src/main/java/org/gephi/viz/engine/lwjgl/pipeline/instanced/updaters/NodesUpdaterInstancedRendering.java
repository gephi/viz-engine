package org.gephi.viz.engine.lwjgl.pipeline.instanced.updaters;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.InstancedDraw;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterInstancedRendering implements WorldUpdater<PanamaGLRenderingTarget> {

    private final VizEngine engine;
    private final InstancedNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterInstancedRendering(VizEngine engine, InstancedNodeData nodeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.nodeData = nodeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(PanamaGLRenderingTarget target) {
        nodeData.init();
    }

    @Override
    public void dispose(PanamaGLRenderingTarget target) {
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
        return InstancedDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Instanced)";
    }

    @Override
    public boolean isAvailable(PanamaGLRenderingTarget target) {
        return InstancedDraw.isAvailable(engine);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
