package fiji.plugin.imaging_fcs.new_imfcs.utils;

/**
 * A simple generic class to hold a pair of values.
 *
 * @param <L> the type of the left element
 * @param <R> the type of the right element
 */
public class Pair<L, R> {
    private final L left;
    private final R right;

    /**
     * Constructs a new pair.
     *
     * @param left  the left value
     * @param right the right value
     */
    public Pair(L left, R right) {
        this.left = left;
        this.right = right;
    }

    /**
     * Returns the left element of the pair.
     *
     * @return the left element
     */
    public L getLeft() {
        return left;
    }

    /**
     * Returns the right element of the pair.
     *
     * @return the right element
     */
    public R getRight() {
        return right;
    }
}
