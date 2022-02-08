package com.particle_life.app;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;
import com.particle_life.Accelerator;
import org.joml.Vector3d;

import java.util.List;

class AcceleratorProvider implements InfoWrapperProvider<AcceleratorCodeData> {

    @Override
    public List<InfoWrapper<AcceleratorCodeData>> create() {
        return createDefaultAccelerators().stream()
                .map(infoWrapper -> new InfoWrapper<>(
                        infoWrapper.name,
                        infoWrapper.description,
                        new AcceleratorCodeData(false, "", "", "", infoWrapper.object)
                ))
                .toList();

        //todo: also load and compile user code
    }

    private List<InfoWrapper<Accelerator>> createDefaultAccelerators() {
        return List.of(
                new InfoWrapper<Accelerator>("particle life", (a, x) -> {
                    double rmin = 0.3;
                    double dist = x.length();
                    double force = dist < rmin ? (dist / rmin - 1) : a * (1 - Math.abs(1 + rmin - 2 * dist) / (1 - rmin));
                    return x.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("rotator 90deg", (a, x) -> {
                    double dist = x.length();
                    double force = a * (1 - dist);
                    Vector3d delta = new Vector3d(-x.y, x.x, 0);
                    return delta.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("rotator attr", (a, x) -> {
                    double dist = x.length();
                    double force = 1 - dist;
                    double angle = -a * Math.PI;
                    Vector3d delta = new Vector3d(
                            Math.cos(angle) * x.x + Math.sin(angle) * x.y,
                            -Math.sin(angle) * x.x + Math.cos(angle) * x.y,
                            0
                    );
                    return delta.mul(force / dist);
                }),
                new InfoWrapper<Accelerator>("planets",
                        "works best without friction (value = 1.0)", (a, x) -> {
                    Vector3d delta = new Vector3d(x);
                    double r = delta.length();
                    r = Math.max(r, 0.01);
                    return delta.mul(0.01 / (r * r * r));
                })
        );
    }
}
