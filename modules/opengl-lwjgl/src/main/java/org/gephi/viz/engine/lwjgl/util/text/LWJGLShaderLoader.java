/*
 * Copyright 2012 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package org.gephi.viz.engine.lwjgl.util.text;

import static org.lwjgl.opengl.GL20.*;

import java.io.PrintStream;

/**
 * Utility to load shaders from files, URLs, and strings.
 *
 * <p>
 * {@code ShaderLoader} is a simple utility for loading shaders.  It takes shaders directly as
 * strings.  It will create and compile the shaders, and link them together into a program.  Both
 * compiling and linking are verified.  If a problem occurs a {@link RuntimeException} is thrown with
 * the appropriate log attached.
 *
 * <p>
 * Note it is highly recommended that if the developer passes the strings directly to {@code
 * ShaderLoader} that they contain newlines.  That way if any errors do occur their line numbers
 * will be reported correctly.  This means that if the shader is to be embedded in Java code, a
 * "\n" should be appended to every line.
 */
public final class LWJGLShaderLoader {

    /**
     * Prevents instantiation.
     */
    private LWJGLShaderLoader() {
        // empty
    }

    /**
     * Checks that a shader was compiled correctly.
     *
     * @param shader OpenGL handle to a shader
     * @return True if shader was compiled without errors
     */
    private static boolean isShaderCompiled(final int shader) {
        return ShaderUtil.isShaderStatusValid(shader, GL_COMPILE_STATUS, null);
    }

    /**
     * Checks that a shader program was linked successfully.
     *
     * @param program OpenGL handle to a shader program
     * @return True if program was linked successfully
     */
    private static boolean isProgramLinked(final int program) {
        return ShaderUtil.isProgramStatusValid(program, GL_LINK_STATUS);
    }

    /**
     * Checks that a shader program was validated successfully.
     *
     * @param program OpenGL handle to a shader program
     * @return True if program was validated successfully
     */
    private static boolean isProgramValidated(final int program) {
        return ShaderUtil.isProgramStatusValid(program, GL_VALIDATE_STATUS);
    }

    /**
     * Loads a shader program from a pair of strings.
     *
     * @param vss Vertex shader source
     * @param fss Fragment shader source
     * @return OpenGL handle to the shader program, not negative
     * @throws NullPointerException if context or either source is null
     * @throws IllegalArgumentException if either source is empty
     * @throws RuntimeException if program did not compile, link, or validate successfully
     */
    /*@Nonnegative*/
    public static int loadProgram(final String vss,
                                  final String fss) {

        Check.notNull(vss, "Vertex shader source cannot be null");
        Check.notNull(fss, "Fragment shader source cannot be null");
        Check.argument(!vss.isEmpty(), "Vertex shader source cannot be empty");
        Check.argument(!fss.isEmpty(), "Fragment shader source cannot be empty");

        // Create the shaders
        final int vs = loadShader(vss, GL_VERTEX_SHADER);
        final int fs = loadShader(fss, GL_FRAGMENT_SHADER);

        // Create a program and attach the shaders
        final int program = glCreateProgram();
        glAttachShader(program, vs);
        glAttachShader(program, fs);

        // Link and validate the program
        glLinkProgram(program);
        glValidateProgram(program);
        if ((!isProgramLinked(program)) || (!isProgramValidated(program))) {
            final String log = ShaderUtil.getProgramInfoLog(program);
            throw new RuntimeException(log);
        }

        // Clean up the shaders
        glDeleteShader(vs);
        glDeleteShader(fs);

        return program;
    }

    /**
     * Loads a shader from a string.
     *
     * @param source Source code of the shader as one long string, assumed not null or empty
     * @param type Type of shader, assumed valid
     * @return OpenGL handle to the shader, not negative
     * @throws RuntimeException if a GLSL-capable context is not active or could not compile shader
     */
    /*@Nonnegative*/
    private static int loadShader(final String source,
                                  final int type) {

        // Create and read source
        final int shader = glCreateShader(type);
        glShaderSource(
                shader,                    // shader handle
                source);

        // Compile
        glCompileShader(shader);
        if (!isShaderCompiled(shader)) {
            final String log = ShaderUtil.getShaderInfoLog(shader);
            throw new RuntimeException(log);
        }

        return shader;
    }

    private static class ShaderUtil {

        public static String getShaderInfoLog(final int shaderObj) {
            final int[] infoLogLength=new int[1];
            glGetShaderiv(shaderObj, GL_INFO_LOG_LENGTH, infoLogLength);

            if(infoLogLength[0]==0) {
                return "(no info log)";
            }
            return glGetShaderInfoLog(shaderObj);
        }

        public static String getProgramInfoLog(final int programObj) {
            final int[] infoLogLength=new int[1];
            glGetProgramiv(programObj, GL_INFO_LOG_LENGTH, infoLogLength);

            if(infoLogLength[0]==0) {
                return "(no info log)";
            }
            return glGetProgramInfoLog(programObj);
        }

        public static boolean isShaderStatusValid(final int shaderObj, final int name, final PrintStream verboseOut) {
            final int[] ires = new int[1];
            glGetShaderiv(shaderObj, name, ires);

            final boolean res = ires[0]==1;
            if(!res && null!=verboseOut) {
                verboseOut.println("Shader status invalid: "+ getShaderInfoLog(shaderObj));
            }
            return res;
        }

        public static boolean isProgramStatusValid(final int programObj, final int name) {
            final int[] ires = new int[1];
            glGetProgramiv(programObj, name, ires);

            return ires[0]==1;
        }
    }
}
