package com.particle_life.app.io;

import com.particle_life.backend.DefaultMatrix;
import com.particle_life.backend.Matrix;

import java.io.*;
import java.util.Arrays;

public class MatrixIO {
    public static Matrix loadMatrix(InputStream in) {
        double[][] rows = new BufferedReader(new InputStreamReader(in))
                .lines()
                .map(line -> Arrays.stream(line.split("\t"))
                        .mapToDouble(Double::parseDouble)
                        .toArray()
                )
                .toArray(double[][]::new);
        Matrix matrix = new DefaultMatrix(rows.length);
        for (int i = 0; i < rows.length; i++) {
            for (int j = 0; j < rows.length; j++) {
                matrix.set(i, j, rows[i][j]);
            }
        }
        return matrix;
    }

    public static void saveMatrix(Matrix matrix, OutputStream out) throws IOException {
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (PrintWriter writer = new PrintWriter(byteStream)) {
                int matrixSize = matrix.size();
                for (int i = 0; i < matrixSize; i++) {
                    for (int j = 0; j < matrixSize; j++) {
                        writer.print(matrix.get(i, j));
                        if (j < matrixSize - 1) writer.print("\t");
                    }
                    writer.println();
                }
                writer.flush();
            }
            out.write(byteStream.toByteArray());
        }
    }
}
