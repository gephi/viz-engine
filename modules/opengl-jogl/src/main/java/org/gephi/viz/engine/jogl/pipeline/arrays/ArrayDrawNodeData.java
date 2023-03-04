package org.gephi.viz.engine.jogl.pipeline.arrays;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_FLOAT;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.util.GLBuffers;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.models.NodeDiskModel;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.jogl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import static org.gephi.viz.engine.util.gl.Constants.*;
import org.gephi.viz.engine.jogl.util.ManagedDirectBuffer;
import org.gephi.viz.engine.jogl.util.gl.BufferUtils;
import org.gephi.viz.engine.jogl.util.gl.GLBufferMutable;

/**
 *
 * @author Eduardo Ramos
 */
public class ArrayDrawNodeData extends AbstractNodeData {

    private final NodeDiskModel diskModel64 = new NodeDiskModel(64);
    private final NodeDiskModel diskModel32 = new NodeDiskModel(32);
    private final NodeDiskModel diskModel16 = new NodeDiskModel(16);
    private final NodeDiskModel diskModel8 = new NodeDiskModel(8);

    private final int circleVertexCount64;
    private final int circleVertexCount32;
    private final int circleVertexCount16;
    private final int circleVertexCount8;
    private final int firstVertex64;
    private final int firstVertex32;
    private final int firstVertex16;
    private final int firstVertex8;

    private final InstanceCounter instanceCounter = new InstanceCounter();

    private IntBuffer bufferName;

    private static final int VERT_BUFFER = 0;

    public ArrayDrawNodeData() {
        super(false);
        circleVertexCount64 = diskModel64.getVertexCount();
        circleVertexCount32 = diskModel32.getVertexCount();
        circleVertexCount16 = diskModel16.getVertexCount();
        circleVertexCount8 = diskModel8.getVertexCount();

        firstVertex64 = 0;
        firstVertex32 = circleVertexCount64;
        firstVertex16 = firstVertex32 + circleVertexCount32;
        firstVertex8 = firstVertex16 + circleVertexCount16;
    }

    public void init(GL2ES2 gl) {
        initBuffers(gl);
        diskModel64.initGLPrograms(gl);
    }

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(spatialIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class),
                engine.getZoom()
        );
    }

    public void drawArrays(GL2ES2 gl, RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
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
            setupVertexArrayAttributes(engine, gl);
            diskModel64.useProgram(gl, mvpFloats, backgroundColorFloats, colorLightenFactor);

            final float[] attrs = new float[ATTRIBS_STRIDE];
            int index = instancesOffset * ATTRIBS_STRIDE;

            //We have to perform one draw call per intance because repeating the attributes without instancing per each vertex would use too much memory:
            //TODO: Maybe we can batch a few nodes at once though
            final FloatBuffer attribs = attributesBuffer.floatBuffer();

            attribs.position(index);
            for (int i = 0; i < instanceCount; i++) {
                attribs.get(attrs);

                //Choose LOD:
                final float size = attrs[5];
                final float observedSize = size * zoom;

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

                //Define instance attributes:
                gl.glVertexAttrib2fv(SHADER_POSITION_LOCATION, attrs, 0);

                //No vertexAttribArray, we have to unpack rgba manually:
                final int argb = Float.floatToRawIntBits(attrs[2]);

                final int a = ((argb >> 24) & 0xFF);
                final int r = ((argb >> 16) & 0xFF);
                final int g = ((argb >> 8) & 0xFF);
                final int b = (argb & 0xFF);

                gl.glVertexAttrib4f(SHADER_COLOR_LOCATION, b, g, r, a);

                gl.glVertexAttrib1fv(SHADER_COLOR_BIAS_LOCATION, attrs, 3);
                gl.glVertexAttrib1fv(SHADER_COLOR_MULTIPLIER_LOCATION, attrs, 4);
                gl.glVertexAttrib1f(SHADER_SIZE_LOCATION, size);

                //Draw the instance:
                diskModel64.drawArraysSingleInstance(gl, firstVertex, circleVertexCount);
            }

            diskModel64.stopUsingProgram(gl);
            unsetupVertexArrayAttributes(gl);
        }
    }

    public void updateBuffers() {
        instanceCounter.promoteCountToDraw();
    }

    private ManagedDirectBuffer attributesBuffer;

    private float[] attributesBufferBatch;
    private static final int BATCH_NODES_SIZE = 32768;

    private void initBuffers(GL2ES2 gl) {
        attributesBufferBatch = new float[ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2];

        bufferName = GLBuffers.newDirectIntBuffer(3);

        final float[] circleVertexData = new float[diskModel64.getVertexData().length + diskModel32.getVertexData().length + diskModel16.getVertexData().length + diskModel8.getVertexData().length];
        int offset = 0;
        System.arraycopy(diskModel64.getVertexData(), 0, circleVertexData, offset, diskModel64.getVertexData().length);
        offset += diskModel64.getVertexData().length;
        System.arraycopy(diskModel32.getVertexData(), 0, circleVertexData, offset, diskModel32.getVertexData().length);
        offset += diskModel32.getVertexData().length;
        System.arraycopy(diskModel16.getVertexData(), 0, circleVertexData, offset, diskModel16.getVertexData().length);
        offset += diskModel16.getVertexData().length;
        System.arraycopy(diskModel8.getVertexData(), 0, circleVertexData, offset, diskModel8.getVertexData().length);
        final FloatBuffer circleVertexBuffer = GLBuffers.newDirectFloatBuffer(circleVertexData);

        gl.glGenBuffers(bufferName.capacity(), bufferName);

        vertexGLBuffer = new GLBufferMutable(bufferName.get(VERT_BUFFER), GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        vertexGLBuffer.bind(gl);
        vertexGLBuffer.init(gl, circleVertexBuffer, GLBufferMutable.GL_BUFFER_USAGE_STATIC_DRAW);
        vertexGLBuffer.unbind(gl);

        BufferUtils.destroyDirectBuffer(circleVertexBuffer);

        attributesBuffer = new ManagedDirectBuffer(GL_FLOAT, ATTRIBS_STRIDE * BATCH_NODES_SIZE * 2);
    }

    private void updateData(final GraphIndexImpl spatialIndex, final GraphRenderingOptions renderingOptions, final GraphSelection selection, final float zoom) {
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

        final FloatBuffer attribs = attributesBuffer.floatBuffer();

        spatialIndex.getVisibleNodes(nodesCallback);

        final Node[] visibleNodesArray = nodesCallback.getNodesArray();
        final int visibleNodesCount = nodesCallback.getCount();

        int newNodesCountUnselected = 0;
        int newNodesCountSelected = 0;

        float maxNodeSize = 0;
        for (int j = 0; j < visibleNodesCount; j++) {
            final float size = visibleNodesArray[j].size();
            maxNodeSize = size >= maxNodeSize ? size : maxNodeSize;
        }

        int index = 0;
        if (someSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;

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

                    final boolean selected = selection.isNodeSelected(node);
                    if (selected) {
                        continue;
                    }

                    newNodesCountUnselected++;

                    index = fillNodeAttributesData(attributesBufferBatch, node, index, someSelection, false);

                    if (index == attributesBufferBatch.length) {
                        attribs.put(attributesBufferBatch, 0, attributesBufferBatch.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleNodesCount; j++) {
                    final Node node = visibleNodesArray[j];

                    final boolean selected = selection.isNodeSelected(node);
                    if (!selected) {
                        continue;
                    }

                    newNodesCountSelected++;

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

        instanceCounter.unselectedCount = newNodesCountUnselected;
        instanceCounter.selectedCount = newNodesCountSelected;
    }

    @Override
    public void dispose(GL gl) {
        super.dispose(gl);
        attributesBufferBatch = null;
        if (attributesBuffer != null) {
            attributesBuffer.destroy();
            attributesBuffer = null;
        }
    }
}
