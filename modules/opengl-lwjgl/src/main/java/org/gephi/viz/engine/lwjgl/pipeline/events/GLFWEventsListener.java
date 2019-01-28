package org.gephi.viz.engine.lwjgl.pipeline.events;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.gephi.viz.engine.VizEngine;
import org.gephi.viz.engine.lwjgl.LWJGLRenderingTarget;
import org.gephi.viz.engine.util.TimeUtils;
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
    private static final int DEFAULT_DOUBLE_CLICK_TIME_PERIOD_MILLIS = 300;

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

    private int lastLeftMouseButtonClickCount = 0;
    private int lastRightMouseButtonClickCount = 0;

    private AtomicBoolean isLeftMouseButtonPressed = new AtomicBoolean();
    private AtomicBoolean isLeftMouseButtonPressEventSent = new AtomicBoolean();
    private AtomicBoolean isRightMouseButtonPressed = new AtomicBoolean();
    private AtomicBoolean isRightMouseButtonPressEventSent = new AtomicBoolean();

    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor((Runnable r) -> {
        final Thread t = Executors.defaultThreadFactory().newThread(r);
        t.setDaemon(true);
        return t;
    });

    public void destroy() {
        executorService.shutdown();
    }

    public void register() {
        glfwSetKeyCallback(windowHandle, (windowHnd, key, scancode, action, mods) -> {
            String state;
            switch (action) {
                case GLFW_RELEASE:
                    state = "released";
                    engine.queueEvent(new KeyEvent(key, KeyEvent.Action.RELEASE));//TODO: map to universal key codes
                    break;
                case GLFW_PRESS:
                    engine.queueEvent(new KeyEvent(key, KeyEvent.Action.PRESS));//TODO: map to universal key codes
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
            final long now = TimeUtils.getTimeMillis();
            switch (action) {
                case GLFW_RELEASE:
                    final boolean isClick
                            = (button == MouseEvent.Button.LEFT && isLeftMouseButtonPressed.get() && !isLeftMouseButtonPressEventSent.get() && (now - lastLeftMouseButtonPressMillis) < clickTimeDurationMillis)
                            || (button == MouseEvent.Button.MIDDLE && (now - lastMiddleMouseButtonPressMillis) < clickTimeDurationMillis)
                            || (button == MouseEvent.Button.RIGHT && isRightMouseButtonPressed.get() && !isRightMouseButtonPressEventSent.get() && (now - lastRightMouseButtonPressMillis) < clickTimeDurationMillis);

                    if (button == MouseEvent.Button.LEFT) {
                        isLeftMouseButtonPressed.set(false);
                    }
                    if (button == MouseEvent.Button.RIGHT) {
                        isRightMouseButtonPressed.set(false);
                    }

                    if (isClick) {
                        //Detect double clicks:
                        if (button == MouseEvent.Button.LEFT || button == MouseEvent.Button.RIGHT) {
                            final int clickCount;
                            if (button == MouseEvent.Button.LEFT) {
                                clickCount = ++lastLeftMouseButtonClickCount;
                            } else {
                                clickCount = ++lastRightMouseButtonClickCount;
                            }

                            if (clickCount == 1) {
                                executorService.schedule(() -> {
                                    final int newClickCount;
                                    if (button == MouseEvent.Button.LEFT) {
                                        newClickCount = lastLeftMouseButtonClickCount;
                                        lastLeftMouseButtonClickCount = 0;
                                    } else {
                                        newClickCount = lastRightMouseButtonClickCount;
                                        lastRightMouseButtonClickCount = 0;
                                    }

                                    if (newClickCount == 1) {
                                        engine.queueEvent(
                                                MouseEvent.click(button, mouseX, mouseY)
                                        );
                                    } else if (newClickCount > 1) {
                                        engine.queueEvent(
                                                MouseEvent.doubleClick(button, mouseX, mouseY)
                                        );
                                    }

                                }, doubleClickTimePeriodMillis, TimeUnit.MILLISECONDS);
                            }
                        } else {
                            engine.queueEvent(
                                    MouseEvent.click(button, mouseX, mouseY)
                            );
                        }
                    } else {
                        engine.queueEvent(
                                MouseEvent.release(button, mouseX, mouseY)
                        );
                    }

                    break;
                case GLFW_PRESS:
                    switch (button) {
                        case LEFT:
                            lastLeftMouseButtonPressMillis = now;
                            isLeftMouseButtonPressed.set(true);
                            isLeftMouseButtonPressEventSent.set(false);
                            break;
                        case MIDDLE:
                            lastMiddleMouseButtonPressMillis = now;
                            break;
                        case RIGHT:
                            lastRightMouseButtonPressMillis = now;
                            isRightMouseButtonPressed.set(true);
                            isRightMouseButtonPressEventSent.set(false);
                            break;
                    }

                    executorService.schedule(() -> {
                        final boolean sendPressEvent;
                        switch (button) {
                            case LEFT:
                                sendPressEvent = isLeftMouseButtonPressed.get() && isLeftMouseButtonPressEventSent.compareAndSet(false, true);
                                break;
                            case RIGHT:
                                sendPressEvent = isRightMouseButtonPressed.get() && isRightMouseButtonPressEventSent.compareAndSet(false, true);
                                break;
                            default:
                                sendPressEvent = false;
                                break;
                        }

                        if (sendPressEvent) {
                            engine.queueEvent(
                                    MouseEvent.press(button, mouseX, mouseY)
                            );
                        }
                    }, clickTimeDurationMillis, TimeUnit.MILLISECONDS);

                    break;
            }
        });
        glfwSetCursorPosCallback(windowHandle, (windowHnd, xpos, ypos) -> {
            final boolean isDragging = isLeftMouseButtonPressed.get() || isRightMouseButtonPressed.get();

            if (isDragging) {
                if (isLeftMouseButtonPressed.get() && isLeftMouseButtonPressEventSent.compareAndSet(false, true)) {
                    engine.queueEvent(
                            MouseEvent.press(MouseEvent.Button.LEFT, mouseX, mouseY)
                    );
                }
                if (isRightMouseButtonPressed.get() && isRightMouseButtonPressEventSent.compareAndSet(false, true)) {
                    engine.queueEvent(
                            MouseEvent.press(MouseEvent.Button.RIGHT, mouseX, mouseY)
                    );
                }
            }

            mouseX = (int) xpos;
            mouseY = (int) ypos;

            if (isDragging) {
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
