package org.gephi.viz.engine.pipeline.arrays.updaters;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.ArrayDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterArrayDrawRendering implements WorldUpdater {

    private final VizEngine engine;
    private final ArrayDrawNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterArrayDrawRendering(VizEngine engine, ArrayDrawNodeData nodeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.nodeData = nodeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        nodeData.init(drawable.getGL().getGL2ES2());
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        nodeData.dispose(drawable.getGL().getGL2ES2());
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return ArrayDraw.isAvailable(engine, drawable);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
