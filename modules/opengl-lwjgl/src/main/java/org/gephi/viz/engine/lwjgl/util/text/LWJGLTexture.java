package org.gephi.viz.engine.lwjgl.util.text;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class LWJGLTexture implements Texture {

    /**
     * ID of internal OpenGL texture.
     */
    /*@Nonnegative*/
    protected final int handle;

    /**
     * {@code GL_TEXTURE2D}, etc.
     */
    protected final int type;

    /**
     * True for quality texturing.
     */
    protected final boolean mipmap;

    protected LWJGLTexture(final int type, final boolean mipmap) {

        Check.argument(isValidTextureType(type), "Texture type is invalid");

        this.handle = generate();
        this.type = type;
        this.mipmap = mipmap;
    }

    @Override
    public void bind(final int unit) {

        Check.argument(isValidTextureUnit(unit), "Texture unit is invalid");

        glActiveTexture(unit);
        glBindTexture(type, handle);
    }

    @Override
    public void dispose() {
        final int[] handles = new int[] { handle };
        glDeleteTextures(handles);
    }

    @Override
    public int generate() {
        final int[] handles = new int[1];
        glGenTextures(handles);
        return handles[0];
    }

    /**
     * Checks if an integer is a valid OpenGL enumeration for a texture type.
     *
     * @param type Integer to check
     * @return True if type is valid
     */
    private boolean isValidTextureType(int type) {
        switch (type) {
            case GL_TEXTURE_1D:
            case GL_TEXTURE_2D:
            case GL_TEXTURE_3D:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setFiltering(final boolean smooth) {
        final int mag;
        final int min;
        if (smooth) {
            mag = GL_LINEAR;
            min = mipmap ? GL_LINEAR_MIPMAP_NEAREST : GL_LINEAR;
        } else {
            mag = GL_NEAREST;
            min = mipmap ? GL_NEAREST_MIPMAP_NEAREST : GL_NEAREST;
        }

        setParameter(GL_TEXTURE_MAG_FILTER, mag);
        setParameter(GL_TEXTURE_MIN_FILTER, min);
    }

    /**
     * Changes a texture parameter for a 2D texture.
     *
     * @param name Name of the parameter, assumed valid
     * @param value Value of the parameter, assumed valid
     */
    private void setParameter(final int name, final int value) {
        glTexParameteri(type, name, value);
    }



    /**
     * Checks if an integer is a valid OpenGL enumeration for a texture unit.
     *
     * @param unit Integer to check
     * @return True if unit is valid
     */
    private static boolean isValidTextureUnit(final int unit) {
        return (unit >= GL_TEXTURE0) && (unit <= GL_TEXTURE31);
    }
}
