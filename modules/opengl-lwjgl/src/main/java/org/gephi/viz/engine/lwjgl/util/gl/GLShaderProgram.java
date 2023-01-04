package org.gephi.viz.engine.lwjgl.util.gl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import org.lwjgl.opengl.GL20;

/**
 *
 * @author Eduardo Ramos
 */
public class GLShaderProgram {

    private final String srcRoot;
    private final String vertBasename;
    private final String fragBasename;
    private int vertexShaderId = -1;
    private int fragmentShaderId = -1;
    private int id = -1;

    private final Map<String, Integer> uniformLocations;
    private final Map<String, Integer> attribLocations;
    private boolean initDone = false;

    public GLShaderProgram(String srcRoot, String vertBasename) {
        this(srcRoot, vertBasename, null);
    }

    public GLShaderProgram(String srcRoot, String vertBasename, String fragBasename) {
        this.srcRoot = srcRoot;
        this.vertBasename = vertBasename;
        this.fragBasename = fragBasename;
        this.uniformLocations = new HashMap<>();
        this.attribLocations = new HashMap<>();
    }

    public GLShaderProgram addUniformName(String name) {
        uniformLocations.put(name, null);
        return this;
    }

    public GLShaderProgram addAttribName(String name) {
        attribLocations.put(name, null);
        return this;
    }

    public GLShaderProgram addAttribLocation(String name, int location) {
        attribLocations.put(name, location);
        return this;
    }

    public GLShaderProgram init() {
        try {
            if (initDone) {
                throw new IllegalStateException("Already initialized");
            }

            vertexShaderId = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
            GL20.glShaderSource(vertexShaderId, loadSource(srcRoot, vertBasename, "vert"));
            GL20.glCompileShader(vertexShaderId);

            final int vertexShaderStatus = GL20.glGetShaderi(vertexShaderId, GL20.GL_COMPILE_STATUS);
            if (vertexShaderStatus != GL20.GL_TRUE) {
                throw new RuntimeException(GL20.glGetShaderInfoLog(id));
            }

            fragmentShaderId = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
            GL20.glShaderSource(fragmentShaderId, loadSource(srcRoot, fragBasename, "frag"));
            GL20.glCompileShader(fragmentShaderId);
            final int fragmentShaderStatus = GL20.glGetShaderi(fragmentShaderId, GL20.GL_COMPILE_STATUS);
            if (fragmentShaderStatus != GL20.GL_TRUE) {
                throw new RuntimeException(GL20.glGetShaderInfoLog(id));
            }

            id = GL20.glCreateProgram();
            GL20.glAttachShader(id, vertexShaderId);
            GL20.glAttachShader(id, fragmentShaderId);

            //Set explicit locations:
            for (String name : attribLocations.keySet().toArray(new String[0])) {
                if (attribLocations.get(name) != null) {
                    GL20.glBindAttribLocation(id, attribLocations.get(name), name);
                }
            }

            GL20.glLinkProgram(id);
            final int programStatus = GL20.glGetProgrami(id, GL20.GL_LINK_STATUS);
            if (programStatus != GL20.GL_TRUE) {
                throw new RuntimeException(GL20.glGetProgramInfoLog(id));
            }

            // Get variables locations
            for (String name : uniformLocations.keySet().toArray(new String[0])) {
                uniformLocations.put(name, GL20.glGetUniformLocation(id, name));
            }

            for (String name : attribLocations.keySet().toArray(new String[0])) {
                if (attribLocations.get(name) == null) {
                    attribLocations.put(name, GL20.glGetAttribLocation(id, name));
                }
            }

            initDone = true;

            return this;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isInitialized() {
        return initDone;
    }

    public int id() {
        return id;
    }

    public int getUniformLocation(String name) {
        if (!isInitialized()) {
            throw new IllegalStateException("Initialize the program first!");
        }

        Integer loc = uniformLocations.get(name);
        if (loc == null) {
            throw new IllegalArgumentException("Name of uniform " + name + " was not added before init");
        }

        return loc;
    }

    public int getAttribLocation(String name) {
        if (!isInitialized()) {
            throw new IllegalStateException("Initialize the program first!");
        }

        Integer loc = attribLocations.get(name);
        if (loc == null) {
            throw new IllegalArgumentException("Name of attribute " + name + " was not added before init");
        }

        return loc;
    }

    public void use() {
        if (!isInitialized()) {
            throw new IllegalStateException("Initialize the program first!");
        }

        GL20.glUseProgram(id);
    }

    public void stopUsing() {
        GL20.glUseProgram(0);
    }

    private static String loadSource(String srcRoot, String baseName, String extension) throws IOException {
        final StringBuilder builder = new StringBuilder();
        final String path = srcRoot + "/" + baseName + "." + extension;

        try (InputStream in = GLShaderProgram.class.getResourceAsStream(path);
                BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append("\n");
            }
        } catch (IOException ex) {
            System.err.println("Error loading path: " + path);
            throw ex;
        }

        return builder.toString();
    }
}
