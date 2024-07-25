package com.particle_life.app.shaders;

import com.particle_life.app.io.ResourceAccess;

import java.io.IOException;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

final class ShaderUtil {

    public static int makeShaderProgram(String vertexShaderResource,
                                        String geometryShaderResource,
                                        String fragmentShaderResource) throws IOException {
        int program = glCreateProgram();

        glAttachShader(program, makeShaderObject(vertexShaderResource, GL_VERTEX_SHADER, "vertex"));
        if (geometryShaderResource != null)  // it's okay to omit the geometry shader
            glAttachShader(program, makeShaderObject(geometryShaderResource, GL_GEOMETRY_SHADER, "geometry"));
        glAttachShader(program, makeShaderObject(fragmentShaderResource, GL_FRAGMENT_SHADER, "fragment"));
        glLinkProgram(program);
        return program;
    }

    private static int makeShaderObject(String resource, int type, String name) throws IOException {
        String src = ResourceAccess.readTextFile(resource);
        int shaderObject = glCreateShader(type);
        glShaderSource(shaderObject, src);
        glCompileShader(shaderObject);
        printShaderErrors(shaderObject, name);
        return shaderObject;
    }

    private static void printShaderErrors(int shader, String shaderName) {

        int[] params = new int[10];
        glGetShaderiv(shader, GL_COMPILE_STATUS, params);
        int isCompiled = params[0];
        if (isCompiled == GL_FALSE) {
            System.err.printf("Error while compiling %s shader. Info log:%n", shaderName);

            glGetShaderiv(shader, GL_INFO_LOG_LENGTH, params);
            int maxLength = params[0];

            String infoLog = glGetShaderInfoLog(shader, maxLength);

            System.err.println(infoLog);

        }
    }
}
