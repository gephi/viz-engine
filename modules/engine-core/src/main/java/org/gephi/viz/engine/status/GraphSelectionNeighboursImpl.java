package org.gephi.viz.engine.status;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;

public class GraphSelectionNeighboursImpl implements GraphSelectionNeighbours {

    private final VizEngine engine;
    private final Set<Node> nodes = new HashSet<>();

    public GraphSelectionNeighboursImpl(VizEngine engine) {
        this.engine = engine;
    }

    @Override
    public boolean isNodeSelected(Node node) {
        return nodes.contains(node);
    }

    @Override
    public int getSelectedNodesCount() {
        return nodes.size();
    }

    @Override
    public Set<Node> getSelectedNodes() {
        return Collections.unmodifiableSet(nodes);
    }

    @Override
    public void setSelectedNodes(Collection<Node> nodes) {
        this.nodes.clear();
        if (nodes != null) {
            this.nodes.addAll(nodes);
        }
    }

    @Override
    public void addSelectedNodes(Collection<Node> nodes) {
        if (nodes != null) {
            this.nodes.addAll(nodes);
        }
    }

    @Override
    public void removeSelectedNodes(Collection<Node> nodes) {
        if (nodes != null) {
            this.nodes.removeAll(nodes);
        }
    }

    @Override
    public void setSelectedNode(Node node) {
        if (node == null) {
            this.clearSelectedNodes();
        } else {
            this.nodes.clear();
            this.nodes.add(node);
        }
    }

    @Override
    public void addSelectedNode(Node node) {
        if (node != null) {
            this.nodes.add(node);
        }
    }

    @Override
    public void removeSelectedNode(Node node) {
        if (node != null) {
            this.nodes.remove(node);
        }
    }

    @Override
    public void clearSelectedNodes() {
        this.nodes.clear();
    }

}
