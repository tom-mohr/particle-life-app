package com.particle_life.app.shaders;

import com.esotericsoftware.yamlbeans.YamlException;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.particle_life.app.io.ResourceAccess;
import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ShaderProvider implements InfoWrapperProvider<ParticleShader> {

    private static final String SHADERS_DIRECTORY = "shaders";
    private static final String SHADERS_CONFIG_FILE = "shaders.yaml";

    private static class ShaderConfigEntry {
        public String name = "";
        public String description = "";
        public String vertex = "default.vert";
        public String geometry = "default.geom";
        public String fragment = "default.geom";
        public BlendMode blend = BlendMode.normal;
    }

    @Override
    public List<InfoWrapper<ParticleShader>> create() throws Exception {

        List<String> files = listFilesInShadersDirectory();
        boolean containsConfigFile = files.remove(SHADERS_CONFIG_FILE);

        // Throw runtime error if no shaders config file is provided
        if (!containsConfigFile) throw new RuntimeException("Can't find file '%s'.".formatted(SHADERS_CONFIG_FILE));

        // parse shaders config file
        String configYamlFileContent = ResourceAccess.readTextFile("shaders/" + SHADERS_CONFIG_FILE);
        List<ShaderConfigEntry> configs = getConfigs(configYamlFileContent);

        // provide good default values if values are omitted
        int unnamedCounter = 0;
        for (ShaderConfigEntry config : configs) {

            // if no name is provided, use "unnamed", "unnamed 2", "unnamed 3", ...
            if (config.name == null || config.name.isEmpty()) {
                unnamedCounter++;
                config.name = (unnamedCounter == 1) ? "unnamed" : "unnamed %d".formatted(unnamedCounter);
            }

            // set description to "" because GUI might crash if "null" is passed as displayable text
            if (config.description == null) {
                config.description = "";
            }
        }

        return configs.stream()
                // only add the shaders that use shader files that actually exist
                // and print warnings for all other shaders
                .filter(config -> {
                    for (String filename : new String[]{config.vertex, config.geometry, config.fragment}) {
                        if (!files.contains(filename)) {
                            System.err.printf(
                                    "Shader file '%s' used by shader '%s' doesn't exist. %n",
                                    config.name, filename
                            );
                            return false;
                        }
                    }
                    return true;
                })
                .map(config -> {
                    try {
                        return new InfoWrapper<>(
                                config.name,
                                config.description,
                                new ParticleShader(
                                        "shaders/" + config.vertex,
                                        "shaders/" + config.geometry,
                                        "shaders/" + config.fragment,
                                        config.blend
                                )
                        );
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private List<ShaderConfigEntry> getConfigs(String yamlFileContent) {
        List<ShaderConfigEntry> entries = new ArrayList<>();
        YamlReader reader = new YamlReader(yamlFileContent);
        while (true) {
            ShaderConfigEntry entry;
            try {
                entry = reader.read(ShaderConfigEntry.class);
            } catch (YamlException e) {
                // parsing failed, skip this entry but print error
                e.printStackTrace();
                continue;
            }
            if (entry == null) break;
            entries.add(entry);
        }
        return entries;
    }

    private List<String> listFilesInShadersDirectory() throws IOException {
        return ResourceAccess.listFiles(SHADERS_DIRECTORY).stream()
                .map(p -> p.getFileName().toString())
                .collect(Collectors.toList());
    }
}
