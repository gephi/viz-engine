package org.gephi.viz.engine.util;

/**
 *
 * @author Eduardo Ramos
 */
public class TimeUtils {

    public static long getTimeMillis() {
        return System.nanoTime() / 1_000_000;
    }
    public static float getFloatSecondGlobalTime() {
            return getTimeMillis() /1000.f;
    }
}
