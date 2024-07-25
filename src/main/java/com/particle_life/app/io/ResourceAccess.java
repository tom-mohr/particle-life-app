package com.particle_life.app.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceAccess {

    /**
     * @param path must not start with "/" or "./", e.g. "textures/image.png", "settings.properties", ...
     */
    public static InputStream getInputStream(String path) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(path);
    }

    public static boolean fileExists(String path) {
        return new File(path).exists();
    }

    public static void createFile(String path) throws IOException {
        File file = new File(path);

        // ensure containing directories exist
        File dir = file.getParentFile();
        if (dir != null && !dir.exists()) {
            if (!dir.mkdirs()) {
                throw new IOException("Failed to create directories: " + dir.getAbsolutePath());
            }
        }

        // create file
        if (!file.createNewFile()) {
            throw new IOException("File already exists: " + file.getAbsolutePath());
        }
    }

    public static String readTextFile(String path) throws IOException {
        return new String(Files.readAllBytes(Paths.get(path)));
    }

    /**
     * Lists all files in the given directory.
     *
     * @param directory Path relative to the app's working directory.
     *                  Must not start with "/" or "./".
     *                  Examples for allowed paths: "textures", "assets/music", ...
     */
    public static List<Path> listFiles(String directory) throws IOException {
        Path path = new File(directory).toPath();
        return Files.walk(path, 1)
                .skip(1)  // first entry is just the directory
                .collect(Collectors.toList());
    }
}
