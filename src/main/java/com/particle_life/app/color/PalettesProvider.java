package com.particle_life.app.color;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PalettesProvider implements InfoWrapperProvider<Palette> {

    @Override
    public List<InfoWrapper<Palette>> create() {
        List<InfoWrapper<Palette>> palettes = new ArrayList<>();

        palettes.add(new InfoWrapper<>("Digital Rainbow", new FallbackPalette()));

        try {
            palettes.addAll(loadPalettesFromFiles());
        } catch (IOException | URISyntaxException e) {
            e.printStackTrace();
        }

        return palettes;
    }

    private List<InfoWrapper<Palette>> loadPalettesFromFiles() throws IOException, URISyntaxException {
        List<InfoWrapper<Palette>> palettes = new ArrayList<>();

        List<Path> paletteFiles = getResourceFolderFiles("/palettes");

        for (Path path : paletteFiles) {

            String fileContent;
            try {
                fileContent = Files.readString(path);
            } catch (IOException e) {
                e.printStackTrace();
                continue;
            }


            Optional<Palette> palette = parsePalette(fileContent);

            if (palette.isPresent()) {
                palettes.add(new InfoWrapper<>(path.getFileName().toString(), palette.get()));
            }
        }

        return palettes;
    }

    private static List<Path> getResourceFolderFiles(String folder) throws IOException, URISyntaxException {
        URI uri = PalettesProvider.class.getResource(folder).toURI();
        Path path = uri.getScheme().equals("jar") ?
                FileSystems.newFileSystem(uri, Collections.emptyMap()).getPath(folder)
                : Paths.get(uri);
        return getFilesInDirectory(path);


    }

    private static List<Path> getFilesInDirectory(Path path) throws IOException {
        return Files.walk(path, 1)
                .skip(1)  // first entry is just the directory
                .collect(Collectors.toList());
    }

    private Optional<Palette> parsePalette(String fileContent) {

        String[] colorStrings = fileContent.split("\\r?\\n");

        List<Color> list = new ArrayList<>();
        for (String colorString : colorStrings) {
            Optional<Color> color = parseColor(colorString);
            color.ifPresent(list::add);
        }
        Color[] colors = new Color[list.size()];
        int i = 0;
        for (Color color : list) {
            colors[i] = color;
            i++;
        }

        if (colors.length > 0) {
            return Optional.of(new InterpolatingPalette(colors));
        } else {
            return Optional.empty();
        }
    }

    private Optional<Color> parseColor(String s) {

        String[] elements = s.split(" ");

        if (elements.length != 3) {
            return Optional.empty();
        }

        int[] colorValues;
        try {
            colorValues = Arrays.stream(elements).mapToInt(Integer::parseInt).toArray();
        } catch (NumberFormatException e) {
            return Optional.empty();
        }

        return Optional.of(new Color(
                colorValues[0] / 255f,
                colorValues[1] / 255f,
                colorValues[2] / 255f,
                1f
        ));
    }
}
