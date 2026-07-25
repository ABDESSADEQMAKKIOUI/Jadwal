package ma.jadwal.common.exception;

/**
 * Levée quand l'opération est interdite dans l'état courant (HTTP 403).
 */
public class OperationNonAutoriseeException extends RuntimeException {

    public OperationNonAutoriseeException(String message) {
        super(message);
    }
}
