package com.particle_life.app;

import com.particle_life.app.io.MatrixIO;
import com.particle_life.app.io.ParticlesIO;
import com.particle_life.app.io.ResourceAccess;
import com.particle_life.backend.Particle;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

class SaveLoadService {

    private final ExtendedPhysics physics;
    private final Consumer<Exception> onError;
    private final AtomicBoolean requestedSaveCardsLoading;

    SaveLoadService(ExtendedPhysics physics, Consumer<Exception> onError, AtomicBoolean requestedSaveCardsLoading) {
        this.physics = physics;
        this.onError = onError;
        this.requestedSaveCardsLoading = requestedSaveCardsLoading;
    }

    ImGuiCardView.Card[] loadSaveCards() {
        try {
            List<Path> saves = ResourceAccess.listFiles("saves");
            return ImGuiCardView.loadCards(saves);
        } catch (IOException e) {
            onError.accept(e);
            return new ImGuiCardView.Card[0];
        }
    }

    void saveState(File file, int[] saveImage) {
        try (FileOutputStream fileOutputStream = new FileOutputStream(file)) {
            try (ZipOutputStream zip = new ZipOutputStream(fileOutputStream)) {
                zip.putNextEntry(new ZipEntry("particles.tsv"));
                ParticlesIO.saveParticles(physics.particles, zip);
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("physics.toml"));
                PhysicsSettingsToml.fromPhysicsSettings(physics.settings).save(zip);
                zip.closeEntry();

                zip.putNextEntry(new ZipEntry("matrix.tsv"));
                MatrixIO.saveMatrix(physics.settings.matrix, zip);
                zip.closeEntry();

                if (saveImage != null) {
                    zip.putNextEntry(new ZipEntry("img.png"));
                    BufferedImage bufferedImage = new BufferedImage(
                            SaveThumbnailRenderer.SAVE_IMAGE_SIZE, SaveThumbnailRenderer.SAVE_IMAGE_SIZE,
                            BufferedImage.TYPE_INT_ARGB
                    );
                    bufferedImage.setRGB(
                            0, 0, SaveThumbnailRenderer.SAVE_IMAGE_SIZE, SaveThumbnailRenderer.SAVE_IMAGE_SIZE,
                            saveImage, 0, SaveThumbnailRenderer.SAVE_IMAGE_SIZE
                    );
                    ImageIO.write(bufferedImage, "png", zip);
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            onError.accept(e);
        }
        requestedSaveCardsLoading.set(true);
    }

    void loadState(File file) {
        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(file))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                switch (entry.getName()) {
                    case "particles.tsv" -> physics.particles = ParticlesIO.loadParticles(zip);
                    case "physics.toml" -> {
                        PhysicsSettingsToml toml = new PhysicsSettingsToml();
                        toml.load(zip);
                        toml.toPhysicsSettings(physics.settings);
                    }
                    case "matrix.tsv" -> {
                        physics.settings.matrix = MatrixIO.loadMatrix(zip);
                        physics.ensureTypes();
                    }
                    case "img.png" -> {
                        // ignore preview image
                    }
                    default -> System.err.println("Unknown file in ZIP: " + entry.getName());
                }
                zip.closeEntry();
            }
            physics.ensureTypes();
            validateLoadedState();
        } catch (IOException | IllegalStateException e) {
            onError.accept(e);
        }
    }

    private void validateLoadedState() {
        int matrixSize = physics.settings.matrix.size();
        for (Particle particle : physics.particles) {
            if (particle.type < 0 || particle.type >= matrixSize) {
                throw new IllegalStateException(
                        "Particle type " + particle.type + " is out of range for matrix size " + matrixSize);
            }
        }
    }
}
