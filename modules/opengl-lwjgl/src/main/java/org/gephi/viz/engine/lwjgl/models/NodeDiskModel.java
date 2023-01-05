package org.gephi.viz.engine.lwjgl.models;

import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.util.gl.Constants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;

import static org.gephi.viz.engine.util.gl.Constants.ATTRIB_NAME_COLOR;
import static org.gephi.viz.engine.util.gl.Constants.ATTRIB_NAME_POSITION;
import static org.gephi.viz.engine.util.gl.Constants.ATTRIB_NAME_SIZE;
import static org.gephi.viz.engine.util.gl.Constants.ATTRIB_NAME_VERT;
import static org.gephi.viz.engine.util.gl.Constants.SHADER_COLOR_LOCATION;
import static org.gephi.viz.engine.util.gl.Constants.SHADER_POSITION_LOCATION;
import static org.gephi.viz.engine.util.gl.Constants.SHADER_SIZE_LOCATION;
import static org.gephi.viz.engine.util.gl.Constants.SHADER_VERT_LOCATION;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_BACKGROUND_COLOR;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_COLOR_BIAS;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_COLOR_LIGHTEN_FACTOR;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_COLOR_MULTIPLIER;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_MODEL_VIEW_PROJECTION;
import static org.gephi.viz.engine.util.gl.Constants.UNIFORM_NAME_SIZE_MULTIPLIER;

/**
 * @author Eduardo Ramos
 */
public class NodeDiskModel {

    public static final int VERTEX_FLOATS = 2;
    public static final int POSITION_FLOATS = 2;
    public static final int COLOR_FLOATS = 1;
    public static final int SIZE_FLOATS = 1;

    public static final int TOTAL_ATTRIBUTES_FLOATS
            = POSITION_FLOATS
            + COLOR_FLOATS
            + SIZE_FLOATS;

    private final int triangleAmount;
    private final float[] vertexData;
    private final int vertexCount;

    private GLShaderProgram program;
    private GLShaderProgram programWithSelection;

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

    public void initGLPrograms() {
        initProgram();
    }

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "node";

    private static final String SHADERS_NODE_CIRCLE_SOURCE = "node";
    private static final String SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION = "node_with_selection";

    private void initProgram() {
        program = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE, SHADERS_NODE_CIRCLE_SOURCE)
                .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
                .addUniformName(UNIFORM_NAME_SIZE_MULTIPLIER)
                .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
                .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
                .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
                .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
                .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
                .init();

        programWithSelection = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION, SHADERS_NODE_CIRCLE_SOURCE)
            .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
            .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
            .addUniformName(UNIFORM_NAME_SIZE_MULTIPLIER)
            .addUniformName(UNIFORM_NAME_COLOR_BIAS)
            .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
            .addUniformName(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR)
            .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
            .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
            .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
            .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
            .init();
    }

    public void drawArraysSingleInstance(int firstVertexIndex, int vertexCount) {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, firstVertexIndex, vertexCount);
    }

    public void drawInstanced(int vertexOffset, int instanceCount) {
        if (instanceCount <= 0) {
            return;
        }
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, vertexOffset, vertexCount, instanceCount);
    }

    public void useProgramWithSelection(float[] mvpFloats, float[] backgroundColorFloats, float sizeMultiplier, float colorBias, float colorMultiplier, float colorLightenFactor) {
        //Circle:
        programWithSelection.use();

        GL20.glUniformMatrix4fv(programWithSelection.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform4fv(programWithSelection.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), backgroundColorFloats);
        GL20.glUniform1f(programWithSelection.getUniformLocation(UNIFORM_NAME_SIZE_MULTIPLIER), sizeMultiplier);
        GL20.glUniform1f(programWithSelection.getUniformLocation(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR), colorLightenFactor);
        GL20.glUniform1f(programWithSelection.getUniformLocation(UNIFORM_NAME_COLOR_BIAS), colorBias);
        GL20.glUniform1f(programWithSelection.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
    }

    public void useProgram(float[] mvpFloats, float[] backgroundColorFloats, float sizeMultiplier, float colorMultiplier) {
        //Circle:
        program.use();

        GL20.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform4fv(program.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), backgroundColorFloats);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_SIZE_MULTIPLIER), sizeMultiplier);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
    }

    public void stopUsingProgram() {
        GL20.glUseProgram(0);
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
