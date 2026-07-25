package ma.jadwal.api.securite;

/**
 * Levée quand l'email ou le mot de passe est incorrect (HTTP 401).
 */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiants invalides");
    }
}
