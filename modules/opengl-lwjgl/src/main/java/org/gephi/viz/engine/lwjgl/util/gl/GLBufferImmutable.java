package org.gephi.viz.engine.lwjgl.util.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import static org.lwjgl.opengl.GL20.glBindBuffer;
import static org.lwjgl.opengl.GL20.glDeleteBuffers;
import static org.lwjgl.opengl.GL20.glBufferSubData;
import static org.lwjgl.opengl.GL44.glBufferStorage;

/**
 *
 * @author Eduardo Ramos
 */
public class GLBufferImmutable implements GLBuffer {

    private final int id;
    private final int type;

    private int flags = -1;
    private long sizeBytes = -1;

    private boolean isBound = false;

    public GLBufferImmutable(int id, int type) {
        this.id = id;
        this.type = type;
    }

    @Override
    public void bind() {
        glBindBuffer(type, id);
        isBound = true;
    }

    @Override
    public void unbind() {
        glBindBuffer(type, 0);
        isBound = false;
    }

    private void bufferStorage(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            glBufferStorage(type, (FloatBuffer) buf, flags);
        } else if (buf instanceof IntBuffer) {
            glBufferStorage(type, (IntBuffer) buf, flags);
        } else if (buf instanceof ShortBuffer) {
            glBufferStorage(type, (ShortBuffer) buf, flags);
        } else if (buf instanceof ByteBuffer) {
            glBufferStorage(type, (ByteBuffer) buf, flags);
        } else if (buf instanceof DoubleBuffer) {
            glBufferStorage(type, (DoubleBuffer) buf, flags);
        } else {
            throw new UnsupportedOperationException("Buffer class not supported: " + buf.getClass().getName());
        }
    }

    private void bufferSubData(Buffer buf, long offset) {
        if (buf instanceof FloatBuffer) {
            glBufferSubData(type, offset, (FloatBuffer) buf);
        } else if (buf instanceof IntBuffer) {
            glBufferSubData(type, offset, (IntBuffer) buf);
        } else if (buf instanceof ShortBuffer) {
            glBufferSubData(type, offset, (ShortBuffer) buf);
        } else if (buf instanceof ByteBuffer) {
            glBufferSubData(type, offset, (ByteBuffer) buf);
        } else if (buf instanceof DoubleBuffer) {
            glBufferSubData(type, offset, (DoubleBuffer) buf);
        } else if (buf instanceof LongBuffer) {
            glBufferSubData(type, offset, (LongBuffer) buf);
        } else {
            throw new UnsupportedOperationException("Buffer class not supported: " + buf.getClass().getName());
        }
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
    public void init(long sizeBytes, int flags) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        this.flags = flags;
        this.sizeBytes = sizeBytes;

        glBufferStorage(type, sizeBytes, flags);
    }

    @Override
    public void init(Buffer buffer, int flags) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        if (isInitialized()) {
            throw new UnsupportedOperationException("Cannot reinitialize an immutable buffer");
        }

        this.flags = flags;

        bufferStorage(buffer);
    }

    @Override
    public void update(Buffer buffer) {
        update(buffer, buffer.limit() * bufferElementBytes(buffer));
    }

    @Override
    public void update(Buffer buffer, long size) {
        update(buffer, 0, size);
    }

    @Override
    public void update(Buffer buffer, long offset, long size) {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        final long neededBytesCapacity = offset + size;
        ensureCapacity(neededBytesCapacity);

        final int originalLimit = buffer.limit();
        buffer.limit((int) (size / bufferElementBytes(buffer)));
        bufferSubData(buffer, offset);
        buffer.limit(originalLimit);
    }

    @Override
    public long size() {
        return sizeBytes;
    }

    private void ensureCapacity(long neededBytes) {
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
    public boolean isBound() {
        return isBound;
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
    public void destroy() {
        if (!isInitialized()) {
            throw new IllegalStateException("You should initialize the buffer first!");
        }

        glDeleteBuffers(id);
        sizeBytes = -1;
    }

    @Override
    public boolean isMutable() {
        return false;
    }
}
