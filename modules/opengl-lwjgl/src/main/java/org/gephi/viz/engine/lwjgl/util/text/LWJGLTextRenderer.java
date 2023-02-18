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

import java.awt.Color;
import java.awt.Font;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.lwjgl.opengl.GLCapabilities;


/**
 * Utility for rendering bitmapped Java 2D text into an OpenGL window.
 *
 * <p>
 * {@code TextRenderer} has high performance, full Unicode support, and a simple API.  It performs
 * appropriate caching of text rendering results in an OpenGL texture internally to avoid repeated
 * font rasterization.  The caching is completely automatic, does not require any user
 * intervention, and has no visible controls in the public API.
 *
 * <p>
 * Using {@code TextRenderer} is simple.  Add a {@code TextRenderer} field to your {@code
 * GLEventListener} and in your {@code init} method, add:
 *
 * <pre>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </pre>
 *
 * <p>
 * In the {@code display} method of your {@code GLEventListener}, add:
 *
 * <pre>
 * renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
 * // optionally set the color
 * renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
 * renderer.draw("Text to draw", xPosition, yPosition);
 * // ... more draw commands, color changes, etc.
 * renderer.endRendering();
 * </pre>
 *
 * <p>
 * Unless you are sharing textures between OpenGL contexts, you do not need to call the {@link
 * #dispose dispose} method of the {@code TextRenderer}; the OpenGL resources it uses internally
 * will be cleaned up automatically when the OpenGL context is destroyed.
 *
 * <p>
 * Note that a {@code TextRenderer} will cause the Vertex Array Object binding to change, or to be
 * unbound.
 *
 * <p>
 * Internally, the renderer uses a rectangle packing algorithm to pack both glyphs and full
 * strings' rendering results (which are variable size) onto a larger OpenGL texture.
 * A least recently used (LRU) algorithm is used to discard previously rendered
 * strings; the specific algorithm is undefined, but is currently implemented by flushing unused
 * strings' rendering results every few hundred rendering cycles, where a rendering cycle is
 * defined as a pair of calls to {@link #beginRendering beginRendering} / {@link #endRendering
 * endRendering}.
 *
 * @author John Burkey
 * @author Kenneth Russell
 */
/*@NotThreadSafe*/
public final class LWJGLTextRenderer {

    /**
     * True to print debugging information.
     */
    static final boolean DEBUG = false;

    /**
     * Face, style, and size of text to render with.
     */
    /*@Nonnull*/
    private final Font font;

    /**
     * Delegate to store glyphs.
     */
    /*@Nonnull*/
    private final LWJGLGlyphCache glyphCache;

    /**
     * Delegate to create glyphs.
     */
    /*@Nonnull*/
    private final GlyphProducer glyphProducer;

    /**
     * Delegate to draw glyphs.
     */
    /*@Nonnull*/
    private final LWJGLGlyphRenderer glyphRenderer = new GlyphRendererProxy();

    /**
     * Mediator coordinating components.
     */
    /*@Nonnull*/
    private final Mediator mediator = new Mediator();

    /**
     * True if this text renderer is ready to be used.
     */
    private boolean ready = false;

    /**
     * Constructs a {@link LWJGLTextRenderer}.
     *
     * <p>
     * The resulting {@code TextRenderer} will use no antialiasing or fractional metrics and the
     * default render delegate.  It will not attempt to use OpenGL's automatic mipmap generation
     * for better scaling.  All Unicode characters will be available.
     *
     * @param font Font to render text with
     * @throws NullPointerException if font is null
     */
    public LWJGLTextRenderer(final Font font) {
        this(font, false, false, false, null);
    }

    /**
     * Constructs a {@link LWJGLTextRenderer} with optional mipmapping.
     *
     * <p>
     * The resulting {@code TextRenderer} will use no antialiasing or fractional metrics, and the
     * default render delegate.  If mipmapping is requested, the text renderer will attempt to use
     * OpenGL's automatic mipmap generation for better scaling.  All Unicode characters will be
     * available.
     *
     * @param font Font to render text with
     * @param mipmap True to generate mipmaps (to make the text scale better)
     * @throws NullPointerException if font is null
     */
    public LWJGLTextRenderer(final Font font, final boolean mipmap) {
        this(font, false, false, mipmap, null);
    }

    /**
     * Constructs a {@link LWJGLTextRenderer} with optional text properties and a render delegate.
     *
     * <p>
     * The resulting {@code TextRenderer} will use antialiasing and fractional metrics if
     * requested.  The optional render delegate provides more control over the text rendered.  The
     * {@code TextRenderer} will not attempt to use OpenGL's automatic mipmap generation for better
     * scaling.  All Unicode characters will be available.
     *
     * @param font Font to render text with
     * @param antialias True to smooth edges of text
     * @param subpixel True to use subpixel accuracy
     * @throws NullPointerException if font is null
     */
    public LWJGLTextRenderer(final Font font,
                             final boolean antialias,
                             final boolean subpixel) {
        this(font, antialias, subpixel, false, null);
    }

    /**
     * Constructs a {@link LWJGLTextRenderer} with optional text properties, a render delegate, and
     * mipmapping.
     *
     * <p>
     * The resulting {@code TextRenderer} will use antialiasing and fractional metrics if
     * requested.  The optional render delegate provides more control over the text rendered.  If
     * mipmapping is requested, the {@code TextRenderer} will attempt to use OpenGL's automatic
     * mipmap generation for better scaling.  All Unicode characters will be available.
     *
     * @param font Font to render text with
     * @param antialias True to smooth edges of text
     * @param subpixel True to use subpixel accuracy
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @throws NullPointerException if font is null
     */
    public LWJGLTextRenderer(final Font font,
                             final boolean antialias,
                             final boolean subpixel,
                             final boolean mipmap) {
        this(font, antialias, subpixel, mipmap, null);
    }

    /**
     * Constructs a {@link LWJGLTextRenderer} with optional text properties, a render delegate,
     * mipmapping, and a range of characters.
     *
     * <p>
     * The resulting {@code TextRenderer} will use antialiasing and fractional metrics if
     * requested.  The optional render delegate provides more control over the text rendered.  If
     * mipmapping is requested, the text renderer will attempt to use OpenGL's automatic mipmap
     * generation for better scaling.  If a character range is specified, the text renderer will
     * limit itself to those characters to try to achieve better performance.  Otherwise all
     * Unicode characters will be available.
     *
     * @param font Font to render text with
     * @param antialias True to smooth edges of text
     * @param subpixel True to use subpixel accuracy
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @param ub Range of unicode characters, or null to use the default
     * @throws NullPointerException if font is null
     */
    public LWJGLTextRenderer(final Font font,
                             final boolean antialias,
                             final boolean subpixel,
                             final boolean mipmap,
                            final UnicodeBlock ub) {

        Check.notNull(font, "Font cannot be null");

        this.font = font;
        this.glyphCache = LWJGLGlyphCache.newInstance(font, antialias, subpixel, mipmap);
        this.glyphProducer = GlyphProducers.get(font, glyphCache.getFontRenderContext(), ub);
    }

    /**
     * Starts a 3D render cycle.
     *
     * <p>
     * Assumes the end user is responsible for setting up the modelview and projection matrices,
     * and will render text using the {@link #draw3D} method.
     *
     * @param capabilities GL capabilities
     */
    public void begin3DRendering(GLCapabilities capabilities) {
        beginRendering(capabilities, false, 0, 0, false);
    }

    /**
     * Starts an orthographic render cycle.
     *
     * <p>
     * Sets up a two-dimensional orthographic projection with (0,0) as the lower-left coordinate
     * and (width, height) as the upper-right coordinate.  Binds and enables the internal OpenGL
     * texture object, sets the texture environment mode to GL_MODULATE, and changes the current
     * color to the last color set with this text drawer via {@link #setColor}.
     *
     * <p>
     * This method disables the depth test and is equivalent to beginRendering(width, height,
     * true).
     *
     * @param capabilities GL capabilities
     * @param width Width of the current on-screen OpenGL drawable
     * @param height Height of the current on-screen OpenGL drawable
     * @throws IllegalArgumentException if width or height is negative
     */
    public void beginRendering(final GLCapabilities capabilities,
                               final int width,
                               final int height) {
        beginRendering(capabilities,true, width, height, true);
    }

    /**
     * Starts an orthographic render cycle.
     *
     * <p>
     * Sets up a two-dimensional orthographic projection with (0,0) as the lower-left coordinate
     * and (width, height) as the upper-right coordinate.  Binds and enables the internal OpenGL
     * texture object, sets the texture environment mode to GL_MODULATE, and changes the current
     * color to the last color set with this text drawer via {@link #setColor}.
     *
     * <p>
     * Disables the depth test if requested.
     *
     * @param capabilities GL capabilities
     * @param width Width of the current on-screen OpenGL drawable
     * @param height Height of the current on-screen OpenGL drawable
     * @param disableDepthTest True to disable the depth test
     * @throws IllegalArgumentException if width or height is negative
     */
    public void beginRendering(final GLCapabilities capabilities,
                               final int width,
                               final int height,
                               final boolean disableDepthTest) {
        beginRendering(capabilities, true, width, height, disableDepthTest);
    }

    /**
     * Starts a render cycle.
     *
     * @param capabilities GL capabilities
     * @param ortho True to use orthographic projection
     * @param width Width of the current OpenGL viewport
     * @param height Height of the current OpenGL viewport
     * @param disableDepthTest True to ignore depth values
     * @throws IllegalArgumentException if width or height is negative
     */
    private void beginRendering(final GLCapabilities capabilities,
                                final boolean ortho,
                                final int width,
                                final int height,
                                final boolean disableDepthTest) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Make sure components are set up properly
        if (!ready) {
            glyphCache.addListener(mediator);
            glyphRenderer.addListener(mediator);
            ready = true;
        }

        // Delegate to components
        glyphCache.beginRendering();
        glyphRenderer.beginRendering(capabilities, ortho, width, height, disableDepthTest);
    }

    /**
     * Destroys resources used by the text renderer.
     *
     */
    public void dispose() {

        // Destroy the glyph cache
        glyphCache.dispose();

        // Destroy the glyph renderer
        glyphRenderer.dispose();
    }

    /**
     * Draws a character sequence at a location.
     *
     * <p>
     * The baseline of the leftmost character is at position (x, y) specified in OpenGL
     * coordinates, where the origin is at the lower-left of the drawable and the Y coordinate
     * increases in the upward direction.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @throws NullPointerException if text is null
     */
    public void draw(final CharSequence text,
                     final int x,
                     final int y) {
        draw3D(text, x, y, 0, 1);
    }

    /**
     * Draws a string at a location.
     *
     * <p>
     * The baseline of the leftmost character is at position (x, y) specified in OpenGL
     * coordinates, where the origin is at the lower-left of the drawable and the Y coordinate
     * increases in the upward direction.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @throws NullPointerException if text is null
     */
    public void draw(final String text,
                     final int x,
                     final int y) {
        draw3D(text, x, y, 0, 1);
    }

    /**
     * Draws a character sequence at a location in 3D space.
     *
     * <p>
     * The baseline of the leftmost character is placed at position (x, y, z) in the current
     * coordinate system.
     *
     * @param text Text to draw
     * @param x X coordinate at which to draw
     * @param y Y coordinate at which to draw
     * @param z Z coordinate at which to draw
     * @param scale Uniform scale applied to width and height of text
     * @throws NullPointerException if text is null
     */
    public void draw3D(final CharSequence text,
                       final float x,
                       final float y,
                       final float z,
                       final float scale) {
        draw3D(text.toString(), x, y, z, scale);
    }

    /**
     * Draws text at a location in 3D space.
     *
     * <p>
     * Uses the renderer's current color.  The baseline of the leftmost character is placed at
     * position (x, y, z) in the current coordinate system.
     *
     * @param text Text to draw
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @param z Position to draw on Z axis
     * @param scale Uniform scale applied to width and height of text
     * @throws NullPointerException if text is null
     */
    public void draw3D(final String text,
                       float x,
                       final float y,
                       final float z,
                       final float scale) {

        Check.notNull(text, "Text cannot be null");

        // Get all the glyphs for the string
        final List<Glyph> glyphs = glyphProducer.createGlyphs(text);

        // Render each glyph
        for (final Glyph glyph : glyphs) {
            if (glyph.location == null) {
                glyphCache.upload(glyph);
            }
            final TextureCoords coords = glyphCache.find(glyph);
            final float advance = glyphRenderer.drawGlyph(glyph, x, y, z, scale, coords);
            x += advance * scale;
        }
    }

    /**
     * Finishes a 3D render cycle.
     */
    public void end3DRendering() {
        endRendering();
    }

    /**
     * Finishes a render cycle.
     */
    public void endRendering() {
        // Tear down components
        glyphCache.endRendering();
        glyphRenderer.endRendering();
    }

    /**
     * Forces all stored text to be rendered.
     *
     * <p>
     * This should be called after each call to {@code draw} if you are setting OpenGL state such
     * as the modelview matrix between calls to {@code draw}.
     *
     * @throws IllegalStateException if not in a render cycle
     */
    public void flush() {
        // Make sure glyph cache is up to date
        glyphCache.update();

        // Render outstanding glyphs
        glyphRenderer.flush();
    }

    /**
     * Determines the bounding box of a character sequence.
     *
     * <p>
     * Assumes it was rendered at the origin.
     *
     * <p>
     * The coordinate system of the returned rectangle is Java 2D's, with increasing Y coordinates
     * in the downward direction.  The relative coordinate (0,0) in the returned rectangle
     * corresponds to the baseline of the leftmost character of the rendered string, in similar
     * fashion to the results returned by, for example, {@link GlyphVector#getVisualBounds
     * getVisualBounds}.
     *
     * <p>
     * Most applications will use only the width and height of the returned Rectangle for the
     * purposes of centering or justifying the String.  It is not specified which Java 2D bounds
     * ({@link GlyphVector#getVisualBounds getVisualBounds}, {@link GlyphVector#getPixelBounds
     * getPixelBounds}, etc.) the returned bounds correspond to, although every effort is made to
     * ensure an accurate bound.
     *
     * @param text Text to get bounding box for
     * @return Rectangle surrounding the given text, not null
     * @throws NullPointerException if text is null
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(final CharSequence text) {
        Check.notNull(text, "Text cannot be null");
        return getBounds(text.toString());
    }

    /**
     * Determines the bounding box of a string.
     *
     * @param text Text to get bounding box for
     * @return Rectangle surrounding the given text, not null
     * @throws NullPointerException if text is null
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(final String text) {
        Check.notNull(text, "Text cannot be null");
        return glyphProducer.findBounds(text);
    }

    /**
     * Determines the pixel width of a character.
     *
     * @param c Character to get pixel width of
     * @return Number of pixels required to advance past the character
     */
    public float getCharWidth(final char c) {
        return glyphProducer.findAdvance(c);
    }

    /**
     * Determines the font this {@link LWJGLTextRenderer} is using.
     *
     * @return Font used by this text renderer, not null
     */
    /*@Nonnull*/
    public Font getFont() {
        return font;
    }

    /**
     * Checks if the backing texture is using linear interpolation.
     *
     * @return True if the backing texture is using linear interpolation.
     */
    public boolean getSmoothing() {
        return glyphCache.getUseSmoothing();
    }

    /**
     * Checks if vertex arrays are in-use.
     *
     * <p>
     * Indicates whether vertex arrays are being used internally for rendering, or whether text is
     * rendered using the OpenGL immediate mode commands.  Defaults to true.
     */
    public boolean getUseVertexArrays() {
        return glyphRenderer.getUseVertexArrays();
    }

    /**
     * Specifies the current color of this {@link LWJGLTextRenderer} using a {@link Color}.
     *
     * @param color Color to use for rendering text
     * @throws NullPointerException if color is null
     */
    public void setColor(final Color color) {

        Check.notNull(color, "Color cannot be null");

        final float r = ((float) color.getRed()) / 255f;
        final float g = ((float) color.getGreen()) / 255f;
        final float b = ((float) color.getBlue()) / 255f;
        final float a = ((float) color.getAlpha()) / 255f;
        setColor(r, g, b, a);
    }

    /**
     * Specifies the current color of this {@link LWJGLTextRenderer} using individual components.
     *
     * <p>
     * Each component ranges from 0.0f to 1.0f.  The alpha component, if used, does not need to be
     * premultiplied into the color channels as described in the documentation for {@link
     * Texture} (although premultiplied colors are used
     * internally).  The default color is opaque white.
     *
     * @param r Red component of the new color
     * @param g Green component of the new color
     * @param b Blue component of the new color
     * @param a Alpha component of the new color
     */
    public void setColor(final float r,
                         final float g,
                         final float b,
                         final float a) {
        glyphRenderer.setColor(r, g, b, a);
    }

    /**
     * Specifies whether the backing texture will use linear interpolation.
     *
     * <p>
     * If smoothing is enabled, {@code GL_LINEAR} will be used.  Otherwise it uses {@code
     * GL_NEAREST}.
     *
     * <p>
     * Defaults to true.
     *
     * <p>
     * A few graphics cards do not behave well when this is enabled, resulting in fuzzy text.
     */
    public void setSmoothing(final boolean smoothing) {
        glyphCache.setUseSmoothing(smoothing);
    }

    /**
     * Changes the transformation matrix used for drawing text in 3D.
     *
     * @param matrix Transformation matrix in column-major order
     * @throws NullPointerException if matrix is null
     * @throws IndexOutOfBoundsException if length of matrix is less than sixteen
     * @throws IllegalStateException if in orthographic mode
     */
    public void setTransform(final float matrix[]) {
        Check.notNull(matrix, "Matrix cannot be null");
        glyphRenderer.setTransform(matrix, false);
    }

    /**
     * Changes whether vertex arrays are in use.
     *
     * <p>
     * This is provided as a concession for certain graphics cards which have poor vertex array
     * performance.  If passed true, the text renderer will use vertex arrays or a vertex buffer
     * internally for rendering.  Otherwise it will use immediate mode commands.  Defaults to
     * true.
     *
     * @param useVertexArrays True to render with vertex arrays
     */
    public void setUseVertexArrays(final boolean useVertexArrays) {
        glyphRenderer.setUseVertexArrays(useVertexArrays);
    }

    /**
     * Utility for coordinating text renderer components.
     */
    private final class Mediator implements LWJGLGlyphCache.EventListener, LWJGLGlyphRenderer.EventListener {

        @Override
        public void onGlyphCacheEvent(final LWJGLGlyphCache.EventType type,
                                      final Object data) {

            Check.notNull(type, "Event type cannot be null");
            Check.notNull(data, "Data cannot be null");

            switch (type) {
            case REALLOCATE:
                flush();
                break;
            case CLEAR:
                glyphProducer.clearGlyphs();
                break;
            case CLEAN:
                glyphProducer.removeGlyph((Glyph) data);
                break;
            }
        }

        @Override
        public void onGlyphRendererEvent(final LWJGLGlyphRenderer.EventType type) {

            Check.notNull(type, "Event type cannot be null");

            switch (type) {
            case AUTOMATIC_FLUSH:
                glyphCache.update();
                break;
            }
        }
    }

    /**
     * <em>Proxy</em> for a {@link LWJGLGlyphRenderer}.
     */
    /*@NotThreadSafe*/
    private static final class GlyphRendererProxy implements LWJGLGlyphRenderer {

        /**
         * Delegate to actually render.
         */
        /*@CheckForNull*/
        private LWJGLGlyphRenderer delegate;

        /**
         * Listeners added before a delegate is chosen.
         */
        /*@Nonnull*/
        private final List<EventListener> listeners = new ArrayList<EventListener>();

        /**
         * Red component of color.
         */
        /*@CheckForSigned*/
        private Float r;

        /**
         * Green component of color.
         */
        /*@CheckForSigned*/
        private Float g;

        /**
         * Blue component of color.
         */
        /*@CheckForSigned*/
        private Float b;

        /**
         * Alpha component of color.
         */
        /*@CheckForSigned*/
        private Float a;

        /**
         * Transform matrix.
         */
        /*@CheckForNull*/
        private float[] transform;

        /**
         * True if transform is transposed.
         */
        /*@CheckForNull*/
        private Boolean transposed;

        /**
         * True to use vertex arrays.
         */
        private boolean useVertexArrays = true;

        GlyphRendererProxy() {
            // empty
        }

        @Override
        public void addListener(final EventListener listener) {

            Check.notNull(listener, "Listener cannot be null");

            if (delegate == null) {
                listeners.add(listener);
            } else {
                delegate.addListener(listener);
            }
        }

        @Override
        public void beginRendering(final GLCapabilities capabilities,
                                   final boolean ortho,
                                   final int width,
                                   final int height,
                                   final boolean disableDepthTest) {

            Check.argument(width >= 0, "Width cannot be negative");
            Check.argument(height >= 0, "Height cannot be negative");

            if (delegate == null) {

                // Create the glyph renderer
                delegate = LWJGLGlyphRenderers.get(capabilities);

                // Add the event listeners
                for (EventListener listener : listeners) {
                    delegate.addListener(listener);
                }
                
                // Specify the color
                if ((r != null) && (g != null) && (b != null) && (a != null)) {
                    delegate.setColor(r, g, b, a);
                }

                // Specify the transform
                if ((transform != null) && (transposed != null)) {
                    delegate.setTransform(transform, transposed);
                }

                // Specify whether to use vertex arrays or not
                delegate.setUseVertexArrays(useVertexArrays);
            }
            delegate.beginRendering(capabilities, ortho, width, height, disableDepthTest);
        }

        @Override
        public void dispose() {
            if (delegate != null) {
                delegate.dispose();
            }
        }

        @Override
        public float drawGlyph(final Glyph glyph,
                               final float x,
                               final float y,
                               final float z,
                               final float scale,
                               final TextureCoords coords) {

            Check.notNull(glyph, "Glyph cannot be null");
            Check.notNull(coords, "Texture coordinates cannot be null");

            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                return delegate.drawGlyph(glyph, x, y, z, scale, coords);
            }
        }

        @Override
        public void endRendering() {
            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                delegate.endRendering();
            }
        }

        @Override
        public void flush() {
            if (delegate == null) {
                throw new IllegalStateException("Must be in render cycle!");
            } else {
                delegate.flush();
            }
        }

        @Override
        public boolean getUseVertexArrays() {
            if (delegate == null) {
                return useVertexArrays;
            } else {
                return delegate.getUseVertexArrays();
            }
        }

        @Override
        public void setColor(final float r,
                             final float g,
                             final float b,
                             final float a) {
            if (delegate == null) {
                this.r = r;
                this.g = g;
                this.b = b;
                this.a = a;
            } else {
                delegate.setColor(r, g, b, a);
            }
        }

        @Override
        public void setTransform(final float[] value, final boolean transpose) {

            Check.notNull(value, "Value cannot be null");

            if (delegate == null) {
                this.transform = Arrays.copyOf(value, value.length);
                this.transposed = transpose;
            } else {
                delegate.setTransform(value, transpose);
            }
        }

        @Override
        public void setUseVertexArrays(final boolean useVertexArrays) {
            if (delegate == null) {
                this.useVertexArrays = useVertexArrays;
            } else {
                delegate.setUseVertexArrays(useVertexArrays);
            }
        }
    }
}
