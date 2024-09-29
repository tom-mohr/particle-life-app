package com.particle_life.app;

import com.particle_life.app.io.ResourceAccess;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiMouseCursor;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.lwjgl.opengl.GL11C.*;

class ImGuiCardView {

    public static class Card {
        String name;
        File file;
        int img;

        public Card(File file) {
            this(
                    ResourceAccess.getFileNameWithoutExtension(file),
                    file,
                    loadTexture(file)
            );
        }

        public Card(String name, File file, int img) {
            this.name = name;
            this.file = file;
            this.img = img;
        }

        private static int loadTexture(File file) {
            try (ZipFile zip = new ZipFile(file)) {
                ZipEntry imgEntry = zip.getEntry("img.png");
                if (imgEntry == null) return -1;
                InputStream inputStream = zip.getInputStream(imgEntry);
                byte[] bytes = inputStream.readAllBytes();
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
                byteBuffer.put(bytes);
                byteBuffer.flip();
                return loadTexture(byteBuffer);
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        private static int loadTexture(ByteBuffer byteBuffer) {
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                ByteBuffer pixels = STBImage.stbi_load_from_memory(byteBuffer,
                        width, height, channels, 4);
                if (pixels == null) {
                    throw new RuntimeException("Failed to load image: " + STBImage.stbi_failure_reason());
                }

                // load the texture using OpenGL
                // attention: This must happen from the OpenGL thread,
                // so make sure to call this method from the main thread only.
                int texId = glGenTextures();
                glBindTexture(GL_TEXTURE_2D, texId);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
                glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
                glPixelStorei(GL_UNPACK_ROW_LENGTH, 0);
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA,
                        width.get(0), height.get(0),
                        0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
                return texId;
            }
        }
    }

    public interface CardCallback {
        void selected(Card item);
    }

    public static void draw(float width, float cardSize, float sep, Card[] cards,
                            CardCallback selectCallback, CardCallback deleteCallback) {
        // relative cursor
        ImVec2 relRoot = new ImVec2();
        ImGui.getCursorPos(relRoot);

        // absolute cursor
        ImVec2 absRoot = new ImVec2();
        ImGui.getCursorScreenPos(absRoot);

        // delta abs -> rel
        ImVec2 delta = new ImVec2();
        delta.x = relRoot.x - absRoot.x;
        delta.y = relRoot.y - absRoot.y;

        float padding = sep;

        ImVec2 textSize = new ImVec2();  // buffer
        int row = 0;
        int col = 0;
        int rowLength = (int) ((width + sep) / (cardSize + sep));  // how many cards fit into one row
        float rowContentWidth = rowLength * cardSize + (rowLength - 1) * sep;
        float leftPadding = (width - rowContentWidth) / 2;
        for (Card card : cards) {
            // the absolute coordinates of the card rectangle
            // top left corner
            ImVec2 absPos = new ImVec2(
                    absRoot.x + leftPadding + col * (cardSize + sep),
                    absRoot.y + row * (cardSize + sep)
            );
            // bottom right corner
            ImVec2 absPos2 = new ImVec2(
                    absPos.x + cardSize,
                    absPos.y + cardSize
            );

            // relative coordinates (relative to window position) of the card rectangle
            // top left corner
            ImVec2 relPos = new ImVec2(
                    absPos.x + delta.x,
                    absPos.y + delta.y
            );

            boolean hovered = ImGui.isMouseHoveringRect(absPos.x, absPos.y, absPos2.x, absPos2.y);

            // image
            if (card.img != -1) {
                ImGui.setCursorScreenPos(absPos.x, absPos.y);
                ImGui.getWindowDrawList().addImageRounded(
                        card.img,
                        absPos.x, absPos.y,
                        absPos.x + cardSize, absPos.y + cardSize,
                        0, 0, 1, 1,
                        0xFFFFFFFF,
                        8
                );
//                ImGui.image(card.img, cardSize, cardSize);
            }

            // title text
            if (true) {
                ImGui.pushStyleColor(ImGuiCol.Text, ImGui.getColorU32(ImGuiCol.TextDisabled));

                String title = ResourceAccess.getFileNameWithoutExtension(card.file);
                ImGui.calcTextSize(textSize, title, cardSize - 2 * padding);

                ImVec2 textAbsPos = new ImVec2(
                        absPos.x + (cardSize - textSize.x) / 2,
                        absPos.y + padding
                );
                float textPaddingX = 4;
                float textPaddingY = 1;
                ImGui.getWindowDrawList().addRectFilled(
                        textAbsPos.x - textPaddingX,
                        textAbsPos.y - textPaddingY,
                        textAbsPos.x + textSize.x + textPaddingX,
                        textAbsPos.y + textSize.y + textPaddingY,
                        ImGui.getColorU32(0, 0, 0, 256),
                        2
                );

                // text position needs absolute coordinates
                ImGui.setCursorScreenPos(textAbsPos.x, textAbsPos.y);
                // wrap position needs relative coordinates
                ImGui.pushTextWrapPos(relPos.x + cardSize - padding);
                ImGui.textWrapped(title);
                ImGui.popTextWrapPos();

                ImGui.popStyleColor();
            }
            int rectColor = 0x88888888;
            if (hovered) {
                rectColor = 0xFFFFFFFF;
            }
            ImGui.getWindowDrawList().addRect(absPos.x, absPos.y, absPos2.x, absPos2.y,
                    rectColor, 8, 0, 1);

            if (hovered) {
                ImGui.setMouseCursor(ImGuiMouseCursor.Hand);
                if (ImGui.isMouseClicked(0)) {
                    selectCallback.selected(card);
                } else if (ImGui.isMouseClicked(2)) {
                    deleteCallback.selected(card);
                }
            }

            col++;
            if (col >= rowLength) {
                col = 0;
                row++;
            }

        }
        ImGui.setCursorPos(relRoot.x, relRoot.y);
        if (col != 0) row++;
        ImGui.dummy(width, row * cardSize + (row - 1) * sep);
    }

    public static Card[] loadCards(List<Path> saves) {
        return saves.stream()
                .map(Path::toFile)
                .map(Card::new)
                .toArray(Card[]::new);
    }
}
