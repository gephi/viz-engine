package org.gephi.viz.engine.lwjgl.util.text;

import java.awt.Rectangle;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

public abstract class LWJGLTexture2D extends LWJGLTexture implements Texture2D {

    // Size on X axis
    protected final int width;

    // Size on Y axis
    protected final int height;

    /**
     * Creates a 2D texture.
     *
     * @param width Size of texture on X axis
     * @param height Size of texture on Y axis
     * @param smooth True to interpolate samples
     * @param mipmap True for high quality
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if width or height is negative
     */
    LWJGLTexture2D(final int width,
              final int height,
              final boolean smooth,
              final boolean mipmap) {

        super(GL_TEXTURE_2D, mipmap);

        Check.argument(width >= 0, "Width cannot be negative");
        Check.argument(height >= 0, "Height cannot be negative");

        // Copy parameters
        this.width = width;
        this.height = height;

        // Set up
        bind(GL_TEXTURE0);
        allocate();
        setFiltering(smooth);
    }

    @Override
    public void allocate() {
        glTexImage2D(
            GL_TEXTURE_2D,          // target
            0,                         // level
            getInternalFormat(),     // internal format
            width,                     // width
            height,                    // height
            0,                         // border
            GL_RGB,                 // format (unused)
            GL_UNSIGNED_BYTE,       // type (unused)
            (ByteBuffer) null);                     // pixels
    }

    @Override
    public void update(final ByteBuffer pixels,
                final Rectangle area) {

        Check.notNull(pixels, "Pixels cannot be null");
        Check.notNull(area, "Area cannot be null");

        final int[] parameters = new int[4];

        // Store unpack parameters
        parameters[0] = glGetInteger(GL_UNPACK_ALIGNMENT);
        parameters[1] = glGetInteger(GL_UNPACK_SKIP_ROWS);
        parameters[2] = glGetInteger(GL_UNPACK_SKIP_PIXELS);
        parameters[3] = glGetInteger(GL_UNPACK_ROW_LENGTH);

        // Change unpack parameters
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, area.y);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, area.x);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, width);

        // Update the texture
        glTexSubImage2D(
            GL_TEXTURE_2D,     // target
            0,                    // mipmap level
            area.x,               // x offset
            area.y,               // y offset
            area.width,           // width
            area.height,          // height
            getFormat(),        // format
            GL_UNSIGNED_BYTE,  // type
            pixels);              // pixels

        // Reset unpack parameters
        glPixelStorei(GL_UNPACK_ALIGNMENT, parameters[0]);
        glPixelStorei(GL_UNPACK_SKIP_ROWS, parameters[1]);
        glPixelStorei(GL_UNPACK_SKIP_PIXELS, parameters[2]);
        glPixelStorei(GL_UNPACK_ROW_LENGTH, parameters[3]);

        // Generate mipmaps
        if (mipmap) {
            glGenerateMipmap(GL_TEXTURE_2D);
        }
    }
}
