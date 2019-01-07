package org.gephi.viz.engine.lwjgl.pipeline.instanced;

import java.nio.FloatBuffer;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.lwjgl.models.NodeDiskModel;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGenBuffers;
import org.lwjgl.system.MemoryStack;

/**
 *
 * @author Eduardo Ramos
 */
public class InstancedNodeData extends AbstractNodeData {

    private final NodeDiskModel diskModel64;
    private final NodeDiskModel diskModel32;
    private final NodeDiskModel diskModel16;
    private final NodeDiskModel diskModel8;

    private final int firstVertex64;
    private final int firstVertex32;
    private final int firstVertex16;
    private final int firstVertex8;

    private final InstanceCounter instanceCounter = new InstanceCounter();
    private float maxNodeSize = 0;
    private float maxNodeSizeToDraw = 0;

    public InstancedNodeData() {
        super(true);
        diskModel64 = new NodeDiskModel(64);
        diskModel32 = new NodeDiskModel(32);
        diskModel16 = new NodeDiskModel(16);
        diskModel8 = new NodeDiskModel(8);

        firstVertex64 = 0;
        firstVertex32 = diskModel64.getVertexCount();
        firstVertex16 = firstVertex32 + diskModel32.getVertexCount();
        firstVertex8 = firstVertex16 + diskModel16.getVertexCount();
    }

    private final int[] bufferName = new int[2];

    private static final int VERT_BUFFER = 0;
    private static final int ATTRIBS_BUFFER = 1;

    public void init() {
        initBuffers();
        diskModel64.initGLPrograms();
        diskModel32.initGLPrograms();
        diskModel16.initGLPrograms();
        diskModel8.initGLPrograms();
    }

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(
                spatialIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class),
                engine.getLookup().lookup(GraphSelectionNeighbours.class)
        );
    }

    public void drawInstanced(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        final float[] backgroundColorFloats = engine.getBackgroundColor();
        final float zoom = engine.getZoom();

        final int instanceCount;
        final int instancesOffset;
        final float colorLightenFactor;

        if (layer == RenderingLayer.BACK) {
            instanceCount = instanceCounter.unselectedCountToDraw * 2;
            instancesOffset = 0;
            colorLightenFactor = engine.getLookup().lookup(GraphRenderingOptions.class).getLightenNonSelectedFactor();
        } else {
            instanceCount = instanceCounter.selectedCountToDraw * 2;
            instancesOffset = instanceCounter.unselectedCountToDraw * 2;
            colorLightenFactor = 0;
        }

        if (instanceCount > 0) {
            final float maxObservedSize = maxNodeSizeToDraw * zoom;

            final NodeDiskModel diskModelToRender;
            final int firstVertex;
            if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_64) {
                diskModelToRender = diskModel64;
                firstVertex = firstVertex64;
            } else if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_32) {
                diskModelToRender = diskModel32;
                firstVertex = firstVertex32;
            } else if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_16) {
                diskModelToRender = diskModel16;
                firstVertex = firstVertex16;
            } else {
                diskModelToRender = diskModel8;
                firstVertex = firstVertex8;
            }

            setupVertexArrayAttributes(engine);
            diskModelToRender.drawInstanced(firstVertex, mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset);
            unsetupVertexArrayAttributes();
        }
    }

    //Triple buffering to ensure CPU and GPU don't access the same buffer at the same time:
    private static final int NUM_BUFFERS = 3;
    private int currentBufferIndex = 0;
    private final ManagedDirectBuffer[] attributesBuffersList = new ManagedDirectBuffer[NUM_BUFFERS];

    private float[] attributesBufferBatch;
    private static final int BATCH_NODES_SIZE = 32768;

    private void initBuffers() {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2];

        glGenBuffers(bufferName);

        final float[] circleVertexData = new float[diskModel64.getVertexData().length + diskModel32.getVertexData().length + diskModel16.getVertexData().length + +diskModel8.getVertexData().length];
        int offset = 0;
        System.arraycopy(diskModel64.getVertexData(), 0, circleVertexData, offset, diskModel64.getVertexData().length);
        offset += diskModel64.getVertexData().length;
        System.arraycopy(diskModel32.getVertexData(), 0, circleVertexData, offset, diskModel32.getVertexData().length);
        offset += diskModel32.getVertexData().length;
        System.arraycopy(diskModel16.getVertexData(), 0, circleVertexData, offset, diskModel16.getVertexData().length);
        offset += diskModel16.getVertexData().length;
        System.arraycopy(diskModel8.getVertexData(), 0, circleVertexData, offset, diskModel8.getVertexData().length);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer circleVertexBuffer = stack.floats(circleVertexData);

            vertexGLBuffer = new GLBufferMutable(bufferName[VERT_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBuffer.bind();
            vertexGLBuffer.init(circleVertexBuffer, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBuffer.unbind();
        }

        //Initialize for batch nodes size:
        attributesGLBuffer = new GLBufferMutable(bufferName[ATTRIBS_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind();
        attributesGLBuffer.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_NODES_SIZE * 2, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind();

        for (int i = 0; i < NUM_BUFFERS; i++) {
            attributesBuffersList[i] = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2);
        }
    }

    public void updateBuffers() {
        attributesGLBuffer.bind();
        attributesGLBuffer.update(attributesBuffersList[currentBufferIndex].floatBuffer());
        attributesGLBuffer.unbind();

        instanceCounter.promoteCountToDraw();
        maxNodeSizeToDraw = maxNodeSize;

        //TODO: send only changed parts? (glBufferSubData)
        //Persistent buffer if available?
    }

    private void updateData(final GraphIndexImpl spatialIndex, final GraphRenderingOptions renderingOptions, final GraphSelection selection, final GraphSelectionNeighbours neighboursSelection) {
        if (!renderingOptions.isShowNodes()) {
            instanceCounter.clearCount();
            return;
        }

        spatialIndex.indexNodes();

        //Selection:
        final boolean someSelection = selection.getSelectedNodesCount() > 0;
        final float lightenNonSelectedFactor = renderingOptions.getLightenNonSelectedFactor();
        final boolean hideNonSelected = someSelection && (renderingOptions.isHideNonSelected() || lightenNonSelectedFactor >= 1);

        final int totalNodes = spatialIndex.getNodeCount();

        final byte nextBufferIndex = (byte) ((currentBufferIndex + 1) % 3);
        final ManagedDirectBuffer attributesBuffer = attributesBuffersList[nextBufferIndex];

        attributesBuffer.ensureCapacity(totalNodes * ATTRIBS_STRIDE * 2);

        final FloatBuffer attribs = attributesBuffer.floatBuffer();

        spatialIndex.getVisibleNodes(nodesCallback);

        final Node[] visibleNodesArray = nodesCallback.getNodesArray();
        final int visibleNodesCount = nodesCallback.getCount();

        int newNodesCountUnselected = 0;
        int newNodesCountSelected = 0;

        float newMaxNodeSize = 0;

        int index = 0;
        if (someSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node) || neighboursSelection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;
                    newMaxNodeSize = Math.max(newMaxNodeSize, node.size());

                    index = fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }
                }
            } else {
                //First non-selected (bottom):
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node) || neighboursSelection.isNodeSelected(node);
                    if (selected) {
                        continue;
                    }

                    newNodesCountUnselected++;
                    newMaxNodeSize = Math.max(newMaxNodeSize, node.size());

                    index = fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, false);

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node) || neighboursSelection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;
                    newMaxNodeSize = Math.max(newMaxNodeSize, node.size());

                    index = fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all nodes, no selection active:
            for (int j = 0; j < visibleNodesCount; j++) {
                final Node node = visibleNodesArray[j];

                newNodesCountSelected++;
                newMaxNodeSize = Math.max(newMaxNodeSize, node.size());

                index = fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                if (index == attributesBufferBatch.length) {
                    attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (index > 0) {
            attribs.put(attributesBufferBatch, 0, index);
        }

        currentBufferIndex = nextBufferIndex;
        instanceCounter.unselectedCount = newNodesCountUnselected;
        instanceCounter.selectedCount = newNodesCountSelected;
        maxNodeSize = newMaxNodeSize;
    }

    @Override
    public void dispose() {
        super.dispose();
        attributesBufferBatch = null;
        for (ManagedDirectBuffer buffer : attributesBuffersList) {
            if (buffer != null) {
                buffer.destroy();
            }
        }
    }
}
