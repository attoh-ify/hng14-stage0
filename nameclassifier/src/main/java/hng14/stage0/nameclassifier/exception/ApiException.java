package hng14.stage0.nameclassifier.exception;

public abstract class ApiException extends RuntimeException {
    protected ApiException(String message) {
        super(message);
    }
}
