package com.particle_life.app.cursors;

import org.joml.Vector3d;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public class CircleCursorShape extends CursorShape {

    private static final int NUM_SEGMENTS = 96;
    private int vertexArray;

    @Override
    protected void onInitialize() {
        final float[] vertexData = new float[NUM_SEGMENTS * 2];
        for (int i = 0; i < NUM_SEGMENTS; i++) {
            double angle = 2 * Math.PI * i / (float) NUM_SEGMENTS;
            double x = Math.cos(angle);
            double y = Math.sin(angle);
            vertexData[2 * i] = (float) x;
            vertexData[2 * i + 1] = (float) y;
        }

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
        return connection.length() <= 1.0;
    }

    @Override
    void draw() {
        glLineWidth(2.0f);
        glBindVertexArray(vertexArray);
        glDrawArrays(GL_LINE_LOOP, 0, NUM_SEGMENTS);
    }

    @Override
    Vector3d sampleRandomPoint() {
        double angle = Math.random() * 2 * Math.PI;
        return new Vector3d(Math.cos(angle), Math.sin(angle), 0)
                .mul(Math.sqrt(Math.random()));
    }
}