package org.gephi.viz.engine.lwjgl.util.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import static org.gephi.viz.engine.util.ArrayUtils.getNextPowerOf2;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryUtil;

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

        this.buffer = createGLBuffer(initialCapacity);
    }

    private Buffer createGLBuffer(int initialCapacity) throws UnsupportedOperationException {
        switch (glType) {
            case GL20.GL_FLOAT:
                return MemoryUtil.memAllocFloat(initialCapacity);
            case GL20.GL_DOUBLE:
                return MemoryUtil.memAllocDouble(initialCapacity);
            case GL20.GL_INT:
            case GL20.GL_UNSIGNED_INT:
                return MemoryUtil.memAllocInt(initialCapacity);
            case GL20.GL_SHORT:
            case GL20.GL_UNSIGNED_SHORT:
                return MemoryUtil.memAllocShort(initialCapacity);
            case GL20.GL_BYTE:
            case GL20.GL_UNSIGNED_BYTE:
                return MemoryUtil.memAlloc(initialCapacity);
            default:
                throw new UnsupportedOperationException("Unsupported glType for ManagedDirectBuffer: " + glType);
        }
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
            final int newElementsCapacity = getNextPowerOf2(elements);

            System.out.println("Growing managed direct buffer from " + elementsCapacity + " to " + newElementsCapacity + " elements");

            buffer = realloc(buffer, newElementsCapacity);

            this.elementsCapacity = newElementsCapacity;
        }
    }

    private Buffer realloc(Buffer buf, int newElementsCapacity) {
        if (buf instanceof FloatBuffer) {
            return MemoryUtil.memRealloc((FloatBuffer) buf, newElementsCapacity);
        }
        if (buf instanceof IntBuffer) {
            return MemoryUtil.memRealloc((IntBuffer) buf, newElementsCapacity);
        }
        if (buf instanceof ShortBuffer) {
            return MemoryUtil.memRealloc((ShortBuffer) buf, newElementsCapacity);
        }
        if (buf instanceof ByteBuffer) {
            return MemoryUtil.memRealloc((ByteBuffer) buf, newElementsCapacity);
        }
        if (buf instanceof DoubleBuffer) {
            return MemoryUtil.memRealloc((DoubleBuffer) buf, newElementsCapacity);
        }
        if (buf instanceof LongBuffer) {
            return MemoryUtil.memRealloc((LongBuffer) buf, newElementsCapacity);
        }

        throw new UnsupportedOperationException("realloc not supported for " + buf.getClass().getName());
    }

    public int getElementsCapacity() {
        return elementsCapacity;
    }

    public void destroy() {
        MemoryUtil.memFree(buffer);
    }
}
