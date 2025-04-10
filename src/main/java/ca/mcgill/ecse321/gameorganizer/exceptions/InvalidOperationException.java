package ca.mcgill.ecse321.gameorganizer.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown when an operation is invalid in the current context.
 * 
 * @author @YoussGm3o8
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidOperationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new InvalidOperationException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidOperationException(String message) {
        super(message);
    }

    /**
     * Constructs a new InvalidOperationException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause
     */
    public InvalidOperationException(String message, Throwable cause) {
        super(message, cause);
    }
} 