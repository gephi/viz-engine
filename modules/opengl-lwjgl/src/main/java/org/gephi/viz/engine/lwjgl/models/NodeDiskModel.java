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

    private GLShaderProgram program;
    private GLShaderProgram programWithSelection;

    public void initGLPrograms() {
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

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "node";

    private static final String SHADERS_NODE_CIRCLE_SOURCE = "node";
    private static final String SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION = "node_with_selection";

    public void drawArraysSingleInstance(int firstVertexIndex, int vertexCount) {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, firstVertexIndex, vertexCount);
    }

    public void drawInstanced(int vertexOffset, int vertexCount, int instanceCount) {
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
}
