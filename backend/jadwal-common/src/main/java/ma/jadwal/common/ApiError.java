package ma.jadwal.common;

/**
 * Corps de réponse standard pour toutes les erreurs de l'API.
 */
public record ApiError(int statut, String message, Object details) {

    public ApiError(int statut, String message) {
        this(statut, message, null);
    }
}
