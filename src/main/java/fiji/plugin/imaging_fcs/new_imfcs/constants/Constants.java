package fiji.plugin.imaging_fcs.new_imfcs.constants;

import java.awt.*;

public final class Constants {
    // define constants
    public static final String PANEL_FONT = "SansSerif";

    // models name
    public static final String DC_FCCS_2D = "DC-FCCS (2D)";
    public static final String ITIR_FCS_2D = "ITIR-FCS (2D)";

    // This model only exists for FCCS, it's not defined by the user but it is used to define that we need to use the
    // psfsize2 in FCSFit.java file
    public static final String ITIR_FCS_2D_2 = "ITIR-FCS (2D) 2";

    public static final String SPIM_FCS_3D = "SPIM-FCS (3D)";

    // bleach correction methods
    public static final String NO_BLEACH_CORRECTION = "none";
    public static final String BLEACH_CORRECTION_SLIDING_WINDOW = "Sliding Window";
    public static final String BLEACH_CORRECTION_SINGLE_EXP = "Single Exp";
    public static final String BLEACH_CORRECTION_DOUBLE_EXP = "Double Exp";
    public static final String BLEACH_CORRECTION_POLYNOMIAL = "Polynomial";
    public static final String BLEACH_CORRECTION_LINEAR_SEGMENT = "Lin Segment";

    // filtering method
    public static final String NO_FILTER = "none";
    public static final String FILTER_INTENSITY = "Intensity";
    public static final String FILTER_MEAN = "Mean";

    // background subtraction method
    public static final String CONSTANT_BACKGROUND = "Constant Background";
    public static final String MIN_FRAME_BY_FRAME = "Min frame by frame";
    public static final String MIN_PER_IMAGE_STACK = "Min per image stack";
    public static final String MIN_PIXEL_WISE_PER_IMAGE_STACK = "Min Pixel wise per image stack";
    public static final String LOAD_BGR_IMAGE = "Load BGR image";

    // DCCF DIRECTION
    public static final String X_DIRECTION = "x direction";
    public static final String Y_DIRECTION = "y direction";
    public static final String DIAGONAL_UP_DIRECTION = "diagonal /";
    public static final String DIAGONAL_DOWN_DIRECTION = "diagonal \\";

    public static final int PANEL_FONT_SIZE = 12;
    public static final Point MAIN_PANEL_POS = new Point(10, 125);
    public static final Dimension MAIN_PANEL_DIM = new Dimension(410, 370);
    public static final int TEXT_FIELD_COLUMNS = 8;

    // conversion
    public static final double PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR = Math.pow(10, 6);
    public static final double DIFFUSION_COEFFICIENT_BASE = Math.pow(10, 12);
    public static final double NANO_CONVERSION_FACTOR = Math.pow(10, 9);

    // constants
    public static final double REFRACTIVE_INDEX = 1.3333; // refractive index of water;
    public static final double PI = Math.PI;
    public static final double SQRT_PI = Math.sqrt(PI);

    // Private constructor to prevent instantiation
    private Constants() {
    }
}
