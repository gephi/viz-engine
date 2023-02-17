package org.gephi.viz.engine.lwjgl.util.text;

public interface Texture {

    /**
     * Binds underlying OpenGL texture on a texture unit.
     *
     * @param unit OpenGL enumeration for a texture unit, i.e., {@code GL_TEXTURE0}
     * @throws NullPointerException if context is null
     * @throws IllegalArgumentException if unit is invalid
     */
    void bind(final int unit);

    /**
     * Destroys the texture.
     *
     * @throws NullPointerException if context is null
     */
    void dispose();

    /**
     * Generates an OpenGL texture object.
     *
     * @return Handle to the OpenGL texture
     */
    int generate();

    /**
     * Updates filter parameters for the texture.
     *
     * @param smooth True to interpolate samples
     * @throws NullPointerException if context is null
     */
    void setFiltering(final boolean smooth);
}
