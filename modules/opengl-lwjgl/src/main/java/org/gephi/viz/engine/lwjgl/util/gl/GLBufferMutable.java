package org.gephi.viz.engine.lwjgl.util.gl;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import static org.gephi.viz.engine.util.ArrayUtils.getNextPowerOf2;
import static org.lwjgl.opengl.GL20.glBindBuffer;
import static org.lwjgl.opengl.GL20.glBufferData;
import static org.lwjgl.opengl.GL20.glBufferSubData;
import static org.lwjgl.opengl.GL20.glDeleteBuffers;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;

/**
 *
 * @author Eduardo Ramos
 */
public class GLBufferMutable implements GLBuffer {

    public static final int GL_BUFFER_TYPE_ARRAY = GL20.GL_ARRAY_BUFFER;
    public static final int GL_BUFFER_TYPE_ELEMENT_INDICES = GL20.GL_ELEMENT_ARRAY_BUFFER;
    public static final int GL_BUFFER_TYPE_DRAW_INDIRECT = GL40.GL_DRAW_INDIRECT_BUFFER;
    public static final int GL_BUFFER_USAGE_STATIC_DRAW = GL20.GL_STATIC_DRAW;
    public static final int GL_BUFFER_USAGE_STREAM_DRAW = GL20.GL_STREAM_DRAW;
    public static final int GL_BUFFER_USAGE_DYNAMIC_DRAW = GL20.GL_DYNAMIC_DRAW;

    private final int id;
    private final int type;

    private int usage = -1;
    private long sizeBytes = -1;

    private boolean isBound = false;

    public GLBufferMutable(int id, int type) {
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

    private void bufferData(Buffer buf) {
        if (buf instanceof FloatBuffer) {
            glBufferData(type, (FloatBuffer) buf, usage);
        } else if (buf instanceof IntBuffer) {
            glBufferData(type, (IntBuffer) buf, usage);
        } else if (buf instanceof ShortBuffer) {
            glBufferData(type, (ShortBuffer) buf, usage);
        } else if (buf instanceof ByteBuffer) {
            glBufferData(type, (ByteBuffer) buf, usage);
        } else if (buf instanceof DoubleBuffer) {
            glBufferData(type, (DoubleBuffer) buf, usage);
        } else if (buf instanceof LongBuffer) {
            glBufferData(type, (LongBuffer) buf, usage);
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
    public void init(long sizeBytes, int usage) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        this.usage = usage;
        this.sizeBytes = sizeBytes;

        glBufferData(type, sizeBytes, usage);
    }

    @Override
    public void init(Buffer buffer, int usage) {
        if (!isBound()) {
            throw new IllegalStateException("You should bind the buffer first!");
        }

        this.usage = usage;

        bufferData(buffer);
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
        System.out.println(originalLimit);
        buffer.limit((int) (size / bufferElementBytes(buffer)));
        bufferSubData(buffer, offset);
        buffer.limit(originalLimit);
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
    public long size() {
        return sizeBytes;
    }

    public void ensureCapacity(long neededBytes) {
        if (sizeBytes < neededBytes) {
            long newSizeBytes = getNextPowerOf2(neededBytes);

            System.out.println("Growing GL buffer from " + sizeBytes + " to " + newSizeBytes + " bytes");
            init(newSizeBytes, usage);
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
