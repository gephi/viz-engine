package org.gephi.viz.engine.lwjgl.models;

import org.gephi.viz.engine.util.Constants;
import static org.gephi.viz.engine.util.Constants.*;
import org.gephi.viz.engine.util.NumberUtils;
import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL42;

/**
 *
 * @author Eduardo Ramos
 */
public class EdgeLineModelDirected {

    public static final int VERTEX_FLOATS = 3;
    public static final int POSITION_SOURCE_FLOATS = 2;
    public static final int POSITION_TARGET_FLOATS = 2;
    public static final int SOURCE_COLOR_FLOATS = 1;
    public static final int COLOR_FLOATS = 1;
    public static final int COLOR_BIAS_FLOATS = 1;
    public static final int COLOR_MULTIPLIER_FLOATS = 1;
    public static final int TARGET_SIZE_FLOATS = 1;
    public static final int SIZE_FLOATS = 1;

    public static final int TOTAL_ATTRIBUTES_FLOATS
            = POSITION_SOURCE_FLOATS
            + POSITION_TARGET_FLOATS
            + SOURCE_COLOR_FLOATS
            + COLOR_FLOATS
            + COLOR_BIAS_FLOATS
            + COLOR_MULTIPLIER_FLOATS
            + TARGET_SIZE_FLOATS
            + SIZE_FLOATS;

    private static final int VERTEX_PER_TRIANGLE = 3;

    public static final int TRIANGLE_COUNT = 3;
    public static final int VERTEX_COUNT = TRIANGLE_COUNT * VERTEX_PER_TRIANGLE;
    public static final int FLOATS_COUNT = VERTEX_COUNT * VERTEX_FLOATS;

    private GLShaderProgram program;

    public int getVertexCount() {
        return VERTEX_COUNT;
    }

    public void initGLPrograms() {
        initProgram();
    }

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "edge";

    private static final String SHADERS_EDGE_LINE_SOURCE = "edge-line-directed";

    private void initProgram() {
        program = new GLShaderProgram(SHADERS_ROOT, SHADERS_EDGE_LINE_SOURCE, SHADERS_EDGE_LINE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
                .addUniformName(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MIN)
                .addUniformName(UNIFORM_NAME_EDGE_SCALE_MAX)
                .addUniformName(UNIFORM_NAME_MIN_WEIGHT)
                .addUniformName(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION_TARGET, SHADER_POSITION_TARGET_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SOURCE_COLOR, SHADER_SOURCE_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR_BIAS, SHADER_COLOR_BIAS_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR_MULTIPLIER, SHADER_COLOR_MULTIPLIER_LOCATION)
                .addAttribLocation(ATTRIB_NAME_TARGET_SIZE, SHADER_TARGET_SIZE_LOCATION)
                .init();
    }

    public void drawArraysSingleInstance() {
        GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, VERTEX_COUNT);
    }

    public void drawArraysMultipleInstance(int drawBatchCount) {
        //Multiple lines, attributes must be in the buffer once per vertex count:
        GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, VERTEX_COUNT * drawBatchCount);
    }

    public void drawInstanced(float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, int instanceCount, int instancesOffset, float scale, float minWeight, float maxWeight) {
        useProgram(mvpFloats, backgroundColorFloats, colorLightenFactor, scale, minWeight, maxWeight);
        if (instancesOffset > 0) {
            GL42.glDrawArraysInstancedBaseInstance(GL20.GL_TRIANGLES, 0, VERTEX_COUNT, instanceCount, instancesOffset);
        } else {
            GL31.glDrawArraysInstanced(GL20.GL_TRIANGLES, 0, VERTEX_COUNT, instanceCount);
        }
        stopUsingProgram();
    }

    public void useProgram(float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, float scale, float minWeight, float maxWeight) {
        program.use();
        prepareProgramData(mvpFloats, backgroundColorFloats, colorLightenFactor, scale, minWeight, maxWeight);
    }

    public void stopUsingProgram() {
        program.stopUsing();
    }

    private void prepareProgramData(float[] mvpFloats, float[] backgroundColorFloats, float colorLightenFactor, float scale, float minWeight, float maxWeight) {
        GL20.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform4fv(program.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), backgroundColorFloats);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR), colorLightenFactor);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MIN), EDGE_SCALE_MIN * scale);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_EDGE_SCALE_MAX), EDGE_SCALE_MAX * scale);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_MIN_WEIGHT), minWeight);

        if (NumberUtils.equalsEpsilon(minWeight, maxWeight, 1e-3f)) {
            GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), 1);
        } else {
            GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_WEIGHT_DIFFERENCE_DIVISOR), maxWeight - minWeight);
        }
    }

    public static float[] getVertexData() {
        //lineEnd, sideVector, arrowHeight
        return new float[]{
            //First 6 are the edge line as a rectangle:
            //Triangle 1
            0, 1, 0,// bottom right corner
            0, -1, 0,// bottom left corner
            1, -1, -1,// top left corner
            //Triangle 2
            1, -1, -1,// top left corner
            1, 1, -1,// top right corner
            0, 1, 0,// bottom right corner
            //Last 3 are the arrow tip triangle:
            1, 0, 0,//arrow tip
            1, -2, -1,// arrow bottom left vertex
            1, 2, -1// arrow bottom right vertex
        };
    }
}
