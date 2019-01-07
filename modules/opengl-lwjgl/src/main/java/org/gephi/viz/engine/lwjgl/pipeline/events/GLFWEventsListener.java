package org.gephi.viz.engine.lwjgl.pipeline.events;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.APIUtil.apiClassTokens;

/**
 *
 * @author Eduardo Ramos
 */
public class GLFWEventsListener {

    private static final Map<Integer, String> KEY_CODES = apiClassTokens((field, value) -> field.getName().startsWith("GLFW_KEY_"), null, GLFW.class);
    private static final int DEFAULT_CLICK_TIME_DURATION_MILLIS = 200;
    private static final int DEFAULT_DOUBLE_CLICK_TIME_PERIOD_MILLIS = 500;

    private final long windowHandle;
    private final VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine;

    private final int clickTimeDurationMillis;
    private final int doubleClickTimePeriodMillis;

    public GLFWEventsListener(long windowHandle, VizEngine<LWJGLRenderingTarget, LWJGLInputEvent> engine) {
        this.windowHandle = windowHandle;
        this.engine = engine;
        this.clickTimeDurationMillis = DEFAULT_CLICK_TIME_DURATION_MILLIS;
        this.doubleClickTimePeriodMillis = DEFAULT_DOUBLE_CLICK_TIME_PERIOD_MILLIS;
    }

    private int mouseX = 0;
    private int mouseY = 0;

    private long lastLeftMouseButtonPressMillis = 0;
    private long lastMiddleMouseButtonPressMillis = 0;
    private long lastRightMouseButtonPressMillis = 0;

    private boolean isLeftMouseButtonPressed;
    private boolean isRightMouseButtonPressed;

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
        final Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    public void register() {
        glfwSetKeyCallback(windowHandle, (windowHnd, key, scancode, action, mods) -> {
            String state;
            switch (action) {
                case GLFW_RELEASE:
                    state = "released";
                    break;
                case GLFW_PRESS:
                    state = "pressed";
                    break;
                case GLFW_REPEAT:
                    state = "repeated";
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unsupported key action: 0x%X", action));
            }

            printEvent("key %s[%s - %d] was %s", windowHnd, getModState(mods), KEY_CODES.get(key), scancode, state);
        });
        glfwSetCharCallback(windowHandle, (windowHnd, codepoint) -> printEvent("char %s", windowHnd, Character.toString((char) codepoint)));
        glfwSetCharModsCallback(windowHandle, (windowHnd, codepoint, mods) -> printEvent(
                "char mods %s%s", windowHnd, getModState(mods), Character.toString((char) codepoint)
        ));
        glfwSetMouseButtonCallback(windowHandle, (windowHnd, buttonCode, action, mods) -> {
            final MouseEvent.Button button = toButtonEnum(buttonCode);
            final long now = System.currentTimeMillis();
            switch (action) {
                case GLFW_RELEASE:
                    if (button == MouseEvent.Button.LEFT) {
                        isLeftMouseButtonPressed = false;
                    }
                    if (button == MouseEvent.Button.RIGHT) {
                        isRightMouseButtonPressed = false;
                    }

                    final boolean isClick
                            = (button == MouseEvent.Button.LEFT && (now - lastLeftMouseButtonPressMillis) < clickTimeDurationMillis)
                            || (button == MouseEvent.Button.MIDDLE && (now - lastMiddleMouseButtonPressMillis) < clickTimeDurationMillis)
                            || (button == MouseEvent.Button.RIGHT && (now - lastRightMouseButtonPressMillis) < clickTimeDurationMillis);

                    if (isClick) {
                        engine.queueEvent(
                                MouseEvent.click(button, mouseX, mouseY)
                        );
                    } else {
                        engine.queueEvent(
                                MouseEvent.release(button, mouseX, mouseY)
                        );
                    }

                    break;
                case GLFW_PRESS:
                    final boolean isDoubleClick
                            = (button == MouseEvent.Button.LEFT && (now - lastLeftMouseButtonPressMillis) < doubleClickTimePeriodMillis)
                            || (button == MouseEvent.Button.MIDDLE && (now - lastMiddleMouseButtonPressMillis) < doubleClickTimePeriodMillis)
                            || (button == MouseEvent.Button.RIGHT && (now - lastRightMouseButtonPressMillis) < doubleClickTimePeriodMillis);

                    if (isDoubleClick) {
                        engine.queueEvent(
                                MouseEvent.doubleClick(button, mouseX, mouseY)
                        );
                    }

                    switch (button) {
                        case LEFT:
                            lastLeftMouseButtonPressMillis = now;
                            isLeftMouseButtonPressed = true;
                            break;
                        case MIDDLE:
                            lastMiddleMouseButtonPressMillis = now;
                            break;
                        case RIGHT:
                            lastRightMouseButtonPressMillis = now;
                            isRightMouseButtonPressed = true;
                            break;
                    }

                    executorService.schedule(() -> {
                        final boolean isPress
                                = (button == MouseEvent.Button.LEFT && isLeftMouseButtonPressed)
                                || (button == MouseEvent.Button.RIGHT && isRightMouseButtonPressed);

                        if (isPress) {
                            engine.queueEvent(
                                    MouseEvent.press(button, mouseX, mouseY)
                            );
                        }
                    }, clickTimeDurationMillis, TimeUnit.MILLISECONDS);

                    //TODO: send press when timeout
                    break;
            }

            //TODO: generate click, drag...
        });
        glfwSetCursorPosCallback(windowHandle, (windowHnd, xpos, ypos) -> {
            mouseX = (int) xpos;
            mouseY = (int) ypos;

            if (isLeftMouseButtonPressed || isRightMouseButtonPressed) {
                engine.queueEvent(
                        MouseEvent.drag(mouseX, mouseY)
                );
            } else {
                engine.queueEvent(
                        MouseEvent.move(mouseX, mouseY)
                );
            }
        });
        glfwSetCursorEnterCallback(windowHandle, (windowHnd, entered) -> printEvent("cursor %s", windowHnd, entered ? "entered" : "left"));
        glfwSetScrollCallback(windowHandle, (windowHnd, xoffset, yoffset) -> {
            engine.queueEvent(
                    MouseEvent.scroll(mouseX, mouseY, xoffset, yoffset)
            );
        });
    }

    private static MouseEvent.Button toButtonEnum(int button) {
        switch (button) {
            case GLFW_MOUSE_BUTTON_RIGHT:
                return MouseEvent.Button.RIGHT;
            case GLFW_MOUSE_BUTTON_MIDDLE:
                return MouseEvent.Button.MIDDLE;
            default:
                return MouseEvent.Button.LEFT;
        }
    }

    private static String getModState(int mods) {
        if (mods == 0) {
            return "";
        }

        StringBuilder modState = new StringBuilder(16);
        if ((mods & GLFW_MOD_SHIFT) != 0) {
            modState.append("SHIFT+");
        }
        if ((mods & GLFW_MOD_CONTROL) != 0) {
            modState.append("CONTROL+");
        }
        if ((mods & GLFW_MOD_ALT) != 0) {
            modState.append("ALT+");
        }
        if ((mods & GLFW_MOD_SUPER) != 0) {
            modState.append("SUPER+");
        }

        return modState.toString();
    }

    private static void printEvent(String format, long window, Object... args) {
        printEvent("Window", format, window, args);
    }

    private static void printEvent(String type, String format, long object, Object... args) {
        Object[] formatArgs = new Object[3 + args.length];

        formatArgs[0] = glfwGetTime();
        formatArgs[1] = type;
        formatArgs[2] = object;
        System.arraycopy(args, 0, formatArgs, 3, args.length);

        System.out.format("%.3f: %s [0x%X] " + format + "%n", formatArgs);
    }
}
