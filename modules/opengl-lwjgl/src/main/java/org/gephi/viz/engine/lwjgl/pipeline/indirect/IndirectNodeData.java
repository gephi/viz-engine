package org.gephi.viz.engine.lwjgl.pipeline.indirect;

import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.NodeDiskModel;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBuffer;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferImmutable;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Optional;

import static org.gephi.viz.engine.util.gl.GLConstants.INDIRECT_DRAW_COMMAND_BYTES;
import static org.gephi.viz.engine.util.gl.GLConstants.INDIRECT_DRAW_COMMAND_INTS_COUNT;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_INT;
import static org.lwjgl.opengl.GL20.glGenBuffers;

/**
 *
 * @author Eduardo Ramos
 */
public class IndirectNodeData extends AbstractNodeData {

    private final NodeDiskModel diskModel64;
    private final NodeDiskModel diskModel32;
    private final NodeDiskModel diskModel16;
    private final NodeDiskModel diskModel8;

    private final int circleVertexCount64;
    private final int circleVertexCount32;
    private final int circleVertexCount16;
    private final int circleVertexCount8;
    private final int firstVertex64;
    private final int firstVertex32;
    private final int firstVertex16;
    private final int firstVertex8;

    private final InstanceCounter instanceCounter = new InstanceCounter();

    public IndirectNodeData() {
        super(true);
        diskModel64 = new NodeDiskModel(64);
        diskModel32 = new NodeDiskModel(32);
        diskModel16 = new NodeDiskModel(16);
        diskModel8 = new NodeDiskModel(8);

        circleVertexCount64 = diskModel64.getVertexCount();
        circleVertexCount32 = diskModel32.getVertexCount();
        circleVertexCount16 = diskModel16.getVertexCount();
        circleVertexCount8 = diskModel8.getVertexCount();

        firstVertex64 = 0;
        firstVertex32 = circleVertexCount64;
        firstVertex16 = firstVertex32 + circleVertexCount32;
        firstVertex8 = firstVertex16 + circleVertexCount16;
    }

    private final int[] bufferName = new int[3];

    private static final int VERT_BUFFER = 0;
    private static final int ATTRIBS_BUFFER = 1;
    private static final int INDIRECT_DRAW_BUFFER = 2;

    public void init() {
        initBuffers();
        diskModel64.initGLPrograms();
    }

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(
                engine.getZoom(),
                spatialIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class),
                engine.getLookup().lookup(GraphSelectionNeighbours.class)
        );
    }

    public void drawIndirect(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        float globalTime = engine.getGlobalTime();
        Optional<Float> selectedTime = engine.getSelectedTime();
        final float[] backgroundColorFloats = engine.getBackgroundColor();

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
            setupVertexArrayAttributes(engine);

            commandsGLBuffer.bind();
            diskModel64.drawIndirect(mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset, globalTime, selectedTime);
            commandsGLBuffer.unbind();

            unsetupVertexArrayAttributes();
        }
    }

    private ManagedDirectBuffer attributesBuffer;
    private ManagedDirectBuffer commandsBuffer;

    private GLBuffer commandsGLBuffer;

    private float[] attributesBufferBatch;
    private int[] commandsBufferBatch;
    private static final int BATCH_NODES_SIZE = 32768;

    private void initBuffers() {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2];
        commandsBufferBatch = new int[INDIRECT_DRAW_COMMAND_INTS_COUNT * BATCH_NODES_SIZE * 2];

        glGenBuffers(bufferName);

        final float[] circleVertexData = new float[diskModel64.getVertexData().length + diskModel32.getVertexData().length + diskModel16.getVertexData().length + diskModel8.getVertexData().length];
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

            final int flags = 0;
            vertexGLBuffer = new GLBufferImmutable(bufferName[VERT_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBuffer.bind();
            vertexGLBuffer.init(circleVertexBuffer, flags);
            vertexGLBuffer.unbind();
        }

        //Initialize for batch nodes size:
        attributesGLBuffer = new GLBufferMutable(bufferName[ATTRIBS_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind();
        attributesGLBuffer.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_NODES_SIZE * 2, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind();

        commandsGLBuffer = new GLBufferMutable(bufferName[INDIRECT_DRAW_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_DRAW_INDIRECT);
        commandsGLBuffer.bind();
        commandsGLBuffer.init(INDIRECT_DRAW_COMMAND_BYTES * BATCH_NODES_SIZE * 2, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        commandsGLBuffer.unbind();

        attributesBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2);
        commandsBuffer = new ManagedDirectBuffer(GL_UNSIGNED_INT, INDIRECT_DRAW_COMMAND_INTS_COUNT * BATCH_NODES_SIZE * 2);
    }

    public void updateBuffers() {
        instanceCounter.promoteCountToDraw();

        final FloatBuffer attributesBufferData = attributesBuffer.floatBuffer();
        attributesBufferData.limit(instanceCounter.totalToDraw() * ATTRIBS_STRIDE * 2);

        attributesGLBuffer.bind();
        attributesGLBuffer.updateWithOrphaning(attributesBufferData);
        attributesGLBuffer.unbind();

        final IntBuffer commandsBufferData = commandsBuffer.intBuffer();
        commandsBufferData.limit(instanceCounter.totalToDraw() * INDIRECT_DRAW_COMMAND_INTS_COUNT * 2);

        commandsGLBuffer.bind();
        commandsGLBuffer.updateWithOrphaning(commandsBufferData);
        commandsGLBuffer.unbind();
        //TODO: Persistent buffer if available?
    }

    private void updateData(final float zoom, final GraphIndexImpl spatialIndex, final GraphRenderingOptions renderingOptions, final GraphSelection selection, final GraphSelectionNeighbours neighboursSelection) {
        //TODO: unify this copy-paste in nodes renderers...
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

        attributesBuffer.ensureCapacity(totalNodes * ATTRIBS_STRIDE * 2);
        commandsBuffer.ensureCapacity(totalNodes * INDIRECT_DRAW_COMMAND_INTS_COUNT * 2);

        final FloatBuffer attribs = attributesBuffer.floatBuffer();
        final IntBuffer commands = commandsBuffer.intBuffer();

        spatialIndex.getVisibleNodes(nodesCallback);

        final Node[] visibleNodesArray = nodesCallback.getNodesArray();
        final int visibleNodesCount = nodesCallback.getCount();

        int newNodesCountUnselected = 0;
        int newNodesCountSelected = 0;

        int index = 0;
        int commandIndex = 0;
        int instanceId = 0;
        final int stride = ATTRIBS_STRIDE * 2;
        final int commandsStride = 8;

        if (someSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node) || neighboursSelection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;

                    fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                    fillNodeCommandData(node, zoom, commandIndex, instanceId);

                    if (index + stride == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }

                    if (commandIndex + commandsStride == commandsBufferBatch.length) {
                        commands.put(commandsBufferBatch, 0, commandsBufferBatch.length);
                        commandIndex = 0;
                    }

                    index += stride;
                    commandIndex += commandsStride;
                    instanceId += 2;
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

                    fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, false);

                    fillNodeCommandData(node, zoom, commandIndex, instanceId);

                    if (index + stride == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }

                    if (commandIndex + commandsStride == commandsBufferBatch.length) {
                        commands.put(commandsBufferBatch, 0, commandsBufferBatch.length);
                        commandIndex = 0;
                    }

                    index += stride;
                    commandIndex += commandsStride;
                    instanceId += 2;
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node) || neighboursSelection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;

                    fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                    fillNodeCommandData(node, zoom, commandIndex, instanceId);

                    if (index + stride == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }

                    if (commandIndex + commandsStride == commandsBufferBatch.length) {
                        commands.put(commandsBufferBatch, 0, commandsBufferBatch.length);
                        commandIndex = 0;
                    }

                    index += stride;
                    commandIndex += commandsStride;
                    instanceId += 2;
                }
            }
        } else {
            //Just all nodes, no selection active:
            for (int j = 0; j < visibleNodesCount; j++) {
                final Node node = visibleNodesArray[j];

                newNodesCountSelected++;

                fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, true);

                fillNodeCommandData(node, zoom, commandIndex, instanceId);

                if (index + stride == attributesBufferBatch.length) {
                    attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                    index = 0;
                }

                if (commandIndex + commandsStride == commandsBufferBatch.length) {
                    commands.put(commandsBufferBatch, 0, commandsBufferBatch.length);
                    commandIndex = 0;
                }

                index += stride;
                commandIndex += commandsStride;
                instanceId += 2;
            }
        }

        //Remaining:
        if (index > 0) {
            attribs.put(attributesBufferBatch, 0, index);
        }

        if (commandIndex > 0) {
            commands.put(commandsBufferBatch, 0, commandIndex);
        }

        instanceCounter.unselectedCount = newNodesCountUnselected;
        instanceCounter.selectedCount = newNodesCountSelected;
    }

    private void fillNodeCommandData(final Node node, final float zoom, final int index, final int instanceId) {
        //Indirect Draw:
        //Choose LOD:
        final float observedSize = node.size() * zoom;

        final int circleVertexCount;
        final int firstVertex;
        if (observedSize > OBSERVED_SIZE_LOD_THRESHOLD_64) {
            circleVertexCount = circleVertexCount64;
            firstVertex = firstVertex64;
        } else if (observedSize > OBSERVED_SIZE_LOD_THRESHOLD_32) {
            circleVertexCount = circleVertexCount32;
            firstVertex = firstVertex32;
        } else if (observedSize > OBSERVED_SIZE_LOD_THRESHOLD_16) {
            circleVertexCount = circleVertexCount16;
            firstVertex = firstVertex16;
        } else {
            circleVertexCount = circleVertexCount8;
            firstVertex = firstVertex8;
        }

        //Outside circle:
        commandsBufferBatch[index + 0] = circleVertexCount;//vertex count
        commandsBufferBatch[index + 1] = 1;//instance count
        commandsBufferBatch[index + 2] = firstVertex;//first vertex
        commandsBufferBatch[index + 3] = instanceId;//base instance

        //Inside circle:
        commandsBufferBatch[index + 4] = circleVertexCount;//vertex count
        commandsBufferBatch[index + 5] = 1;//instance count
        commandsBufferBatch[index + 6] = firstVertex;//first vertex
        commandsBufferBatch[index + 7] = instanceId + 1;//base instance
    }

    @Override
    public void dispose() {
        super.dispose();
        attributesBufferBatch = null;
        commandsBufferBatch = null;

        if (attributesBuffer != null) {
            attributesBuffer.destroy();
            attributesBuffer = null;
        }
        if (commandsBuffer != null) {
            commandsBuffer.destroy();
            commandsBuffer = null;
        }
    }
}
