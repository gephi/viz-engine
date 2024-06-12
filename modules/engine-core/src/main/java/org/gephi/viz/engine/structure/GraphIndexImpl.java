package org.gephi.viz.engine.structure;

import org.gephi.graph.api.*;
import org.gephi.graph.impl.GraphStoreConfiguration;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.util.EdgeIterableFilteredWrapper;
import org.gephi.viz.engine.util.NodeIterableFilteredWrapper;
import org.joml.Intersectionf;

import java.util.function.Predicate;

/**
 * <p>
 * TODO: make intersection functions customizable for different shape handling</p>
 * <p>
 *
 * @author Eduardo Ramos
 */
public class GraphIndexImpl implements GraphIndex {

    private final VizEngine engine;

    public GraphIndexImpl(VizEngine engine) {
        this.engine = engine;
    }

    //Graph
    private GraphModel graphModel;
    private float edgesMinWeight = 1;
    private float edgesMaxWeight = 1;

    private void init() {
        graphModel = engine.getGraphModel();
    }

    private void ensureInitialized() {
        if (graphModel == null) {
            init();
        }
    }

    public Graph getVisibleGraph() {
        ensureInitialized();
        return graphModel.getGraphVisible();
    }

    public void indexNodes() {
        //NOOP
    }

    public void indexEdges() {
        ensureInitialized();

        final Graph visibleGraph = getVisibleGraph();
        if (visibleGraph.getEdgeCount() > 0) {
            final Column weightColumn = visibleGraph.getModel().getEdgeTable().getColumn(GraphStoreConfiguration.EDGE_WEIGHT_INDEX);

            if (weightColumn.isIndexed() && AttributeUtils.isSimpleType(weightColumn.getTypeClass())) {
                visibleGraph.readLock();
                try {
                    edgesMinWeight = visibleGraph.getModel().getEdgeIndex().getMinValue(weightColumn).floatValue();
                    edgesMaxWeight = visibleGraph.getModel().getEdgeIndex().getMaxValue(weightColumn).floatValue();
                } finally {
                    visibleGraph.readUnlockAll();
                }
            } else {
                visibleGraph.readLock();
                try {
                    final GraphView graphView = visibleGraph.getView();
                    final boolean isView = !graphView.isMainView();

                    if (!isView) {
                        float minWeight = Float.MAX_VALUE;
                        float maxWeight = Float.MIN_VALUE;

                        for (Edge edge : visibleGraph.getEdges()) {
                            float weight = (float) edge.getWeight(graphView);
                            minWeight = weight <= minWeight ? weight : minWeight;
                            maxWeight = weight >= maxWeight ? weight : maxWeight;
                        }

                        edgesMinWeight = minWeight;
                        edgesMaxWeight = maxWeight;
                    }
                } finally {
                    visibleGraph.readUnlockAll();
                }
            }
        } else {
            edgesMinWeight = edgesMaxWeight = 1;
        }
    }

    @Override
    public int getNodeCount() {
        ensureInitialized();

        return getVisibleGraph().getNodeCount();
    }

    @Override
    public int getEdgeCount() {
        ensureInitialized();

        return getVisibleGraph().getEdgeCount();
    }

    @Override
    public float getEdgesMinWeight() {
        return edgesMinWeight;
    }

    @Override
    public float getEdgesMaxWeight() {
        return edgesMaxWeight;
    }

    @Override
    public NodeIterable getVisibleNodes() {
        ensureInitialized();

        return getVisibleGraph().getSpatialIndex().getNodesInArea(engine.getViewBoundaries());
    }

    @Override
    public void getVisibleNodes(ElementsCallback<Node> callback) {
        ensureInitialized();

        final Graph visibleGraph = getVisibleGraph();
        callback.start(visibleGraph);

        final NodeIterable nodeIterable = visibleGraph.getSpatialIndex().getNodesInArea(engine.getViewBoundaries());
        try {
            for (Node node : nodeIterable) {
                callback.accept(node);
            }
        } catch (Exception ex) {
            nodeIterable.doBreak();
        }

        callback.end(visibleGraph);
    }

    @Override
    public EdgeIterable getVisibleEdges() {
        ensureInitialized();

        return getVisibleGraph().getSpatialIndex().getEdgesInArea(engine.getViewBoundaries());
    }

    @Override
    public void getVisibleEdges(ElementsCallback<Edge> callback) {
        ensureInitialized();

        final Graph visibleGraph = getVisibleGraph();
        callback.start(visibleGraph);
        final EdgeIterable edgeIterable = visibleGraph.getSpatialIndex().getEdgesInArea(engine.getViewBoundaries());
        try {
            for (Edge edge : edgeIterable) {
                callback.accept(edge);
            }
        } catch (Exception ex) {
            edgeIterable.doBreak();
        }

        callback.end(visibleGraph);
    }

    @Override
    public NodeIterable getNodesUnderPosition(float x, float y) {
        ensureInitialized();

        return filterNodeIterable(getVisibleGraph().getSpatialIndex().getNodesInArea(getCircleRect2D(x, y, 0)), node -> {
            final float size = node.size();

            return Intersectionf.testPointCircle(x, y, node.x(), node.y(), size * size);
        });
    }

    @Override
    public NodeIterable getNodesInsideCircle(float centerX, float centerY, float radius) {
        ensureInitialized();

        return filterNodeIterable(getVisibleGraph().getSpatialIndex().getNodesInArea(getCircleRect2D(centerX, centerY, radius)), node -> {
            return Intersectionf.testCircleCircle(centerX, centerY, radius, node.x(), node.y(), node.size());
        });
    }

    @Override
    public NodeIterable getNodesInsideRectangle(Rect2D rect) {
        ensureInitialized();

        return filterNodeIterable(getVisibleGraph().getSpatialIndex().getNodesInArea(rect), node -> {
            final float size = node.size();

            return Intersectionf.testAarCircle(rect.minX, rect.minY, rect.maxX, rect.maxY, node.x(), node.y(), size * size);
        });
    }

    @Override
    public EdgeIterable getEdgesInsideRectangle(Rect2D rect) {
        ensureInitialized();

        return filterEdgeIterable(getVisibleGraph().getSpatialIndex().getEdgesInArea(rect), edge -> {
            final Node source = edge.getSource();
            final Node target = edge.getTarget();

            //TODO: take width into account!
            return Intersectionf.testAarLine(rect.minX, rect.minY, rect.maxX, rect.maxY, source.x(), source.y(), target.x(), target.y());
        });
    }

    @Override
    public EdgeIterable getEdgesInsideCircle(float centerX, float centerY, float radius) {
        ensureInitialized();

        return filterEdgeIterable(getVisibleGraph().getSpatialIndex().getEdgesInArea(getCircleRect2D(centerX, centerY, radius)), edge -> {
            final Node source = edge.getSource();
            final Node target = edge.getTarget();

            float x0 = source.x();
            float y0 = source.y();
            float x1 = target.x();
            float y1 = target.y();

            //TODO: take width into account!
            return Intersectionf.testLineCircle(y0 - y1, x1 - x0, (x0 - x1) * y0 + (y1 - y0) * x0, centerX, centerY, radius);
        });
    }

    @Override
    public Rect2D getGraphBoundaries() {
        ensureInitialized();

        final Graph visibleGraph = getVisibleGraph();
        if (visibleGraph.getNodeCount() > 0) {
            float minX = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = Float.MIN_VALUE;

            for (Node node : visibleGraph.getNodes()) {
                final float x = node.x();
                final float y = node.y();
                final float size = node.size();

                minX = x - size <= minX ? x - size : minX;
                minY = y - size <= minY ? y - size : minY;
                maxX = x + size >= maxX ? x + size : maxX;
                maxY = y + size >= maxY ? y + size : maxY;
            }

            return new Rect2D(minX, minY, maxX, maxY);
        } else {
            return new Rect2D(0, 0, 0, 0);
        }
    }

    private Rect2D getCircleRect2D(float x, float y, float radius) {
        return new Rect2D(x - radius, y - radius, x + radius, y - radius);
    }

    private NodeIterable filterNodeIterable(NodeIterable nodesIterable, Predicate<Node> predicate) {
        return new NodeIterableFilteredWrapper(nodesIterable, predicate);
    }

    private EdgeIterable filterEdgeIterable(EdgeIterable edgesIterable, Predicate<Edge> predicate) {
        return new EdgeIterableFilteredWrapper(edgesIterable, predicate);
    }
}
