package com.particle_life.app;

import com.particle_life.app.shaders.ParticleShader;
import com.particle_life.backend.LoadDistributor;
import com.particle_life.backend.Loop;
import com.particle_life.backend.MatrixGenerator;
import com.particle_life.backend.PhysicsSettings;
import com.particle_life.backend.PositionSetter;
import com.particle_life.backend.TypeSetter;

import java.util.concurrent.atomic.AtomicBoolean;

public class PhysicsSession {

    ExtendedPhysics physics;
    Loop loop;
    PhysicsSnapshot physicsSnapshot;
    LoadDistributor physicsSnapshotLoadDistributor;
    final AtomicBoolean newSnapshotAvailable = new AtomicBoolean(false);

    record SnapshotUpdate(PhysicsSettings settings, int particleCount, int preferredNumberOfThreads) {
    }

    void create(
            PositionSetter positionSetter,
            MatrixGenerator matrixGenerator,
            TypeSetter typeSetter
    ) {
        physics = new ExtendedPhysics(
                DefaultAccelerator.create(),
                positionSetter,
                matrixGenerator,
                typeSetter);
        physicsSnapshot = new PhysicsSnapshot();
        physicsSnapshotLoadDistributor = new LoadDistributor();
        physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
        newSnapshotAvailable.set(true);
    }

    void start(Loop.Callback updatePhysics) {
        loop = new Loop();
        loop.start(updatePhysics);
    }

    void restart(Loop.Callback updatePhysics) {
        stopAndKill();
        create(
                physics.positionSetter,
                physics.matrixGenerator,
                physics.typeSetter);
        start(updatePhysics);
    }

    void stopAndKill() {
        if (loop != null) {
            if (!loop.stop(1000)) {
                loop.kill();
            }
        }
        if (physics != null) {
            physics.kill();
        }
        if (physicsSnapshotLoadDistributor != null) {
            physicsSnapshotLoadDistributor.kill();
        }
    }

    public static void setTypesFromSelection(ExtendedPhysics physics, TypeSetter typeSetter) {
        TypeSetter previousTypeSetter = physics.typeSetter;
        physics.typeSetter = typeSetter;
        physics.setTypes();
        physics.typeSetter = previousTypeSetter;
    }

    SnapshotUpdate consumeSnapshotIfAvailable(ParticleRenderer renderer, ParticleShader shader) {
        if (!newSnapshotAvailable.get()) {
            return null;
        }
        renderer.bufferParticleData(shader,
                physicsSnapshot.positions,
                physicsSnapshot.velocities,
                physicsSnapshot.types);
        newSnapshotAvailable.set(false);
        return new SnapshotUpdate(
                physicsSnapshot.settings.deepCopy(),
                physicsSnapshot.particleCount,
                physics.preferredNumberOfThreads);
    }

    void scheduleSnapshotCapture() {
        loop.doOnce(() -> {
            physicsSnapshot.take(physics, physicsSnapshotLoadDistributor);
            newSnapshotAvailable.set(true);
        });
    }
}
