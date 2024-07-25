package com.particle_life.app;

import java.io.IOException;

/**
 * This exception exists in order to handle
 * errors thrown while loading the app settings.
 * They need to be treated differently than any
 * other exception, in order to prevent overriding
 * the user's settings file with a new default
 * settings file.
 */
class AppSettingsLoadException extends IOException {

    public AppSettingsLoadException(String message, Throwable cause) {
        super(message + ": " + cause.getMessage());
        this.setStackTrace(cause.getStackTrace());
    }
}
