package com.particle_life.app.toml_util;

import java.io.IOException;
import java.io.InputStream;

/**
 * This is a simple wrapper around an InputStream that prevents the stream from being closed.
 * This is just needed for a bugfix in the TOML library,
 * see {@link TomlFile#load(InputStream)}.
 */
class UnclosableStream extends InputStream {

    private final InputStream in;

    public UnclosableStream(InputStream in) {
        this.in = in;
    }

    @Override
    public void close() {
        // do nothing
    }

    @Override
    public int read() throws IOException {
        return in.read();
    }
}
