package com.particle_life.app.toml_util;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import com.particle_life.app.io.ResourceAccess;
import org.apache.commons.text.similarity.LevenshteinDistance;

import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * This class can be extended to create a TOML file that stores settings.
 * The fields of the subclass are annotated with {@link TomlKey} to specify the key in the TOML file.
 * The subclass can then load and save the settings to a file.
 * <p>
 * Example:
 * <pre>{@code
 *     public class AppSettings extends TomlFile {
 *         @TomlKey("fullscreen")
 *         public boolean fullScreen = true;
 *         @TomlKey("zoom")
 *         public double zoom = 1.2;
 *     }
 *     }</pre>
 * </p>
 * It currently supports the following field types:
 * <ul>
 *     <li>boolean</li>
 *     <li>double</li>
 *     <li>float</li>
 *     <li>int</li>
 *     <int>String</int>
 * <ul>
 * <p>
 *     Note that all @TomlKey-annotated fields must be public,
 *     as well as the subclass itself.
 *     This is because the fields are accessed via reflection
 *     and this will otherwise result in IllegalAccessExceptions.
 * </p>
 * <p>
 *     Fields that are not @TomlKey-annotated may be present and
 *     will be ignored.
 * </p>
 */
public abstract class TomlFile {
    private static final LevenshteinDistance levenshtein = new LevenshteinDistance();

    public void load(String fileName) throws IOException {
        if (!ResourceAccess.fileExists(fileName)) {
            save(fileName);
        } else {
            try (FileInputStream inputStream = new FileInputStream(fileName)) {
                load(inputStream);
            }
        }
    }

    public void load(InputStream inputStream) throws IOException {

        // This line is a bug fix for a bug in the Toml library.
        // The Toml library closes the input stream after reading the file,
        // instead of leaving it open for the caller to close.
        inputStream = new UnclosableStream(inputStream);
        Toml toml = new Toml().read(inputStream);

        for (Field f : fields()) {
            String tomlKey = f.getAnnotation(TomlKey.class).value();
            try {
                if (f.getType() == boolean.class) {
                    f.setBoolean(this, toml.getBoolean(tomlKey, f.getBoolean(this)));
                } else if (f.getType() == double.class) {
                    f.setDouble(this, toml.getDouble(tomlKey, f.getDouble(this)));
                } else if (f.getType() == float.class) {
                    f.setFloat(this, toml.getDouble(tomlKey, (double) f.getFloat(this)).floatValue());
                } else if (f.getType() == int.class) {
                    f.setInt(this, toml.getLong(tomlKey, (long) f.getInt(this)).intValue());
                } else if (f.getType() == String.class) {
                    f.set(this, toml.getString(tomlKey, (String) f.get(this)));
                } else {
                    String message = "Unsupported field type: " + f.getType();
                    String candidate;
                    throw new IOException(message);
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
        Set<String> unknownKeys = toml.toMap().keySet();
        Set<String> allowedKeys = fields().stream()
                .map(f -> f.getAnnotation(TomlKey.class).value())
                .collect(Collectors.toSet());
        unknownKeys.removeAll(allowedKeys);
        if (!unknownKeys.isEmpty()) {
            String firstWrongKey = (String) unknownKeys.toArray()[0];
            String message = "Unknown key '" + firstWrongKey + "' in .toml file (" + getClass().getSimpleName() + ").";
            String candidate = guessCandidate(firstWrongKey, allowedKeys, 3);
            if (candidate != null) {
                message += " Did you mean '" + candidate + "'?";
            }
            throw new IOException(message);
        }
    }

    /**
     * Saves the current settings to the settings file.
     * If the file doesn't exist yet, it will be created.
     * If the file does exist, its content will be completely overwritten.
     *
     * @param fileName the name of the TOML file to save the settings to
     * @throws IOException if the file can't be written
     */
    public void save(String fileName) throws IOException {
        if (!ResourceAccess.fileExists(fileName)) {
            ResourceAccess.createFile(fileName);
        }
        save(new FileOutputStream(fileName));
    }

    public void save(OutputStream outputStream) throws IOException {
        HashMap<String, Object> map = new HashMap<>();
        for (Field f : fields()) {
            try {
                map.put(f.getAnnotation(TomlKey.class).value(), f.get(this));
            } catch (IllegalAccessException e) {
                // should never happen
                throw new RuntimeException(e);
            }
        }

        TomlWriter tomlWriter = new TomlWriter();
        tomlWriter.write(map, outputStream);
    }

    private Set<Field> fields() {
        Field[] fields = getClass().getDeclaredFields();
        return Arrays.stream(fields)
                // make sure field is not static
                .filter(f -> !java.lang.reflect.Modifier.isStatic(f.getModifiers()))
                // make sure field is annotated with @TomlKey
                .filter(f -> f.getAnnotation(TomlKey.class) != null)
                .collect(Collectors.toSet());
    }

    /**
     * Tries to guess the correct name of a field if the user made a typo.
     * This allows for a message like "Unknown key 'zoom_step_faktor'. Did you mean 'zoom_step_factor'?"
     *
     * @param wrongName the name that was not found
     * @return the correct field name or null if no similar field was found
     */
    private static String guessCandidate(String wrongName, Set<String> candidates, int maxLevenshteinDistance) {
        for (String candidate : candidates) {
            int distance = levenshtein.apply(wrongName, candidate);
            if (distance <= maxLevenshteinDistance) {
                return candidate;
            }
        }
        return null;
    }
}
