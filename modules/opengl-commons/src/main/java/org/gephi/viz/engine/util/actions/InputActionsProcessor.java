package org.gephi.viz.engine.util.actions;

import org.gephi.graph.api.*;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.status.GraphSelectionNeighbours;
import org.gephi.viz.engine.structure.GraphIndex;
import org.joml.Vector2f;

import java.util.Collection;
import java.util.Iterator;

/**
 *
 * @author Eduardo Ramos
 */
public class InputActionsProcessor {

    private final VizEngine<?, ?> engine;

    public InputActionsProcessor(VizEngine<?, ?> engine) {
        this.engine = engine;
    }

    public void selectNodesUnderPosition(Vector2f worldCoords) {
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

    public void processCameraMoveEvent(int xDiff, int yDiff) {
        float zoom = engine.getZoom();

        engine.translate(xDiff / zoom, -yDiff / zoom);
    }

    public void processZoomEvent(double zoomQuantity, int x, int y) {
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

    public void processCenterOnGraphEvent() {
        final GraphIndex index = engine.getLookup().lookup(GraphIndex.class);
        final Rect2D visibleGraphBoundaries = index.getGraphBoundaries();

        final float[] center = visibleGraphBoundaries.center();
        engine.centerOn(new Vector2f(center[0], center[1]), visibleGraphBoundaries.width(), visibleGraphBoundaries.height());
    }
}
