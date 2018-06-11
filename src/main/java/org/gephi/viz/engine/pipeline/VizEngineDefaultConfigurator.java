package org.gephi.viz.engine.pipeline;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.pipeline.arrays.renderers.EdgeRendererArrayDraw;
import org.gephi.viz.engine.pipeline.arrays.renderers.NodeRendererArrayDraw;
import org.gephi.viz.engine.pipeline.arrays.updaters.EdgesUpdaterArrayDrawRendering;
import org.gephi.viz.engine.pipeline.arrays.updaters.NodesUpdaterArrayDrawRendering;
import org.gephi.viz.engine.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.pipeline.indirect.renderers.NodeRendererIndirect;
import org.gephi.viz.engine.pipeline.indirect.updaters.NodesUpdaterIndirectRendering;
import org.gephi.viz.engine.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.pipeline.instanced.renderers.EdgeRendererInstanced;
import org.gephi.viz.engine.pipeline.instanced.renderers.NodeRendererInstanced;
import org.gephi.viz.engine.pipeline.instanced.updaters.EdgesUpdaterInstancedRendering;
import org.gephi.viz.engine.pipeline.instanced.updaters.NodesUpdaterInstancedRendering;
import org.gephi.viz.engine.spi.VizEngineConfigurator;
import org.gephi.viz.engine.status.GraphRenderingOptionsImpl;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionImpl;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.status.GraphSelectionNeighboursImpl;
import org.gephi.viz.engine.structure.GraphIndexImpl;

/**
 *
 * @author Eduardo Ramos
 */
public class VizEngineDefaultConfigurator implements VizEngineConfigurator {

    @Override
    public void configure(VizEngine engine) {
        final GraphIndexImpl spatialIndex = new GraphIndexImpl(engine);
        final GraphSelection graphSelection = new GraphSelectionImpl(engine);
        final GraphSelectionNeighbours graphSelectionNeighbours = new GraphSelectionNeighboursImpl(engine);
        final GraphRenderingOptionsImpl renderingOptions = new GraphRenderingOptionsImpl();

        engine.addToLookup(spatialIndex);
        engine.addToLookup(graphSelection);
        engine.addToLookup(graphSelectionNeighbours);
        engine.addToLookup(renderingOptions);

        setupIndirectRendering(engine, spatialIndex);
        setupInstancedRendering(engine, spatialIndex);
        setupVertexArrayRendering(engine, spatialIndex);

        setupInputListeners(engine);
    }

    private void setupIndirectRendering(VizEngine engine, GraphIndexImpl spatialIndex) {
        //Only nodes supported, edges don't have a LOD to benefit from
        final IndirectNodeData nodeData = new IndirectNodeData();

        engine.addRenderer(new NodeRendererIndirect(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterIndirectRendering(engine, nodeData, spatialIndex));
    }

    private void setupInstancedRendering(VizEngine engine, GraphIndexImpl spatialIndex) {
        //Nodes:
        final InstancedNodeData nodeData = new InstancedNodeData();
        engine.addRenderer(new NodeRendererInstanced(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterInstancedRendering(engine, nodeData, spatialIndex));

        //Edges:
        final InstancedEdgeData indirectEdgeData = new InstancedEdgeData();

        engine.addRenderer(new EdgeRendererInstanced(engine, indirectEdgeData));
        engine.addWorldUpdater(new EdgesUpdaterInstancedRendering(engine, indirectEdgeData, spatialIndex));
    }

    private void setupVertexArrayRendering(VizEngine engine, GraphIndexImpl spatialIndex) {
        //Nodes:
        final ArrayDrawNodeData nodeData = new ArrayDrawNodeData();
        engine.addRenderer(new NodeRendererArrayDraw(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterArrayDrawRendering(engine, nodeData, spatialIndex));

        //Edges:
        final ArrayDrawEdgeData edgeData = new ArrayDrawEdgeData();
        engine.addRenderer(new EdgeRendererArrayDraw(engine, edgeData));
        engine.addWorldUpdater(new EdgesUpdaterArrayDrawRendering(engine, edgeData, spatialIndex));
    }

    private void setupInputListeners(VizEngine engine) {
        engine.addInputListener(new DefaultEventListener(engine));
    }
}
