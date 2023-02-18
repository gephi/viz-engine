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

import static org.lwjgl.opengl.GL20.*;

import org.lwjgl.opengl.GLCapabilities;

/**
 * {@link LWJGLGlyphRenderer} for use with OpenGL 2.
 */
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class LWJGLGlyphRendererGL2 extends AbstractLWJGLGlyphRenderer {

    /**
     * True if using vertex arrays.
     */
    private boolean useVertexArrays = true;

    /**
     * Constructs a {@link LWJGLGlyphRendererGL2}.
     */
    /*@VisibleForTesting*/
    public LWJGLGlyphRendererGL2() {
        // empty
    }

    @Override
    protected void doBeginRendering(final boolean ortho,
                                    final int width,
                                    final int height,
                                    final boolean disableDepthTest) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Change general settings
        glPushAttrib(getAttribMask(ortho));
        glDisable(GL_LIGHTING);
        glEnable(GL_BLEND);
        glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_TEXTURE_2D);
        glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_MODULATE);

        // Set up transformations
        if (ortho) {
            if (disableDepthTest) {
                glDisable(GL_DEPTH_TEST);
            }
            glDisable(GL_CULL_FACE);
            glMatrixMode(GL_PROJECTION);
            glPushMatrix();
            glLoadIdentity();
            glOrtho(0, width, 0, height, -1, +1);
            glMatrixMode(GL_MODELVIEW);
            glPushMatrix();
            glLoadIdentity();
            glMatrixMode(GL_TEXTURE);
            glPushMatrix();
            glLoadIdentity();
        }
    }

    protected LWJGLQuadPipeline doCreateQuadPipeline(GLCapabilities capabilities) {

        if (useVertexArrays) {
            //TODO Check versioning
            if (capabilities.OpenGL15) {
                return new LWJGLQuadPipelineGL15();
            } else if (capabilities.OpenGL11) {
                return new LWJGLQuadPipelineGL11();
            } else {
                return new LWJGLQuadPipelineGL10();
            }
        } else {
            return new LWJGLQuadPipelineGL10();
        }
    }

    protected void doDispose() {

    }

    @Override
    protected void doEndRendering() {
        // Reset transformations
        if (isOrthoMode()) {
            glMatrixMode(GL_PROJECTION);
            glPopMatrix();
            glMatrixMode(GL_MODELVIEW);
            glPopMatrix();
            glMatrixMode(GL_TEXTURE);
            glPopMatrix();
        }

        // Reset general settings
        glPopAttrib();
    }

    @Override
    protected void doSetColor(final float r,
                              final float g,
                              final float b,
                              final float a) {

        glColor4f(r, g, b, a);
    }

    @Override
    protected void doSetTransform3d(final float[] value,
                                    final boolean transpose) {

        Check.notNull(value, "Value cannot be null");

        // FIXME: Could implement this...
        throw new UnsupportedOperationException("Use standard GL instead");
    }

    @Override
    protected void doSetTransformOrtho(final int width,
                                       final int height) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        glMatrixMode(GL_PROJECTION);
        glPushMatrix();
        glLoadIdentity();
        glOrtho(0, width, 0, height, -1, +1);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
    }

    /**
     * Returns attribute bits for {@code glPushAttrib} calls.
     *
     * @param ortho True if using orthographic projection
     * @return Attribute bits for {@code glPushAttrib} calls
     */
    private static int getAttribMask(final boolean ortho) {
        return GL_ENABLE_BIT |
               GL_TEXTURE_BIT |
               GL_COLOR_BUFFER_BIT |
               (ortho ? (GL_DEPTH_BUFFER_BIT | GL_TRANSFORM_BIT) : 0);
    }

    @Override
    public boolean getUseVertexArrays() {
        return useVertexArrays;
    }

    @Override
    public void setUseVertexArrays(final boolean useVertexArrays) {
        if (useVertexArrays != this.useVertexArrays) {
            dirtyPipeline();
            this.useVertexArrays = useVertexArrays;
        }
    }
}
