package org.gephi.viz.engine.lwjgl.pipeline;

import org.gephi.graph.api.Rect2D;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.PanamaGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.lwjgl.pipeline.events.MouseEvent;
import org.gephi.viz.engine.lwjgl.pipeline.events.MouseEvent.Action;
import org.gephi.viz.engine.lwjgl.pipeline.events.MouseEvent.Button;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.util.actions.InputActionsProcessor;
import org.joml.Vector2f;

/**
 * @author Eduardo Ramos
 */
public class DefaultLWJGLEventListener implements InputListener<PanamaGLRenderingTarget, LWJGLInputEvent> {

    private final VizEngine<PanamaGLRenderingTarget, LWJGLInputEvent> engine;
    private final InputActionsProcessor inputActionsProcessor;
    private final GraphSelection graphSelection;

    public DefaultLWJGLEventListener(VizEngine<PanamaGLRenderingTarget, LWJGLInputEvent> engine) {
        this.engine = engine;
        this.inputActionsProcessor = new InputActionsProcessor(engine);
        this.graphSelection = engine.getLookup().lookup(GraphSelection.class);
    }

    private boolean mouseRightButtonPresed = false;
    private boolean mouseLeftButtonPresed = false;

    private MouseEvent lastMovedPosition = null;

    @Override
    public void frameStart() {
        lastMovedPosition = null;
    }

    @Override
    public void frameEnd() {
        if (lastMovedPosition != null) {
            //TODO: move to independent selection input listener
            if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.SIMPLE_MOUSE_SELECTION) {
                final Vector2f worldCoords = engine.screenCoordinatesToWorldCoordinates(lastMovedPosition.x, lastMovedPosition.y);
                inputActionsProcessor.selectNodesUnderPosition(worldCoords);
            }
        }
    }

    @Override
    public boolean processEvent(LWJGLInputEvent event) {
        if (event instanceof MouseEvent) {
            return processMouseEvent((MouseEvent) event);
        }

        return false;
    }

    private boolean processMouseEvent(MouseEvent event) {
        if (null != event.action) {
            switch (event.action) {
                case CLICK:
                case DOUBLE_CLICK:
                    return mouseClicked(event);
                case PRESS:
                    return mousePressed(event);
                case RELEASE:
                    return mouseReleased(event);
                case MOVE:
                    return mouseMoved(event);
                case DRAG:
                    return mouseDragged(event);
                case SCROLL:
                    return mouseWheelMoved(event);
                default:
                    break;
            }
        }

        return false;
    }

    public boolean mouseClicked(MouseEvent e) {
        final boolean leftClick = e.action == Action.CLICK && e.button == Button.LEFT;
        final boolean doubleLeftClick = e.action == Action.DOUBLE_CLICK && e.button == Button.LEFT;
        final boolean doubleRightClick = e.action == Action.DOUBLE_CLICK && e.button == Button.RIGHT;
        final boolean wheelClick = e.action == Action.CLICK && e.button == Button.MIDDLE;

        final int x = e.x;
        final int y = e.y;

        if (wheelClick) {
            inputActionsProcessor.processCenterOnGraphEvent();
            return true;
        } else if (doubleLeftClick) {
            //Zoom in:
            inputActionsProcessor.processZoomEvent(10, x, y);
            return true;
        } else if (doubleRightClick) {
            //Zoom out:
            inputActionsProcessor.processZoomEvent(-10, x, y);
            return true;
        } else if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.SIMPLE_MOUSE_SELECTION && leftClick) {
            //TODO: move to independent selection input listener
            final Vector2f worldCoords = engine.screenCoordinatesToWorldCoordinates(x, y);
            System.out.println(String.format(
                "Click on %s %s = %s, %s", x, y, worldCoords.x, worldCoords.y
            ));
            return true;
        } else if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION) {
            inputActionsProcessor.clearSelection();
            return true;
        }

        return false;
    }

    public boolean mousePressed(MouseEvent e) {
        if (e.button == Button.LEFT) {
            mouseLeftButtonPresed = true;

            if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION) {
                inputActionsProcessor.clearSelection();
                graphSelection.startRectangleSelection(engine.screenCoordinatesToWorldCoordinates(e.x, e.y));
                return true;
            }
        }

        if (e.button == Button.RIGHT) {
            mouseRightButtonPresed = true;
        }

        lastX = e.x;
        lastY = e.y;

        return false;
    }

    public boolean mouseReleased(MouseEvent e) {
        if (e.button == Button.LEFT) {
            mouseLeftButtonPresed = false;
        }

        if (e.button == Button.RIGHT) {
            mouseRightButtonPresed = false;
        }

        if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION) {
            graphSelection.stopRectangleSelection(engine.screenCoordinatesToWorldCoordinates(e.x, e.y));
        }

        return false;
    }

    public boolean mouseMoved(MouseEvent e) {
        lastMovedPosition = e;

        return true;
    }

    public boolean mouseDragged(MouseEvent e) {
        try {
            if (mouseLeftButtonPresed && mouseRightButtonPresed) {
                //Zoom in/on the screen center with both buttons pressed and vertical movement:
                final double zoomQuantity = (lastY - e.y) / 7f;//Divide by some number so zoom is not too fast

                inputActionsProcessor.processZoomEvent(zoomQuantity, engine.getWidth() / 2, engine.getHeight() / 2);
                return true;
            } else if (graphSelection.getMode() != GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION && (mouseLeftButtonPresed || mouseRightButtonPresed)) {
                inputActionsProcessor.processCameraMoveEvent(e.x - lastX, e.y - lastY);
                return true;
            } else if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION && mouseLeftButtonPresed) {
                graphSelection.updateRectangleSelection(engine.screenCoordinatesToWorldCoordinates(e.x, e.y));

                final Vector2f initialPosition = graphSelection.getRectangleInitialPosition();
                final Vector2f currentPosition = graphSelection.getRectangleCurrentPosition();

                if (initialPosition != null && currentPosition != null) {
                    final Rect2D rectangle = new Rect2D(
                        Math.min(initialPosition.x, currentPosition.x),
                        Math.min(initialPosition.y, currentPosition.y),
                        Math.max(initialPosition.x, currentPosition.x),
                        Math.max(initialPosition.y, currentPosition.y)
                    );
                    inputActionsProcessor.selectNodesOnRectangle(rectangle);
                }
                return true;
            } else if (graphSelection.getMode() == GraphSelection.GraphSelectionMode.RECTANGLE_SELECTION && mouseRightButtonPresed) {
                inputActionsProcessor.processCameraMoveEvent(e.x - lastX, e.y - lastY);
                return true;
            }
        } finally {
            lastX = e.x;
            lastY = e.y;
        }

        return false;
    }

    public boolean mouseWheelMoved(MouseEvent e) {
        final double verticalRotation = e.yScroll;

        inputActionsProcessor.processZoomEvent(verticalRotation, e.x, e.y);

        return true;
    }

    private int lastX;
    private int lastY;

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getCategory() {
        return "default";
    }

    @Override
    public int getPreferenceInCategory() {
        return 0;
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public boolean isAvailable(PanamaGLRenderingTarget target) {
        return true;
    }

    @Override
    public void init(PanamaGLRenderingTarget target) {
        //NOOP
    }
}
