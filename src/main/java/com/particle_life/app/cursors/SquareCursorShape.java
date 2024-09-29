package com.particle_life.app.cursors;

import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11C.*;
import static org.lwjgl.opengl.GL15C.*;
import static org.lwjgl.opengl.GL20C.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20C.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30C.glBindVertexArray;
import static org.lwjgl.opengl.GL30C.glGenVertexArrays;

public class SquareCursorShape extends CursorShape {

    private int vertexArray;

    @Override
    public void onInitialize() {
        final float[] vertexData = new float[]{
                -.5f, -.5f,
                .5f, -.5f,
                .5f, .5f,
                -.5f, .5f,
        };

        int vertexBuffer = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, vertexData, GL_STATIC_DRAW);

        vertexArray = glGenVertexArrays();
        glBindVertexArray(vertexArray);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
    }

    @Override
    boolean isInside(Vector3d connection) {
        Vector3d diff = connection.absolute();
        return diff.x <= .5 && diff.y <= .5;
    }

    @Override
    void draw() {
        glLineWidth(2.0f);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_LINE_LOOP, 0, 4);
    }

    @Override
    Vector3d sampleRandomPoint() {
        return new Vector3d(Math.random() - .5, Math.random() - .5, 0);
    }
}
