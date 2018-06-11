package org.gephi.viz.engine.pipeline.indirect.updaters;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.IndirectDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterIndirectRendering implements WorldUpdater {

    private final VizEngine engine;
    private final IndirectNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterIndirectRendering(VizEngine engine, IndirectNodeData nodeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.nodeData = nodeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        nodeData.init(drawable.getGL().getGL4());
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        nodeData.dispose(drawable.getGL().getGL4());
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return IndirectDraw.isAvailable(engine, drawable);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
