package com.particle_life.app.cursors;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import org.lwjgl.system.MemoryStack;
import static org.lwjgl.stb.STBImage.stbi_load;

public class ImageClass {
    public ByteBuffer getImage() {
        return image;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return heigh;
    }

    private ByteBuffer image;
    private int width, heigh;

    ImageClass(int width, int heigh, ByteBuffer image) {
        this.image = image;
        this.heigh = heigh;
        this.width = width;
    }

    public static ImageClass loadImage(String path) {
        ByteBuffer image;
        int width, height;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer comp = stack.mallocInt(20);
            IntBuffer w = stack.mallocInt(20);
            IntBuffer h = stack.mallocInt(20);

            image = stbi_load(path, w, h, comp, 4);
            if (image == null) {
                throw new RuntimeException("Could not load image resources.");
            }
            width = w.get();
            height = h.get();
        }
        return new ImageClass(width, height, image);
    }
}
