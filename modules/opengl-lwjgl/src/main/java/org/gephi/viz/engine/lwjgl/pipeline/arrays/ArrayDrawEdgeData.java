package org.gephi.viz.engine.lwjgl.pipeline.arrays;

import java.nio.FloatBuffer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndex;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.util.ArrayUtils;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDrawEdgeData extends AbstractEdgeData {

    private final int[] bufferName = new int[3];

    private static final int VERT_BUFFER_UNDIRECTED = 0;
    private static final int VERT_BUFFER_DIRECTED = 1;
    private static final int ATTRIBS_BUFFER = 2;

    public ArrayDrawEdgeData() {
        super(false);
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

    public void drawArrays(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        GraphRenderingOptions renderingOptions = engine.getLookup().lookup(GraphRenderingOptions.class);

        final float[] backgroundColorFloats = engine.getBackgroundColor();
        final float edgeScale = renderingOptions.getEdgeScale();
        float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();

        final GraphIndex graphIndex = engine.getLookup().lookup(GraphIndex.class);

        final float minWeight = graphIndex.getEdgesMinWeight();
        final float maxWeight = graphIndex.getEdgesMaxWeight();

        drawUndirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
        drawDirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
    }

    private float[] currentAttributesBuffer;

    private void drawUndirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
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
            lineModelUndirected.useProgram(mvpFloats, backgroundColorFloats, colorLightenFactor, edgeScale, minWeight, maxWeight);

            final FloatBuffer batchUpdateBuffer = attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.floatBuffer();

            final int maxIndex = (instancesOffset + instanceCount);
            for (int edgeBase = instancesOffset; edgeBase < maxIndex; edgeBase += BATCH_EDGES_SIZE) {
                final int drawBatchCount = Math.min(maxIndex - edgeBase, BATCH_EDGES_SIZE);

                //Need to copy attributes as many times as vertex per model:
                for (int edgeIndex = 0; edgeIndex < drawBatchCount; edgeIndex++) {
                    System.arraycopy(
                            currentAttributesBuffer, (edgeBase + edgeIndex) * ATTRIBS_STRIDE,
                            attributesDrawBufferBatchOneCopyPerVertex, edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED,
                            ATTRIBS_STRIDE
                    );

                    ArrayUtils.repeat(
                            attributesDrawBufferBatchOneCopyPerVertex,
                            edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED,
                            ATTRIBS_STRIDE,
                            VERTEX_COUNT_UNDIRECTED
                    );
                }

                batchUpdateBuffer.put(attributesDrawBufferBatchOneCopyPerVertex, 0, drawBatchCount * ATTRIBS_STRIDE * VERTEX_COUNT_UNDIRECTED);
                batchUpdateBuffer.rewind();

                attributesGLBuffer.bind();
                attributesGLBuffer.update(batchUpdateBuffer);
                attributesGLBuffer.unbind();

                lineModelUndirected.drawArraysMultipleInstance(drawBatchCount);
            }

            lineModelUndirected.stopUsingProgram();
            unsetupUndirectedVertexArrayAttributes();
        }
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
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
            lineModelDirected.useProgram(mvpFloats, backgroundColorFloats, colorLightenFactor, edgeScale, minWeight, maxWeight);

            final FloatBuffer batchUpdateBuffer = attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.floatBuffer();

            final int maxIndex = (instancesOffset + instanceCount);
            for (int edgeBase = instancesOffset; edgeBase < maxIndex; edgeBase += BATCH_EDGES_SIZE) {
                final int drawBatchCount = Math.min(maxIndex - edgeBase, BATCH_EDGES_SIZE);

                //Need to copy attributes as many times as vertex per model:
                for (int edgeIndex = 0; edgeIndex < drawBatchCount; edgeIndex++) {
                    System.arraycopy(
                            currentAttributesBuffer, (edgeBase + edgeIndex) * ATTRIBS_STRIDE,
                            attributesDrawBufferBatchOneCopyPerVertex, edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED,
                            ATTRIBS_STRIDE
                    );

                    ArrayUtils.repeat(
                            attributesDrawBufferBatchOneCopyPerVertex,
                            edgeIndex * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED,
                            ATTRIBS_STRIDE,
                            VERTEX_COUNT_DIRECTED
                    );
                }

                batchUpdateBuffer.put(attributesDrawBufferBatchOneCopyPerVertex, 0, drawBatchCount * ATTRIBS_STRIDE * VERTEX_COUNT_DIRECTED);
                batchUpdateBuffer.rewind();

                attributesGLBuffer.bind();
                attributesGLBuffer.update(batchUpdateBuffer);
                attributesGLBuffer.unbind();

                lineModelDirected.drawArraysMultipleInstance(drawBatchCount);
            }

            lineModelDirected.stopUsingProgram();
            unsetupDirectedVertexArrayAttributes();
        }
    }

    //Triple buffering to ensure CPU and GPU don't access the same buffer at the same time:
    private static final int NUM_BUFFERS = 3;
    private int currentBufferIndex = 0;
    private final float[][] attributesBuffersList = new float[NUM_BUFFERS][];

    private static final int BATCH_EDGES_SIZE = 65536;

    //For drawing in a loop:
    private float[] attributesDrawBufferBatchOneCopyPerVertex;
    private ManagedDirectBuffer attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer;

    private void initBuffers() {
        attributesDrawBufferBatchOneCopyPerVertex = new float[ATTRIBS_STRIDE * VERTEX_COUNT_MAX * BATCH_EDGES_SIZE];//Need to copy attributes as many times as vertex per model
        attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * VERTEX_COUNT_MAX * BATCH_EDGES_SIZE);

        glGenBuffers(bufferName);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            float[] singleElementData = EdgeLineModelUndirected.getVertexData();
            float[] undirectedVertexDataArray = new float[singleElementData.length * BATCH_EDGES_SIZE];
            System.arraycopy(singleElementData, 0, undirectedVertexDataArray, 0, singleElementData.length);
            ArrayUtils.repeat(undirectedVertexDataArray, 0, singleElementData.length, BATCH_EDGES_SIZE);

            final FloatBuffer undirectedVertexData = stack.floats(undirectedVertexDataArray);
            vertexGLBufferUndirected = new GLBufferMutable(bufferName[VERT_BUFFER_UNDIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferUndirected.bind();
            vertexGLBufferUndirected.init(undirectedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferUndirected.unbind();
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            float[] singleElementData = EdgeLineModelDirected.getVertexData();
            float[] directedVertexDataArray = new float[singleElementData.length * BATCH_EDGES_SIZE];
            System.arraycopy(singleElementData, 0, directedVertexDataArray, 0, singleElementData.length);
            ArrayUtils.repeat(directedVertexDataArray, 0, singleElementData.length, BATCH_EDGES_SIZE);

            final FloatBuffer directedVertexData = stack.floats(directedVertexDataArray);
            vertexGLBufferDirected = new GLBufferMutable(bufferName[VERT_BUFFER_DIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferDirected.bind();
            vertexGLBufferDirected.init(directedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferDirected.unbind();
        }

        //Initialize for batch edges size:
        attributesGLBuffer = new GLBufferMutable(bufferName[ATTRIBS_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind();
        attributesGLBuffer.init(VERTEX_COUNT_MAX * ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind();

        for (int i = 0; i < NUM_BUFFERS; i++) {
            attributesBuffersList[i] = new float[ATTRIBS_STRIDE * BATCH_EDGES_SIZE];
        }
    }

    public void updateBuffers() {
        currentAttributesBuffer = attributesBuffersList[currentBufferIndex];
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

        final byte nextBufferIndex = (byte) ((currentBufferIndex + 1) % 3);

        final float[] attribs
                = attributesBuffersList[nextBufferIndex]
                = ArrayUtils.ensureCapacityNoCopy(attributesBuffersList[nextBufferIndex], totalEdges * ATTRIBS_STRIDE);

        graphIndex.getVisibleEdges(edgesCallback);

        final Edge[] visibleEdgesArray = edgesCallback.getEdgesArray();
        final int visibleEdgesCount = edgesCallback.getCount();

        final Graph graph = graphIndex.getGraph();

        int attribsIndex = 0;
        attribsIndex = updateUndirectedData(
                graph,
                someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray,
                graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attribs, attribsIndex
        );
        updateDirectedData(
                graph, someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray,
                graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
                attribs, attribsIndex
        );

        currentBufferIndex = nextBufferIndex;
    }

    @Override
    public void dispose() {
        super.dispose();
        attributesDrawBufferBatchOneCopyPerVertex = null;
        attributesDrawBufferBatchOneCopyPerVertexManagedDirectBuffer.destroy();

        for (int i = 0; i < attributesBuffersList.length; i++) {
            attributesBuffersList[i] = null;
        }
    }
}
