package org.gephi.viz.engine.pipeline.instanced.updaters;

import com.jogamp.opengl.GLAutoDrawable;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.availability.InstancedDraw;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class NodesUpdaterInstancedRendering implements WorldUpdater {

    private final VizEngine engine;
    private final InstancedNodeData nodeData;
    private final GraphIndexImpl spatialIndex;

    public NodesUpdaterInstancedRendering(VizEngine engine, InstancedNodeData nodeData, GraphIndexImpl spatialIndex) {
        this.engine = engine;
        this.nodeData = nodeData;
        this.spatialIndex = spatialIndex;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        nodeData.init(drawable.getGL().getGL2ES3());
    }
    
    @Override
    public void dispose(GLAutoDrawable drawable) {
        nodeData.dispose(drawable.getGL().getGL2ES3());
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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return InstancedDraw.isAvailable(engine, drawable);
    }

    @Override
    public int getOrder() {
        return 0;
    }

}
