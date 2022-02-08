package com.particle_life.app.shaders;

import com.particle_life.app.color.Color;
import org.joml.Matrix4d;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.GL_GEOMETRY_SHADER;

/**
 * Represents data about a particle shader.
 */
public class ParticleShader {

    public static int MIN_DETAIL = 3;
    public static int MAX_DETAIL = 11;

    public final int shaderProgram;

    private final int timeUniformLocation;
    private final int paletteUniformLocation;
    private final int transformUniformLocation;
    private final int sizeUniformLocation;
    private final int detailUniformLocation;

    public final int xAttribLocation;
    public final int vAttribLocation;
    public final int typeAttribLocation;

    /**
     * Remember the last buffered size in order to use subBufferData instead of bufferData whenever possible.
     */
    private int lastBufferedSize = -1;

    private final float[] transform = new float[16];

    public ParticleShader(String vertexShaderResource, String geometryShaderResource, String fragmentShaderResource) {

        shaderProgram = loadShaders(vertexShaderResource, geometryShaderResource, fragmentShaderResource);

        // GET LOCATIONS
        timeUniformLocation = glGetUniformLocation(shaderProgram, "time");
        paletteUniformLocation = glGetUniformLocation(shaderProgram, "palette");
        transformUniformLocation = glGetUniformLocation(shaderProgram, "transform");
        sizeUniformLocation = glGetUniformLocation(shaderProgram, "size");
        detailUniformLocation = glGetUniformLocation(shaderProgram, "detail");

        xAttribLocation = glGetAttribLocation(shaderProgram, "x");
        vAttribLocation = glGetAttribLocation(shaderProgram, "v");
        typeAttribLocation = glGetAttribLocation(shaderProgram, "type");
    }

    private int loadShaders(String vertexShaderResource, String geometryShaderResource, String fragmentShaderResource) {

        String vertSrc = getResourceFileContent(vertexShaderResource);
        String geomSrc = getResourceFileContent(geometryShaderResource);
        String fragSrc = getResourceFileContent(fragmentShaderResource);

        int vertShaderObject = glCreateShader(GL_VERTEX_SHADER);
        int geomShaderObject = glCreateShader(GL_GEOMETRY_SHADER);
        int fragShaderObject = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(vertShaderObject, vertSrc);
        glShaderSource(geomShaderObject, geomSrc);
        glShaderSource(fragShaderObject, fragSrc);
        glCompileShader(vertShaderObject);
        glCompileShader(geomShaderObject);
        glCompileShader(fragShaderObject);

        //todo: check for compilation errors
        printShaderErrors(vertShaderObject, "vertex");
        printShaderErrors(geomShaderObject, "geometry");
        printShaderErrors(fragShaderObject, "fragment");

        int program = glCreateProgram();
        glAttachShader(program, vertShaderObject);
        glAttachShader(program, geomShaderObject);
        glAttachShader(program, fragShaderObject);

        glLinkProgram(program);
        return program;
    }

    private void printShaderErrors(int shader, String shaderName) {

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

    private String getResourceFileContent(String path) {
        return new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)))
                .lines()
                .collect(Collectors.joining("\n"));
    }

    /**
     * Need to call this before setting uniforms.
     */
    public void use() {
        glUseProgram(shaderProgram);
    }

    public void setTime(float value) {
        glUniform1f(timeUniformLocation, value);
    }

    public void setPalette(Color[] palette) {

        float[] colorArray = new float[palette.length * 4];
        for (int i = 0; i < palette.length; i++) {
            colorArray[4 * i] = palette[i].r;
            colorArray[4 * i + 1] = palette[i].g;
            colorArray[4 * i + 2] = palette[i].b;
            colorArray[4 * i + 3] = palette[i].a;
        }

        glUniform4fv(paletteUniformLocation, colorArray);
    }

    public void setSize(float size) {
        glUniform1f(sizeUniformLocation, size);
    }

    public void setDetail(int detail) {
        if (detail < MIN_DETAIL) {
            throw new IllegalArgumentException(String.format("Tried to set detail to %d, but allowed minimum is %d.", detail, MIN_DETAIL));
        }

        if (detail > MAX_DETAIL) {
            throw new IllegalArgumentException(String.format("Tried to set detail to %d, but allowed maximum is %d.", detail, MAX_DETAIL));
        }
        glUniform1i(detailUniformLocation, detail);
    }

    public void setTransform(Matrix4d transform) {
        glUniformMatrix4fv(transformUniformLocation, false, transform.get(this.transform));
    }

    private void clearGlErrors() {
        while (glGetError() != GL_NO_ERROR) ;
    }

    private void checkGlErrors() {
        int errorCode;
        while ((errorCode = glGetError()) != GL_NO_ERROR) {
            System.err.printf("OpenGL Error: %d%n", errorCode);
        }
    }
}
