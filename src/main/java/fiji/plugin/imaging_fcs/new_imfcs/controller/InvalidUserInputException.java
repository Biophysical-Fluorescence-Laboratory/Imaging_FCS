package fiji.plugin.imaging_fcs.new_imfcs.controller;

/**
 * This exception is thrown when invalid user input is detected.
 * It extends the {@link RuntimeException} and is used to signal
 * errors in user input.
 */
public class InvalidUserInputException extends RuntimeException {
    /**
     * Constructs a new InvalidUserInputException with the specified detail message.
     * The cause is not initialized, and may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()} method.
     */
    public InvalidUserInputException(String message) {
        super(message);
    }
}
