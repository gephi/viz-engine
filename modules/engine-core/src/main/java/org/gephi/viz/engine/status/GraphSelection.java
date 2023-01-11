package org.gephi.viz.engine.status;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.joml.Vector2f;

import java.util.Collection;
import java.util.Set;

/**
 * @author Eduardo Ramos
 */
public interface GraphSelection {

     enum GraphSelectionMode {
        SIMPLE_MOUSE_SELECTION,
        RECTANGLE_SELECTION
    }
    boolean isNodeSelected(Node node);

    int getSelectedNodesCount();

    Set<Node> getSelectedNodes();

    void setSelectedNodes(Collection<Node> nodes);

    void addSelectedNodes(Collection<Node> nodes);

    void removeSelectedNodes(Collection<Node> nodes);

    void setSelectedNode(Node node);

    void addSelectedNode(Node node);

    void removeSelectedNode(Node node);

    void clearSelectedNodes();

    boolean isEdgeSelected(Edge edge);

    int getSelectedEdgesCount();

    Set<Edge> getSelectedEdges();

    void setSelectedEdges(Collection<Edge> edges);

    void addSelectedEdges(Collection<Edge> edges);

    void removeSelectedEdges(Collection<Edge> edges);

    void setSelectedEdge(Edge edge);

    void addSelectedEdge(Edge edge);

    void removeSelectedEdge(Edge edge);

    void clearSelectedEdges();

    GraphSelectionMode getMode();

    void setMode(GraphSelectionMode mode);

    void clearSelection();

    void startRectangleSelection(Vector2f initialPosition);

    void stopRectangleSelection(Vector2f endPosition);

    void updateRectangleSelection(Vector2f updatedPosition);

    Vector2f getRectangleInitialPosition();

    Vector2f getRectangleCurrentPosition();
}
