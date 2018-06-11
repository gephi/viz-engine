package org.gephi.viz.engine.spi;

/**
 *
 * @author Eduardo Ramos
 */
public interface WorldUpdater extends PipelinedExecutor {

    void updateWorld();
}
