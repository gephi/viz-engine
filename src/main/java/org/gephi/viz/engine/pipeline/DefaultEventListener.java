package org.gephi.viz.engine.pipeline;

import com.jogamp.newt.event.MouseEvent;
import com.jogamp.opengl.GLAutoDrawable;
import java.util.Collection;
import java.util.Iterator;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.Rect2D;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndex;
import org.joml.Vector2f;

/**
 *
 * @author Eduardo Ramos
 */
public class DefaultEventListener implements InputListener {

    private final VizEngine engine;

    private static final short MOUSE_LEFT_BUTTON = MouseEvent.BUTTON1;
    private static final short MOUSE_WHEEL_BUTTON = MouseEvent.BUTTON2;
    private static final short MOUSE_RIGHT_BUTTON = MouseEvent.BUTTON3;
    private boolean mouseRightButtonPresed = false;
    private boolean mouseLeftButtonPresed = false;

    public DefaultEventListener(VizEngine engine) {
        this.engine = engine;
    }

    private MouseEvent lastMovedPosition = null;

    @Override
    public void frameStart() {
        lastMovedPosition = null;
    }

    @Override
    public void frameEnd() {
        if (lastMovedPosition != null) {
            //TODO: move to independent selection input listener
            final Vector2f worldCoords = engine.screenCoordinatesToWorldCoordinates(lastMovedPosition.getX(), lastMovedPosition.getY());

            selectNodesUnderPosition(worldCoords);
        }
    }

    @Override
    public boolean mouseClicked(MouseEvent e) {
        boolean leftClick = e.getClickCount() == 1 && e.getButton() == MOUSE_LEFT_BUTTON;
        boolean doubleLeftClick = e.getClickCount() == 2 && e.getButton() == MOUSE_LEFT_BUTTON;
        boolean doubleRightClick = e.getClickCount() == 2 && e.getButton() == MOUSE_RIGHT_BUTTON;
        boolean wheelClick = e.getButton() == MOUSE_WHEEL_BUTTON;

        final int x = e.getX();
        final int y = e.getY();

        if (wheelClick) {
            processCenterOnGraphEvent();
            return true;
        } else if (doubleLeftClick) {
            //Zoom in:
            processZoomEvent(10, x, y);
            return true;
        } else if (doubleRightClick) {
            //Zoom out:
            processZoomEvent(-10, x, y);
            return true;
        } else if (leftClick) {
            //TODO: move to independent selection input listener
            final Vector2f worldCoords = engine.screenCoordinatesToWorldCoordinates(x, y);
            System.out.println(String.format(
                    "Click on %s %s = %s, %s", x, y, worldCoords.x, worldCoords.y
            ));

            return true;
        }

        return false;
    }

    @Override
    public boolean mousePressed(MouseEvent e) {
        if (e.getButton() == MOUSE_LEFT_BUTTON) {
            mouseLeftButtonPresed = true;
        }

        if (e.getButton() == MOUSE_RIGHT_BUTTON) {
            mouseRightButtonPresed = true;
        }

        lastX = e.getX();
        lastY = e.getY();

        return false;
    }

    @Override
    public boolean mouseReleased(MouseEvent e) {
        if (e.getButton() == MOUSE_LEFT_BUTTON) {
            mouseLeftButtonPresed = false;
        }

        if (e.getButton() == MOUSE_RIGHT_BUTTON) {
            mouseRightButtonPresed = false;
        }

        return false;
    }

    @Override
    public boolean mouseMoved(MouseEvent e) {
        lastMovedPosition = e;

        return true;
    }

    @Override
    public boolean mouseDragged(MouseEvent e) {
        try {
            if (mouseLeftButtonPresed && mouseRightButtonPresed) {
                //Zoom in/on the screen center with both buttons pressed and vertical movement:
                double zoomQuantity = (lastY - e.getY()) / 7f;//Divide by some number so zoom is not too fast
                processZoomEvent(zoomQuantity, engine.getWidth() / 2, engine.getHeight() / 2);
                return true;
            } else if (mouseLeftButtonPresed || mouseRightButtonPresed) {
                processCameraMoveEvent(e.getX(), e.getY());
                return true;
            }
        } finally {
            lastX = e.getX();
            lastY = e.getY();
        }

        return false;
    }

    @Override
    public boolean mouseWheelMoved(MouseEvent e) {
        float[] rotation = e.getRotation();
        float verticalRotation = rotation[1] * e.getRotationScale();

        processZoomEvent(verticalRotation, e.getX(), e.getY());

        return true;
    }

    private int lastX;
    private int lastY;

    private void selectNodesUnderPosition(Vector2f worldCoords) {
        final GraphIndex index = engine.getLookup().lookup(GraphIndex.class);
        final GraphSelection selection = engine.getLookup().lookup(GraphSelection.class);
        final GraphSelectionNeighbours neighboursSelection = engine.getLookup().lookup(GraphSelectionNeighbours.class);
        final GraphRenderingOptions renderingOptions = engine.getLookup().lookup(GraphRenderingOptions.class);
        final Graph graph = engine.getGraphModel().getGraphVisible();

        final NodeIterable iterable = index.getNodesUnderPosition(worldCoords.x, worldCoords.y);
        final Iterator<Node> iterator = iterable.iterator();

        try {
            if (iterator.hasNext()) {
                //Select the node:
                final Node frontNode = iterator.next();

                //Select edges of node:
                final Collection<Edge> selectedEdges = graph.getEdges(frontNode).toCollection();

                //Add neighbours of node:
                final Collection<Node> selectedNeighbours;
                if (renderingOptions.isAutoSelectNeighbours()) {
                    selectedNeighbours = graph.getNeighbors(frontNode).toCollection();
                } else {
                    selectedNeighbours = null;
                }

                //Select everything as atomically as possible:
                selection.setSelectedNode(frontNode);
                selection.setSelectedEdges(selectedEdges);
                neighboursSelection.setSelectedNodes(selectedNeighbours);
            } else {
                selection.clearSelectedNodes();
                selection.clearSelectedEdges();
                neighboursSelection.clearSelectedNodes();
            }
        } finally {
            if (iterator.hasNext()) {
                iterable.doBreak();
            }
        }
    }

    protected void processCameraMoveEvent(int x, int y) {
        float zoom = engine.getZoom();

        engine.translate((x - lastX) / zoom, -(y - lastY) / zoom);
    }

    protected void processZoomEvent(double zoomQuantity, int x, int y) {
        final float currentZoom = engine.getZoom();
        float newZoom = currentZoom;

        newZoom *= Math.pow(1.1, zoomQuantity);
        if (newZoom < 0.001f) {
            newZoom = 0.001f;
        }

        if (newZoom > 1000f) {
            newZoom = 1000f;
        }

        //This does directional zoom, to follow where the mouse points:
        final Rect2D viewRect = engine.getViewBoundaries();
        final Vector2f center = new Vector2f(
                (viewRect.maxX + viewRect.minX) / 2,
                (viewRect.maxY + viewRect.minY) / 2
        );

        final Vector2f diff
                = engine.screenCoordinatesToWorldCoordinates(x, y)
                        .sub(center);

        final Vector2f directionalZoomTranslation = new Vector2f(diff)
                .mul(currentZoom / newZoom)
                .sub(diff);

        engine.translate(directionalZoomTranslation);
        engine.setZoom(newZoom);
    }

    private void processCenterOnGraphEvent() {
        final GraphIndex index = engine.getLookup().lookup(GraphIndex.class);
        final Rect2D visibleGraphBoundaries = index.getGraphBoundaries();

        final float[] center = visibleGraphBoundaries.center();
        engine.centerOn(new Vector2f(center[0], center[1]), visibleGraphBoundaries.width(), visibleGraphBoundaries.height());
    }

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
    public boolean isAvailable(GLAutoDrawable drawable) {
        return true;
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        //NOOP
    }
}
