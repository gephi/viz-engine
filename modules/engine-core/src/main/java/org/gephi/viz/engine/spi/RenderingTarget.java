package org.gephi.viz.engine.spi;

import org.gephi.viz.engine.VizEngine;

/**
 *
 * @author Eduardo Ramos
 */
public interface RenderingTarget {

    void setup(VizEngine engine);

    void start();

    void stop();

    void frameStart();

    void frameEnd();
}
