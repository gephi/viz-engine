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

import java.awt.Rectangle;
import java.nio.ByteBuffer;


/**
 * Two-dimensional OpenGL texture.
 */
public interface Texture2D extends Texture {

    /**
     * Allocates a 2D texture for use with a backing store.
     *
     */
    void allocate();

    /**
     * Determines the proper texture format for an OpenGL context.
     *
     * @return Texture format enumeration for OpenGL context
     * @throws NullPointerException if context is null (optional)
     */
    int getFormat();

    /**
     * Determines the proper internal texture format for an OpenGL context.
     *
     * @return Internal texture format enumeration for OpenGL context
     * @throws NullPointerException if context is null (optional)
     */
    int getInternalFormat();

    /**
     * Updates the texture.
     *
     * <p>
     * Copies any areas marked with {@link #mark(int, int, int, int)} from the local image to the
     * OpenGL texture.  Only those areas will be modified.
     *
     * @param pixels Data of entire image
     * @param area Region to update
     * @throws NullPointerException if context, pixels, or area is null
     */
    void update(final ByteBuffer pixels, final Rectangle area);
}
