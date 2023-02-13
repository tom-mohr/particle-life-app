package com.particle_life.app;

import com.particle_life.Accelerator;
import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;
import org.joml.Vector3d;

import java.util.List;

class AcceleratorProvider implements InfoWrapperProvider<Accelerator> {

    @Override
    public List<InfoWrapper<Accelerator>> create() {
        return List.of(
                new InfoWrapper<Accelerator>("particle life", (a, pos) -> {
                    double rmin = 0.3;
                    double dist = pos.length();
                    double force = dist < rmin ? (dist / rmin - 1) : a * (1 - Math.abs(1 + rmin - 2 * dist) / (1 - rmin));
                    return pos.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("particle life / r", (a, pos) -> {
                    double rmin = 0.3;
                    double dist = pos.length();
                    double force = dist < rmin ? (dist / rmin - 1) : a * (1 - Math.abs(1 + rmin - 2 * dist) / (1 - rmin));
                    return pos.mul(force / (dist * dist));
                }),
                new InfoWrapper<Accelerator>("particle life / r^2", (a, pos) -> {
                    double rmin = 0.3;
                    double dist = pos.length();
                    double force = dist < rmin ? (dist / rmin - 1) : a * (1 - Math.abs(1 + rmin - 2 * dist) / (1 - rmin));
                    return pos.mul(force / (dist * dist * dist));
                }),
                new InfoWrapper<Accelerator>("rotator 90deg", (a, pos) -> {
                    double dist = pos.length();
                    double force = a * (1 - dist);
                    Vector3d delta = new Vector3d(-pos.y, pos.x, 0);
                    return delta.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("rotator attr", (a, pos) -> {
                    double dist = pos.length();
                    double force = 1 - dist;
                    double angle = -a * Math.PI;
                    Vector3d delta = new Vector3d(
                            Math.cos(angle) * pos.x + Math.sin(angle) * pos.y,
                            -Math.sin(angle) * pos.x + Math.cos(angle) * pos.y,
                            0
                    );
                    return delta.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("planets",
                        "works best without friction (value = 1.0)", (a, pos) -> {
                    Vector3d delta = new Vector3d(pos);
                    double r = delta.length();
                    r = Math.max(r, 0.01);
                    return delta.mul(0.01 / (r * r * r));
                })
        );
    }
}
