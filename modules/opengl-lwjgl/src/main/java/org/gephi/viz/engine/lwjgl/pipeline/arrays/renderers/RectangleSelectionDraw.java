package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.util.gl.Constants;
import org.joml.Vector2f;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL20;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.EnumSet;

import static org.gephi.viz.engine.util.gl.Constants.*;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_VERTEX_ARRAY;
import static org.lwjgl.opengl.GL11.glEnableClientState;
import static org.lwjgl.opengl.GL11.glVertexPointer;
import static org.lwjgl.opengl.GL15.*;

public class RectangleSelectionDraw implements Renderer<LWJGLRenderingTarget> {
    private final VizEngine engine;

    private final float[] mvpFloats = new float[16];
    private GraphSelection graphSelection ;

    public RectangleSelectionDraw(VizEngine engine) {
        this.engine = engine;

    }
    @Override
    public String getCategory() {
        return PipelineCategory.SELECTION;
    }

    @Override
    public int getPreferenceInCategory() {
        return 0;
    }

    @Override
    public String getName() {
        return "Rectangle Selection";
    }

    @Override
    public void init(LWJGLRenderingTarget target) {

    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void worldUpdated(LWJGLRenderingTarget target) {

    }
    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "rectangleSelection";
    private GLShaderProgram program;
    @Override
    public void render(LWJGLRenderingTarget target, RenderingLayer layer) {
        this.graphSelection  = engine.getLookup().lookup(GraphSelection.class);
        if(graphSelection!= null && graphSelection.getCurrentPosition() != null && graphSelection.getInitialPosition()!=null) {
             program = new GLShaderProgram(SHADERS_ROOT, "rectangleSelection", "rectangleSelection")
                    .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
                    .init();
            program.use();
            engine.getModelViewProjectionMatrixFloats(mvpFloats);
            glfwSetErrorCallback( GLFWErrorCallback.createPrint(System.err));
            GL20.glUniformMatrix4fv(program.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);

            Vector2f topLeft = graphSelection.getInitialPosition();
            Vector2f topRight = new Vector2f(graphSelection.getCurrentPosition().x, graphSelection.getInitialPosition().y);
            Vector2f bottomRight = graphSelection.getCurrentPosition();
            Vector2f bottomLeft = new Vector2f(graphSelection.getInitialPosition().x, graphSelection.getCurrentPosition().y);

            float[] rectangleVertexData = {
                    bottomLeft.x,
                    bottomLeft.y,
                    topLeft.x,
                    topLeft.y,
                    topRight.x,
                    topRight.y,
                    bottomRight.x,
                    bottomRight.y
            };
            System.out.println(Arrays.toString(mvpFloats));

            int vbo = glGenBuffers();
            int ibo = glGenBuffers();

            int[] indices = {0, 1, 2, 2 ,0 ,3};
            glBindBuffer(GL_ARRAY_BUFFER, vbo);
            glBufferData(GL_ARRAY_BUFFER, (FloatBuffer) BufferUtils.createFloatBuffer(rectangleVertexData.length).put(rectangleVertexData).flip(), GL_STATIC_DRAW);
            glEnableClientState(GL_VERTEX_ARRAY);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, (IntBuffer) BufferUtils.createIntBuffer(indices.length).put(indices).flip(), GL_STATIC_DRAW);
            glVertexPointer(2, GL_FLOAT, 0, 0L);
            glColor3f(.5f,.7f,.9f);

            glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0L);
            program.stopUsing();

        }

    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.BACK);
    }
}
