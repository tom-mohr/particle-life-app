package com.particle_life.app.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceAccess {

    /**
     * @param path must not start with "/" or "./", e.g. "textures/image.png", "settings.properties", ...
     */
    public static InputStream getInputStream(String path) {
        return ClassLoader.getSystemClassLoader().getResourceAsStream(path);
    }

    public static String readTextFile(String path) {
        InputStream inputStream = getInputStream(path);
        String text = new BufferedReader(new InputStreamReader(inputStream))
                .lines()
                .collect(Collectors.joining("\n"));
        try {
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return text;
    }

    /**
     * @param directory must not start with "/" or "./", e.g. "textures", "assets/music", ...
     */
    public static List<Path> listFiles(String directory) throws IOException, URISyntaxException {
        URI uri = ClassLoader.getSystemClassLoader().getResource(directory).toURI();

        Path path;
        if (uri.getScheme().equals("jar")) {
            path = FileSystems.newFileSystem(uri, Collections.emptyMap()).getPath(directory);
        } else {
            path = Paths.get(uri);
        }

        return Files.walk(path, 1)
                .skip(1)  // first entry is just the directory
                .collect(Collectors.toList());
    }
}
