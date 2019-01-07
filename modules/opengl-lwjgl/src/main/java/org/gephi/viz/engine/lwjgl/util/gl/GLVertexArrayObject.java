package org.gephi.viz.engine.lwjgl.util.gl;

import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL33;
import org.lwjgl.opengl.GLCapabilities;

/**
 * VAO abstraction that checks for actual support of VAOs and emulates it if not supported.
 *
 * @author Eduardo Ramos
 */
public abstract class GLVertexArrayObject {

    private final boolean vaoSupported;

    private int[] attributeLocations;
    private int[] instancedAttributeLocations;
    private int arrayId = -1;

    public GLVertexArrayObject(GLCapabilities capabilities, OpenGLOptions openGLOptions) {
        vaoSupported = capabilities.GL_ARB_vertex_array_object && !openGLOptions.isDisableVAOS();
    }

    private void init() {
        attributeLocations = getUsedAttributeLocations();
        if (attributeLocations == null) {
            attributeLocations = new int[0];
        } else {
            attributeLocations = attributeLocations.clone();
        }

        instancedAttributeLocations = getInstancedAttributeLocations();
        if (instancedAttributeLocations == null) {
            instancedAttributeLocations = new int[0];
        } else {
            instancedAttributeLocations = instancedAttributeLocations.clone();
        }

        if (vaoSupported) {
            arrayId = GL30.glGenVertexArrays();

            bind();
            configureAll();
            unbind();
        }
    }

    public void use() {
        if (attributeLocations == null) {
            init();
        }

        if (vaoSupported) {
            bind();
        } else {
            configureAll();
        }
    }

    public void stopUsing() {
        if (vaoSupported) {
            unbind();
        } else {
            unconfigureEnabledAttributes();
        }
    }

    private void configureAll() {
        configure();
        configureEnabledAttributes();
    }

    private void bind() {
        GL30.glBindVertexArray(arrayId);
    }

    private void unbind() {
        GL30.glBindVertexArray(0);
    }

    private void configureEnabledAttributes() {
        for (int attributeLocation : attributeLocations) {
            GL20.glEnableVertexAttribArray(attributeLocation);
        }
        for (int instancedAttributeLocation : instancedAttributeLocations) {
            GL33.glVertexAttribDivisor(instancedAttributeLocation, 1);
        }
    }

    private void unconfigureEnabledAttributes() {
        for (int attributeLocation : attributeLocations) {
            GL20.glDisableVertexAttribArray(attributeLocation);
        }
        for (int instancedAttributeLocation : instancedAttributeLocations) {
            GL33.glVertexAttribDivisor(instancedAttributeLocation, 0);
        }
    }

    protected abstract void configure();

    protected abstract int[] getUsedAttributeLocations();

    protected abstract int[] getInstancedAttributeLocations();

}
