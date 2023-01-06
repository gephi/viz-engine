package org.gephi.viz.engine.lwjgl.pipeline.arrays;

import java.nio.FloatBuffer;

import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.models.NodeDiskModel;
import org.gephi.viz.engine.lwjgl.pipeline.common.AbstractNodeData;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndexImpl;
import org.lwjgl.system.MemoryStack;

import static org.gephi.viz.engine.util.gl.Constants.*;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL20.glGenBuffers;
import static org.lwjgl.opengl.GL20.glVertexAttrib1f;
import static org.lwjgl.opengl.GL20.glVertexAttrib2fv;
import static org.lwjgl.opengl.GL20.glVertexAttrib4f;

/**
 * @author Eduardo Ramos
 */
public class ArrayDrawNodeData extends AbstractNodeData {

    private final int[] bufferName = new int[1];

    private static final int VERT_BUFFER = 0;

    public ArrayDrawNodeData() {
        super(false);
    }

    public void init() {
        super.init();
    }

    public void update(VizEngine engine, GraphIndexImpl spatialIndex) {
        updateData(
            spatialIndex,
            engine.getLookup().lookup(GraphRenderingOptions.class),
            engine.getLookup().lookup(GraphSelection.class),
            engine.getLookup().lookup(GraphSelectionNeighbours.class)
        );
    }

    public void drawArrays(RenderingLayer layer, VizEngine engine, float[] mvpFloats) {
        final int instanceCount = setupShaderProgramForRenderingLayer(layer, engine, mvpFloats);

        if (instanceCount <= 0) {
            return;
        }

        final boolean renderingUnselectedNodes = layer.isBack();
        final int instancesOffset;
        if (renderingUnselectedNodes) {
            instancesOffset = 0;
        } else {
            instancesOffset = instanceCounter.unselectedCountToDraw;
        }


        final float zoom = engine.getZoom();
        final float[] attrs = new float[ATTRIBS_STRIDE];
        int index = instancesOffset * ATTRIBS_STRIDE;

        //We have to perform one draw call per instance because repeating the attributes without instancing per each vertex would use too much memory:
        //TODO: Maybe we can batch a few nodes at once though
        final FloatBuffer attribs = attributesBuffer.floatBuffer();

        attribs.position(index);
        for (int i = 0; i < instanceCount; i++) {
            attribs.get(attrs);

            //Choose LOD:
            final float size = attrs[3];
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
            glVertexAttrib2fv(SHADER_POSITION_LOCATION, attrs);

            //No vertexAttribArray, we have to unpack rgba manually:
            final int argb = Float.floatToRawIntBits(attrs[2]);

            final int a = ((argb >> 24) & 0xFF);
            final int r = ((argb >> 16) & 0xFF);
            final int g = ((argb >> 8) & 0xFF);
            final int b = (argb & 0xFF);

            glVertexAttrib4f(SHADER_COLOR_LOCATION, b, g, r, a);

            glVertexAttrib1f(SHADER_SIZE_LOCATION, size);

            //Draw the instance:
            diskModel.drawArraysSingleInstance(firstVertex, circleVertexCount);
        }

        diskModel.stopUsingProgram();
        unsetupVertexArrayAttributes();
    }

    public void updateBuffers() {
        instanceCounter.promoteCountToDraw();
        maxNodeSizeToDraw = maxNodeSize;
    }

    protected void initBuffers() {
        super.initBuffers();

        glGenBuffers(bufferName);

        initCirclesGLVertexBuffer(bufferName[VERT_BUFFER]);
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}
