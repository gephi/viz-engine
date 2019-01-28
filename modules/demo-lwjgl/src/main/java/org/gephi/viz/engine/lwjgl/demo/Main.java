package org.gephi.viz.engine.lwjgl.demo;

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

        if (mode.equalsIgnoreCase("AWT")) {
            MainAWT.main(args);
        } else {
            MainGLFW.main(args);
        }
    }
}
