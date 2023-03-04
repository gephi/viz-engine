package org.gephi.viz.engine.lwjgl.models;

import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.util.gl.Constants;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL43;

import static org.gephi.viz.engine.util.gl.Constants.*;
import static org.gephi.viz.engine.util.gl.GLConstants.INDIRECT_DRAW_COMMAND_BYTES;

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
    private GLShaderProgram programWithSelectionSelected;
    private GLShaderProgram programWithSelectionUnselected;

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "node";

    private static final String SHADERS_NODE_CIRCLE_SOURCE = "node";
    private static final String SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION_SELECTED = "node_with_selection_selected";
    private static final String SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION_UNSELECTED = "node_with_selection_unselected";

    public void initGLPrograms() {
        program = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE, SHADERS_NODE_CIRCLE_SOURCE)
            .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
            .addUniformName(UNIFORM_NAME_SIZE_MULTIPLIER)
            .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
            .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
            .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
            .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
            .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
            .init();

        programWithSelectionSelected = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION_SELECTED, SHADERS_NODE_CIRCLE_SOURCE)
            .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
            .addUniformName(UNIFORM_NAME_SIZE_MULTIPLIER)
            .addUniformName(UNIFORM_NAME_COLOR_BIAS)
            .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
            .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
            .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
            .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
            .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
            .init();

        programWithSelectionUnselected = new GLShaderProgram(SHADERS_ROOT, SHADERS_NODE_CIRCLE_SOURCE_WITH_SELECTION_UNSELECTED, SHADERS_NODE_CIRCLE_SOURCE)
            .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
            .addUniformName(UNIFORM_NAME_COLOR_MULTIPLIER)
            .addUniformName(UNIFORM_NAME_BACKGROUND_COLOR)
            .addUniformName(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR)
            .addUniformName(UNIFORM_NAME_SIZE_MULTIPLIER)
            .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
            .addAttribLocation(ATTRIB_NAME_POSITION, SHADER_POSITION_LOCATION)
            .addAttribLocation(ATTRIB_NAME_COLOR, SHADER_COLOR_LOCATION)
            .addAttribLocation(ATTRIB_NAME_SIZE, SHADER_SIZE_LOCATION)
            .init();
    }

    public void drawArraysSingleInstance(int firstVertexIndex, int vertexCount) {
        GL11.glDrawArrays(GL11.GL_TRIANGLES, firstVertexIndex, vertexCount);
    }

    public void drawInstanced(int vertexOffset, int vertexCount, int instanceCount) {
        if (instanceCount <= 0) {
            return;
        }
        GL31.glDrawArraysInstanced(GL11.GL_TRIANGLES, vertexOffset, vertexCount, instanceCount);
    }

    public void drawIndirect(int instanceCount, int instancesOffset) {
        if (instanceCount <= 0) {
            return;
        }
        GL43.glMultiDrawArraysIndirect(GL11.GL_TRIANGLES, (long) instancesOffset * INDIRECT_DRAW_COMMAND_BYTES, instanceCount, 0);
    }

    public void useProgramWithSelectionSelected(float[] mvpFloats, float sizeMultiplier, float colorBias, float colorMultiplier) {
        //Circle:
        programWithSelectionSelected.use();

        GL20.glUniformMatrix4fv(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_SIZE_MULTIPLIER), sizeMultiplier);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_COLOR_BIAS), colorBias);
        GL20.glUniform1f(programWithSelectionSelected.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
    }

    public void useProgramWithSelectionUnselected(float[] mvpFloats, float sizeMultiplier, float[] backgroundColorFloats, float colorLightenFactor, float colorMultiplier) {
        //Circle:
        programWithSelectionUnselected.use();

        GL20.glUniformMatrix4fv(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
        GL20.glUniform4fv(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_BACKGROUND_COLOR), backgroundColorFloats);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_COLOR_LIGHTEN_FACTOR), colorLightenFactor);
        GL20.glUniform1f(programWithSelectionUnselected.getUniformLocation(UNIFORM_NAME_SIZE_MULTIPLIER), sizeMultiplier);
    }

    public void useProgram(float[] mvpFloats, float sizeMultiplier, float colorMultiplier) {
        //Circle:
        program.use();

        GL20.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_SIZE_MULTIPLIER), sizeMultiplier);
        GL20.glUniform1f(program.getUniformLocation(UNIFORM_NAME_COLOR_MULTIPLIER), colorMultiplier);
    }

    public void stopUsingProgram() {
        GL20.glUseProgram(0);
    }
}
