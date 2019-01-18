package org.gephi.viz.engine.jogl.util;

import com.jogamp.opengl.util.GLBuffers;
import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import org.gephi.viz.engine.jogl.util.gl.BufferUtils;
import static org.gephi.viz.engine.util.ArrayUtils.getNextPowerOf2;

/**
 *
 * @author Eduardo Ramos
 */
public class ManagedDirectBuffer {

    private final int glType;
    private Buffer buffer;
    private int elementsCapacity;

    public ManagedDirectBuffer(int glType, int initialCapacity) {
        this.glType = glType;
        this.elementsCapacity = initialCapacity;
        this.buffer = GLBuffers.newDirectGLBuffer(glType, initialCapacity);
    }

    public Buffer getBuffer() {
        return buffer;
    }

    public FloatBuffer floatBuffer() {
        return (FloatBuffer) buffer.clear();
    }

    public IntBuffer intBuffer() {
        return (IntBuffer) buffer.clear();
    }

    public void ensureCapacity(int elements) {
        if (elementsCapacity < elements) {
            int newElementsCapacity = getNextPowerOf2(elements);

            System.out.println("Growing managed direct buffer from " + elementsCapacity + " to " + newElementsCapacity + " elements");
            Buffer newBuffer = GLBuffers.newDirectGLBuffer(glType, newElementsCapacity);

            buffer.clear();
            GLBuffers.put(newBuffer, buffer);
            BufferUtils.destroyDirectBuffer(buffer);

            this.buffer = newBuffer;
            this.elementsCapacity = newElementsCapacity;
        }
    }

    public int getElementsCapacity() {
        return elementsCapacity;
    }

    public void destroy() {
        BufferUtils.destroyDirectBuffer(buffer);
    }
}
