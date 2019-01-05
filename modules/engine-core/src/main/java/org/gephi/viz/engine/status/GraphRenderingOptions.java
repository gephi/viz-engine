package org.gephi.viz.engine.status;

import java.awt.Color;

/**
 *
 * @author Eduardo Ramos
 */
public interface GraphRenderingOptions {

    //Show:
    public static final boolean DEFAULT_SHOW_NODES = true;
    public static final boolean DEFAULT_SHOW_EDGES = true;
    public static final boolean DEFAULT_SHOW_NODE_LABELS = false;
    public static final boolean DEFAULT_SHOW_EDGE_LABELS = false;

    //Edges:
    public static final float DEFAULT_EDGE_SCALE = 2f;
    public static final boolean DEFAULT_ENABLE_EDGE_SELECTION_COLOR = false;
    public static final Color DEFAULT_EDGE_IN_SELECTION_COLOR = new Color(32, 95, 154, 255);
    public static final Color DEFAULT_EDGE_OUT_SELECTION_COLOR = new Color(196, 66, 79, 255);
    public static final Color DEFAULT_EDGE_BOTH_SELECTION_COLOR = new Color(248, 215, 83, 255);

    //Selection:
    public static final boolean DEFAULT_HIDE_NON_SELECTED = false;
    public static final boolean DEFAULT_LIGHTEN_NON_SELECTED = true;
    public static final boolean DEFAULT_AUTO_SELECT_NEIGHBOURS = true;
    public static final float DEFAULT_LIGHTEN_NON_SELECTED_FACTOR = 0.85f;

    float getEdgeScale();

    void setEdgeScale(float edgeScale);

    boolean isShowNodes();

    void setShowNodes(boolean showNodes);

    boolean isShowEdges();

    void setShowEdges(boolean showEdges);

    boolean isShowNodeLabels();

    void setShowNodeLabels(boolean showNodeLabels);

    boolean isShowEdgeLabels();

    void setShowEdgeLabels(boolean showEdgeLabels);

    public boolean isHideNonSelected();

    public void setHideNonSelected(boolean hideNonSelected);

    public boolean isLightenNonSelected();

    public void setLightenNonSelected(boolean lightenNonSelected);

    public float getLightenNonSelectedFactor();

    public void setLightenNonSelectedFactor(float lightenNonSelectedFactor);

    boolean isAutoSelectNeighbours();

    void setAutoSelectNeighbours(boolean autoSelectNeighbours);

    boolean isEdgeSelectionColor();

    void setEdgeSelectionColor(boolean edgeSelectionColor);

    Color getEdgeBothSelectionColor();

    void setEdgeBothSelectionColor(Color color);

    Color getEdgeOutSelectionColor();

    void setEdgeOutSelectionColor(Color color);

    Color getEdgeInSelectionColor();

    void setEdgeInSelectionColor(Color color);
}
