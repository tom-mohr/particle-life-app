package com.particle_life.app;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;
import com.particle_life.DefaultTypeSetter;
import com.particle_life.TypeSetter;

import java.util.List;

public class TypeSetterProvider implements InfoWrapperProvider<TypeSetter> {
        @Override
        public List<InfoWrapper<TypeSetter>> create() {
                return List.of(
                                new InfoWrapper<>("fully random", new DefaultTypeSetter()),
                                new InfoWrapper<>("randomize 10%",
                                                (position, velocity, type, nTypes) -> Math.random() < 0.1
                                                                ? mapType(Math.random(), nTypes)
                                                                : type),
                                new InfoWrapper<>("slices",
                                                (position, velocity, type, nTypes) -> mapType(0.5 * position.x + 0.5,
                                                                nTypes)),
                                new InfoWrapper<>("onion",
                                                (position, velocity, type, nTypes) -> mapType(position.length(),
                                                                nTypes)),
                                new InfoWrapper<>("rotate",
                                                (position, velocity, type, nTypes) -> (type + 1) % nTypes),
                                new InfoWrapper<>("flip",
                                                (position, velocity, type, nTypes) -> nTypes - 1 - type),
                                new InfoWrapper<>("more of first",
                                                (position, velocity, type, nTypes) -> mapType(
                                                                Math.random() * Math.random(), nTypes)),
                                new InfoWrapper<>("kill still",
                                                (position, velocity, type, nTypes) -> velocity.length() < 0.01
                                                                ? nTypes - 1
                                                                : type));
        }

        private static int constrain(int value, int nTypes) {
                return Math.max(0, Math.min(nTypes - 1, value));
        }

        private static int mapType(double value, int nTypes) {
                return constrain((int) Math.floor(value * nTypes), nTypes);
        }
}
