package org.gephi.viz.engine.lwjgl.pipeline.instanced;

import java.nio.FloatBuffer;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndex;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.lwjgl.system.MemoryStack;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.glGenBuffers;

/**
 * @author Eduardo Ramos
 */
public class InstancedEdgeData extends AbstractEdgeData {

    private final int[] bufferName = new int[6];

    private static final int VERT_BUFFER_UNDIRECTED = 0;
    private static final int VERT_BUFFER_DIRECTED = 1;
    private static final int ATTRIBS_BUFFER_UNDIRECTED = 2;
    private static final int ATTRIBS_BUFFER_UNDIRECTED_SECONDARY = 3;
    private static final int ATTRIBS_BUFFER_DIRECTED = 4;
    private static final int ATTRIBS_BUFFER_DIRECTED_SECONDARY = 5;
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

        final float[] backgroundColorFloats = engine.getBackgroundColor();
        final float edgeScale = renderingOptions.getEdgeScale();
        float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();

        final GraphIndex graphIndex = engine.getLookup().lookup(GraphIndex.class);

        final float minWeight = graphIndex.getEdgesMinWeight();
        final float maxWeight = graphIndex.getEdgesMaxWeight();

        drawUndirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
        drawDirected(engine, layer, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
    }

    private void drawUndirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
        final int instanceCount;
        final float colorLightenFactor;

        if (layer == RenderingLayer.BACK) {
            instanceCount = undirectedInstanceCounter.unselectedCountToDraw;
            colorLightenFactor = lightenNonSelectedFactor;
        } else {
            instanceCount = undirectedInstanceCounter.selectedCountToDraw;
            colorLightenFactor = 0;
        }

        if (instanceCount > 0) {
            if (layer == RenderingLayer.BACK) {
                setupUndirectedVertexArrayAttributesSecondary(engine);
            } else {
                setupUndirectedVertexArrayAttributes(engine);
            }
            lineModelUndirected.drawInstanced(mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, edgeScale, minWeight, maxWeight);
            unsetupUndirectedVertexArrayAttributes();
        }
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
        final int instanceCount;
        final float colorLightenFactor;

        if (layer == RenderingLayer.BACK) {
            instanceCount = directedInstanceCounter.unselectedCountToDraw;
            colorLightenFactor = lightenNonSelectedFactor;
        } else {
            instanceCount = directedInstanceCounter.selectedCountToDraw;
            colorLightenFactor = 0;
        }

        if (instanceCount > 0) {
            if (layer == RenderingLayer.BACK) {
                setupDirectedVertexArrayAttributesSecondary(engine);
            } else {
                setupDirectedVertexArrayAttributes(engine);
            }
            lineModelDirected.drawInstanced(mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, edgeScale, minWeight, maxWeight);
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
        attributesGLBufferDirected = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_DIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferDirected.bind();
        attributesGLBufferDirected.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferDirected.unbind();

        attributesGLBufferDirectedSecondary = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_DIRECTED_SECONDARY], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferDirectedSecondary.bind();
        attributesGLBufferDirectedSecondary.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferDirectedSecondary.unbind();

        attributesGLBufferUndirected = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_UNDIRECTED], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferUndirected.bind();
        attributesGLBufferUndirected.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferUndirected.unbind();

        attributesGLBufferUndirectedSecondary = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_UNDIRECTED_SECONDARY], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferUndirectedSecondary.bind();
        attributesGLBufferUndirectedSecondary.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferUndirectedSecondary.unbind();

        attributesBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_EDGES_SIZE);
    }

    public void updateBuffers() {
        int offset = 0;
        final FloatBuffer buf = attributesBuffer.floatBuffer();

        buf.limit(undirectedInstanceCounter.unselectedCount * ATTRIBS_STRIDE);
        buf.position(0);

        attributesGLBufferUndirectedSecondary.bind();
        attributesGLBufferUndirectedSecondary.updateWithOrphaning(buf);
        attributesGLBufferUndirectedSecondary.unbind();

        offset = buf.limit();
        buf.limit(offset + undirectedInstanceCounter.selectedCount * ATTRIBS_STRIDE);
        buf.position(offset);

        attributesGLBufferUndirected.bind();
        attributesGLBufferUndirected.updateWithOrphaning(buf);
        attributesGLBufferUndirected.unbind();

        offset = buf.limit();
        buf.limit(offset + directedInstanceCounter.unselectedCount * ATTRIBS_STRIDE);
        buf.position(offset);

        attributesGLBufferDirectedSecondary.bind();
        attributesGLBufferDirectedSecondary.updateWithOrphaning(buf);
        attributesGLBufferDirectedSecondary.unbind();

        offset = buf.limit();
        buf.limit(offset + directedInstanceCounter.selectedCount * ATTRIBS_STRIDE);
        buf.position(offset);

        attributesGLBufferDirected.bind();
        attributesGLBufferDirected.updateWithOrphaning(buf);
        attributesGLBufferDirected.unbind();

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
