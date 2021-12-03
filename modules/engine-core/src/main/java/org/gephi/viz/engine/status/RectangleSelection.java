package org.gephi.viz.engine.status;

import org.joml.Vector2f;

public interface RectangleSelection {
    void startRectangleSelection(Vector2f initialPosition);
    void stopRectangleSelection(Vector2f endPosition);
    void updateRectangleSelection(Vector2f updatedPosition);

    Vector2f getInitialPosition();
    Vector2f getCurrentPosition();
}
