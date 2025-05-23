package com.particle_life.app.shaders;

import com.particle_life.app.color.Color;
import org.joml.Matrix4d;

import java.io.IOException;

import static org.lwjgl.opengl.GL20C.*;

/**
 * Represents data about a particle shader.
 */
public class ParticleShader {

    public final int shaderProgram;

    private final int timeUniformLocation;
    private final int paletteUniformLocation;
    private final int transformUniformLocation;
    private final int camTopLeftUniformLocation;
    private final int wrapUniformLocation;
    private final int sizeUniformLocation;

    public final int xAttribLocation;
    public final int vAttribLocation;
    public final int typeAttribLocation;

    private final float[] transform = new float[16];

    public final BlendMode blendMode;

    public ParticleShader(String vertexShaderResource,
                          String geometryShaderResource,
                          String fragmentShaderResource,
                          BlendMode blendMode) throws IOException {

        shaderProgram = ShaderUtil.makeShaderProgram(vertexShaderResource, geometryShaderResource, fragmentShaderResource);

        // GET LOCATIONS
        timeUniformLocation = glGetUniformLocation(shaderProgram, "time");
        paletteUniformLocation = glGetUniformLocation(shaderProgram, "palette");
        transformUniformLocation = glGetUniformLocation(shaderProgram, "transform");
        camTopLeftUniformLocation = glGetUniformLocation(shaderProgram, "camTopLeft");
        wrapUniformLocation = glGetUniformLocation(shaderProgram, "wrap");
        sizeUniformLocation = glGetUniformLocation(shaderProgram, "size");

        xAttribLocation = glGetAttribLocation(shaderProgram, "x");
        vAttribLocation = glGetAttribLocation(shaderProgram, "v");
        typeAttribLocation = glGetAttribLocation(shaderProgram, "type");

        this.blendMode = blendMode;
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

    public void setCamTopLeft(float camLeft, float camTop) {
        glUniform2f(camTopLeftUniformLocation, camLeft, camTop);
    }

    public void setWrap(boolean wrap) {
        glUniform1i(wrapUniformLocation, wrap ? 1 : 0);
    }
}
