package org.gephi.viz.engine.lwjgl.pipeline.indirect.updaters;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.availability.IndirectDraw;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterIndirectRendering implements WorldUpdater<LWJGLRenderingTarget> {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterIndirectRendering(VizEngine engine, IndirectNodeData nodeData, GraphIndexImpl spatialIndex) {
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
        return IndirectDraw.getPreferenceInCategory();
    }

    @Override
    public String getName() {
        return "Nodes (Indirect)";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return IndirectDraw.isAvailable(engine);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
