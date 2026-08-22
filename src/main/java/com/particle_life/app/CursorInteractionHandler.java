package com.particle_life.app;

import com.particle_life.app.cursors.Cursor;
import com.particle_life.app.cursors.CursorAction;
import com.particle_life.app.selection.SelectionManager;
import com.particle_life.app.utils.ScreenCoordinates;
import com.particle_life.backend.Loop;
import com.particle_life.backend.Particle;
import org.joml.Vector3d;

import java.io.IOException;
import java.util.Arrays;

class CursorInteractionHandler {

    static void handleDragging(
            InputState input,
            Cursor cursor,
            ScreenCoordinates screen,
            SelectionManager<CursorAction> cursorActions1,
            SelectionManager<CursorAction> cursorActions2,
            AppSettings appSettings,
            Loop loop,
            ExtendedPhysics physics,
            double pmouseX,
            double pmouseY,
            double mouseX,
            double mouseY
    ) {
        if (!input.leftDraggingParticles && !input.rightDraggingParticles) {
            return;
        }

        final Cursor cursorCopy;
        try {
            cursorCopy = cursor.copy();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        SelectionManager<CursorAction> cursorActions = input.leftDraggingParticles ? cursorActions1 : cursorActions2;
        switch (cursorActions.getActive()) {
            case MOVE -> {
                final Vector3d dragStartWorld = screen.screenToWorld(pmouseX, pmouseY);
                final Vector3d dragStopWorld = screen.screenToWorld(mouseX, mouseY);
                final Vector3d delta = dragStopWorld.sub(dragStartWorld);
                cursorCopy.position.set(dragStartWorld.x, dragStartWorld.y, 0.0);
                loop.enqueue(() -> {
                    for (Particle p : cursorCopy.getSelection(physics.particles, physics.settings.wrap)) {
                        p.position.add(delta.x, delta.y, 0);
                        physics.ensurePosition(p.position);
                    }
                });
            }
            case BRUSH -> {
                final int addCount = appSettings.brushPower;
                loop.enqueue(() -> {
                    int prevLength = physics.particles.length;
                    physics.particles = Arrays.copyOf(physics.particles, prevLength + addCount);
                    for (int i = 0; i < addCount; i++) {
                        Particle particle = new Particle();
                        particle.position.set(cursorCopy.sampleRandomPoint());
                        physics.ensurePosition(particle.position);
                        particle.type = physics.typeSetter.getType(
                                particle.position,
                                particle.velocity,
                                particle.type,
                                physics.settings.matrix.size()
                        );
                        physics.particles[prevLength + i] = particle;
                    }
                });
            }
            case DELETE -> loop.enqueue(() -> {
                Particle[] newParticles = new Particle[physics.particles.length];
                int j = 0;
                for (Particle particle : physics.particles) {
                    if (!cursorCopy.isInside(particle, physics.settings.wrap)) {
                        newParticles[j] = particle;
                        j++;
                    }
                }
                physics.particles = Arrays.copyOf(newParticles, j);
            });
        }
    }
}
