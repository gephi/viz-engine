package org.gephi.viz.engine.jogl.pipeline.instanced;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_FLOAT;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.jogl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.common.AbstractEdgeData;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndex;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.util.BufferUtils;
import org.gephi.viz.engine.jogl.util.ManagedDirectBuffer;
import org.gephi.viz.engine.jogl.util.gl.GLBufferMutable;

/**
 *
 * @author Eduardo Ramos
 */
public class InstancedEdgeData extends AbstractEdgeData {

    private IntBuffer bufferName;

    private static final int VERT_BUFFER_UNDIRECTED = 0;
    private static final int VERT_BUFFER_DIRECTED = 1;
    private static final int ATTRIBS_BUFFER = 2;

    public InstancedEdgeData() {
        super(true);
    }

    public void init(GL2ES3 gl) {
        super.init(gl);
        initBuffers(gl);
    }

    public void update(VizEngine engine, GraphIndexImpl graphIndex) {
        updateData(
                graphIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class)
        );
    }

    public void drawInstanced(GL2ES3 gl, RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        GraphRenderingOptions renderingOptions = engine.getLookup().lookup(GraphRenderingOptions.class);

        final float[] backgroundColorFloats = engine.getBackgroundColor();
        final float edgeScale = renderingOptions.getEdgeScale();
        float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();

        final GraphIndex graphIndex = engine.getLookup().lookup(GraphIndex.class);

        final float minWeight = graphIndex.getEdgesMinWeight();
        final float maxWeight = graphIndex.getEdgesMaxWeight();

        drawUndirected(engine, layer, gl, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
        drawDirected(engine, layer, gl, mvpFloats, backgroundColorFloats, lightenNonSelectedFactor, edgeScale, minWeight, maxWeight);
    }

    private void drawUndirected(VizEngine engine, RenderingLayer layer, GL2ES3 gl, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
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
            setupUndirectedVertexArrayAttributes(engine, gl);
            lineModelUndirected.drawInstanced(gl, mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset, edgeScale, minWeight, maxWeight);
            unsetupUndirectedVertexArrayAttributes(gl);
        }
    }

    private void drawDirected(VizEngine engine, RenderingLayer layer, GL2ES3 gl, float[] mvpFloats, float[] backgroundColorFloats, float lightenNonSelectedFactor, float edgeScale, float minWeight, float maxWeight) {
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
            setupDirectedVertexArrayAttributes(engine, gl);
            lineModelDirected.drawInstanced(gl, mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset, edgeScale, minWeight, maxWeight);
            unsetupDirectedVertexArrayAttributes(gl);
        }
    }

    //Triple buffering to ensure CPU and GPU don't access the same buffer at the same time:
    private static final int NUM_BUFFERS = 3;
    private int currentBufferIndex = 0;
    private final ManagedDirectBuffer[] attributesBuffersList = new ManagedDirectBuffer[NUM_BUFFERS];

    private float[] attributesBufferBatch;
    private static final int BATCH_EDGES_SIZE = 32768;

    private void initBuffers(GL2ES3 gl) {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_EDGES_SIZE];

        bufferName = GLBuffers.newDirectIntBuffer(3);

        gl.glGenBuffers(bufferName.capacity(), bufferName);
        {
            final FloatBuffer undirectedVertexData = GLBuffers.newDirectFloatBuffer(EdgeLineModelUndirected.getVertexData());
            vertexGLBufferUndirected = new GLBufferMutable(bufferName.get(VERT_BUFFER_UNDIRECTED), GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferUndirected.bind(gl);
            vertexGLBufferUndirected.init(gl, undirectedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferUndirected.unbind(gl);
            BufferUtils.destroyDirectBuffer(undirectedVertexData);
        }

        {
            final FloatBuffer directedVertexData = GLBuffers.newDirectFloatBuffer(EdgeLineModelDirected.getVertexData());
            vertexGLBufferDirected = new GLBufferMutable(bufferName.get(VERT_BUFFER_DIRECTED), GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBufferDirected.bind(gl);
            vertexGLBufferDirected.init(gl, directedVertexData, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBufferDirected.unbind(gl);
            BufferUtils.destroyDirectBuffer(directedVertexData);
        }

        //Initialize for batch edges size:
        attributesGLBuffer = new GLBufferMutable(bufferName.get(ATTRIBS_BUFFER), GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind(gl);
        attributesGLBuffer.init(gl, ATTRIBS_STRIDE * Float.BYTES * BATCH_EDGES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind(gl);

        for (int i = 0; i < NUM_BUFFERS; i++) {
            attributesBuffersList[i] = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_EDGES_SIZE);
        }
    }

    public void updateBuffers(GL2ES3 gl) {
        attributesGLBuffer.bind(gl);
        attributesGLBuffer.update(gl, attributesBuffersList[currentBufferIndex].floatBuffer());
        attributesGLBuffer.unbind(gl);

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
        final ManagedDirectBuffer attributesBuffer = attributesBuffersList[nextBufferIndex];

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

        currentBufferIndex = nextBufferIndex;
    }

    @Override
    public void dispose(GL gl) {
        super.dispose(gl);
        attributesBufferBatch = null;

        for (ManagedDirectBuffer buffer : attributesBuffersList) {
            if (buffer != null) {
                buffer.destroy();
            }
        }
    }
}
