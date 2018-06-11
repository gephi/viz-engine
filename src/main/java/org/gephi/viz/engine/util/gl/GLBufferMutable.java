package org.gephi.viz.engine.util.gl;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL3ES3;
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
public class GLBufferMutable implements GLBuffer {

    public static final int GL_BUFFER_TYPE_ARRAY = GL.GL_ARRAY_BUFFER;
    public static final int GL_BUFFER_TYPE_ELEMENT_INDICES = GL.GL_ELEMENT_ARRAY_BUFFER;
    public static final int GL_BUFFER_TYPE_DRAW_INDIRECT = GL3ES3.GL_DRAW_INDIRECT_BUFFER;
    public static final int GL_BUFFER_USAGE_STATIC_DRAW = GL.GL_STATIC_DRAW;
    public static final int GL_BUFFER_USAGE_STREAM_DRAW = GL2ES2.GL_STREAM_DRAW;
    public static final int GL_BUFFER_USAGE_DYNAMIC_DRAW = GL.GL_DYNAMIC_DRAW;

    private final int id;
    private final int type;

    private int usage = -1;
    private long sizeBytes = -1;

    public GLBufferMutable(int id, int type) {
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

        return buf.capacity();
    }

    @Override
    public void init(GL gl, long sizeBytes, int usage) {
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        this.usage = usage;
        this.sizeBytes = sizeBytes;

        gl.glBufferData(type, sizeBytes, null, usage);
    }

    @Override
    public void init(GL gl, Buffer buffer, int usage) {
        if (!isBound(gl)) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        this.usage = usage;
        final int elementBytes = bufferElementBytes(buffer);

        sizeBytes = buffer.capacity() * elementBytes;

        gl.glBufferData(type, sizeBytes, buffer, usage);
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

    @Override
    public void destroy(GL gl) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }

        gl.glDeleteBuffers(1, new int[]{id}, 0);
        sizeBytes = -1;
    }

    @Override
    public long size() {
        return sizeBytes;
    }

    public void ensureCapacity(GL gl, long neededBytes) {
        if (sizeBytes < neededBytes) {
            long newSizeBytes = getNextPowerOf2(neededBytes);

            System.out.println("Growing GL buffer from " + sizeBytes + " to " + newSizeBytes + " bytes");
            init(gl, newSizeBytes, usage);
        }
    }

    public static final long getNextPowerOf2(long number) {
        if (((number - 1) & number) == 0) {
            //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
            return number;
        }
        int power = 0;
        while (number > 0) {
            number = number >> 1;
            power++;
        }
        return (1 << power);
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
        return usage;
    }

    @Override
    public long getSizeBytes() {
        return sizeBytes;
    }

    @Override
    public boolean isMutable() {
        return true;
    }
}
