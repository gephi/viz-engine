/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package org.gephi.viz.engine.lwjgl.util.text;

import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.opengl.GLCapabilities;


/**
 * Utility for drawing glyphs with OpenGL 3.
 */
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class LWJGLGlyphRendererGL3 extends AbstractLWJGLGlyphRenderer {

    /**
     * Source code of vertex shader.
     */
    /*@Nonnull*/
    private static final String VERT_SOURCE =
        "#version 140\n" +
        "uniform mat4 MVPMatrix;\n" +
        "in vec4 MCVertex;\n" +
        "in vec2 TexCoord0;\n" +
        "out vec2 Coord0;\n" +
        "void main() {\n" +
        "   gl_Position = MVPMatrix * MCVertex;\n" +
        "   Coord0 = TexCoord0;\n" +
        "}\n";

    /**
     * Source code of fragment shader.
     */
    /*@Nonnull*/
    private static final String FRAG_SOURCE =
        "#version 140\n" +
        "uniform sampler2D Texture;\n" +
        "uniform vec4 Color=vec4(1,1,1,1);\n" +
        "in vec2 Coord0;\n" +
        "out vec4 FragColor;\n" +
        "void main() {\n" +
        "   float sample;\n" +
        "   sample = texture(Texture,Coord0).r;\n" +
        "   FragColor = Color * sample;\n" +
        "}\n";

    /**
     * True if blending needs to be reset.
     */
    private boolean restoreBlending;

    /**
     * True if depth test needs to be reset.
     */
    private boolean restoreDepthTest;

    /**
     * Shader program.
     */
    /*@Nonnegative*/
    private final int program;

    /**
     * Uniform for modelview projection.
     */
    /*@Nonnull*/
    private final Mat4Uniform transform;

    /**
     * Uniform for color of glyphs.
     */
    /*@Nonnull*/
    private final Vec4Uniform color;

    /**
     * Width of last orthographic render.
     */
    /*@Nonnegative*/
    private int lastWidth = 0;

    /**
     * Height of last orthographic render
     */
    /*@Nonnegative*/
    private int lastHeight = 0;

    /**
     * Constructs a {@link LWJGLGlyphRendererGL3}.
     *
     * @throws NullPointerException if context is null
     */
    /*@VisibleForTesting*/
    public LWJGLGlyphRendererGL3() {
        this.program = LWJGLShaderLoader.loadProgram(VERT_SOURCE, FRAG_SOURCE);
        this.transform = new Mat4Uniform(program, "MVPMatrix");
        this.color = new Vec4Uniform(program, "Color");
    }

    @Override
    protected void doBeginRendering(final boolean ortho,
                                    final int width,
                                    final int height,
                                    final boolean disableDepthTest) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Activate program
        glUseProgram(program);

        // Check blending and depth test
        restoreBlending = false;
        if (!glIsEnabled(GL_BLEND)) {
            glEnable(GL_BLEND);
            glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
            restoreBlending = true;
        }
        restoreDepthTest = false;
        if (disableDepthTest && glIsEnabled(GL_DEPTH_TEST)) {
            glDisable(GL_DEPTH_TEST);
            restoreDepthTest = true;
        }

        // Check transform
        if (ortho) {
            doSetTransformOrtho(width, height);
        }
    }

    @Override
    protected LWJGLQuadPipeline doCreateQuadPipeline(GLCapabilities capabilities) {
        return new LWJGLQuadPipelineGL30(program);
    }

    protected void doDispose() {
        glUseProgram(0);
        glDeleteProgram(program);
    }

    @Override
    protected void doEndRendering() {
        // Deactivate program
        glUseProgram(0);

        // Check blending and depth test
        if (restoreBlending) {
            glDisable(GL_BLEND);
        }
        if (restoreDepthTest) {
            glEnable(GL_DEPTH_TEST);
        }
    }

    @Override
    protected void doSetColor(final float r,
                              final float g,
                              final float b,
                              final float a) {
        color.value[0] = r;
        color.value[1] = g;
        color.value[2] = b;
        color.value[3] = a;
        color.update();
    }

    @Override
    protected void doSetTransform3d(final float[] value,
                                    final boolean transpose) {
        Check.notNull(value, "Value cannot be null");

        glUniformMatrix4fv(transform.location, transpose, value);
        transform.dirty = true;
    }

    @Override
    protected void doSetTransformOrtho(final int width,
                                       final int height) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Recompute if width and height changed
        if (width != lastWidth || height != lastHeight) {
            Projection.orthographic(transform.value, width, height);
            transform.transpose = true;
            transform.dirty = true;
            lastWidth = width;
            lastHeight = height;
        }

        // Upload if made dirty anywhere
        if (transform.dirty) {
            transform.update();
            transform.dirty = false;
        }
    }

    @Override
    public boolean getUseVertexArrays() {
        return true;
    }

    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        // empty
    }
}
