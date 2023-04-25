package com.particle_life.app;

import com.particle_life.app.selection.InfoWrapper;
import com.particle_life.app.selection.InfoWrapperProvider;
import com.particle_life.DefaultMatrix;
import com.particle_life.DefaultMatrixGenerator;
import com.particle_life.MatrixGenerator;

import java.util.List;

class MatrixGeneratorProvider implements InfoWrapperProvider<MatrixGenerator> {

    @Override
    public List<InfoWrapper<MatrixGenerator>> create() {
        return List.of(
                new InfoWrapper<>("fully random", new DefaultMatrixGenerator()),
                new InfoWrapper<>("symmetry", size -> {
                    DefaultMatrix m = new DefaultMatrix(size);
                    m.randomize();
                    for (int i = 0; i < m.size(); i++) {
                        for (int j = i; j < m.size(); j++) {
                            m.set(i, j, m.get(j, i));
                        }
                    }
                    return m;
                }),
                new InfoWrapper<>("chains", size -> {
                    DefaultMatrix m = new DefaultMatrix(size);
                    for (int i = 0; i < size; i++) {
                        for (int j = 0; j < size; j++) {
                            if (j == i || j == (i + 1) % size || j == (i + size - 1) % size) {
                                m.set(i, j, 1);
                            } else {
                                m.set(i, j, -1);
                            }
                        }
                    }
                    return m;
                }),
                new InfoWrapper<>("chains 2", size -> {
                    DefaultMatrix m = new DefaultMatrix(size);
                    for (int i = 0; i < size; i++) {
                        for (int j = 0; j < size; j++) {
                            if (j == i) {
                                m.set(i, j, 1);
                            } else if (j == (i + 1) % size || j == (i + size - 1) % size) {
                                m.set(i, j, 0.2);
                            } else {
                                m.set(i, j, -1);
                            }
                        }
                    }
                    return m;
                }),
                new InfoWrapper<>("chains 3", size -> {
                    DefaultMatrix m = new DefaultMatrix(size);
                    for (int i = 0; i < size; i++) {
                        for (int j = 0; j < size; j++) {
                            if (j == i) {
                                m.set(i, j, 1);
                            } else if (j == (i + 1) % size || j == (i + size - 1) % size) {
                                m.set(i, j, 0.2);
                            } else {
                                m.set(i, j, 0);
                            }
                        }
                    }
                    return m;
                }),
                new InfoWrapper<>("snakes", size -> {
                    DefaultMatrix m = new DefaultMatrix(size);
                    for (int i = 0; i < size; i++) {
                        m.set(i, i, 1);
                        m.set(i, (i + 1) % m.size(), 0.2);
                    }
                    return m;
                }),
                new InfoWrapper<>("zero", DefaultMatrix::new));
    }
}
