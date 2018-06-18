package org.gephi.viz.engine;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.NEWTEvent;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL;
import static com.jogamp.opengl.GL.GL_COLOR_BUFFER_BIT;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.util.Animator;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import org.gephi.graph.api.GraphModel;
import org.gephi.graph.api.Rect2D;
import org.gephi.viz.engine.pipeline.RenderingLayer;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.spi.PipelinedExecutor;
import org.gephi.viz.engine.spi.Renderer;
import org.gephi.viz.engine.spi.WorldUpdater;
import org.gephi.viz.engine.util.gl.GlDebugOutput;
import org.gephi.viz.engine.util.gl.capabilities.GLCapabilities;
import org.gephi.viz.engine.util.gl.capabilities.Profile;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector2f;
import org.joml.Vector2fc;
import org.joml.Vector3f;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author Eduardo Ramos
 */
public class VizEngine implements GLEventListener, com.jogamp.newt.event.KeyListener, com.jogamp.newt.event.MouseListener {

    //State
    private int width = 0;
    private int height = 0;
    private Rect2D viewBoundaries = new Rect2D(0, 0, 0, 0);
    private GLCapabilities capabilities;

    //Matrix
    private final Matrix4f modelMatrix = new Matrix4f().identity();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f modelViewProjectionMatrix = new Matrix4f();
    private final Matrix4f modelViewProjectionMatrixInverted = new Matrix4f();

    private final float[] modelViewProjectionMatrixFloats = new float[16];

    private float zoom = 0.3f;
    private final Vector2f translate = new Vector2f();

    //Renderers:
    private final Set<Renderer> allRenderers = new LinkedHashSet<>();
    private final List<Renderer> renderersPipeline = new ArrayList<>();

    //World updaters:
    private final Set<WorldUpdater> allUpdaters = new LinkedHashSet<>();
    private final List<WorldUpdater> updatersPipeline = new ArrayList<>();
    private ExecutorService updatersThreadPool;

    //Input listeners:
    private final List<NEWTEvent> eventsQueue = new ArrayList<>();
    private final Set<InputListener> allInuptListeners = new LinkedHashSet<>();
    private final List<InputListener> inputListenersPipeline = new ArrayList<>();

    //Animators
    private Animator animator;

    //Graph:
    private final GraphModel graphModel;

    //Settings:
    private final float[] backgroundColor = new float[]{1, 1, 1, 1};

    //Lookup for communication between components:
    private final InstanceContent instanceContent;
    private final AbstractLookup lookup;

    public VizEngine(GraphModel graphModel) {
        this.graphModel = graphModel;
        this.instanceContent = new InstanceContent();
        this.lookup = new AbstractLookup(instanceContent);
        loadModelViewProjection();
    }

    public void setup(final GLAutoDrawable drawable) {
        if (animator != null && animator.isStarted()) {
            throw new IllegalStateException("Call stop first!");
        }

        drawable.addGLEventListener(this);

        if (drawable instanceof GLWindow) {
            setup((GLWindow) drawable);
        } else if (drawable instanceof GLJPanel) {
            setup((GLJPanel) drawable);
        } else if (drawable instanceof GLCanvas) {
            setup((GLCanvas) drawable);
        } else {
            System.out.println(drawable.getClass() + " event bridge not supported yet. Be sure to manually setup your events listener");
        }

        animator = new Animator();
        animator.add(drawable);
        animator.setRunAsFastAsPossible(false);
        animator.setExclusiveContext(false);
        animator.setUpdateFPSFrames(300, System.out);
    }

    public synchronized void start() {
        if (animator == null) {
            throw new IllegalStateException("Call setup first!");
        }
        animator.start();
    }

    public synchronized void stop() {
        if (animator == null || !animator.isAnimating()) {
            throw new IllegalStateException("Call setup and start first!");
        }


        try {
            updatersThreadPool.shutdown();
            boolean terminated = updatersThreadPool.awaitTermination(10, TimeUnit.SECONDS);
            if (!terminated) {
                updatersThreadPool.shutdownNow();
            }
        } catch (InterruptedException ex) {
            ex.printStackTrace();
            //NOOP
        } finally {
            animator.stop();
        }
    }

    private void setup(GLWindow gLWindow) {
        gLWindow.addKeyListener(this);
        gLWindow.addMouseListener(this);
    }

    private void setup(GLJPanel glJpanel) {
        new AWTKeyAdapter(this, glJpanel).addTo(glJpanel);
        new AWTMouseAdapter(this, glJpanel).addTo(glJpanel);
    }

    private void setup(GLCanvas glCanvas) {
        new AWTKeyAdapter(this, glCanvas).addTo(glCanvas);
        new AWTMouseAdapter(this, glCanvas).addTo(glCanvas);
    }

    private <T extends PipelinedExecutor> void setupPipelineOfElements(GLAutoDrawable drawable, Set<T> allAvailable, List<T> dest, Class<T> elementType) {
        final List<T> elements = new ArrayList<>();

        final Set<String> categories = new HashSet<>();

        for (T t : allAvailable) {
            categories.add(t.getCategory());
        }

        categories.forEach((category) -> {
            //Find the best renderer:
            T bestElement = null;
            for (T r : allAvailable) {
                if (r.isAvailable(drawable) && category.equals(r.getCategory())
                        && (bestElement == null || bestElement.getPreferenceInCategory() < r.getPreferenceInCategory())) {
                    bestElement = r;
                }
            }

            if (bestElement != null) {
                elements.add(bestElement);
                System.out.println("Using best available " + elementType.getName() + " '" + bestElement.getName() + "' for category " + category);
            } else {
                System.out.println("No available " + elementType.getName() + " for category " + category);
            }
        });

        dest.clear();
        dest.addAll(elements);
        Collections.sort(dest, new PipelinedExecutor.Comparator());
    }

    private void setupRenderersPipeline(GLAutoDrawable drawable) {
        setupPipelineOfElements(drawable, allRenderers, renderersPipeline, Renderer.class);
    }

    private void setupWorldUpdatersPipeline(GLAutoDrawable drawable) {
        setupPipelineOfElements(drawable, allUpdaters, updatersPipeline, WorldUpdater.class);
    }

    private void setupInputListenersPipeline(GLAutoDrawable drawable) {
        setupPipelineOfElements(drawable, allInuptListeners, inputListenersPipeline, InputListener.class);
    }

    public void addInputListener(InputListener listener) {
        allInuptListeners.add(listener);
    }

    public boolean hasInputListener(InputListener listener) {
        return allInuptListeners.contains(listener);
    }

    public boolean removeInputListener(InputListener listener) {
        return allInuptListeners.remove(listener);
    }

    public Set<InputListener> getAllInputListeners() {
        return Collections.unmodifiableSet(allInuptListeners);
    }

    public List<InputListener> getInputListenersPipeline() {
        return Collections.unmodifiableList(inputListenersPipeline);
    }

    public void addRenderer(Renderer renderer) {
        if (renderer != null) {
            allRenderers.add(renderer);
        }
    }

    public boolean hasRenderer(Renderer renderer) {
        return allRenderers.contains(renderer);
    }

    public boolean removeRenderer(Renderer renderer) {
        return allRenderers.remove(renderer);
    }

    public Set<Renderer> getAllRenderers() {
        return Collections.unmodifiableSet(allRenderers);
    }

    public List<Renderer> getRenderersPipeline() {
        //TODO: check initialized
        return Collections.unmodifiableList(renderersPipeline);
    }

    public boolean isRendererInPipeline(Renderer renderer) {
        return renderersPipeline.contains(renderer);
    }

    public void addWorldUpdater(WorldUpdater updater) {
        if (updater != null) {
            allUpdaters.add(updater);
        }
    }

    public boolean hasWorldUpdater(WorldUpdater updater) {
        return allUpdaters.contains(updater);
    }

    public boolean removeWorldUpdater(WorldUpdater updater) {
        return allUpdaters.remove(updater);
    }

    public Set<WorldUpdater> getAllWorldUpdaters() {
        return Collections.unmodifiableSet(allUpdaters);
    }

    public List<WorldUpdater> getWorldUpdatersPipeline() {
        //TODO: check initialized
        return Collections.unmodifiableList(updatersPipeline);
    }

    public boolean isWorldUpdaterInPipeline(WorldUpdater renderer) {
        return updatersPipeline.contains(renderer);
    }

    public Vector2fc getTranslate() {
        return translate;
    }

    public Vector2f getTranslate(Vector2f dest) {
        return dest.set(translate);
    }

    public void setTranslate(float x, float y) {
        translate.set(x, y);
        loadModelViewProjection();
    }

    public void setTranslate(Vector2fc value) {
        translate.set(value);
        loadModelViewProjection();
    }

    public void translate(float x, float y) {
        translate.add(x, y);
        loadModelViewProjection();
    }

    public void translate(Vector2fc value) {
        translate.add(value);
        loadModelViewProjection();
    }

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
        loadModelViewProjection();
    }

    public float aspectRatio() {
        return (float) this.width / this.height;
    }

    public void centerOn(Vector2fc center, float width, float height) {
        setTranslate(-center.x(), -center.y());

        if (width > 1 && height > 1) {
            final Rect2D visibleRange = getViewBoundaries();
            final float zoomFactor = Math.max(width / visibleRange.width(), height / visibleRange.height());

            zoom /= zoomFactor;
        }

        loadModelViewProjection();
    }

    private void loadModelViewProjection() {
        loadModel();
        loadView();
        loadProjection();

        projectionMatrix.mulAffine(viewMatrix, modelViewProjectionMatrix);
        modelViewProjectionMatrix.mulAffine(modelMatrix);

        modelViewProjectionMatrix.get(modelViewProjectionMatrixFloats);
        modelViewProjectionMatrix.invertAffine(modelViewProjectionMatrixInverted);

        calculateWorldBoundaries();
    }

    private void loadModel() {
        //Always identity at the moment
    }

    private void loadView() {
        viewMatrix.scaling(zoom, zoom, 1f);
        viewMatrix.translate(translate.x, translate.y, 0);
    }

    private void loadProjection() {
        projectionMatrix.setOrtho2D(-width / 2, width / 2, -height / 2, height / 2);
    }

    private void calculateWorldBoundaries() {
        final Vector3f minCoords = new Vector3f();
        final Vector3f maxCoords = new Vector3f();

        modelViewProjectionMatrixInverted.transformAab(-1, -1, 0, 1, 1, 0, minCoords, maxCoords);

        viewBoundaries = new Rect2D(minCoords.x, minCoords.y, maxCoords.x, maxCoords.y);
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL gl = drawable.getGL();
        gl.glViewport(0, 0, width, height);

        this.width = width;
        this.height = height;

        loadModelViewProjection();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.getContext().addGLDebugListener(new GlDebugOutput());

        capabilities = new GLCapabilities(drawable.getGL(), Profile.CORE);

        System.out.println(capabilities.getVersion());
        System.out.println(capabilities.getExtensions());

        setupRenderersPipeline(drawable);
        setupWorldUpdatersPipeline(drawable);
        setupInputListenersPipeline(drawable);

        final GL gl = drawable.getGL();

        gl.setSwapInterval(0);//Disable Vertical synchro

        gl.glDisable(GL.GL_DEPTH_TEST);//Z-order is set by the order of drawing

        //Disable blending for better performance
        gl.glDisable(GL.GL_BLEND);

        final int numThreads = Math.max(Math.min(updatersPipeline.size(), 4), 1);
        updatersThreadPool = Executors.newFixedThreadPool(numThreads, new ThreadFactory() {
            private int id = 1;

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "World Updater " + id++);
            }
        });

        updatersPipeline.forEach((worldUpdater) -> {
            worldUpdater.init(drawable);
        });

        renderersPipeline.forEach((renderer) -> {
            renderer.init(drawable);
        });

        loadModelViewProjection();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.out.println("Dispose updaters");
        updatersPipeline.forEach((worldUpdater) -> {
            worldUpdater.dispose(drawable);
        });

        System.out.println("Dispose renderers");
        renderersPipeline.forEach((renderer) -> {
            renderer.dispose(drawable);
        });
    }

    private CompletableFuture allUpdatersCompletableFuture = null;

    private CompletableFuture<WorldUpdater> completableFutureOfUpdater(final WorldUpdater updater) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                updater.updateWorld();
            } catch (Throwable t) {
                t.printStackTrace();//TODO Logger
            }
            return updater;
        }, updatersThreadPool);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        final GL gl = drawable.getGL().getGL();

        processInputEvents();

        gl.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);
        gl.glClear(GL_COLOR_BUFFER_BIT);

        //Call updaters again when all of them are finished:
        boolean worldUpdateDone = allUpdatersCompletableFuture != null && allUpdatersCompletableFuture.isDone();
        if (worldUpdateDone) {
            //Signal renderers:
            for (Renderer renderer : renderersPipeline) {
                renderer.worldUpdated(drawable);
            }
        }

        //Call renderers:
        for (RenderingLayer layer : RenderingLayer.values()) {
            for (Renderer renderer : renderersPipeline) {
                if (renderer.getLayers().contains(layer)) {
                    renderer.render(drawable, layer);
                }
            }
        }

        if (!updatersThreadPool.isShutdown()) {
            if (allUpdatersCompletableFuture == null || worldUpdateDone) {
                final CompletableFuture[] futures = new CompletableFuture[updatersPipeline.size()];
                for (int i = 0; i < futures.length; i++) {
                    final WorldUpdater worldUpdater = updatersPipeline.get(i);
                    futures[i] = completableFutureOfUpdater(worldUpdater);
                }

                allUpdatersCompletableFuture = CompletableFuture.allOf(futures);
            }
        }
    }

    public Lookup getLookup() {
        return lookup;
    }

    public void addToLookup(Object instance) {
        instanceContent.add(instance);
    }

    public void removeFromLookup(Object instance) {
        instanceContent.remove(instance);
    }

    public GraphModel getGraphModel() {
        return graphModel;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Matrix4fc getModelMatrix() {
        return modelMatrix;
    }

    public Matrix4fc getViewMatrix() {
        return viewMatrix;
    }

    public Matrix4fc getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4fc getModelViewProjectionMatrix() {
        return modelViewProjectionMatrix;
    }

    public Matrix4fc getModelViewProjectionMatrixInverted() {
        return modelViewProjectionMatrixInverted;
    }

    public Rect2D getViewBoundaries() {
        return viewBoundaries;
    }

    public float[] getBackgroundColor(float[] backgroundColorFloats) {
        System.arraycopy(this.backgroundColor, 0, backgroundColorFloats, 0, 4);
        return backgroundColorFloats;
    }

    public float[] getBackgroundColor() {
        return Arrays.copyOf(backgroundColor, backgroundColor.length);
    }

    public void setBackgroundColor(Color color) {
        float[] backgroundColorComponents = new float[4];
        color.getRGBComponents(backgroundColorComponents);

        setBackgroundColor(backgroundColorComponents);
    }

    public void setBackgroundColor(float[] color) {
        if (color.length != 4) {
            throw new IllegalArgumentException("Expected 4 float RGBA color");
        }

        System.arraycopy(color, 0, backgroundColor, 0, 4);
    }

    public float[] getModelViewProjectionMatrixFloats(float[] mvpFloats) {
        modelViewProjectionMatrix.get(mvpFloats);
        return mvpFloats;
    }

    public float[] getModelViewProjectionMatrixFloats() {
        return Arrays.copyOf(modelViewProjectionMatrixFloats, modelViewProjectionMatrixFloats.length);
    }

    public GLCapabilities getCapabilities() {
        return capabilities;
    }

    public Vector2f screenCoordinatesToWorldCoordinates(int x, int y) {
        return screenCoordinatesToWorldCoordinates(x, y, new Vector2f());
    }

    public Vector2f screenCoordinatesToWorldCoordinates(int x, int y, Vector2f dest) {
        final float halfWidth = width / 2.0f;
        final float halfHeight = height / 2.0f;

        float xScreenNormalized = (-halfWidth + x) / halfWidth;
        float yScreenNormalized = (halfHeight - y) / halfHeight;

        final Vector3f worldCoordinates = new Vector3f();
        modelViewProjectionMatrixInverted.transformProject(xScreenNormalized, yScreenNormalized, 0, worldCoordinates);

        return dest.set(worldCoordinates.x, worldCoordinates.y);
    }

    public Vector2f worldCoordinatesToScreenCoordinates(float x, float y) {
        return worldCoordinatesToScreenCoordinates(x, y, new Vector2f());
    }

    public Vector2f worldCoordinatesToScreenCoordinates(float x, float y, Vector2f dest) {
        final Vector3f screenCoordinates = new Vector3f();
        modelViewProjectionMatrix.transformProject(x, y, 0, screenCoordinates);

        return dest.set(screenCoordinates.x, screenCoordinates.y);
    }

    public Vector2f worldCoordinatesToScreenCoordinates(Vector2fc worldCoordinates) {
        return worldCoordinatesToScreenCoordinates(worldCoordinates, new Vector2f());
    }

    public Vector2f worldCoordinatesToScreenCoordinates(Vector2fc worldCoordinates, Vector2f dest) {
        final Vector3f screenCoordinates = new Vector3f();
        modelViewProjectionMatrix.transformProject(worldCoordinates.x(), worldCoordinates.y(), 0, screenCoordinates);

        return dest.set(screenCoordinates.x, screenCoordinates.y);
    }

    private void processInputEvents() {
        for (InputListener inputListener : inputListenersPipeline) {
            inputListener.frameStart();
        }

        NEWTEvent[] events = eventsQueue.toArray(new NEWTEvent[0]);
        eventsQueue.clear();
        for (NEWTEvent event : events) {
            if (event instanceof KeyEvent) {
                final BiFunction<InputListener, KeyEvent, Boolean> f;

                switch (event.getEventType()) {
                    case KeyEvent.EVENT_KEY_PRESSED:
                        f = InputListener::keyPressed;
                        break;
                    case KeyEvent.EVENT_KEY_RELEASED:
                        f = InputListener::keyReleased;
                        break;
                    default:
                        f = null;
                }

                if (f != null) {
                    for (InputListener inputListener : inputListenersPipeline) {
                        boolean consumed = f.apply(inputListener, (KeyEvent) event);
                        if (consumed) {
                            break;
                        }
                    }
                }
            } else if (event instanceof MouseEvent) {
                final BiFunction<InputListener, MouseEvent, Boolean> f;

                switch (event.getEventType()) {
                    case MouseEvent.EVENT_MOUSE_CLICKED:
                        f = InputListener::mouseClicked;
                        break;
                    case MouseEvent.EVENT_MOUSE_DRAGGED:
                        f = InputListener::mouseDragged;
                        break;
                    case MouseEvent.EVENT_MOUSE_ENTERED:
                        f = InputListener::mouseEntered;
                        break;
                    case MouseEvent.EVENT_MOUSE_EXITED:
                        f = InputListener::mouseExited;
                        break;
                    case MouseEvent.EVENT_MOUSE_MOVED:
                        f = InputListener::mouseMoved;
                        break;
                    case MouseEvent.EVENT_MOUSE_PRESSED:
                        f = InputListener::mousePressed;
                        break;
                    case MouseEvent.EVENT_MOUSE_RELEASED:
                        f = InputListener::mouseReleased;
                        break;
                    case MouseEvent.EVENT_MOUSE_WHEEL_MOVED:
                        f = InputListener::mouseWheelMoved;
                        break;
                    default:
                        f = null;
                }

                if (f != null) {
                    for (InputListener inputListener : inputListenersPipeline) {
                        boolean consumed = f.apply(inputListener, (MouseEvent) event);
                        if (consumed) {
                            break;
                        }
                    }
                }
            }
        }

        for (InputListener inputListener : inputListenersPipeline) {
            inputListener.frameEnd();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mousePressed(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        eventsQueue.add(e);
    }

    @Override
    public void mouseWheelMoved(MouseEvent e) {
        eventsQueue.add(e);
    }
}
