package org.gephi.viz.engine.util.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

/**
 *
 * @author Eduardo Ramos
 */
public class GLBufferImmutable implements GLBuffer {

    private final int id;
    private final int type;

    private int flags = -1;
    private long sizeBytes = -1;

    public GLBufferImmutable(int id, int type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public void bind(GL gl) {
        gl.glBindBuffer(type, id);
    }

    @Override
    public void unbind(GL gl) {
        gl.glBindBuffer(type, 0);
    }

    private int bufferElementBytes(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            return Float.BYTES;
        }
        if (buf instanceof IntBuffer) {
            return Integer.BYTES;
        }
        if (buf instanceof ShortBuffer) {
            return Short.BYTES;
        }
        if (buf instanceof ByteBuffer) {
            return Byte.BYTES;
        }
        if (buf instanceof DoubleBuffer) {
            return Double.BYTES;
        }
        if (buf instanceof LongBuffer) {
            return Long.BYTES;
        }
        if (buf instanceof CharBuffer) {
            return Character.BYTES;
        }

        return 1;
    }

    @Override
    public void init(GL gl, long sizeBytes, int flags) {
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        if (!gl.isGL4()) {
            throw new UnsupportedOperationException("Need GL4 for using immutable buffer");
        }

        this.flags = flags;
        this.sizeBytes = sizeBytes;

        gl.getGL4().glBufferStorage(type, sizeBytes, null, flags);
    }

    @Override
    public void init(GL gl, Buffer buffer, int flags) {
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        if (!gl.isGL4()) {
            throw new UnsupportedOperationException("Need GL4 for using immutable buffer");
        }

        this.flags = flags;
        final int elementBytes = bufferElementBytes(buffer);

        sizeBytes = buffer.capacity() * elementBytes;

        gl.getGL4().glBufferStorage(type, sizeBytes, buffer, flags);
    }

    @Override
    public void update(GL gl, Buffer buffer) {
        update(gl, buffer, buffer.limit() * bufferElementBytes(buffer));
    }

    @Override
    public void update(GL gl, Buffer buffer, long size) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        ensureCapacity(gl, size);

        gl.glBufferSubData(type, 0, size, buffer);
    }

    @Override
    public void update(GL gl, Buffer buffer, long offset, long size) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        final long neededBytesCapacity = offset + size;
        ensureCapacity(gl, neededBytesCapacity);

        gl.glBufferSubData(type, offset, size, buffer);
    }

    public void destroy(GL2ES2 gl) {
        gl.glDeleteBuffers(1, new int[]{id}, 0);
    }

    @Override
    public long size() {
        return sizeBytes;
    }

    private void ensureCapacity(GL gl, long neededBytes) {
        if (sizeBytes < neededBytes) {
            throw new UnsupportedOperationException("This buffer is immutable and needed capacity (" + neededBytes + ") is not enough. Size = " + sizeBytes);
        }
    }

    @Override
    public boolean isInitialized() {
        return sizeBytes != -1;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getType() {
        return type;
    }

    @Override
    public boolean isBound(GL gl) {
        return gl.getBoundBuffer(type) == id;
    }

    @Override
    public int getUsageFlags() {
        return flags;
    }

    @Override
    public long getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public void destroy(GL gl) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }

        gl.glDeleteBuffers(1, new int[]{id}, 0);
        sizeBytes = -1;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
