package org.gephi.viz.engine.lwjgl.demo;

import java.util.Arrays;

/**
 *
 * @author Eduardo Ramos
 */
public class Main {

    public static void main(String[] args) throws Exception {
        final String mode;
        if (args.length > 0) {
            mode = args[0].trim();
        } else {
            mode = "AWT";
        }

        final String[] argsWithoutMode = args.length > 0 ? Arrays.copyOfRange(args, 1, args.length) : args;

        if (mode.equalsIgnoreCase("GLFW")) {
            MainGLFW.main(argsWithoutMode);
        } else {
            MainAWT.main(argsWithoutMode);
        }
    }
}
