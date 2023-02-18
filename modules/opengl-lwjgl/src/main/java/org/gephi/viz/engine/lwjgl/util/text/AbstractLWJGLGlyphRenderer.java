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

import java.util.ArrayList;
import java.util.List;
import org.lwjgl.opengl.GLCapabilities;

import static org.lwjgl.opengl.GL11.*;

/**
 * Skeletal implementation of {@link LWJGLGlyphRenderer}.
 */
abstract class AbstractLWJGLGlyphRenderer implements LWJGLGlyphRenderer, LWJGLQuadPipeline.EventListener {

    // Default color
    private static final float DEFAULT_RED = 1.0f;
    private static final float DEFAULT_GREEN = 1.0f;
    private static final float DEFAULT_BLUE = 1.0f;
    private static final float DEFAULT_ALPHA = 1.0f;

    /**
     * Listeners to send events to.
     */
    private final List<EventListener> listeners = new ArrayList<>();

    /**
     * Quad to send to pipeline.
     */
    private final Quad quad = new Quad();

    /**
     * Buffer of quads.
     */
    private LWJGLQuadPipeline pipeline = null;

    /**
     * Whether pipeline needs to be flushed.
     */
    private boolean pipelineDirty = true;

    /**
     * True if between begin and end calls.
     */
    private boolean inRenderCycle = false;

    /**
     * True if orthographic.
     */
    private boolean orthoMode = false;

    /**
     * Red component of color.
     */
    private float r = DEFAULT_RED;

    /**
     * Green component of color.
     */
    private float g = DEFAULT_GREEN;

    /**
     * Blue component of color.
     */
    private float b = DEFAULT_BLUE;

    /**
     * Alpha component of color.
     */
    private float a = DEFAULT_ALPHA;

    /**
     * True if color needs to be updated.
     */
    private boolean colorDirty = true;

    /**
     * Transformation matrix for 3D mode.
     */
    /*@Nonnull*/
    private final float[] transform = new float[16];

    /**
     * Whether transformation matrix is in row-major order instead of column-major.
     */
    private boolean transposed = false;

    // TODO: Should `transformDirty` start out as true?
    /**
     * Whether transformation matrix needs to be updated.
     */
    private boolean transformDirty = false;

    /**
     * Constructs an {@link AbstractLWJGLGlyphRenderer}.
     */
    AbstractLWJGLGlyphRenderer() {
        // empty
    }

    @Override
    public final void addListener(final EventListener listener) {

        Check.notNull(listener, "Listener cannot be null");

        listeners.add(listener);
    }

    @Override
    public final void beginRendering(final GLCapabilities capabilities,
                                     final boolean ortho,
                                     final int width,
                                     final int height,
                                     final boolean disableDepthTest) {

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Perform hook
        doBeginRendering(ortho, width, height, disableDepthTest);

        // Store text renderer state
        inRenderCycle = true;
        orthoMode = ortho;

        // Make sure the pipeline is made
        if (pipelineDirty) {
            setPipeline(doCreateQuadPipeline(capabilities));
        }

        // Pass to quad renderer
        pipeline.beginRendering();

        // Make sure color is correct
        if (colorDirty) {
            doSetColor(r, g, b, a);
            colorDirty = false;
        }

        // Make sure transform is correct
        if (transformDirty) {
            doSetTransform3d(transform, transposed);
            transformDirty = false;
        }
    }

    /**
     * Requests that the pipeline be replaced on the next call to {@link #beginRendering}.
     */
    protected final void dirtyPipeline() {
        pipelineDirty = true;
    }

    @Override
    public final void dispose() {
        doDispose();
        listeners.clear();
        pipeline.dispose();
    }

    /**
     * Actually starts a render cycle.
     *
     * @param ortho True if using orthographic projection
     * @param width Width of current OpenGL viewport
     * @param height Height of current OpenGL viewport
     * @param disableDepthTest True if should ignore depth values
     * @throws IllegalArgumentException if width or height is negative
     */
    protected abstract void doBeginRendering(final boolean ortho,
                                             final int width,
                                             final int height,
                                             final boolean disableDepthTest);

    /**
     * Actually creates the quad pipeline for rendering quads.
     *
     * @return Quad pipeline to render quads with
     */
    protected abstract LWJGLQuadPipeline doCreateQuadPipeline(GLCapabilities capabilities);

    /**
     * Actually frees resources used by the renderer.
     */
    protected abstract void doDispose();

    /**
     * Actually finishes a render cycle.
     *
     */
    protected abstract void doEndRendering();

    /**
     * Actually changes the color when user calls {@link #setColor}.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     */
    protected abstract void doSetColor(float r,
                                       float g,
                                       float b,
                                       float a);

    /**
     * Actually changes the MVP matrix when using an arbitrary projection.
     *
     * @param value Matrix as float array
     * @param transpose True if in row-major order
     * @throws IndexOutOfBoundsException if length of value is less than sixteen
     */
    protected abstract void doSetTransform3d(float[] value,
                                             boolean transpose);

    /**
     * Actually changes the MVP matrix when using orthographic projection.
     *
     * @param width Width of viewport
     * @param height Height of viewport
     * @throws IllegalArgumentException if width or height is negative
     */
    protected abstract void doSetTransformOrtho(int width,
                                                int height);

    @Override
    public final float drawGlyph(final Glyph glyph,
                                 final float x,
                                 final float y,
                                 final float z,
                                 final float scale,
                                 final TextureCoords coords) {

        Check.notNull(glyph, "Glyph cannot be null");
        Check.notNull(coords, "Texture coordinates cannot be null");

        // Compute position and size
        quad.xl = x + (scale * glyph.kerning);
        quad.xr = quad.xl + (scale * glyph.width);
        quad.yb = y - (scale * glyph.descent);
        quad.yt = quad.yb + (scale * glyph.height);
        quad.z = z;
        quad.sl = coords.left();
        quad.sr = coords.right();
        quad.tb = coords.bottom();
        quad.tt = coords.top();

        // Draw quad
        pipeline.addQuad(quad);

        // Return distance to next character
        return glyph.advance;
    }

    @Override
    public final void endRendering() {
        // Store text renderer state
        inRenderCycle = false;

        // Pass to quad renderer
        pipeline.endRendering();

        // Perform hook
        doEndRendering();
    }

    /**
     * Fires an event to all observers.
     *
     * @param type Kind of event
     * @throws NullPointerException if type is null
     */
    protected final void fireEvent(final EventType type) {

        Check.notNull(type, "Event type cannot be null");

        for (final EventListener listener : listeners) {
            assert listener != null : "addListener rejects null";
            listener.onGlyphRendererEvent(type);
        }
    }

    @Override
    public final void flush() {

// Commented to work in Jzy3D (uncomment won't prevent tests to pass)
// Check.state(inRenderCycle, "Must be in render cycle");

        pipeline.flush();
        glFlush();
    }

    /**
     * Determines if a color is the same one that is stored.
     *
     * @param r Red component of color
     * @param g Green component of color
     * @param b Blue component of color
     * @param a Alpha component of color
     * @return True if each component matches
     */
    final boolean hasColor(final float r, final float g, final float b, final float a) {
        return (this.r == r) && (this.g == g) && (this.b == b) && (this.a == a);
    }

    // TODO: Rename to `isOrthographic`?
    /**
     * Checks if this {@link LWJGLGlyphRenderer} using an orthographic projection.
     *
     * @return True if this renderer is using an orthographic projection
     */
    final boolean isOrthoMode() {
        return orthoMode;
    }

    @Override
    public final void onQuadPipelineEvent(final LWJGLQuadPipeline.EventType type) {

        Check.notNull(type, "Event type cannot be null");

        if (type == LWJGLQuadPipeline.EventType.AUTOMATIC_FLUSH) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
        }
    }

    @Override
    public final void setColor(final float r, final float g, final float b, final float a) {

        // Check if already has the color
        if (hasColor(r, g, b, a)) {
            return;
        }

        // Render any outstanding quads first
        if (pipeline!=null && !pipeline.isEmpty()) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            flush();
        }

        // Store the color
        this.r = r;
        this.g = g;
        this.b = g;
        this.a = a;

        // Change the color
        if (inRenderCycle) {
            doSetColor(r, g, b, a);
        } else {
            colorDirty = true;
        }
    }

    /**
     * Changes the quad pipeline.
     *
     * @param pipeline Quad pipeline to change to
     */
    private final void setPipeline(final LWJGLQuadPipeline pipeline) {

        assert pipeline != null : "Pipeline should not be null";

        final LWJGLQuadPipeline oldPipeline = this.pipeline;
        final LWJGLQuadPipeline newPipeline = pipeline;

        // Remove the old pipeline
        if (oldPipeline != null) {
            oldPipeline.removeListener(this);
            oldPipeline.dispose();
            this.pipeline = null;
        }

        // Store the new pipeline
        newPipeline.addListener(this);
        this.pipeline = newPipeline;
        pipelineDirty = false;
    }

    @Override
    public final void setTransform(final float[] value, final boolean transpose) {

        Check.notNull(value, "Transform value cannot be null");
        Check.state(!orthoMode, "Must be in 3D mode");

        // Render any outstanding quads first
        if (!pipeline.isEmpty()) {
            fireEvent(EventType.AUTOMATIC_FLUSH);
            flush();
        }

        // Store the transform
        System.arraycopy(value, 0, this.transform, 0, value.length);
        this.transposed = transpose;

        // Change the transform
        if (inRenderCycle) {
            doSetTransform3d(value, transpose);
        } else {
            transformDirty = true;
        }
    }
}
