package com.particle_life.app;

import com.particle_life.backend.DefaultMatrix;
import com.particle_life.backend.Matrix;

import java.util.ArrayList;
import java.util.Locale;

public final class MatrixParser {

    private interface DoubleEncoder {
        String encode(double f);
    }

    private static final DoubleEncoder doubleEncoderDefault = f -> String.format(Locale.US, "%f", f);
    private static final DoubleEncoder doubleEncoderRoundAndFormat = f -> String.format(Locale.US, "%4.1f", f);

    /**
     * Expects input to look like this:<br><br>
     * <code>
     * 0.1 0.2 -0.3<br>
     * -0.1 0.4 0.1<br>
     * 1.0 -1.0 0.0
     * </code>
     *
     * @return the parsed matrix, or null.
     */
    public static Matrix parseMatrix(String s) {
        String[] parts = s.split("\\s");
        ArrayList<Float> numbers = new ArrayList<>(parts.length);
        for (String p : parts) {
            float f;
            try {
                f = Float.parseFloat(p);
            } catch (NumberFormatException e) {
                continue;
            }
            numbers.add(f);
        }

        int matrixSize = (int) Math.sqrt(numbers.size());
        if (matrixSize < 1) {
            return null;
        }

        DefaultMatrix matrix = new DefaultMatrix(matrixSize);
        for (int i = 0; i < matrixSize; i++) {
            for (int j = 0; j < matrixSize; j++) {
                matrix.set(i, j, numbers.get(i * matrixSize + j));
            }
        }
        return matrix;
    }

    public static String matrixToString(Matrix matrix) {
        return matrixToString(matrix, doubleEncoderDefault);
    }

    public static String matrixToStringRoundAndFormat(Matrix matrix) {
        return matrixToString(matrix, doubleEncoderRoundAndFormat);
    }

    private static String matrixToString(Matrix matrix, DoubleEncoder doubleEncoder) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < matrix.size(); i++) {
            for (int j = 0; j < matrix.size() - 1; j++) {
                sb.append(doubleEncoder.encode(matrix.get(i, j)));
                sb.append("\t");
            }
            sb.append(doubleEncoder.encode(matrix.get(i, matrix.size() - 1)));
            sb.append("\n");
        }
        return sb.toString();
    }
}
