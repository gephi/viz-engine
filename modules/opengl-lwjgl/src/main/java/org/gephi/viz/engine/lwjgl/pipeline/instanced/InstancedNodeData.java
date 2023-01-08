package org.gephi.viz.engine.lwjgl.pipeline.instanced;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndexImpl;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.glGenBuffers;

/**
 * @author Eduardo Ramos
 */
public class InstancedNodeData extends AbstractNodeData {

    public InstancedNodeData() {
        super(true, false);
    }

    private final int[] bufferName = new int[3];

    private static final int VERT_BUFFER = 0;
    private static final int ATTRIBS_BUFFER = 1;
    private static final int ATTRIBS_BUFFER_SECONDARY = 2;

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(
                engine.getZoom(),
                spatialIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class),
                engine.getLookup().lookup(GraphSelectionNeighbours.class)
        );
    }

    public void drawInstanced(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        //First we draw outside circle (for border) and then inside circle:
        drawInstancedInternal(layer, engine, mvpFloats, true);
        drawInstancedInternal(layer, engine, mvpFloats, false);
    }

    private void drawInstancedInternal(final RenderingLayer layer,
                                      final VizEngine engine,
                                      final float[] mvpFloats,
                                      final boolean isRenderingOutsideCircle) {
        final int instanceCount = setupShaderProgramForRenderingLayer(layer, engine, mvpFloats, isRenderingOutsideCircle);

        if (instanceCount <= 0) {
            diskModel.stopUsingProgram();
            unsetupVertexArrayAttributes();
            return;
        }

        final float maxObservedSize = maxNodeSizeToDraw * engine.getZoom();
        final int circleVertexCount;
        final int firstVertex;
        if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_64) {
            circleVertexCount = circleVertexCount64;
            firstVertex = firstVertex64;
        } else if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_32) {
            circleVertexCount = circleVertexCount32;
            firstVertex = firstVertex32;
        } else if (maxObservedSize > OBSERVED_SIZE_LOD_THRESHOLD_16) {
            circleVertexCount = circleVertexCount16;
            firstVertex = firstVertex16;
        } else {
            circleVertexCount = circleVertexCount8;
            firstVertex = firstVertex8;
        }

        diskModel.drawInstanced(
                firstVertex, circleVertexCount, instanceCount
        );
        diskModel.stopUsingProgram();
        unsetupVertexArrayAttributes();
    }

    protected void initBuffers() {
        super.initBuffers();
        glGenBuffers(bufferName);

        initCirclesGLVertexBuffer(bufferName[VERT_BUFFER]);

        //Initialize for batch nodes size:
        attributesGLBuffer = new GLBufferMutable(bufferName[ATTRIBS_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBuffer.bind();
        attributesGLBuffer.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_NODES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBuffer.unbind();

        attributesGLBufferSecondary = new GLBufferMutable(bufferName[ATTRIBS_BUFFER_SECONDARY], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        attributesGLBufferSecondary.bind();
        attributesGLBufferSecondary.init(ATTRIBS_STRIDE * Float.BYTES * BATCH_NODES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        attributesGLBufferSecondary.unbind();
    }

    public void updateBuffers() {
        final FloatBuffer buf = attributesBuffer.floatBuffer();

        buf.limit(instanceCounter.unselectedCount * ATTRIBS_STRIDE);
        buf.position(0);

        attributesGLBufferSecondary.bind();
        attributesGLBufferSecondary.updateWithOrphaning(buf);
        attributesGLBufferSecondary.unbind();

        final int offset = buf.limit();
        buf.limit(offset + instanceCounter.selectedCount * ATTRIBS_STRIDE);
        buf.position(offset);

        attributesGLBuffer.bind();
        attributesGLBuffer.updateWithOrphaning(buf);
        attributesGLBuffer.unbind();

        instanceCounter.promoteCountToDraw();
        maxNodeSizeToDraw = maxNodeSize;
    }
}
