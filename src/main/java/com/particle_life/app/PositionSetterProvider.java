package com.particle_life.app;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;
import com.particle_life.DefaultPositionSetter;
import com.particle_life.PositionSetter;

import java.util.List;
import java.util.Random;

class PositionSetterProvider implements InfoWrapperProvider<PositionSetter> {

    private static final Random random = new Random();

    @Override
    public List<InfoWrapper<PositionSetter>> create() {
        return List.of(
                new InfoWrapper<>("uniform", new DefaultPositionSetter()),
                new InfoWrapper<>("uniform circle", (x, type, nTypes) -> {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.sqrt(Math.random());
                    x.x = Math.cos(angle) * radius;
                    x.y = Math.sin(angle) * radius;
                }),
                new InfoWrapper<>("centered", (x, type, nTypes) -> {
                    x.x = random.nextGaussian() * 0.3f;
                    x.y = random.nextGaussian() * 0.3f;
                }),
                new InfoWrapper<>("centered circle", (x, type, nTypes) -> {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random();
                    x.x = Math.cos(angle) * radius;
                    x.y = Math.sin(angle) * radius;
                }),
                new InfoWrapper<>("ring", (x, type, nTypes) -> {
                    double angle = Math.random() * 2 * Math.PI;
                    double radius = 0.9 + 0.02 * random.nextGaussian();
                    x.x = Math.cos(angle) * radius;
                    x.y = Math.sin(angle) * radius;
                }),
                new InfoWrapper<>("type battle", (x, type, nTypes) -> {

                    double centerAngle = type / (double) nTypes * 2 * Math.PI;
                    double centerRadius = 0.5f;

                    double angle = Math.random() * 2 * Math.PI;
                    double radius = Math.random() * 0.1f;
                    x.x = centerRadius * Math.cos(centerAngle) + Math.cos(angle) * radius;
                    x.y = centerRadius * Math.sin(centerAngle) + Math.sin(angle) * radius;
                }),
                new InfoWrapper<>("type wheel", (x, type, nTypes) -> {

                    double centerAngle = type / (double) nTypes * 2 * Math.PI;
                    double centerRadius = 0.3f;
                    double individualRadius = 0.2f;

                    x.x = centerRadius * Math.cos(centerAngle) + random.nextGaussian() * individualRadius;
                    x.y = centerRadius * Math.sin(centerAngle) + random.nextGaussian() * individualRadius;
                }),
                new InfoWrapper<>("line", (x, type, nTypes) -> {
                    x.x = (2 * random.nextDouble() - 1);

                    x.y = (2 * random.nextDouble() - 1) * 0.15f;
                }),
                new InfoWrapper<>("spiral", (x, type, nTypes) -> {
                    double maxRotations = 2;
                    double f = random.nextDouble();
                    double angle = maxRotations * 2 * Math.PI * f;

                    double spread = 0.5 * Math.min(f, 0.2);
                    double radius = 0.9 * f + spread * random.nextGaussian() * spread;
                    x.x = radius * Math.cos(angle);
                    x.y = radius * Math.sin(angle);
                }),
                new InfoWrapper<>("rainbow spiral", (x, type, nTypes) -> {
                    double maxRotations = 2;
                    double typeSpread = 0.3 / nTypes;
                    double f = (type + 1) / (double) (nTypes + 2) + typeSpread * random.nextGaussian();
                    if (f < 0) {
                        f = 0;
                    } else if (f > 1) {
                        f = 1;
                    }
                    double angle = maxRotations * 2 * Math.PI * f;

                    double spread = 0.5 * Math.min(f, 0.2);
                    double radius = 0.9 * f + spread * random.nextGaussian() * spread;
                    x.x = radius * Math.cos(angle);
                    x.y = radius * Math.sin(angle);
                })
        );
    }
}
