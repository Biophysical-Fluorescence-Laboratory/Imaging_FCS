package fiji.plugin.imaging_fcs.new_imfcs.utils;

import java.util.Arrays;

/**
 * Utility class for deep copying 2D arrays.
 * This class provides a method to create a deep copy of a given 2D array.
 * The class is designed to be non-instantiable by having a private constructor.
 */
public final class MatrixDeepCopy {
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private MatrixDeepCopy() {
    }

    /**
     * Creates a deep copy of a 2D array of any type.
     *
     * @param matrix the 2D array to be copied
     * @param <T>    the type of the array elements
     * @return a deep copy of the input array, or null if the input array is null
     */
    public static <T> T[][] deepCopy(T[][] matrix) {
        if (matrix == null) {
            return null;
        }

        return Arrays.stream(matrix)
                .map(row -> Arrays.copyOf(row, row.length))
                .toArray($ -> matrix.clone());
    }

    /**
     * Creates a deep copy of a 2D double array.
     *
     * @param matrix the 2D double array to be copied
     * @return a deep copy of the input array, or null if the input array is null
     */
    public static double[][] deepCopy(double[][] matrix) {
        if (matrix == null) {
            return null;
        }

        return Arrays.stream(matrix)
                .map(row -> Arrays.copyOf(row, row.length))
                .toArray(double[][]::new);
    }
}
