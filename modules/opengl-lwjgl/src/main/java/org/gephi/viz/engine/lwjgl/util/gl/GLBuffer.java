package org.gephi.viz.engine.lwjgl.util.gl;

import java.nio.Buffer;

/**
 *
 * @author Eduardo Ramos
 */
public interface GLBuffer {

    void init(long sizeBytes, int usageFlags);

    void init(Buffer buffer, int usageFlags);

    void update(Buffer buffer, long offsetBytes);
    
    void updateWithOrphaning(Buffer buffer, long offsetBytes);

    void bind();

    void destroy();

    int getId();

    long getSizeBytes();

    int getType();

    int getUsageFlags();

    boolean isBound();

    boolean isInitialized();

    long size();

    void unbind();

    boolean isMutable();
}
