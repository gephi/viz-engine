package org.gephi.viz.engine.lwjgl.pipeline;

import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.lwjgl.pipeline.events.LWJGLInputEvent;
import org.gephi.viz.engine.spi.InputListener;
import org.gephi.viz.engine.util.actions.InputActionsProcessor;

/**
 *
 * @author Eduardo Ramos
 */
public class DefaultLWJGLEventListener implements InputListener<LWJGLRenderingTarget, LWJGLInputEvent> {

    private final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine;
    private final InputActionsProcessor inputActionsProcessor;

    public DefaultLWJGLEventListener(VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
        this.engine = engine;
        this.inputActionsProcessor = new InputActionsProcessor(engine);
    }

    @Override
    public boolean processEvent(LWJGLInputEvent event) {
        //TODO
        inputActionsProcessor.processCenterOnGraphEvent();//DEBUGGING 

        return true;
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public String getCategory() {
        return "default";
    }

    @Override
    public int getPreferenceInCategory() {
        return 0;
    }

    @Override
    public String getName() {
        return "Default";
    }

    @Override
    public boolean isAvailable(LWJGLRenderingTarget target) {
        return true;
    }

    @Override
    public void init(LWJGLRenderingTarget target) {
        //NOOP
    }
}
