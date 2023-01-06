package org.gephi.viz.engine.lwjgl.pipeline.common;

import java.nio.FloatBuffer;

import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.NodeDiskModel;
import org.gephi.viz.engine.lwjgl.models.NodeDiskVertexDataGenerator;
import org.gephi.viz.engine.lwjgl.util.gl.GLBuffer;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.GLVertexArrayObject;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.gephi.viz.engine.util.structure.NodesCallback;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.MemoryStack;

import static org.gephi.viz.engine.util.gl.Constants.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

/**
 * @author Eduardo Ramos
 */
public abstract class AbstractNodeData {

    protected static final float BORDER_SIZE = 0.16f;
    protected static final float INSIDE_CIRCLE_SIZE = 1 - BORDER_SIZE;

    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_64 = 128;
    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_32 = 16;
    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_16 = 2;

    // NOTE: Why secondary buffers and VAOs?
    // Sadly, we cannot use glDrawArraysInstancedBaseInstance in MacOS and it will be never available

    protected GLBuffer vertexGLBuffer;
    protected GLBuffer attributesGLBuffer;
    protected GLBuffer attributesGLBufferSecondary;
    protected final NodesCallback nodesCallback = new NodesCallback();

    protected static final int ATTRIBS_STRIDE = NodeDiskModel.TOTAL_ATTRIBUTES_FLOATS;

    protected final NodeDiskModel diskModel;

    private final NodeDiskVertexDataGenerator generator64;
    private final NodeDiskVertexDataGenerator generator32;
    private final NodeDiskVertexDataGenerator generator16;
    private final NodeDiskVertexDataGenerator generator8;

    protected final int circleVertexCount64;
    protected final int circleVertexCount32;
    protected final int circleVertexCount16;
    protected final int circleVertexCount8;
    protected final int firstVertex64;
    protected final int firstVertex32;
    protected final int firstVertex16;
    protected final int firstVertex8;
    protected final boolean instancedRendering;

    // State:
    protected final InstanceCounter instanceCounter = new InstanceCounter();
    protected float maxNodeSize = 0;
    protected float maxNodeSizeToDraw = 0;

    // Buffers for vertex attributes:
    protected static final int BATCH_NODES_SIZE = 32768;
    protected float[] attributesBufferBatch;
    protected ManagedDirectBuffer attributesBuffer;

    public AbstractNodeData(boolean instancedRendering) {
        this.instancedRendering = instancedRendering;

        diskModel = new NodeDiskModel();

        generator64 = new NodeDiskVertexDataGenerator(64);
        generator32 = new NodeDiskVertexDataGenerator(32);
        generator16 = new NodeDiskVertexDataGenerator(16);
        generator8 = new NodeDiskVertexDataGenerator(8);

        circleVertexCount64 = generator64.getVertexCount();
        circleVertexCount32 = generator32.getVertexCount();
        circleVertexCount16 = generator16.getVertexCount();
        circleVertexCount8 = generator8.getVertexCount();

        firstVertex64 = 0;
        firstVertex32 = generator64.getVertexCount();
        firstVertex16 = firstVertex32 + generator32.getVertexCount();
        firstVertex8 = firstVertex16 + generator16.getVertexCount();
    }

    protected void init() {
        diskModel.initGLPrograms();
        initBuffers();
    }

    protected void initBuffers() {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_NODES_SIZE];
        attributesBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_NODES_SIZE);
    }

    protected void initCirclesGLVertexBuffer(final int bufferName) {
        final NodeDiskVertexDataGenerator generator64 = new NodeDiskVertexDataGenerator(64);
        final NodeDiskVertexDataGenerator generator32 = new NodeDiskVertexDataGenerator(32);
        final NodeDiskVertexDataGenerator generator16 = new NodeDiskVertexDataGenerator(16);
        final NodeDiskVertexDataGenerator generator8 = new NodeDiskVertexDataGenerator(8);

        final float[] circleVertexData = new float[
                generator64.getVertexData().length
                + generator32.getVertexData().length
                + generator16.getVertexData().length
                + generator8.getVertexData().length
            ];

        int offset = 0;
        System.arraycopy(generator64.getVertexData(), 0, circleVertexData, offset, generator64.getVertexData().length);
        offset += generator64.getVertexData().length;
        System.arraycopy(generator32.getVertexData(), 0, circleVertexData, offset, generator32.getVertexData().length);
        offset += generator32.getVertexData().length;
        System.arraycopy(generator16.getVertexData(), 0, circleVertexData, offset, generator16.getVertexData().length);
        offset += generator16.getVertexData().length;
        System.arraycopy(generator8.getVertexData(), 0, circleVertexData, offset, generator8.getVertexData().length);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            final FloatBuffer circleVertexBuffer = stack.floats(circleVertexData);

            vertexGLBuffer = new GLBufferMutable(bufferName, GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
            vertexGLBuffer.bind();
            vertexGLBuffer.init(circleVertexBuffer, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
            vertexGLBuffer.unbind();
        }
    }

    protected int setupShaderProgramForRenderingLayer(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        final boolean someSelection = engine.getLookup().lookup(GraphSelection.class).getSelectedNodesCount() > 0;
        final boolean renderingUnselectedNodes = layer.isBack();
        if (!someSelection && renderingUnselectedNodes) {
            return 0;
        }

        final float[] backgroundColorFloats = engine.getBackgroundColor();

        final int instanceCount;
        final boolean isRenderingOutsideCircle = layer.getLevel() == 2;
        final float sizeMultiplier = isRenderingOutsideCircle ? 1f : INSIDE_CIRCLE_SIZE;

        if (renderingUnselectedNodes) {
            instanceCount = instanceCounter.unselectedCountToDraw;
            final float colorLightenFactor = engine.getLookup().lookup(GraphRenderingOptions.class).getLightenNonSelectedFactor();
            final float colorBias = 0f;
            final float colorMultiplier = isRenderingOutsideCircle ? NODER_BORDER_DARKEN_FACTOR : 1f;
            diskModel.useProgramWithSelection(
                mvpFloats, backgroundColorFloats,
                sizeMultiplier,
                colorBias,
                colorMultiplier,
                colorLightenFactor
            );

            setupSecondaryVertexArrayAttributes(engine);
        } else {
            instanceCount = instanceCounter.selectedCountToDraw;
            final float colorLightenFactor = 0;

            if (someSelection) {
                final float colorBias = isRenderingOutsideCircle ? 0f : 0.5f;
                final float colorMultiplier = isRenderingOutsideCircle ? 1f : 0.5f;
                diskModel.useProgramWithSelection(
                    mvpFloats, backgroundColorFloats,
                    sizeMultiplier,
                    colorBias,
                    colorMultiplier,
                    colorLightenFactor
                );
            } else {
                final float colorMultiplier = isRenderingOutsideCircle ? NODER_BORDER_DARKEN_FACTOR : 1f;
                diskModel.useProgram(mvpFloats, backgroundColorFloats, sizeMultiplier, colorMultiplier);
            }

            setupVertexArrayAttributes(engine);
        }

        return instanceCount;
    }

    protected void updateData(final GraphIndexImpl spatialIndex,
                              final GraphRenderingOptions renderingOptions,
                              final GraphSelection selection,
                              final GraphSelectionNeighbours neighboursSelection) {
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

        attributesBuffer.ensureCapacity(totalNodes * ATTRIBS_STRIDE);

        final FloatBuffer attribs = attributesBuffer.floatBuffer();

        spatialIndex.getVisibleNodes(nodesCallback);

        final Node[] visibleNodesArray = nodesCallback.getNodesArray();
        final int visibleNodesCount = nodesCallback.getCount();

        int newNodesCountUnselected = 0;
        int newNodesCountSelected = 0;

        float newMaxNodeSize = 0;
        for (int j = 0; j < visibleNodesCount; j++) {
            final float size = visibleNodesArray[j].size();
            newMaxNodeSize = size >= newMaxNodeSize ? size : newMaxNodeSize;
        }

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
                    fillNodeAttributesData(attributesBufferBatch, node, index);
                    index += ATTRIBS_STRIDE;

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch);
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

                    fillNodeAttributesData(attributesBufferBatch, node, index);
                    index += ATTRIBS_STRIDE;

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch);
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

                    fillNodeAttributesData(attributesBufferBatch, node, index);
                    index += ATTRIBS_STRIDE;

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all nodes, no selection active:
            for (int j = 0; j < visibleNodesCount; j++) {
                final Node node = visibleNodesArray[j];

                newNodesCountSelected++;

                fillNodeAttributesData(attributesBufferBatch, node, index);
                index += ATTRIBS_STRIDE;

                if (index == attributesBufferBatch.length) {
                    attribs.put(attributesBufferBatch);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (index > 0) {
            attribs.put(attributesBufferBatch, 0, index);
        }

        instanceCounter.unselectedCount = newNodesCountUnselected;
        instanceCounter.selectedCount = newNodesCountSelected;
        maxNodeSize = newMaxNodeSize;
    }

    protected void fillNodeAttributesData(final float[] buffer, final Node node, final int index) {
        final float x = node.x();
        final float y = node.y();
        final float size = node.size();
        final int rgba = node.getRGBA();

        //Position:
        buffer[index] = x;
        buffer[index + 1] = y;

        //Color:
        buffer[index + 2] = Float.intBitsToFloat(rgba);

        //Size:
        buffer[index + 3] = size;
    }

    private NodesVAO nodesVAO;
    private NodesVAO nodesVAOSecondary;

    public void setupVertexArrayAttributes(VizEngine engine) {
        if (nodesVAO == null) {
            nodesVAO = new NodesVAO(
                engine.getLookup().lookup(GLCapabilities.class),
                engine.getLookup().lookup(OpenGLOptions.class),
                vertexGLBuffer, attributesGLBuffer
            );
        }

        nodesVAO.use();
    }

    public void setupSecondaryVertexArrayAttributes(VizEngine engine) {
        if (nodesVAOSecondary == null) {
            nodesVAOSecondary = new NodesVAO(
                engine.getLookup().lookup(GLCapabilities.class),
                engine.getLookup().lookup(OpenGLOptions.class),
                vertexGLBuffer, attributesGLBufferSecondary
            );
        }

        nodesVAOSecondary.use();
    }

    public void unsetupVertexArrayAttributes() {
        if (nodesVAO != null) {
            nodesVAO.stopUsing();
        }

        if (nodesVAOSecondary != null) {
            nodesVAOSecondary.stopUsing();
        }
    }

    public void dispose() {
        attributesBufferBatch = null;
        if (attributesBuffer != null) {
            attributesBuffer.destroy();
            attributesBuffer = null;
        }

        if (vertexGLBuffer != null) {
            vertexGLBuffer.destroy();
            vertexGLBuffer = null;
        }

        if (attributesGLBuffer != null) {
            attributesGLBuffer.destroy();
            attributesGLBuffer = null;
        }

        if (attributesGLBufferSecondary != null) {
            attributesGLBufferSecondary.destroy();
            attributesGLBufferSecondary = null;
        }

        nodesCallback.reset();
    }

    private class NodesVAO extends GLVertexArrayObject {

        private final GLBuffer vertexBuffer;
        private final GLBuffer attributesBuffer;

        public NodesVAO(GLCapabilities capabilities, OpenGLOptions openGLOptions, final GLBuffer vertexBuffer, final GLBuffer attributesBuffer) {
            super(capabilities, openGLOptions);
            this.vertexBuffer = vertexBuffer;
            this.attributesBuffer = attributesBuffer;
        }

        @Override
        protected void configure() {
            vertexBuffer.bind();
            {
                glVertexAttribPointer(SHADER_VERT_LOCATION, NodeDiskModel.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexBuffer.unbind();

            if (instancedRendering) {
                attributesBuffer.bind();
                {
                    final int stride = ATTRIBS_STRIDE * Float.BYTES;
                    int offset = 0;

                    glVertexAttribPointer(SHADER_POSITION_LOCATION, NodeDiskModel.POSITION_FLOATS, GL_FLOAT, false, stride, offset);
                    offset += NodeDiskModel.POSITION_FLOATS * Float.BYTES;

                    glVertexAttribPointer(SHADER_COLOR_LOCATION, NodeDiskModel.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                    offset += NodeDiskModel.COLOR_FLOATS * Float.BYTES;

                    glVertexAttribPointer(SHADER_SIZE_LOCATION, NodeDiskModel.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                }
                attributesBuffer.unbind();
            }
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            if (instancedRendering) {
                return new int[]{
                    SHADER_VERT_LOCATION,
                    SHADER_POSITION_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_SIZE_LOCATION
                };
            } else {
                return new int[]{
                    SHADER_VERT_LOCATION
                };
            }
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            if (instancedRendering) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_SIZE_LOCATION
                };
            } else {
                return null;
            }
        }

    }
}
