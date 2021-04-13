package org.gephi.viz.engine.util;

import java.util.Optional;

/**
 *
 * @author Eduardo Ramos
 */
public class TimeUtils {

    static Optional<Float> ANIMATED_START_TIME = Optional.empty();

    static public void setAnimatedStartTime() {
        if(!ANIMATED_START_TIME.isPresent()){
            ANIMATED_START_TIME = Optional.of(getFloatSecondGlobalTime());
        }
    }
    static public float getAnimartedStardTime() {
        if(ANIMATED_START_TIME.isPresent()) {
            return ANIMATED_START_TIME.get();
        } else {
            return getFloatSecondGlobalTime();
        }
    }
    static public void unsetAnimatedStartTime() {
        if(ANIMATED_START_TIME.isPresent()){
            ANIMATED_START_TIME = Optional.empty();
        }
    }

    public static long getTimeMillis() {
        return System.nanoTime() / 1_000_000;
    }
    public static float getFloatSecondGlobalTime() {
            return getTimeMillis() /1000.f;
    }
}
