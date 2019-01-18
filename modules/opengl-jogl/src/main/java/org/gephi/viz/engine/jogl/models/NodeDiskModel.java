package org.gephi.viz.engine.jogl.models;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL4;
import org.gephi.viz.engine.util.gl.Constants;
import static org.gephi.viz.engine.util.gl.Constants.*;
import org.gephi.viz.engine.jogl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.util.gl.GLConstants;

/**
 * @author Eduardo Ramos
 */
public class NodeDiskModel {

    public static final int VERTEX_FLOATS = 2;
    public static final int POSITION_FLOATS = 2;
    public static final int COLOR_FLOATS = 1;
    public static final int COLOR_BIAS_FLOATS = 1;
    public static final int COLOR_MULTIPLIER_FLOATS = 1;
    public static final int SIZE_FLOATS = 1;

    public static final int TOTAL_ATTRIBUTES_FLOATS
            = POSITION_FLOATS
            + COLOR_FLOATS
            + COLOR_BIAS_FLOATS
            + COLOR_MULTIPLIER_FLOATS
            + SIZE_FLOATS;

    private final int triangleAmount;
    private final float[] vertexData;
    private final int vertexCount;

    private GLShaderProgram program;

    public NodeDiskModel(int triangleAmount) {
        this.triangleAmount = triangleAmount;
        this.vertexData = generateFilledCircle(triangleAmount);

        this.vertexCount = triangleAmount * 3;
    }

    public int getTriangleAmount() {
        return triangleAmount;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public float[] getVertexData() {
        return vertexData;
    }

    public void initGLPrograms(GL2ES2 gl) {
        initProgram(gl);
    }

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "node";

    private static final String SHADERS_NODE_CIRCLE_SOURCE = "node";

    private void initProgram(GL2ES2 gl) {
        program = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE, SHADERS_NODE_CIRCLE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
                .addUniformName(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR_BIAS, SHADER_COLOR_BIAS_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR_MULTIPLIER, SHADER_COLOR_MULTIPLIER_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .init(gl);
    }

    public void drawArraysSingleInstance(GL2ES2 gl, int firstVertexIndex, int vertexCount) {
        gl.glDrawArrays(GL.GL_TRIANGLES, firstVertexIndex, vertexCount);
    }

    public void drawInstanced(GL2ES3 gl, int vertexOffset, float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, int instanceCount, int instancesOffset) {
        useProgram(gl, mvpFloats, backgroundColorFloats, colorLightenFactor);
        if (instancesOffset > 0) {
            gl.glDrawArraysInstancedBaseInstance(GL.GL_TRIANGLES, vertexOffset, vertexCount, instanceCount, instancesOffset);
        } else {
            gl.glDrawArraysInstanced(GL.GL_TRIANGLES, vertexOffset, vertexCount, instanceCount);
        }
        stopUsingProgram(gl);
    }

    public void drawInstanced(GL2ES3 gl, float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, int instanceCount, int instancesOffset) {
        drawInstanced(gl, 0, mvpFloats, backgroundColorFloats, colorLightenFactor, instanceCount, instancesOffset);
    }

    public void drawIndirect(GL4 gl, float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, int instanceCount, int instancesOffset) {
        useProgram(gl, mvpFloats, backgroundColorFloats, colorLightenFactor);
        gl.glMultiDrawArraysIndirect(GL.GL_TRIANGLES, instancesOffset * GLConstants.INDIRECT_DRAW_COMMAND_BYTES, instanceCount, GLConstants.INDIRECT_DRAW_COMMAND_BYTES);
        stopUsingProgram(gl);
    }

    public void useProgram(GL2ES2 gl, float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor) {
        //Circle:
        program.use(gl);
        gl.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), 1, false, mvpFloats, 0);
        gl.glUniform4fv(program.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), 1, backgroundColorFloats, 0);
        gl.glUniform1f(program.getUniformLocation(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR), colorLightenFactor);
    }

    public void stopUsingProgram(GL2ES2 gl) {
        program.stopUsing(gl);
    }

    public GLShaderProgram getCircleProgram() {
        return program;
    }

    private static float[] generateFilledCircle(int triangleAmount) {
        final double twicePi = 2.0 * Math.PI;

        final int circleFloatsCount = (triangleAmount * 3) * VERTEX_FLOATS;
        final float[] data = new float[circleFloatsCount];
        final int triangleFloats = 3 * VERTEX_FLOATS;

        //Circle:
        for (int i = 1, j = 0; i <= triangleAmount; i++, j += triangleFloats) {
            //Center
            data[j + 0] = 0;//X
            data[j + 1] = 0;//Y

            //Triangle start:
            data[j + 2] = (float) Math.cos((i - 1) * twicePi / triangleAmount);//X
            data[j + 3] = (float) Math.sin((i - 1) * twicePi / triangleAmount);//Y

            //Triangle end:
            if (i == triangleAmount) {
                //Last point
                data[j + 4] = 1;//X
                data[j + 5] = 0;//Y
            } else {
                data[j + 4] = (float) Math.cos(i * twicePi / triangleAmount);//X
                data[j + 5] = (float) Math.sin(i * twicePi / triangleAmount);//Y
            }
        }

        return data;
    }
}
