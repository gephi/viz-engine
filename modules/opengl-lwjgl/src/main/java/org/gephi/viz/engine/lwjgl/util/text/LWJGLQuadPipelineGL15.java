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
import static org.lwjgl.opengl.GL15.*;

/**
 * {@link LWJGLQuadPipeline} for use with OpenGL 1.5.
 */
/*@VisibleForTesting*/
/*@NotThreadSafe*/
public final class LWJGLQuadPipelineGL15 extends AbstractLWJGLQuadPipeline {

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
     * OpenGL handle to vertex buffer.
     */
    /*@Nonnegative*/
    private final int vbo;

    /**
     * Constructs a {@link LWJGLQuadPipelineGL15}.
     *
     * @throws NullPointerException if context is null
     */
    /*@VisibleForTesting*/
    public LWJGLQuadPipelineGL15() {

        super(VERTS_PER_PRIM, PRIMS_PER_QUAD);

        this.vbo = createVertexBufferObject(BYTES_PER_BUFFER);
    }

    @Override
    public void beginRendering() {

        super.beginRendering();

        // Change state
        glPushClientAttrib(GL_CLIENT_ALL_ATTRIB_BITS);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Points
        glEnableClientState(GL_VERTEX_ARRAY);
        glVertexPointer(
                FLOATS_PER_POINT,   // size
                GL_FLOAT,       // type
                STRIDE,             // stride
                POINT_OFFSET);      // offset

        // Coordinates
        glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        glTexCoordPointer(
                FLOATS_PER_COORD,   // size
                GL_FLOAT,       // type
                STRIDE,             // stride
                COORD_OFFSET);      // offset
    }

    @Override
    public void dispose() {

        super.dispose();

        // Delete the vertex buffer object
        glDeleteBuffers(vbo);
    }

    @Override
    protected void doFlush() {

        // Upload data
        rewind();
        //TODO check signature, had to remove size
        glBufferSubData(
                GL_ARRAY_BUFFER, // target
                0,                   // offset
                getData());          // data

        // Draw
        glDrawArrays(
                GL_QUADS,         // mode
                0,                    // first
                getSizeInVertices()); // count

        clear();
    }

    @Override
    public void endRendering() {

        super.endRendering();

        // Restore state
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glPopClientAttrib();
    }
}
