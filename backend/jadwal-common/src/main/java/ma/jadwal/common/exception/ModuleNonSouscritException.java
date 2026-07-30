package ma.jadwal.common.exception;

/**
 * Levée quand l'établissement tente d'utiliser un module que son abonnement
 * ne couvre pas (HTTP 403).
 */
public class ModuleNonSouscritException extends RuntimeException {

    private final String module;

    public ModuleNonSouscritException(String module, String message) {
        super(message);
        this.module = module;
    }

    /**
     * Code technique du module refusé (par exemple {@code VIE_SCOLAIRE}).
     */
    public String getModule() {
        return module;
    }
}
