package com.particle_life.app;

import com.particle_life.backend.LoadDistributor;
import com.particle_life.backend.Loop;
import com.particle_life.backend.MatrixGenerator;
import com.particle_life.backend.PositionSetter;
import com.particle_life.backend.TypeSetter;

import java.util.concurrent.atomic.AtomicBoolean;

class PhysicsSession {

    ExtendedPhysics physics;
    Loop loop;
    PhysicsSnapshot physicsSnapshot;
    LoadDistributor physicsSnapshotLoadDistributor;
    final AtomicBoolean newSnapshotAvailable = new AtomicBoolean(false);

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

    static void setTypesFromSelection(ExtendedPhysics physics, TypeSetter typeSetter) {
        TypeSetter previousTypeSetter = physics.typeSetter;
        physics.typeSetter = typeSetter;
        physics.setTypes();
        physics.typeSetter = previousTypeSetter;
    }
}
