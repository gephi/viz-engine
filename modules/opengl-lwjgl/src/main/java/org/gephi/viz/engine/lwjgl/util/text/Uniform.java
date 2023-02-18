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

/**
 * Uniform variable in a shader.
 */
abstract class Uniform {

    /**
     * Index of uniform in shader.
     */
    final int location;

    /**
     * True if local value should be pushed.
     */
    boolean dirty;

    /**
     * Constructs a {@link Uniform}.
     *
     * @param program OpenGL handle to shader program
     * @param name Name of the uniform in shader source code
     * @throws IllegalArgumentException if program is negative
     */
    Uniform(final int program,
            final String name) {

        Check.notNull(name, "Name cannot be null");
        Check.argument(program >= 0, "Program cannot be negative");

        location = glGetUniformLocation(program, name);
        if (location == -1) {
            throw new IllegalStateException("Could not find uniform in program");
        }
    }

    /**
     * Pushes the local value to the shader program.
     *
     */
    abstract void update();
}
