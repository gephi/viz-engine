package org.gephi.viz.engine.jogl.pipeline.common;

import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_FLOAT;
import static com.jogamp.opengl.GL.GL_UNSIGNED_BYTE;
import com.jogamp.opengl.GL2ES2;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.jogl.models.NodeDiskModel;
import org.gephi.viz.engine.util.gl.Constants;
import static org.gephi.viz.engine.util.gl.Constants.*;
import org.gephi.viz.engine.jogl.util.gl.GLBuffer;
import org.gephi.viz.engine.jogl.util.gl.GLVertexArrayObject;
import org.gephi.viz.engine.jogl.util.gl.capabilities.GLCapabilitiesSummary;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.gephi.viz.engine.util.structure.NodesCallback;

/**
 *
 * @author Eduardo Ramos
 */
public abstract class AbstractNodeData {

    protected static final float BORDER_SIZE = 0.16f;
    protected static final float INSIDE_CIRCLE_SIZE = 1 - BORDER_SIZE;

    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_64 = 128;
    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_32 = 16;
    protected static final int OBSERVED_SIZE_LOD_THRESHOLD_16 = 2;

    protected GLBuffer vertexGLBuffer;
    protected GLBuffer attributesGLBuffer;
    protected final NodesCallback nodesCallback = new NodesCallback();

    protected static final int ATTRIBS_STRIDE = NodeDiskModel.TOTAL_ATTRIBUTES_FLOATS;

    protected final boolean instanced;

    public AbstractNodeData(boolean instanced) {
        this.instanced = instanced;
    }

    protected int fillNodeAttributesData(final float[] buffer, final Node node, final int index, final boolean someSelection, final boolean selected) {
        final float x = node.x();
        final float y = node.y();
        final float size = node.size();
        final int rgba = node.getRGBA();

        //Outside circle:
        {
            //Position:
            buffer[index + 0] = x;
            buffer[index + 1] = y;

            //Color:
            buffer[index + 2] = Float.intBitsToFloat(rgba);
            //Bias, multiplier and lighten factor:
            buffer[index + 3] = 0;
            if (someSelection) {
                if (selected) {
                    buffer[index + 4] = 1;
                } else {
                    buffer[index + 4] = Constants.NODER_BORDER_DARKEN_FACTOR;//Darken the color
                }
            } else {
                buffer[index + 4] = Constants.NODER_BORDER_DARKEN_FACTOR;//Darken the color
            }

            //Size:
            buffer[index + 5] = size;
        }

        final int nextIndex = index + ATTRIBS_STRIDE;

        //Inside circle:
        {
            //Position:
            buffer[nextIndex + 0] = x;
            buffer[nextIndex + 1] = y;

            //Color:
            buffer[nextIndex + 2] = Float.intBitsToFloat(rgba);
            //Bias and multiplier:
            if (someSelection) {
                if (selected) {
                    buffer[nextIndex + 3] = 0.5f;
                    buffer[nextIndex + 4] = 0.5f;
                } else {
                    buffer[nextIndex + 3] = 0;
                    buffer[nextIndex + 4] = 1;
                }
            } else {
                buffer[nextIndex + 3] = 0;
                buffer[nextIndex + 4] = 1;
            }

            //Size:
            buffer[nextIndex + 5] = size * INSIDE_CIRCLE_SIZE;
        }

        return nextIndex + ATTRIBS_STRIDE;
    }

    private NodesVAO nodesVAO;

    public void setupVertexArrayAttributes(VizEngine engine, GL2ES2 gl) {
        if (nodesVAO == null) {
            nodesVAO = new NodesVAO(
                    engine.getLookup().lookup(GLCapabilitiesSummary.class),
                    engine.getLookup().lookup(OpenGLOptions.class)
            );
        }

        nodesVAO.use(gl);
    }

    public void unsetupVertexArrayAttributes(GL2ES2 gl) {
        nodesVAO.stopUsing(gl);
    }

    public void dispose(GL gl) {
        if (vertexGLBuffer != null) {
            vertexGLBuffer.destroy(gl);
        }

        if (attributesGLBuffer != null) {
            attributesGLBuffer.destroy(gl);
        }

        nodesCallback.reset();
    }

    private class NodesVAO extends GLVertexArrayObject {

        public NodesVAO(GLCapabilitiesSummary capabilities, OpenGLOptions openGLOptions) {
            super(capabilities, openGLOptions);
        }

        @Override
        protected void configure(GL2ES2 gl) {
            vertexGLBuffer.bind(gl);
            {
                gl.glVertexAttribPointer(SHADER_VERT_LOCATION, NodeDiskModel.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBuffer.unbind(gl);

            if (instanced) {
                attributesGLBuffer.bind(gl);
                {
                    final int stride = ATTRIBS_STRIDE * Float.BYTES;
                    int offset = 0;

                    gl.glVertexAttribPointer(SHADER_POSITION_LOCATION, NodeDiskModel.POSITION_FLOATS, GL_FLOAT, false, stride, offset);
                    offset += NodeDiskModel.POSITION_FLOATS * Float.BYTES;

                    gl.glVertexAttribPointer(SHADER_COLOR_LOCATION, NodeDiskModel.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                    offset += NodeDiskModel.COLOR_FLOATS * Float.BYTES;

                    gl.glVertexAttribPointer(SHADER_COLOR_BIAS_LOCATION, NodeDiskModel.COLOR_BIAS_FLOATS, GL_FLOAT, false, stride, offset);
                    offset += NodeDiskModel.COLOR_BIAS_FLOATS * Float.BYTES;

                    gl.glVertexAttribPointer(SHADER_COLOR_MULTIPLIER_LOCATION, NodeDiskModel.COLOR_MULTIPLIER_FLOATS, GL_FLOAT, false, stride, offset);
                    offset += NodeDiskModel.COLOR_MULTIPLIER_FLOATS * Float.BYTES;

                    gl.glVertexAttribPointer(SHADER_SIZE_LOCATION, NodeDiskModel.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                }
                attributesGLBuffer.unbind(gl);
            }
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            if (instanced) {
                return new int[]{
                    SHADER_VERT_LOCATION,
                    SHADER_POSITION_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION,
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
            if (instanced) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION,
                    SHADER_SIZE_LOCATION
                };
            } else {
                return null;
            }
        }

    }
}
