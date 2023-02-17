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

/**
 * Two-dimensional, grayscale OpenGL texture.
 */
public final class LWJGLGrayTexture2D extends LWJGLTexture2D {

    /**
     * Creates a two-dimensional, grayscale texture.
     *
     * @param width Size of texture on X axis
     * @param height Size of texture on Y axis
     * @param smooth True to interpolate samples
     * @param mipmap True for high quality
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if width or height is negative
     */
    LWJGLGrayTexture2D(final int width,
                       final int height,
                       final boolean smooth,
                       final boolean mipmap) {
        super(width, height, smooth, mipmap);
    }

    @Override
    public int getFormat() {
        //TODO
//        return gl.getGLProfile().isGL2() ? GL2.GL_LUMINANCE : GL3.GL_RED;
        return GL_LUMINANCE;
    }

    @Override
    public int getInternalFormat() {
        //TODO
//        return getGLProfile().isGL2() ? GL2.GL_INTENSITY : GL3.GL_RED;
        return GL_INTENSITY;
    }
}
