package com.particle_life.app.cursors;

import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class SquareCursorShape extends CursorShape {

    private int vertexArray;

    @Override
    public void onInitialize() {
        final float[] vertexData = new float[]{
                -1, -1,
                1, -1,
                1, 1,
                -1, 1,
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
        return diff.x <= 1.0 && diff.y <= 1.0;
    }

    @Override
    void draw() {
        glLineWidth(2.0f);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_LINE_LOOP, 0, 4);
    }

    @Override
    Vector3d sampleRandomPoint() {
        return new Vector3d(2 * Math.random() - 1, 2 * Math.random() - 1, 0);
    }
}
