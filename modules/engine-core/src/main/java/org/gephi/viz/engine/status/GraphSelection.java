package org.gephi.viz.engine.status;

import java.util.Collection;
import java.util.Set;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;

/**
 * @author Eduardo Ramos
 */
public interface GraphSelection {

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

    void clearSelection();

}
