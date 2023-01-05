package org.gephi.viz.engine.lwjgl;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.pipeline.DefaultLWJGLEventListener;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawEdgeData;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.ArrayDrawNodeData;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers.EdgeRendererArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers.NodeRendererArrayDraw;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.updaters.EdgesUpdaterArrayDrawRendering;
import org.gephi.viz.engine.lwjgl.pipeline.arrays.updaters.NodesUpdaterArrayDrawRendering;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedEdgeData;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.InstancedNodeData;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.renderers.EdgeRendererInstanced;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.renderers.NodeRendererInstanced;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.updaters.EdgesUpdaterInstancedRendering;
import org.gephi.viz.engine.lwjgl.pipeline.instanced.updaters.NodesUpdaterInstancedRendering;
import org.gephi.viz.engine.spi.VizEngineConfigurator;
import org.gephi.viz.engine.status.GraphRenderingOptionsImpl;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionImpl;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.status.GraphSelectionNeighboursImpl;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.util.gl.OpenGLOptions;

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
        final OpenGLOptions openGLOptions = new OpenGLOptions();

        engine.addToLookup(graphIndex);
        engine.addToLookup(graphSelection);
        engine.addToLookup(graphSelectionNeighbours);
        engine.addToLookup(renderingOptions);
        engine.addToLookup(openGLOptions);

        setupInstancedRendering(engine, graphIndex);//Preferred and better performance
        setupVertexArrayRendering(engine, graphIndex);//Fallback for very old versions of OpenGL

        setupInputListeners(engine);
    }

    private void setupInstancedRendering(VizEngine engine, GraphIndexImpl graphIndex) {
        //Nodes:
        final InstancedNodeData nodeData = new InstancedNodeData();
        engine.addRenderer(new NodeRendererInstanced(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterInstancedRendering(engine, nodeData, graphIndex));

        //Edges:
        final InstancedEdgeData instancedEdgeData = new InstancedEdgeData();

        engine.addRenderer(new EdgeRendererInstanced(engine, instancedEdgeData));
        engine.addWorldUpdater(new EdgesUpdaterInstancedRendering(engine, instancedEdgeData, graphIndex));
    }

    private void setupVertexArrayRendering(VizEngine engine, GraphIndexImpl graphIndex) {
        //Nodes:
        final ArrayDrawNodeData nodeData = new ArrayDrawNodeData();
        engine.addRenderer(new NodeRendererArrayDraw(engine, nodeData));
        engine.addWorldUpdater(new NodesUpdaterArrayDrawRendering(engine, nodeData, graphIndex));

        //Edges:
        final ArrayDrawEdgeData edgeData = new ArrayDrawEdgeData();
        engine.addRenderer(new EdgeRendererArrayDraw(engine, edgeData));
        engine.addWorldUpdater(new EdgesUpdaterArrayDrawRendering(engine, edgeData, graphIndex));
    }

    private void setupInputListeners(VizEngine engine) {
        engine.addInputListener(new DefaultLWJGLEventListener(engine));
    }
}
