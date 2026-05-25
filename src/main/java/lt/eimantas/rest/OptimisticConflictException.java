package lt.eimantas.rest;

public class OptimisticConflictException extends RuntimeException {

    public OptimisticConflictException(String message) {
        super(message);
    }

    public OptimisticConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}

