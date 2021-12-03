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
    private Graph graph;
    private float edgesMinWeight = 1;
    private float edgesMaxWeight = 1;

    private void init() {
        graphModel = engine.getGraphModel();
        graph = graphModel.getGraphVisible();
    }

    private void ensureInitialized() {
        if (graph == null) {
            init();
        }
    }

    @Override
    public Graph getGraph() {
        ensureInitialized();
        return graph;
    }

    public void indexNodes() {
        //NOOP
    }

    public void indexEdges() {
        ensureInitialized();

        if (graph.getEdgeCount() > 0) {
            final Column weightColumn = graph.getModel().getEdgeTable().getColumn(GraphStoreConfiguration.EDGE_WEIGHT_INDEX);

            if (weightColumn.isIndexed() && AttributeUtils.isSimpleType(weightColumn.getTypeClass())) {
                graph.readLock();
                try {
                    edgesMinWeight = graph.getModel().getEdgeIndex().getMinValue(weightColumn).floatValue();
                    edgesMaxWeight = graph.getModel().getEdgeIndex().getMaxValue(weightColumn).floatValue();
                } finally {
                    graph.readUnlockAll();
                }
            } else {
                graph.readLock();
                try {
                    final GraphView graphView = graph.getView();
                    final boolean isView = !graphView.isMainView();

                    if (!isView) {
                        float minWeight = Float.MAX_VALUE;
                        float maxWeight = Float.MIN_VALUE;

                        for (Edge edge : graph.getEdges()) {
                            float weight = (float) edge.getWeight(graphView);
                            minWeight = weight <= minWeight ? weight : minWeight;
                            maxWeight = weight >= maxWeight ? weight : maxWeight;
                        }

                        edgesMinWeight = minWeight;
                        edgesMaxWeight = maxWeight;
                    }
                } finally {
                    graph.readUnlockAll();
                }
            }
        } else {
            edgesMinWeight = edgesMaxWeight = 1;
        }
    }

    @Override
    public int getNodeCount() {
        ensureInitialized();

        return graph.getNodeCount();
    }

    @Override
    public int getEdgeCount() {
        ensureInitialized();

        return graph.getEdgeCount();
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

        return graphModel.getSpatialIndex().getNodesInArea(engine.getViewBoundaries());
    }

    @Override
    public void getVisibleNodes(ElementsCallback<Node> callback) {
        ensureInitialized();

        callback.start(graph);

        final NodeIterable nodeIterable = graphModel.getSpatialIndex().getNodesInArea(engine.getViewBoundaries());
        try {
            for (Node node : nodeIterable) {
                callback.accept(node);
            }
        } finally {
            nodeIterable.doBreak();
        }

        callback.end(graph);
    }

    @Override
    public EdgeIterable getVisibleEdges() {
        ensureInitialized();

        return graphModel.getSpatialIndex().getEdgesInArea(engine.getViewBoundaries());
    }

    @Override
    public void getVisibleEdges(ElementsCallback<Edge> callback) {
        ensureInitialized();

        callback.start(graph);
        final EdgeIterable edgeIterable = graphModel.getSpatialIndex().getEdgesInArea(engine.getViewBoundaries());

        try {
            for (Edge edge : edgeIterable) {
                callback.accept(edge);
            }
        } finally {
            edgeIterable.doBreak();
        }

        callback.end(graph);
    }

    @Override
    public NodeIterable getNodesUnderPosition(float x, float y) {
        ensureInitialized();

        return filterNodeIterable(graphModel.getSpatialIndex().getNodesInArea(getCircleRect2D(x, y, 0)), node -> {
            final float size = node.size();

            return Intersectionf.testPointCircle(x, y, node.x(), node.y(), size * size);
        });
    }

    @Override
    public NodeIterable getNodesInsideCircle(float centerX, float centerY, float radius) {
        ensureInitialized();

        return filterNodeIterable(graphModel.getSpatialIndex().getNodesInArea(getCircleRect2D(centerX, centerY, radius)), node -> {
            return Intersectionf.testCircleCircle(centerX, centerY, radius, node.x(), node.y(), node.size());
        });
    }

    @Override
    public NodeIterable getNodesInsideRectangle(Rect2D rect) {
        ensureInitialized();

        return filterNodeIterable(graphModel.getSpatialIndex().getNodesInArea(rect), node -> {
            final float size = node.size();

            return Intersectionf.testAarCircle(rect.minX, rect.minY, rect.maxX, rect.maxY, node.x(), node.y(), size * size);
        });
    }

    @Override
    public EdgeIterable getEdgesInsideRectangle(Rect2D rect) {
        ensureInitialized();

        return filterEdgeIterable(graphModel.getSpatialIndex().getEdgesInArea(rect), edge -> {
            final Node source = edge.getSource();
            final Node target = edge.getTarget();

            //TODO: take width into account!
            return Intersectionf.testAarLine(rect.minX, rect.minY, rect.maxX, rect.maxY, source.x(), source.y(), target.x(), target.y());
        });
    }

    @Override
    public EdgeIterable getEdgesInsideCircle(float centerX, float centerY, float radius) {
        ensureInitialized();

        return filterEdgeIterable(graphModel.getSpatialIndex().getEdgesInArea(getCircleRect2D(centerX, centerY, radius)), edge -> {
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

        if (graph.getNodeCount() > 0) {
            float minX = Float.MAX_VALUE;
            float maxX = Float.MIN_VALUE;
            float minY = Float.MAX_VALUE;
            float maxY = Float.MIN_VALUE;

            for (Node node : graph.getNodes()) {
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
