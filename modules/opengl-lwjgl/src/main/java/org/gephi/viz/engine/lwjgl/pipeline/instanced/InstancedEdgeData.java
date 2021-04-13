package org.gephi.viz.engine.lwjgl.pipeline.instanced;

import java.nio.FloatBuffer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndex;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.glGenBuffers;

import org.gephi.viz.engine.util.TimeUtils;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Eduardo Ramos
 */
public class InstancedEdgeData extends AbstractEdgeData {

    private final int[] bufferName = new int[3];

    private static final int VERT_BUFFER_UNDIRECTED = 0;
    private static final int VERT_BUFFER_DIRECTED = 1;
    private static final int ATTRIBS_BUFFER = 2;

    public InstancedEdgeData() {
        super(true);
    }

    @Override
    public void init() {
        super.init();
        initBuffers();
    }

    public void update(VizEngine engine, GraphIndexImpl graphIndex) {
        updateData(
                graphIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class)
        );
    }

    public void drawInstanced(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        GraphRenderingOptions renderingOptions = engine.getLookup().lookup(GraphRenderingOptions.class);
        float globalTime = TimeUtils.getFloatSecondGlobalTime();
        final float[] backgroundColorFloats = engine.getBackgroundColor();
        final float edgeScale = renderingOptions.getEdgeScale();
        float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();

        final GraphIndex graphIndex = engine.getLookup().lookup(GraphIndex.class);

        final float minWeight = graphIndex.getEdgesMinWeight();
        final float maxWeight = graphIndex.getEdgesMaxWeight();

        drawUndirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight, globalTime);
        drawDirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight, globalTime);
    }

    private void drawUndirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight, float globalTime) {
        final int instanceCount;
        final int instancesOffset;
        final float colorLightenFactor;

        if (layer == RenderingLayer.BACK) {
            instanceCount = undirectedInstanceCounter.unselectedCountToDraw;
            instancesOffset = 0;
            colorLightenFactor = lightenNonSelectedFactor;
        } else {
            instanceCount = undirectedInstanceCounter.selectedCountToDraw;
            instancesOffset = undirectedInstanceCounter.unselectedCountToDraw;
            colorLightenFactor = 0;
        }

        if (instanceCount > 0) {
            setupUndirectedVertexArrayAttributes(engine);
            lineModelUndirected.drawInstanced(mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset, edgeScale, minWeight, maxWeight, globalTime);
            unsetupUndirectedVertexArrayAttributes();
        }
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight, float globalTime) {
        final int instanceCount;
        final int instancesOffset;
        final float colorLightenFactor;

        if (layer == RenderingLayer.BACK) {
            instanceCount = directedInstanceCounter.unselectedCountToDraw;
            instancesOffset = undirectedInstanceCounter.totalToDraw();
            colorLightenFactor = lightenNonSelectedFactor;
        } else {
            instanceCount = directedInstanceCounter.selectedCountToDraw;
            instancesOffset = undirectedInstanceCounter.totalToDraw() + directedInstanceCounter.unselectedCountToDraw;
            colorLightenFactor = 0;
        }

        if (instanceCount > 0) {
            setupDirectedVertexArrayAttributes(engine);
            lineModelDirected.drawInstanced(mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset, edgeScale, minWeight, maxWeight, globalTime);
            unsetupDirectedVertexArrayAttributes();
        }
    }

    private ManagedDirectBuffer attributesBuffer;

    private float[] attributesBufferBatch;
    private static final int BATCH_EDGES_SIZE = 32768;

    private void initBuffers() {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_EDGES_SIZE];

        glGenBuffers(bufferName);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer undirectedVertexData = stack.floats(EdgeLineModelUndirected.getVertexData());
            vertexGLBufferUndirected = new GLBufferMutable(bufferName[VERT_BUFFER_UNDIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferUndirected.bind();
            vertexGLBufferUndirected.init(undirectedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferUndirected.unbind();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer directedVertexData = stack.floats(EdgeLineModelDirected.getVertexData());
            vertexGLBufferDirected = new GLBufferMutable(bufferName[VERT_BUFFER_DIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferDirected.bind();
            vertexGLBufferDirected.init(directedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferDirected.unbind();
        }

        //Initialize for batch edges size:
        attributesGLBuffer = new GLBufferMutable(bufferName[ATTRIBS_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind();
        attributesGLBuffer.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind();

        attributesBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_EDGES_SIZE);
    }

    public void updateBuffers() {
        attributesGLBuffer.bind();
        attributesGLBuffer.updateWithOrphaning(attributesBuffer.floatBuffer());
        attributesGLBuffer.unbind();

        undirectedInstanceCounter.promoteCountToDraw();
        directedInstanceCounter.promoteCountToDraw();
        //TODO: Persistent buffer if available?
    }

    private void updateData(final GraphIndexImpl graphIndex, final GraphRenderingOptions renderingOptions, final GraphSelection graphSelection) {
        if (!renderingOptions.isShowEdges()) {
            undirectedInstanceCounter.clearCount();
            directedInstanceCounter.clearCount();
            return;
        }

        graphIndex.indexEdges();

        //Selection:
        final boolean someEdgesSelection = graphSelection.getSelectedEdgesCount() > 0;
        final boolean someNodesSelection = graphSelection.getSelectedNodesCount() > 0;
        final float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();
        final boolean hideNonSelected = someEdgesSelection && (renderingOptions.isHideNonSelected() || lightenNonSelectedFactor >= 1);
        final boolean edgeSelectionColor = renderingOptions.isEdgeSelectionColor();
        final float edgeBothSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeBothSelectionColor().getRGB());
        final float edgeInSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeInSelectionColor().getRGB());
        final float edgeOutSelectionColor = Float.intBitsToFloat(renderingOptions.getEdgeOutSelectionColor().getRGB());

        final int totalEdges = graphIndex.getEdgeCount();

        attributesBuffer.ensureCapacity(totalEdges * ATTRIBS_STRIDE);

        final FloatBuffer attribsDirectBuffer = attributesBuffer.floatBuffer();

        graphIndex.getVisibleEdges(edgesCallback);

        final Edge[] visibleEdgesArray = edgesCallback.getEdgesArray();
        final int visibleEdgesCount = edgesCallback.getCount();

        final Graph graph = graphIndex.getGraph();

        updateUndirectedData(
                graph,
                someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attributesBufferBatch, 0, attribsDirectBuffer
        );
        updateDirectedData(
                graph,
                someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attributesBufferBatch, 0, attribsDirectBuffer
        );
    }

    @Override
    public void dispose() {
        super.dispose();
        attributesBufferBatch = null;

        if (attributesBuffer != null) {
            attributesBuffer.destroy();
            attributesBuffer = null;
        }
    }
}
