package org.gephi.viz.engine.status;

import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GraphSelectionImpl implements GraphSelection {

    private final VizEngine engine;
    private final Set<Node> nodes = new HashSet<>();
    private final Set<Edge> edges = new HashSet<>();

    public GraphSelectionImpl(VizEngine engine) {
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

    @Override
    public boolean isEdgeSelected(Edge edge) {
        return edges.contains(edge);
    }

    @Override
    public int getSelectedEdgesCount() {
        return edges.size();
    }

    @Override
    public Set<Edge> getSelectedEdges() {
        return Collections.unmodifiableSet(edges);
    }

    @Override
    public void setSelectedEdges(Collection<Edge> edges) {
        this.edges.clear();
        if (edges != null) {
            this.edges.addAll(edges);
        }
    }

    @Override
    public void addSelectedEdges(Collection<Edge> edges) {
        if (edges != null) {
            this.edges.addAll(edges);
        }
    }

    @Override
    public void removeSelectedEdges(Collection<Edge> edges) {
        if (edges != null) {
            this.edges.removeAll(edges);
        }
    }

    @Override
    public void setSelectedEdge(Edge edge) {
        if (edge == null) {
            this.clearSelectedEdges();
        } else {
            this.edges.clear();
            this.edges.add(edge);
        }
    }

    @Override
    public void addSelectedEdge(Edge edge) {
        if (edge != null) {
            this.edges.add(edge);
        }
    }

    @Override
    public void removeSelectedEdge(Edge edge) {
        if (edge != null) {
            this.edges.remove(edge);
        }
    }

    @Override
    public void clearSelectedEdges() {
        this.edges.clear();
    }

    @Override
    public void clearSelection() {
        clearSelectedEdges();
        clearSelectedNodes();
    }
}
