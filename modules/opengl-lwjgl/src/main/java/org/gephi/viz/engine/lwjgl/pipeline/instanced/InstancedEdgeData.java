package org.gephi.viz.engine.lwjgl.pipeline.instanced;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

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

    public InstancedEdgeData() {
        super(true, true);
    }

    public void update(VizEngine engine, GraphIndexImpl graphIndex) {
        updateData(
            graphIndex,
            engine.getLookup().lookup(GraphRenderingOptions.class),
            engine.getLookup().lookup(GraphSelection.class)
        );
    }

    public void drawInstanced(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        drawUndirected(engine, layer, mvpFloats);
        drawDirected(engine, layer, mvpFloats);
    }

    private void drawUndirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats) {
        final int instanceCount = setupShaderProgramForRenderingLayerUndirected(layer, engine, mvpFloats);

        lineModelUndirected.drawInstanced(instanceCount);
        lineModelUndirected.stopUsingProgram();
        unsetupUndirectedVertexArrayAttributes();
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, float[] mvpFloats) {
        final int instanceCount = setupShaderProgramForRenderingLayerDirected(layer, engine, mvpFloats);

        lineModelDirected.drawInstanced(instanceCount);
        lineModelDirected.stopUsingProgram();
        unsetupDirectedVertexArrayAttributes();
    }

    @Override
    protected void initBuffers() {
        super.initBuffers();
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
    }

    public void updateBuffers() {
        final FloatBuffer buf = attributesBuffer.floatBuffer();

        buf.limit(undirectedInstanceCounter.unselectedCount * ATTRIBS_STRIDE);
        buf.position(0);

        attributesGLBufferUndirectedSecondary.bind();
        attributesGLBufferUndirectedSecondary.updateWithOrphaning(buf);
        attributesGLBufferUndirectedSecondary.unbind();

        int offset = buf.limit();
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
    }

    private void updateData(final GraphIndexImpl graphIndex, final GraphRenderingOptions renderingOptions, final GraphSelection graphSelection) {
        if (!renderingOptions.isShowEdges()) {
            undirectedInstanceCounter.clearCount();
            directedInstanceCounter.clearCount();
            return;
        }

        graphIndex.indexEdges();

        //Selection:
        final boolean someSelection = graphSelection.someNodesOrEdgesSelection();
        final float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();
        final boolean hideNonSelected = someSelection && (renderingOptions.isHideNonSelected() || lightenNonSelectedFactor >= 1);
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
            someSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
            attributesBufferBatch, 0, attribsDirectBuffer
        );
        updateDirectedData(
            graph,
            someSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor,
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
