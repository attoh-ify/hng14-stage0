package hng14.stage0.nameclassifier.exception;

public class BadRequestException extends ApiException {
    public BadRequestException(String message) {
        super(message);
    }
}
