package org.gephi.viz.engine.util.actions;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.graph.api.NodeIterable;
import org.gephi.graph.api.Rect2D;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.status.GraphRenderingOptions;
import org.gephi.viz.engine.status.GraphSelection;
import org.gephi.viz.engine.structure.GraphIndex;
import org.joml.Vector2f;

/**
 * @author Eduardo Ramos
 */
public class InputActionsProcessor {

    private final VizEngine<?, ?> engine;

    public InputActionsProcessor(VizEngine<?, ?> engine) {
        this.engine = engine;
    }

    public void selectNodesOnRectangle(final Rect2D rectangle) {
        final GraphIndex index = engine.getLookup().lookup(GraphIndex.class);
        final NodeIterable iterable = index.getNodesInsideRectangle(rectangle);

        selectNodes(iterable);
    }

    public void selectNodesUnderPosition(Vector2f worldCoords) {
        final GraphIndex index = engine.getLookup().lookup(GraphIndex.class);
        final NodeIterable iterable = index.getNodesUnderPosition(worldCoords.x, worldCoords.y);

        selectNodes(iterable);
    }

    public void clearSelection() {
        final GraphSelection selection = engine.getLookup().lookup(GraphSelection.class);
        selection.clearSelectedNodes();
        selection.clearSelectedEdges();
    }

    public void selectNodes(final NodeIterable nodesIterable) {
        final GraphSelection selection = engine.getLookup().lookup(GraphSelection.class);
        final GraphRenderingOptions renderingOptions = engine.getLookup().lookup(GraphRenderingOptions.class);
        final Graph graph = engine.getGraphModel().getGraphVisible();

        final Iterator<Node> iterator = nodesIterable.iterator();
        final Set<Node> selectionNodes = new HashSet<>();
        final Set<Edge> selectionEdges = new HashSet<>();

        final boolean selectNeighbours = renderingOptions.isAutoSelectNeighbours();
        try {
            while (iterator.hasNext()) {
                final Node node = iterator.next();

                selectionNodes.add(node);
                selectionEdges.addAll(graph.getEdges(node).toCollection());
                if (selectNeighbours) {
                    selectionNodes.addAll(graph.getNeighbors(node).toCollection());
                }
            }

            selection.setSelectedNodes(selectionNodes);
            selection.setSelectedEdges(selectionEdges);
        } finally {
            if (iterator.hasNext()) {
                nodesIterable.doBreak();
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
