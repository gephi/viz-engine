package org.gephi.viz.engine.lwjgl;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.pipeline.DefaultLWJGLEventListener;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers.NodeRendererArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.updaters.NodesUpdaterArrayDrawRendering;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.IndirectNodeData;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.renderers.NodeRendererIndirect;
import org.gephi.viz.engine.lwjgl.pipeline.indirect.updaters.NodesUpdaterIndirectRendering;
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
public class VizEngineLWJGLConfigurator implements VizEngineConfigurator<LWJGLRenderingTarget, LWJGLInputEvent> {

    @Override
    public void configure(VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
        final GraphIndexImpl graphIndex = new GraphIndexImpl(engine);
        final GraphSelection graphSelection = new GraphSelectionImpl(engine);
        final GraphSelectionNeighbours graphSelectionNeighbours = new GraphSelectionNeighboursImpl(engine);
        final GraphRenderingOptionsImpl renderingOptions = new GraphRenderingOptionsImpl();

        engine.addToLookup(graphIndex);
        engine.addToLookup(graphSelection);
        engine.addToLookup(graphSelectionNeighbours);
        engine.addToLookup(renderingOptions);

        setupIndirectRendering(engine, graphIndex);
        setupInstancedRendering(engine, graphIndex);
        setupVertexArrayRendering(engine, graphIndex);

        setupInputListeners(engine);
    }

    private void setupIndirectRendering(VizEngine engine, GraphIndexImpl graphIndex) {
        //Only nodes supported, edges don't have a LOD to benefit from
        final IndirectNodeData nodeData = new IndirectNodeData();

        engine.addRenderer(new NodeRendererIndirect(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterIndirectRendering(engine, nodeData, graphIndex));
    }

    private void setupInstancedRendering(VizEngine engine, GraphIndexImpl graphIndex) {
        //Nodes:
        //TODO

        //Edges:
        //TODO
    }

    private void setupVertexArrayRendering(VizEngine engine, GraphIndexImpl graphIndex) {
        //Nodes:
        final ArrayDrawNodeData nodeData = new ArrayDrawNodeData();
        engine.addRenderer(new NodeRendererArrayDraw(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterArrayDrawRendering(engine, nodeData, graphIndex));

        //Edges:
        //TODO
    }

    private void setupInputListeners(VizEngine engine) {
        engine.addInputListener(new DefaultLWJGLEventListener(engine));
    }
}
