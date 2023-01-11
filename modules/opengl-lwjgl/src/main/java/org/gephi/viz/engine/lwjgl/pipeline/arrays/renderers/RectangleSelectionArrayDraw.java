package org.gephi.viz.engine.lwjgl.pipeline.arrays.renderers;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.util.gl.GLBufferMutable;
import org.gephi.viz.engine.lwjgl.util.gl.GLShaderProgram;
import org.gephi.viz.engine.lwjgl.util.gl.GLVertexArrayObject;
import org.gephi.viz.engine.lwjgl.util.gl.ManagedDirectBuffer;
import org.gephi.viz.engine.pipeline.PipelineCategory;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.util.gl.Constants;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.joml.Vector2f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL15.*;

import java.nio.FloatBuffer;
import java.util.EnumSet;

import static org.gephi.viz.engine.util.gl.Constants.*;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;

public class RectangleSelectionArrayDraw implements Renderer<LWJGLRenderingTarget> {
    private final VizEngine engine;

    final float[] mvpFloats = new float[16];

    private static final int VERT_BUFFER = 0;

    public static final int VERTEX_COUNT = 6; // 2 triangles
    public static final int VERTEX_FLOATS = 2;

    private final int[] bufferName = new int[1];
    private ManagedDirectBuffer rectangleVertexDataBuffer;
    private GLBufferMutable vertexGLBuffer;
    private SelectionRectangleVAO vao;

    public RectangleSelectionArrayDraw(VizEngine engine) {
        this.engine = engine;
    }

    @Override
    public String getCategory() {
        return PipelineCategory.RECTANGLE_SELECTION;
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
        shaderProgram = new GLShaderProgram(SHADERS_ROOT, "rectangleSelection", "rectangleSelection")
            .addUniformName(UNIFORM_NAME_MODEL_VIEW_PROJECTION)
            .addAttribLocation(ATTRIB_NAME_VERT, SHADER_VERT_LOCATION)
            .init();

        glGenBuffers(bufferName);

        rectangleVertexDataBuffer = new ManagedDirectBuffer(GL_FLOAT, Float.BYTES * VERTEX_COUNT * VERTEX_FLOATS);

        vertexGLBuffer = new GLBufferMutable(bufferName[VERT_BUFFER], GLBufferMutable.GL_BUFFER_TYPE_ARRAY);
        vertexGLBuffer.bind();
        vertexGLBuffer.init(Float.BYTES * VERTEX_COUNT * VERTEX_FLOATS, GLBufferMutable.GL_BUFFER_USAGE_DYNAMIC_DRAW);
        vertexGLBuffer.unbind();

        vao = new SelectionRectangleVAO(
            engine.getLookup().lookup(GLCapabilities.class),
            engine.getLookup().lookup(OpenGLOptions.class)
        );
    }

    @Override
    public int getOrder() {
        return 0;
    }

    private boolean render = false;

    @Override
    public void worldUpdated(LWJGLRenderingTarget target) {
        final GraphSelection graphSelection = engine.getLookup().lookup(GraphSelection.class);

        if (graphSelection.getMode() != GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION) {
            return;
        }

        final Vector2f initialPosition = graphSelection.getRectangleInitialPosition();
        final Vector2f currentPosition = graphSelection.getRectangleCurrentPosition();

        if (initialPosition != null && currentPosition != null) {

            final float minX = Math.min(initialPosition.x, currentPosition.x);
            final float minY = Math.min(initialPosition.y, currentPosition.y);
            final float maxX = Math.max(initialPosition.x, currentPosition.x);
            final float maxY = Math.max(initialPosition.y, currentPosition.y);

            final FloatBuffer floatBuffer = rectangleVertexDataBuffer.floatBuffer();

            final float[] rectangleVertexData = {
                //Triangle 1:
                minX,
                minY,
                minX,
                maxY,
                maxX,
                minY,
                //Triangle 2:
                minX,
                maxY,
                maxX,
                maxY,
                maxX,
                minY
            };

            floatBuffer.put(rectangleVertexData);
            floatBuffer.position(0);

            vertexGLBuffer.bind();
            vertexGLBuffer.update(floatBuffer);
            vertexGLBuffer.unbind();

            render = true;
        } else {
            render = false;
        }
    }

    private static final String SHADERS_ROOT = Constants.SHADERS_ROOT + "rectangleSelection";
    private GLShaderProgram shaderProgram;

    @Override
    public void render(LWJGLRenderingTarget target, RenderingLayer layer) {
        if (render) {
            shaderProgram.use();
            engine.getModelViewProjectionMatrixFloats(mvpFloats);

            GL20.glUniformMatrix4fv(shaderProgram.getUniformLocation(UNIFORM_NAME_MODEL_VIEW_PROJECTION), false, mvpFloats);

            vao.use();

            final boolean blendEnabled = glGetBoolean(GL_BLEND);
            final int blendFunc = glGetInteger(GL_BLEND_DST_ALPHA);

            if (!blendEnabled) {
                glEnable(GL_BLEND);
            }

            if (blendFunc != GL_ONE_MINUS_SRC_ALPHA) {
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }

            glDrawArrays(GL_TRIANGLES, 0, VERTEX_COUNT);

            //Restore state:
            if (!blendEnabled) {
                glDisable(GL_BLEND);
            }
            if (blendFunc != GL_ONE_MINUS_SRC_ALPHA) {
                glBlendFunc(GL_SRC_ALPHA, blendFunc);
            }

            vao.stopUsing();

            shaderProgram.stopUsing();
        }
    }

    @Override
    public EnumSet<RenderingLayer> getLayers() {
        return EnumSet.of(RenderingLayer.FRONT4);
    }

    private class SelectionRectangleVAO extends GLVertexArrayObject {

        public SelectionRectangleVAO(GLCapabilities capabilities, OpenGLOptions openGLOptions) {
            super(capabilities, openGLOptions);
        }

        @Override
        protected void configure() {
            vertexGLBuffer.bind();
            {
                glVertexAttribPointer(SHADER_VERT_LOCATION, VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBuffer.unbind();
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            return new int[]{
                SHADER_VERT_LOCATION
            };
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            return null;
        }
    }
}
