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


import org.lwjgl.opengl.GLCapabilities;

/**
 * Utility for drawing glyphs.
 */
public interface LWJGLGlyphRenderer {

    /**
     * Registers an {@link EventListener} with this {@link LWJGLGlyphRenderer}.
     *
     * @param listener Listener to register
     * @throws NullPointerException if listener is null
     */
    void addListener(EventListener listener);

    /**
     * Starts a render cycle with this {@link LWJGLGlyphRenderer}.
     *
     * @param capabilities GL capabilities
     * @param ortho True if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest True if should ignore depth values
     * @throws IllegalArgumentException if width or height is negative
     */
    void beginRendering(GLCapabilities capabilities,
                        boolean ortho,
                        int width,
                        int height,
                        boolean disableDepthTest);

    /**
     * Frees resources used by this {@link LWJGLGlyphRenderer}.
     *
     */
    void dispose();

    /**
     * Draws a glyph with this {@link LWJGLGlyphRenderer}.
     *
     * @param glyph Visual representation of a character
     * @param x Position to draw on X axis, which may be negative
     * @param y Position to draw on Y axis, which may be negative
     * @param z Position to draw on Z axis, which may be negative
     * @param scale Relative size of glyph, which may be negative
     * @param coords Texture coordinates of glyph
     * @return Distance to next character, which may be negative
     * @throws NullPointerException if glyph, or texture coordinate is null
     */
    float drawGlyph(Glyph glyph,
                    float x,
                    float y,
                    float z,
                    float scale,
                    TextureCoords coords);

    /**
     * Finishes a render cycle with this {@link LWJGLGlyphRenderer}.
     */
    void endRendering();

    /**
     * Forces all stored text to be rendered.
     *
     * @throws IllegalStateException if not in a render cycle
     */
    void flush();

    /**
     * Checks if this {@link LWJGLGlyphRenderer} is using vertex arrays.
     *
     * @return True if this renderer is using vertex arrays
     */
    boolean getUseVertexArrays();

    /**
     * Changes the color used to draw the text.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     */
    void setColor(float r, float g, float b, float a);

    /**
     * Changes the transformation matrix for drawing in 3D.
     *
     * @param value Matrix as float array
     * @param transpose True if array is in in row-major order
     * @throws IndexOutOfBoundsException if value's length is less than sixteen
     * @throws IllegalStateException if in orthographic mode
     */
    void setTransform(float[] value, boolean transpose);

    /**
     * Changes whether vertex arrays are in use.
     *
     * @param useVertexArrays <tt>true</tt> to use vertex arrays
     */
    void setUseVertexArrays(boolean useVertexArrays);

    /**
     * <i>Observer</i> of a {@link LWJGLGlyphRenderer}.
     */
    public interface EventListener {

        /**
         * Responds to an event from a glyph renderer.
         *
         * @param type Type of event
         * @throws NullPointerException if event type is <tt>null</tt>
         */
        public void onGlyphRendererEvent(EventType type);
    }

    /**
     * Type of event fired from the renderer.
     */
    public static enum EventType {

        /**
         * Renderer is automatically flushing queued glyphs, e.g., when it's full or color changes.
         */
        AUTOMATIC_FLUSH;
    }
}
