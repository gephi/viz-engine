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

import static org.lwjgl.opengl.GL11.*;
import java.nio.FloatBuffer;


/**
 * {@link LWJGLQuadPipeline} for use with OpenGL 1.1.
 */
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class LWJGLQuadPipelineGL11 extends AbstractLWJGLQuadPipeline {

    /**
     * Number of vertices per primitive.
     */
    /*@Nonnegative*/
    private static final int VERTS_PER_PRIM = 4;

    /**
     * Number of primitives per quad.
     */
    /*@Nonnegative*/
    private static final int PRIMS_PER_QUAD = 1;

    /**
     * Vertex array for points.
     */
    /*@Nonnull*/
    private final FloatBuffer pointsArray;

    /**
     * Vertex array for texture coordinates.
     */
    /*@Nonnull*/
    private final FloatBuffer coordsArray;

    /**
     * Constructs a {@link LWJGLQuadPipelineGL11}.
     */
    /*@VisibleForTesting*/
    public LWJGLQuadPipelineGL11() {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        pointsArray = createFloatBufferView(getData(), POINT_OFFSET);
        coordsArray = createFloatBufferView(getData(), COORD_OFFSET);
    }

    @Override
    public void beginRendering() {

        super.beginRendering();

        // Push state
        //TODO Check
        glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);

        // Points
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(
                FLOATS_PER_POINT,   // size
                GL_FLOAT,       // type
                STRIDE,             // stride
                pointsArray);       // pointer

        // Coordinates
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(
                FLOATS_PER_COORD,   // size
                GL_FLOAT,       // type
                STRIDE,             // stride
                coordsArray);       // pointer
    }

    /**
     * Makes a view of a float buffer at a certain position.
     *
     * @param fb Original float buffer
     * @param position Index to start view at
     * @return Resulting float buffer
     * @throws NullPointerException if float buffer is null
     * @throws IllegalArgumentException if position is negative
     */
    /*@Nonnull*/
    private static FloatBuffer createFloatBufferView(final FloatBuffer fb,
                                                     /*@Nonnegative*/ final int position) {

        Check.notNull(fb, "Buffer cannot be null");
        Check.argument(position >= 0, "Possition cannot be negative");

        // Store original position
        final int original = fb.position();

        // Make a view at desired position
        fb.position(position);
        final FloatBuffer view = fb.asReadOnlyBuffer();

        // Reset buffer to original position
        fb.position(original);

        return view;
    }

    @Override
    protected void doFlush() {

        glDrawArrays(
                GL_QUADS,         // mode
                0,                    // first
                getSizeInVertices()); // count
        clear();
    }

    @Override
    public void endRendering() {

        super.endRendering();

        // Pop state
        glPopClientAttrib();
    }
}
