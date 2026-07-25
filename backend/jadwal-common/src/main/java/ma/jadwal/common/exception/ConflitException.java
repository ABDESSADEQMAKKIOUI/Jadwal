package ma.jadwal.common.exception;

/**
 * Levée quand une contrainte d'unicité métier est violée (HTTP 409).
 */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
