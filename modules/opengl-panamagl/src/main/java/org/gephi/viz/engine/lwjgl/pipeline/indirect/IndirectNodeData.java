package org.gephi.viz.engine.lwjgl.pipeline.indirect;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndexImpl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.gephi.viz.engine.util.gl.GLConstants.INDIRECT_DRAW_COMMAND_BYTES;
import static org.gephi.viz.engine.util.gl.GLConstants.INDIRECT_DRAW_COMMAND_INTS_COUNT;
import static org.lwjgl.opengl.GL20.glGenBuffers;

/**
 *
 * @author Eduardo Ramos
 */
public class IndirectNodeData extends AbstractNodeData {

    private final int[] bufferName = new int[4];

    private static final int VERT_BUFFER = 0;
    private static final int ATTRIBS_BUFFER = 1;
    private static final int ATTRIBS_BUFFER_SECONDARY = 2;
    private static final int INDIRECT_DRAW_BUFFER = 3;

    public IndirectNodeData() {
        super(true, true);
    }

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(
                engine.getZoom(),
                spatialIndex,
                engine.getLookup().lookup(GraphRenderingOptions.class),
                engine.getLookup().lookup(GraphSelection.class)
        );
    }

    public void drawIndirect(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        //First we draw outside circle (for border) and then inside circle:
        drawIndirectInternal(layer, engine, mvpFloats, true);
        drawIndirectInternal(layer, engine, mvpFloats, false);
    }

    private void drawIndirectInternal(final RenderingLayer layer,
                                      final VizEngine engine,
                                      final float[] mvpFloats,
                                      final boolean isRenderingOutsideCircle) {
        final int instanceCount = setupShaderProgramForRenderingLayer(layer, engine, mvpFloats, isRenderingOutsideCircle);

        if (instanceCount <= 0) {
            diskModel.stopUsingProgram();
            unsetupVertexArrayAttributes();
            return;
        }

        final boolean renderingUnselectedNodes = layer.isBack();
        final int instancesOffset = renderingUnselectedNodes ? 0 : instanceCounter.unselectedCountToDraw;

        commandsGLBuffer.bind();
        diskModel.drawIndirect(
                instanceCount, instancesOffset
        );
        commandsGLBuffer.unbind();
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

        commandsGLBuffer = new GLBufferMutable(bufferName[INDIRECT_DRAW_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_DRAW_INDIRECT);
        commandsGLBuffer.bind();
        commandsGLBuffer.init(INDIRECT_DRAW_COMMAND_BYTES * BATCH_NODES_SIZE, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        commandsGLBuffer.unbind();
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

        final IntBuffer commandsBufferData = commandsBuffer.intBuffer();
        commandsBufferData.position(0);
        commandsBufferData.limit(instanceCounter.total() * INDIRECT_DRAW_COMMAND_INTS_COUNT);

        commandsGLBuffer.bind();
        commandsGLBuffer.updateWithOrphaning(commandsBufferData);
        commandsGLBuffer.unbind();

        instanceCounter.promoteCountToDraw();
        maxNodeSizeToDraw = maxNodeSize;
    }
}
