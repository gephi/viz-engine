package org.gephi.viz.engine.status;

import org.gephi.graph.api.Node;

import java.util.Collection;
import java.util.Set;

/**
 *
 * @author Eduardo Ramos
 */
public interface GraphSelectionNeighbours {

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
}
