package org.gephi.viz.engine.lwjgl.pipeline.common;

import java.nio.FloatBuffer;
import org.gephi.graph.api.Edge;
import org.gephi.graph.api.Graph;
import org.gephi.graph.api.Node;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.pipeline.common.InstanceCounter;
import org.gephi.viz.engine.status.GraphSelection;
import static org.gephi.viz.engine.util.gl.Constants.*;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelDirected;
import org.gephi.viz.engine.lwjgl.models.EdgeLineModelUndirected;
import org.gephi.viz.engine.lwjgl.util.gl.GLBuffer;
import org.gephi.viz.engine.lwjgl.util.gl.GLVertexArrayObject;
import org.gephi.viz.engine.util.gl.OpenGLOptions;
import org.gephi.viz.engine.util.structure.EdgesCallback;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_UNSIGNED_BYTE;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import org.lwjgl.opengl.GLCapabilities;

/**
 *
 * @author Eduardo Ramos
 */
public class AbstractEdgeData {

    protected final EdgeLineModelUndirected lineModelUndirected = new EdgeLineModelUndirected();
    protected final EdgeLineModelDirected lineModelDirected = new EdgeLineModelDirected();

    protected final InstanceCounter undirectedInstanceCounter = new InstanceCounter();
    protected final InstanceCounter directedInstanceCounter = new InstanceCounter();

    protected GLBuffer vertexGLBufferUndirected;
    protected GLBuffer vertexGLBufferDirected;
    protected GLBuffer attributesGLBuffer;

    protected final EdgesCallback edgesCallback = new EdgesCallback();

    protected static final int ATTRIBS_STRIDE
            = Math.max(
                    EdgeLineModelUndirected.TOTAL_ATTRIBUTES_FLOATS,
                    EdgeLineModelDirected.TOTAL_ATTRIBUTES_FLOATS
            );

    protected static final int VERTEX_COUNT_UNDIRECTED = EdgeLineModelUndirected.VERTEX_COUNT;
    protected static final int VERTEX_COUNT_DIRECTED = EdgeLineModelDirected.VERTEX_COUNT;
    protected static final int VERTEX_COUNT_MAX = Math.max(VERTEX_COUNT_DIRECTED, VERTEX_COUNT_UNDIRECTED);

    protected final boolean instanced;

    public AbstractEdgeData(boolean instanced) {
        this.instanced = instanced;
    }

    public void init() {
        lineModelDirected.initGLPrograms();
        lineModelUndirected.initGLPrograms();
    }

    protected int updateDirectedData(
            final Graph graph,
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index
    ) {
        return updateDirectedData(graph, someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor, attribs, index, null);
    }

    protected int updateDirectedData(
            final Graph graph,
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index, final FloatBuffer directBuffer
    ) {
        checkBufferIndexing(directBuffer, attribs, index);

        if (graph.isUndirected()) {
            directedInstanceCounter.unselectedCount = 0;
            directedInstanceCounter.selectedCount = 0;
            return index;
        }

        saveSelectionState(someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

        int newEdgesCountUnselected = 0;
        int newEdgesCountSelected = 0;
        if (someEdgesSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    final boolean selected = graphSelection.isEdgeSelected(edge);
                    if (!selected) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillDirectedEdgeAttributesDataWithSelection(attribs, edge, index, selected);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            } else {
                //First non-selected (bottom):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    if (graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountUnselected++;

                    index = fillDirectedEdgeAttributesDataWithSelection(attribs, edge, index, false);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (!edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillDirectedEdgeAttributesDataWithSelection(attribs, edge, index, true);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all edges, no selection active:
            for (int j = 0; j < visibleEdgesCount; j++) {
                final Edge edge = visibleEdgesArray[j];
                if (!edge.isDirected()) {
                    continue;
                }

                newEdgesCountSelected++;

                index = fillDirectedEdgeAttributesDataWithoutSelection(attribs, edge, index);

                if (directBuffer != null && index == attribs.length) {
                    directBuffer.put(attribs, 0, attribs.length);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (directBuffer != null && index > 0) {
            directBuffer.put(attribs, 0, index);
            index = 0;
        }

        directedInstanceCounter.unselectedCount = newEdgesCountUnselected;
        directedInstanceCounter.selectedCount = newEdgesCountSelected;

        return index;
    }

    protected int updateUndirectedData(
            final Graph graph,
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index
    ) {
        return updateUndirectedData(graph, someEdgesSelection, hideNonSelected, visibleEdgesCount, visibleEdgesArray, graphSelection, someNodesSelection, edgeSelectionColor, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor, attribs, index, null);
    }

    protected int updateUndirectedData(
            final Graph graph,
            final boolean someEdgesSelection, final boolean hideNonSelected, final int visibleEdgesCount, final Edge[] visibleEdgesArray, final GraphSelection graphSelection, final boolean someNodesSelection, final boolean edgeSelectionColor, final float edgeBothSelectionColor, final float edgeOutSelectionColor, final float edgeInSelectionColor,
            final float[] attribs, int index, final FloatBuffer directBuffer
    ) {
        checkBufferIndexing(directBuffer, attribs, index);

        if (graph.isDirected()) {
            undirectedInstanceCounter.unselectedCount = 0;
            undirectedInstanceCounter.selectedCount = 0;
            return index;
        }

        saveSelectionState(someNodesSelection, edgeSelectionColor, graphSelection, edgeBothSelectionColor, edgeOutSelectionColor, edgeInSelectionColor);

        int newEdgesCountUnselected = 0;
        int newEdgesCountSelected = 0;
        //Undirected edges:
        if (someEdgesSelection) {
            if (hideNonSelected) {
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillUndirectedEdgeAttributesDataWithSelection(attribs, edge, index, true);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            } else {
                //First non-selected (bottom):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountUnselected++;

                    index = fillUndirectedEdgeAttributesDataWithSelection(attribs, edge, index, false);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }

                //Then selected ones (up):
                for (int j = 0; j < visibleEdgesCount; j++) {
                    final Edge edge = visibleEdgesArray[j];
                    if (edge.isDirected()) {
                        continue;
                    }

                    if (!graphSelection.isEdgeSelected(edge)) {
                        continue;
                    }

                    newEdgesCountSelected++;

                    index = fillUndirectedEdgeAttributesDataWithSelection(attribs, edge, index, true);

                    if (directBuffer != null && index == attribs.length) {
                        directBuffer.put(attribs, 0, attribs.length);
                        index = 0;
                    }
                }
            }
        } else {
            //Just all edges, no selection active:
            for (int j = 0; j < visibleEdgesCount; j++) {
                final Edge edge = visibleEdgesArray[j];
                if (edge.isDirected()) {
                    continue;
                }

                newEdgesCountSelected++;

                index = fillUndirectedEdgeAttributesDataWithoutSelection(attribs, edge, index);

                if (directBuffer != null && index == attribs.length) {
                    directBuffer.put(attribs, 0, attribs.length);
                    index = 0;
                }
            }
        }

        //Remaining:
        if (directBuffer != null && index > 0) {
            directBuffer.put(attribs, 0, index);
            index = 0;
        }

        undirectedInstanceCounter.unselectedCount = newEdgesCountUnselected;
        undirectedInstanceCounter.selectedCount = newEdgesCountSelected;

        return index;
    }

    private void checkBufferIndexing(final FloatBuffer directBuffer, final float[] attribs, final int index) {
        if (directBuffer != null) {
            if (attribs.length % ATTRIBS_STRIDE != 0) {
                throw new IllegalArgumentException("When filling a directBuffer, attribs buffer length should be a multiple of ATTRIBS_STRIDE = " + ATTRIBS_STRIDE);
            }

            if (index % ATTRIBS_STRIDE != 0) {
                throw new IllegalArgumentException("When filling a directBuffer, index should be a multiple of ATTRIBS_STRIDE = " + ATTRIBS_STRIDE);
            }
        }
    }

    private boolean someNodesSelection;
    private boolean edgeSelectionColor;
    private GraphSelection graphSelection;
    private float edgeBothSelectionColor;
    private float edgeOutSelectionColor;
    private float edgeInSelectionColor;

    private void saveSelectionState(final boolean someNodesSelection1, final boolean edgeSelectionColor1, final GraphSelection graphSelection1, final float edgeBothSelectionColor1, final float edgeOutSelectionColor1, final float edgeInSelectionColor1) {
        this.someNodesSelection = someNodesSelection1;
        this.edgeSelectionColor = edgeSelectionColor1;
        this.graphSelection = graphSelection1;
        this.edgeBothSelectionColor = edgeBothSelectionColor1;
        this.edgeOutSelectionColor = edgeOutSelectionColor1;
        this.edgeInSelectionColor = edgeInSelectionColor1;
    }

    protected void fillUndirectedEdgeAttributesDataBase(final float[] buffer, final Edge edge, final int index) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        final float sourceX = source.x();
        final float sourceY = source.y();
        final float targetX = target.x();
        final float targetY = target.y();

        //Position:
        buffer[index + 0] = sourceX;
        buffer[index + 1] = sourceY;

        //Target position:
        buffer[index + 2] = targetX;
        buffer[index + 3] = targetY;

        //Size:
        buffer[index + 4] = (float) edge.getWeight();

        //Source color:
        buffer[index + 5] = Float.intBitsToFloat(source.getRGBA());

        //Target color:
        buffer[index + 6] = Float.intBitsToFloat(target.getRGBA());
    }

    protected int fillUndirectedEdgeAttributesDataWithoutSelection(final float[] buffer, final Edge edge, final int index) {
        fillUndirectedEdgeAttributesDataBase(buffer, edge, index);

        //Color, color bias and color multiplier:
        buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
        buffer[index + 8] = 0;//Bias
        buffer[index + 9] = 1;//Multiplier

        return index + ATTRIBS_STRIDE;
    }

    protected int fillUndirectedEdgeAttributesDataWithSelection(final float[] buffer, final Edge edge, final int index, final boolean selected) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        fillUndirectedEdgeAttributesDataBase(buffer, edge, index);

        //Color, color bias and color multiplier:
        if (selected) {
            if (someNodesSelection && edgeSelectionColor) {
                boolean sourceSelected = graphSelection.isNodeSelected(source);
                boolean targetSelected = graphSelection.isNodeSelected(target);

                if (sourceSelected && targetSelected) {
                    buffer[index + 7] = edgeBothSelectionColor;//Color
                } else if (sourceSelected) {
                    buffer[index + 7] = edgeOutSelectionColor;//Color
                } else if (targetSelected) {
                    buffer[index + 7] = edgeInSelectionColor;//Color
                } else {
                    buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
                }

                buffer[index + 8] = 0;//Bias
                buffer[index + 9] = 1;//Multiplier
            } else {
                if (someNodesSelection && edge.alpha() <= 0) {
                    if (graphSelection.isNodeSelected(source)) {
                        buffer[index + 7] = Float.intBitsToFloat(target.getRGBA());//Color
                    } else {
                        buffer[index + 7] = Float.intBitsToFloat(source.getRGBA());//Color
                    }
                } else {
                    buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
                }

                buffer[index + 8] = 0.5f;//Bias
                buffer[index + 9] = 0.5f;//Multiplier
            }
        } else {
            buffer[index + 7] = Float.intBitsToFloat(edge.getRGBA());//Color
            buffer[index + 8] = 0;//Bias
            buffer[index + 9] = 1;//Multiplier
        }

        return index + ATTRIBS_STRIDE;
    }

    protected void fillDirectedEdgeAttributesDataBase(final float[] buffer, final Edge edge, final int index) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        final float sourceX = source.x();
        final float sourceY = source.y();
        final float targetX = target.x();
        final float targetY = target.y();

        //Position:
        buffer[index + 0] = sourceX;
        buffer[index + 1] = sourceY;

        //Target position:
        buffer[index + 2] = targetX;
        buffer[index + 3] = targetY;

        //Size:
        buffer[index + 4] = (float) edge.getWeight();

        //Source color:
        buffer[index + 5] = Float.intBitsToFloat(source.getRGBA());
    }

    protected int fillDirectedEdgeAttributesDataWithoutSelection(final float[] buffer, final Edge edge, final int index) {
        fillDirectedEdgeAttributesDataBase(buffer, edge, index);

        //Color, color bias and color multiplier:
        buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
        buffer[index + 7] = 0;//Bias
        buffer[index + 8] = 1;//Multiplier

        //Target size:
        buffer[index + 9] = edge.getTarget().size();

        return index + ATTRIBS_STRIDE;
    }

    protected int fillDirectedEdgeAttributesDataWithSelection(final float[] buffer, final Edge edge, final int index, final boolean selected) {
        final Node source = edge.getSource();
        final Node target = edge.getTarget();

        fillDirectedEdgeAttributesDataBase(buffer, edge, index);

        //Color, color bias and color multiplier:
        if (selected) {
            if (someNodesSelection && edgeSelectionColor) {
                boolean sourceSelected = graphSelection.isNodeSelected(source);
                boolean targetSelected = graphSelection.isNodeSelected(target);

                if (sourceSelected && targetSelected) {
                    buffer[index + 6] = edgeBothSelectionColor;//Color
                } else if (sourceSelected) {
                    buffer[index + 6] = edgeOutSelectionColor;//Color
                } else if (targetSelected) {
                    buffer[index + 6] = edgeInSelectionColor;//Color
                } else {
                    buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
                }

                buffer[index + 7] = 0;//Bias
                buffer[index + 8] = 1;//Multiplier
            } else {
                if (someNodesSelection && edge.alpha() <= 0) {
                    if (graphSelection.isNodeSelected(source)) {
                        buffer[index + 6] = Float.intBitsToFloat(target.getRGBA());//Color
                    } else {
                        buffer[index + 6] = Float.intBitsToFloat(source.getRGBA());//Color
                    }
                } else {
                    buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
                }

                buffer[index + 7] = 0.5f;//Bias
                buffer[index + 8] = 0.5f;//Multiplier
            }
        } else {
            buffer[index + 6] = Float.intBitsToFloat(edge.getRGBA());//Color
            buffer[index + 7] = 0;//Bias
            buffer[index + 8] = 1;//Multiplier
        }

        //Target size:
        buffer[index + 9] = target.size();

        return index + ATTRIBS_STRIDE;
    }

    private UndirectedEdgesVAO undirectedEdgesVAO;
    private DirectedEdgesVAO directedEdgesVAO;

    public void setupUndirectedVertexArrayAttributes(VizEngine engine) {
        if (undirectedEdgesVAO == null) {
            undirectedEdgesVAO = new UndirectedEdgesVAO(
                    engine.getLookup().lookup(GLCapabilities.class),
                    engine.getLookup().lookup(OpenGLOptions.class)
            );
        }

        undirectedEdgesVAO.use();
    }

    public void unsetupUndirectedVertexArrayAttributes() {
        undirectedEdgesVAO.stopUsing();
    }

    public void setupDirectedVertexArrayAttributes(VizEngine engine) {
        if (directedEdgesVAO == null) {
            directedEdgesVAO = new DirectedEdgesVAO(
                    engine.getLookup().lookup(GLCapabilities.class),
                    engine.getLookup().lookup(OpenGLOptions.class)
            );
        }

        directedEdgesVAO.use();
    }

    public void unsetupDirectedVertexArrayAttributes() {
        directedEdgesVAO.stopUsing();
    }

    public void dispose() {
        if (vertexGLBufferUndirected != null) {
            vertexGLBufferUndirected.destroy();
        }

        if (vertexGLBufferDirected != null) {
            vertexGLBufferDirected.destroy();
        }

        if (attributesGLBuffer != null) {
            attributesGLBuffer.destroy();
        }

        edgesCallback.reset();
    }

    private class UndirectedEdgesVAO extends GLVertexArrayObject {

        public UndirectedEdgesVAO(GLCapabilities capabilities, OpenGLOptions openGLOptions) {
            super(capabilities, openGLOptions);
        }

        @Override
        protected void configure() {
            vertexGLBufferUndirected.bind();
            {
                glVertexAttribPointer(SHADER_VERT_LOCATION, EdgeLineModelUndirected.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBufferUndirected.unbind();

            attributesGLBuffer.bind();
            {
                int stride = ATTRIBS_STRIDE * Float.BYTES;
                int offset = 0;
                glVertexAttribPointer(SHADER_POSITION_LOCATION, EdgeLineModelUndirected.POSITION_SOURCE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.POSITION_SOURCE_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_POSITION_TARGET_LOCATION, EdgeLineModelUndirected.POSITION_TARGET_LOCATION, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.POSITION_TARGET_LOCATION * Float.BYTES;

                glVertexAttribPointer(SHADER_SIZE_LOCATION, EdgeLineModelUndirected.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.SIZE_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_SOURCE_COLOR_LOCATION, EdgeLineModelUndirected.SOURCE_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.SOURCE_COLOR_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_TARGET_COLOR_LOCATION, EdgeLineModelUndirected.TARGET_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.TARGET_COLOR_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_LOCATION, EdgeLineModelUndirected.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelUndirected.COLOR_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_BIAS_LOCATION, EdgeLineModelUndirected.COLOR_BIAS_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelUndirected.COLOR_BIAS_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_MULTIPLIER_LOCATION, EdgeLineModelUndirected.COLOR_MULTIPLIER_FLOATS, GL_FLOAT, false, stride, offset);
            }
            attributesGLBuffer.unbind();
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            return new int[]{
                SHADER_VERT_LOCATION,
                SHADER_POSITION_LOCATION,
                SHADER_POSITION_TARGET_LOCATION,
                SHADER_SIZE_LOCATION,
                SHADER_SOURCE_COLOR_LOCATION,
                SHADER_TARGET_COLOR_LOCATION,
                SHADER_COLOR_LOCATION,
                SHADER_COLOR_BIAS_LOCATION,
                SHADER_COLOR_MULTIPLIER_LOCATION
            };
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            if (instanced) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_POSITION_TARGET_LOCATION,
                    SHADER_SIZE_LOCATION,
                    SHADER_SOURCE_COLOR_LOCATION,
                    SHADER_TARGET_COLOR_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION
                };
            } else {
                return null;
            }
        }

    }

    private class DirectedEdgesVAO extends GLVertexArrayObject {

        public DirectedEdgesVAO(GLCapabilities capabilities, OpenGLOptions openGLOptions) {
            super(capabilities, openGLOptions);
        }

        @Override
        protected void configure() {
            vertexGLBufferDirected.bind();
            {
                glVertexAttribPointer(SHADER_VERT_LOCATION, EdgeLineModelDirected.VERTEX_FLOATS, GL_FLOAT, false, 0, 0);
            }
            vertexGLBufferDirected.unbind();

            attributesGLBuffer.bind();
            {
                int stride = ATTRIBS_STRIDE * Float.BYTES;
                int offset = 0;
                glVertexAttribPointer(SHADER_POSITION_LOCATION, EdgeLineModelDirected.POSITION_SOURCE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.POSITION_SOURCE_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_POSITION_TARGET_LOCATION, EdgeLineModelDirected.POSITION_TARGET_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.POSITION_TARGET_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_SIZE_LOCATION, EdgeLineModelDirected.SIZE_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.SIZE_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_SOURCE_COLOR_LOCATION, EdgeLineModelDirected.SOURCE_COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelDirected.SOURCE_COLOR_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_LOCATION, EdgeLineModelDirected.COLOR_FLOATS * Float.BYTES, GL_UNSIGNED_BYTE, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_BIAS_LOCATION, EdgeLineModelDirected.COLOR_BIAS_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_BIAS_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_COLOR_MULTIPLIER_LOCATION, EdgeLineModelDirected.COLOR_MULTIPLIER_FLOATS, GL_FLOAT, false, stride, offset);
                offset += EdgeLineModelDirected.COLOR_MULTIPLIER_FLOATS * Float.BYTES;

                glVertexAttribPointer(SHADER_TARGET_SIZE_LOCATION, EdgeLineModelDirected.TARGET_SIZE_FLOATS, GL_FLOAT, false, stride, offset);
            }
            attributesGLBuffer.unbind();
        }

        @Override
        protected int[] getUsedAttributeLocations() {
            return new int[]{
                SHADER_VERT_LOCATION,
                SHADER_POSITION_LOCATION,
                SHADER_POSITION_TARGET_LOCATION,
                SHADER_SIZE_LOCATION,
                SHADER_SOURCE_COLOR_LOCATION,
                SHADER_COLOR_LOCATION,
                SHADER_COLOR_BIAS_LOCATION,
                SHADER_COLOR_MULTIPLIER_LOCATION,
                SHADER_TARGET_SIZE_LOCATION
            };
        }

        @Override
        protected int[] getInstancedAttributeLocations() {
            if (instanced) {
                return new int[]{
                    SHADER_POSITION_LOCATION,
                    SHADER_POSITION_TARGET_LOCATION,
                    SHADER_SIZE_LOCATION,
                    SHADER_SOURCE_COLOR_LOCATION,
                    SHADER_COLOR_LOCATION,
                    SHADER_COLOR_BIAS_LOCATION,
                    SHADER_COLOR_MULTIPLIER_LOCATION,
                    SHADER_TARGET_SIZE_LOCATION
                };
            } else {
                return null;
            }
        }

    }
}
